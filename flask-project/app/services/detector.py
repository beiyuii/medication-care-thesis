"""单帧推理服务：复用视频检测模型，对图片帧输出真实推理结果。"""

from __future__ import annotations

import asyncio
import time
from dataclasses import dataclass
from typing import Any

import cv2
import numpy as np

from app.core.config import Settings, get_settings
from app.core.exceptions import ModelInferenceError, ModelNotReadyError
from app.core.logger import get_logger
from app.core.tracing import get_trace_id
from app.schemas import DetectionRequest, DetectionResponse, DetectionTarget
from app.services.video_detector import video_detector_service

logger = get_logger()


@dataclass(slots=True)
class LoadedModel:
    """包装已加载模型的关键信息（设备、版本等元数据）。"""

    device: str
    version: str


class DetectorService:
    """懒加载单例，封装模型资源管理与推理入口。"""

    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()
        self._model: LoadedModel | None = None
        self._model_lock = asyncio.Lock()
        self._semaphore = asyncio.Semaphore(self.settings.semaphore_slots)

    async def load(self) -> LoadedModel:
        """延迟加载：只在首次调用时真正加载模型，避免冷启动阻塞。"""

        if self._model:
            return self._model
        async with self._model_lock:
            if self._model:
                return self._model
            logger.info("loading_model", path=self.settings.model_path)
            self._model = await asyncio.to_thread(self._load_model)
            logger.info("model_loaded", device=self._model.device, version=self._model.version)
        return self._model

    def is_ready(self) -> bool:
        """Ready 探针调用：若模型已载入返回 True。"""

        return self._model is not None

    async def predict(self, payload: DetectionRequest, frame_bytes: bytes) -> DetectionResponse:
        """核心推理接口：确保模型已加载并控制并发。"""

        model = await self.load()
        start = time.perf_counter()
        async with self._semaphore:
            try:
                # 将真正的推理计算放到线程池，避免阻塞事件循环
                result = await asyncio.to_thread(self._run_inference, payload, frame_bytes, model)
            except Exception as exc:  # pragma: no cover - 兜底保护
                logger.exception("inference_failed", error=str(exc))
                raise ModelInferenceError("Detector failed to produce a prediction") from exc
        latency_ms = int((time.perf_counter() - start) * 1000)
        return DetectionResponse(
            status=result["status"],
            confidence=result["confidence"],
            action_detected=result["action_detected"],
            targets=result["targets"],
            latency_ms=latency_ms,
            trace_id=result["trace_id"],
        )

    def _load_model(self) -> LoadedModel:
        """复用视频检测服务的 YOLO 模型加载流程。"""

        try:
            yolo_model = asyncio.run(video_detector_service.load_yolo_model())
            return LoadedModel(device="cpu", version=yolo_model.version)
        except Exception as exc:
            raise ModelNotReadyError("Failed to load image detection model") from exc

    def _run_inference(
        self,
        payload: DetectionRequest,
        frame_bytes: bytes,
        model: LoadedModel,  # noqa: ARG002 - 留给未来真实模型使用
    ) -> dict[str, Any]:
        """执行真实单帧推理并统一输出语义。"""

        frame = self._decode_frame(frame_bytes)
        yolo_model = asyncio.run(video_detector_service.load_yolo_model())
        target_detections = video_detector_service._detect_medication_yolo(frame, yolo_model)
        _, _, hand_mouth_distance = video_detector_service._detect_hand_landmarks(frame)

        targets = [
            DetectionTarget(
                label=target["label"],
                score=target["score"],
                bbox=tuple(target["bbox"]),
            )
            for target in target_detections
        ]
        confidence = max((target.score for target in targets), default=0.0)
        action_detected = bool(
            hand_mouth_distance is not None
            and hand_mouth_distance <= self.settings.action_distance_threshold_cm
        )
        has_target = len(targets) > 0

        if has_target and action_detected:
            status = "confirmed"
        elif has_target:
            status = "suspected"
        else:
            status = "abnormal"

        return {
            "status": status,
            "confidence": confidence,
            "action_detected": action_detected,
            "targets": targets,
            "trace_id": get_trace_id(),
        }

    def _decode_frame(self, frame_bytes: bytes) -> np.ndarray:
        """将输入图片字节解码为 OpenCV BGR 图像。"""

        frame_array = np.frombuffer(frame_bytes, dtype=np.uint8)
        frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
        if frame is None:
            raise ModelInferenceError("Frame bytes are not a valid image")
        return frame


detector_service = DetectorService()
