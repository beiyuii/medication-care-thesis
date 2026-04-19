"""结果聚合器：负责将帧级检测结果聚合为视频级结果。"""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from typing import Any

from app.core.config import Settings
from app.core.logger import get_logger
from app.schemas import ActionTimeline, VideoDetectionTarget

logger = get_logger()


@dataclass
class FrameDetectionResult:
    """单帧检测结果。"""

    frame_index: int
    targets: list[dict[str, Any]]  # 检测到的目标列表
    hand_landmarks: list[tuple[float, float]] | None  # 手部关键点（21个点）
    mouth_position: tuple[float, float] | None  # 口部位置
    hand_mouth_distance: float | None  # 手-口距离（像素）


class ResultAggregator:
    """结果聚合器：将帧级检测结果聚合为视频级结果。"""

    def __init__(self, settings: Settings) -> None:
        """
        初始化结果聚合器。

        Args:
            settings: 应用配置对象
        """
        self.settings = settings
        self.confidence_threshold = settings.confidence_threshold
        self.action_distance_threshold = settings.action_distance_threshold_cm
        self.action_frames_threshold = settings.action_frames_threshold

    def aggregate_targets(
        self, frame_results: list[FrameDetectionResult]
    ) -> list[VideoDetectionTarget]:
        """
        聚合检测目标，合并同一目标在不同帧中的检测结果。

        Args:
            frame_results: 所有帧的检测结果列表

        Returns:
            list[VideoDetectionTarget]: 聚合后的检测目标列表
        """
        # 使用字典存储每个目标的检测信息
        # key: (label, bbox_hash) -> 目标信息
        target_map: dict[str, dict[str, Any]] = {}

        for frame_result in frame_results:
            for target in frame_result.targets:
                label = target["label"]
                bbox = target["bbox"]
                score = target["score"]

                if score < self.confidence_threshold:
                    continue

                # 使用bbox的中心点作为key的一部分（简化处理，实际可以使用IoU匹配）
                bbox_center_x = (bbox[0] + bbox[2]) / 2
                bbox_center_y = (bbox[1] + bbox[3]) / 2
                # 使用标签和中心点区域作为key（简化版，实际应该用IoU跟踪）
                key = f"{label}_{int(bbox_center_x // 50)}_{int(bbox_center_y // 50)}"

                if key not in target_map:
                    target_map[key] = {
                        "label": label,
                        "scores": [],
                        "bboxes": [],
                        "first_frame": frame_result.frame_index,
                        "last_frame": frame_result.frame_index,
                        "detection_count": 0,
                    }

                target_info = target_map[key]
                target_info["scores"].append(score)
                target_info["bboxes"].append(bbox)
                target_info["last_frame"] = max(
                    target_info["last_frame"], frame_result.frame_index
                )
                target_info["first_frame"] = min(
                    target_info["first_frame"], frame_result.frame_index
                )
                target_info["detection_count"] += 1

        # 转换为VideoDetectionTarget列表
        aggregated_targets = []
        for target_info in target_map.values():
            # 计算平均置信度和平均bbox
            avg_score = sum(target_info["scores"]) / len(target_info["scores"])
            # 使用第一个检测框作为代表（实际可以使用平均或最大框）
            representative_bbox = target_info["bboxes"][0]

            aggregated_targets.append(
                VideoDetectionTarget(
                    label=target_info["label"],
                    score=round(avg_score, 3),
                    bbox=representative_bbox,
                    first_detected_frame=target_info["first_frame"],
                    last_detected_frame=target_info["last_frame"],
                    detection_count=target_info["detection_count"],
                )
            )

        logger.debug("targets_aggregated", count=len(aggregated_targets))
        return aggregated_targets

    def detect_action_timeline(
        self, frame_results: list[FrameDetectionResult]
    ) -> list[ActionTimeline]:
        """
        检测动作时间线，基于手-口距离和药品检测。

        Args:
            frame_results: 所有帧的检测结果列表

        Returns:
            list[ActionTimeline]: 动作时间线列表
        """
        action_segments = []
        current_segment_start: int | None = None
        consecutive_action_frames = 0

        for frame_result in frame_results:
            # 检查是否满足动作条件
            has_medication = len(frame_result.targets) > 0
            hand_mouth_close = (
                frame_result.hand_mouth_distance is not None
                and frame_result.hand_mouth_distance <= self.action_distance_threshold
            )

            is_action_frame = has_medication and hand_mouth_close

            if is_action_frame:
                if current_segment_start is None:
                    current_segment_start = frame_result.frame_index
                consecutive_action_frames += 1
            else:
                # 如果之前有连续的动作帧，检查是否达到阈值
                if current_segment_start is not None:
                    if consecutive_action_frames >= self.action_frames_threshold:
                        # 计算置信度（基于连续帧数和检测到的药品数量）
                        confidence = min(
                            0.99,
                            0.5
                            + (consecutive_action_frames / self.action_frames_threshold) * 0.4,
                        )
                        action_segments.append(
                            ActionTimeline(
                                start_frame=current_segment_start,
                                end_frame=frame_result.frame_index - 1,
                                confidence=round(confidence, 3),
                            )
                        )
                    current_segment_start = None
                    consecutive_action_frames = 0

        # 处理视频末尾的动作
        if current_segment_start is not None and consecutive_action_frames >= self.action_frames_threshold:
            confidence = min(
                0.99,
                0.5 + (consecutive_action_frames / self.action_frames_threshold) * 0.4,
            )
            action_segments.append(
                ActionTimeline(
                    start_frame=current_segment_start,
                    end_frame=frame_results[-1].frame_index,
                    confidence=round(confidence, 3),
                )
            )

        logger.debug("action_timeline_detected", segment_count=len(action_segments))
        return action_segments

    def calculate_overall_status(
        self,
        targets: list[VideoDetectionTarget],
        action_timeline: list[ActionTimeline],
    ) -> tuple[str, float, bool]:
        """
        计算整体检测状态、置信度和是否检测到动作。

        Args:
            targets: 聚合后的检测目标列表
            action_timeline: 动作时间线列表

        Returns:
            tuple[str, float, bool]: (status, confidence, action_detected)
        """
        action_detected = len(action_timeline) > 0

        if not targets:
            return ("abnormal", 0.0, action_detected)

        # 计算平均置信度
        avg_confidence = sum(t.score for t in targets) / len(targets)

        # 根据置信度和动作检测确定状态
        if action_detected and avg_confidence >= 0.85:
            status = "confirmed"
        elif action_detected or avg_confidence >= 0.5:
            status = "suspected"
        else:
            status = "abnormal"

        return (status, round(avg_confidence, 3), action_detected)

