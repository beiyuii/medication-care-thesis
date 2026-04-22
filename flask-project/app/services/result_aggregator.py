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


@dataclass
class DetectionAssessment:
    """视频级检测评估结果。"""

    status: str
    target_confidence: float
    action_confidence: float
    final_confidence: float
    action_detected: bool
    reason_code: str
    reason_text: str
    risk_tag: str


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
        total_frames: int,
    ) -> DetectionAssessment:
        """
        计算整体检测状态与解释性证据分。

        Args:
            targets: 聚合后的检测目标列表
            action_timeline: 动作时间线列表
            total_frames: 已处理帧数

        Returns:
            DetectionAssessment: 聚合后的状态与解释性结果
        """
        action_detected = len(action_timeline) > 0
        safe_total_frames = max(total_frames, 1)

        if not targets and not action_detected:
            return DetectionAssessment(
                status="abnormal",
                target_confidence=0.0,
                action_confidence=0.0,
                final_confidence=0.0,
                action_detected=False,
                reason_code="no_medication_detected",
                reason_text="未检测到稳定药品目标，也未捕捉到有效服药动作。",
                risk_tag="no_target",
            )

        target_confidence = self._calculate_target_confidence(targets)
        action_confidence = self._calculate_action_confidence(action_timeline, safe_total_frames)
        final_confidence = round(
            min(0.99, target_confidence * 0.55 + action_confidence * 0.45),
            3,
        )

        if targets and action_detected and target_confidence >= 0.72 and action_confidence >= 0.68:
            return DetectionAssessment(
                status="confirmed",
                target_confidence=target_confidence,
                action_confidence=action_confidence,
                final_confidence=final_confidence,
                action_detected=True,
                reason_code="clear_intake",
                reason_text="药品目标和连续服药动作都较清晰，系统判定为已完成服药。",
                risk_tag="clear_intake",
            )

        if targets and not action_detected:
            reason_code = "possible_fake_intake" if target_confidence >= 0.65 else "target_only"
            reason_text = (
                "检测到药品目标，但缺少连续吞咽动作证据，存在装作服药的风险。"
                if reason_code == "possible_fake_intake"
                else "检测到药品目标，但动作证据不足，请由护工进一步确认。"
            )
            final_confidence = round(
                min(0.95, target_confidence * 0.7 + action_confidence * 0.3),
                3,
            )
            return DetectionAssessment(
                status="suspected",
                target_confidence=target_confidence,
                action_confidence=action_confidence,
                final_confidence=final_confidence,
                action_detected=False,
                reason_code=reason_code,
                reason_text=reason_text,
                risk_tag=(
                    "possible_fake_intake"
                    if reason_code == "possible_fake_intake"
                    else "insufficient_evidence"
                ),
            )

        if not targets and action_detected:
            return DetectionAssessment(
                status="suspected",
                target_confidence=target_confidence,
                action_confidence=action_confidence,
                final_confidence=round(min(0.9, action_confidence * 0.75), 3),
                action_detected=True,
                reason_code="action_only",
                reason_text="捕捉到手口接近动作，但缺少稳定药品目标，证据不足。",
                risk_tag="insufficient_evidence",
            )

        reason_code = (
            "possible_fake_intake"
            if target_confidence >= 0.68 and action_confidence < 0.45
            else "insufficient_evidence"
        )
        reason_text = (
            "检测到药品，但动作段短且反复中断，存在假吃或未真正服下的风险。"
            if reason_code == "possible_fake_intake"
            else "药品或动作证据部分成立，但不足以确认已完成服药。"
        )
        return DetectionAssessment(
            status="suspected",
            target_confidence=target_confidence,
            action_confidence=action_confidence,
            final_confidence=final_confidence,
            action_detected=action_detected,
            reason_code=reason_code,
            reason_text=reason_text,
            risk_tag=(
                "possible_fake_intake"
                if reason_code == "possible_fake_intake"
                else "insufficient_evidence"
            ),
        )

    def _calculate_target_confidence(self, targets: list[VideoDetectionTarget]) -> float:
        if not targets:
            return 0.0
        max_score = max(target.score for target in targets)
        persistence = max(
            min(1.0, target.detection_count / max(self.action_frames_threshold, 1))
            for target in targets
        )
        return round(min(0.99, max_score * 0.72 + persistence * 0.28), 3)

    def _calculate_action_confidence(
        self, action_timeline: list[ActionTimeline], total_frames: int
    ) -> float:
        if not action_timeline:
            return 0.0
        action_frames = sum(segment.end_frame - segment.start_frame + 1 for segment in action_timeline)
        longest_segment = max(segment.end_frame - segment.start_frame + 1 for segment in action_timeline)
        continuity = min(1.0, longest_segment / max(self.action_frames_threshold, 1))
        coverage = min(1.0, action_frames / total_frames)
        segment_confidence = sum(segment.confidence for segment in action_timeline) / len(action_timeline)
        return round(
            min(0.99, continuity * 0.45 + coverage * 0.35 + segment_confidence * 0.20),
            3,
        )
