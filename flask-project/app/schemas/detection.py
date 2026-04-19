"""Pydantic 模型：统一校验请求、响应与错误结构。"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Literal, Optional

from pydantic import Field, HttpUrl, field_validator, model_validator

from .base import StrictModel


class DetectionRequest(StrictModel):
    """前端提交的检测请求。"""

    patient_id: str = Field(min_length=1, max_length=64, alias="patientId")
    schedule_id: str = Field(min_length=1, max_length=64, alias="scheduleId")
    timestamp: datetime
    frame_b64: Optional[str] = Field(default=None, alias="frameB64")
    frame_url: Optional[HttpUrl] = Field(default=None, alias="frameUrl")
    camera_id: Optional[str] = Field(default=None, alias="cameraId", max_length=32)
    model_version: Optional[str] = Field(default=None, alias="modelVersion", max_length=32)

    @model_validator(mode="after")
    def validate_payload(self) -> "DetectionRequest":
        """至少需要 Base64 或 URL 其中之一，否则 raise。"""

        if not (self.frame_b64 or self.frame_url):
            msg = "Provide either frameB64 or frameUrl"
            raise ValueError(msg)
        return self

    @field_validator("timestamp", mode="before")
    @classmethod
    def parse_timestamp(cls, value: Any) -> datetime:
        """兼容 Web 端常见的 `2024-01-01T10:00:00Z` 格式。"""

        if isinstance(value, datetime):
            return value
        if isinstance(value, str):
            normalized = value.replace("Z", "+00:00")
            try:
                return datetime.fromisoformat(normalized)
            except ValueError as exc:  # pragma: no cover - invalid format
                raise ValueError("timestamp must be ISO-8601") from exc
        raise ValueError("timestamp must be ISO-8601 string or datetime")


class DetectionTarget(StrictModel):
    """单个检测框信息。"""

    label: Literal["PILL", "BLISTER", "BOTTLE", "BOX"]
    score: float = Field(ge=0.0, le=1.0)
    bbox: tuple[float, float, float, float]


class DetectionResponse(StrictModel):
    """推理结果响应。"""

    status: Literal["suspected", "confirmed", "abnormal"]
    confidence: float = Field(ge=0.0, le=1.0)
    action_detected: bool = Field(alias="actionDetected")
    targets: list[DetectionTarget] = Field(default_factory=list)
    latency_ms: int = Field(alias="latencyMs", ge=0)
    trace_id: str = Field(alias="traceId")


class VideoDetectionRequest(StrictModel):
    """前端提交的视频检测请求（通过multipart/form-data上传）。"""

    patient_id: str = Field(min_length=1, max_length=64, alias="patientId")
    schedule_id: str = Field(min_length=1, max_length=64, alias="scheduleId")
    timestamp: datetime
    camera_id: Optional[str] = Field(default=None, alias="cameraId", max_length=32)
    model_version: Optional[str] = Field(default=None, alias="modelVersion", max_length=32)
    sampling_rate: Optional[int] = Field(default=30, alias="samplingRate", ge=1, le=60)
    max_frames: Optional[int] = Field(default=300, alias="maxFrames", ge=1, le=1000)

    @field_validator("timestamp", mode="before")
    @classmethod
    def parse_timestamp(cls, value: Any) -> datetime:
        """兼容 Web 端常见的 `2024-01-01T10:00:00Z` 格式。"""

        if isinstance(value, datetime):
            return value
        if isinstance(value, str):
            normalized = value.replace("Z", "+00:00")
            try:
                return datetime.fromisoformat(normalized)
            except ValueError as exc:  # pragma: no cover - invalid format
                raise ValueError("timestamp must be ISO-8601") from exc
        raise ValueError("timestamp must be ISO-8601 string or datetime")


class ActionTimeline(StrictModel):
    """动作时间线信息。"""

    start_frame: int = Field(alias="startFrame", ge=0)
    end_frame: int = Field(alias="endFrame", ge=0)
    confidence: float = Field(ge=0.0, le=1.0)


class VideoDetectionTarget(StrictModel):
    """视频检测中的单个检测目标（包含时序信息）。"""

    label: Literal["PILL", "BLISTER", "BOTTLE", "BOX"]
    score: float = Field(ge=0.0, le=1.0)
    bbox: tuple[float, float, float, float]
    first_detected_frame: int = Field(alias="firstDetectedFrame", ge=0)
    last_detected_frame: int = Field(alias="lastDetectedFrame", ge=0)
    detection_count: int = Field(alias="detectionCount", ge=0)


class VideoMetadata(StrictModel):
    """视频元数据信息。"""

    duration: float = Field(ge=0.0)
    fps: float = Field(ge=0.0)
    total_frames: int = Field(alias="totalFrames", ge=0)
    processed_frames: int = Field(alias="processedFrames", ge=0)


class VideoDetectionResponse(StrictModel):
    """视频检测结果响应。"""

    status: Literal["suspected", "confirmed", "abnormal"]
    confidence: float = Field(ge=0.0, le=1.0)
    action_detected: bool = Field(alias="actionDetected")
    targets: list[VideoDetectionTarget] = Field(default_factory=list)
    action_timeline: list[ActionTimeline] = Field(default_factory=list, alias="actionTimeline")
    video_metadata: VideoMetadata = Field(alias="videoMetadata")
    latency_ms: int = Field(alias="latencyMs", ge=0)
    trace_id: str = Field(alias="traceId")


class ErrorResponse(StrictModel):
    """统一错误返回结构。"""

    error: str
    detail: Any = None
    trace_id: str = Field(alias="traceId")
