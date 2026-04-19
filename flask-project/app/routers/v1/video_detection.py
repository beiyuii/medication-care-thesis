"""v1 视频检测路由：负责接收前端视频文件并调用检测服务。"""

from __future__ import annotations

import asyncio

from flask import Blueprint, jsonify, request
from pydantic import ValidationError
from werkzeug.datastructures import FileStorage
from werkzeug.exceptions import BadRequest as WerkzeugBadRequest

from app.core.config import get_settings
from app.core.exceptions import BadRequestError, UnprocessableEntityError
from app.core.logger import get_logger
from app.core.tracing import get_trace_id
from app.schemas import VideoDetectionRequest
from app.services.video_detector import video_detector_service
from app.services.video_loader import VideoLoader

bp = Blueprint("video_detections_v1", __name__, url_prefix="/v1/detections/video")
logger = get_logger()
settings = get_settings()
video_loader = VideoLoader(settings)


async def _process_video_detection(payload: VideoDetectionRequest, video_bytes: bytes):
    """
    处理视频检测：加载视频、提取帧、执行检测。

    Args:
        payload: 检测请求参数
        video_bytes: 视频文件的字节数据

    Returns:
        VideoDetectionResponse: 检测结果
    """
    # 提取视频元数据
    video_metadata = video_loader.load_video_metadata(video_bytes)

    # 提取帧
    frames = video_loader.extract_frames(
        video_bytes,
        sampling_rate=payload.sampling_rate or 30,
        max_frames=payload.max_frames or 300,
    )

    if not frames:
        raise BadRequestError("No frames extracted from video")

    # 执行检测
    return await video_detector_service.predict(payload, frames, video_metadata)


@bp.post("/predict")
def predict():
    """
    视频检测入口：接收multipart/form-data格式的视频文件。

    Returns:
        JSON响应：包含检测结果
    """
    trace_id = get_trace_id()

    try:
        # 检查Content-Type
        if not request.content_type or "multipart/form-data" not in request.content_type:
            raise BadRequestError("Content-Type must be multipart/form-data")

        # 获取表单数据
        patient_id = request.form.get("patientId")
        schedule_id = request.form.get("scheduleId")
        timestamp = request.form.get("timestamp")
        camera_id = request.form.get("cameraId")
        model_version = request.form.get("modelVersion")
        sampling_rate = request.form.get("samplingRate")
        max_frames = request.form.get("maxFrames")

        # 获取视频文件
        if "videoFile" not in request.files:
            raise BadRequestError("videoFile is required in multipart/form-data")

        video_file: FileStorage = request.files["videoFile"]
        if not video_file or video_file.filename == "":
            raise BadRequestError("videoFile cannot be empty")

        # 验证视频文件格式
        video_loader.validate_video_file(video_file)

        # 读取视频文件字节
        video_file.seek(0)
        video_bytes = video_file.read()

        # 构建请求对象
        request_data = {
            "patientId": patient_id,
            "scheduleId": schedule_id,
            "timestamp": timestamp,
        }

        if camera_id:
            request_data["cameraId"] = camera_id
        if model_version:
            request_data["modelVersion"] = model_version
        if sampling_rate:
            try:
                request_data["samplingRate"] = int(sampling_rate)
            except ValueError:
                raise BadRequestError("samplingRate must be an integer")
        if max_frames:
            try:
                request_data["maxFrames"] = int(max_frames)
            except ValueError:
                raise BadRequestError("maxFrames must be an integer")

        try:
            payload = VideoDetectionRequest.model_validate(request_data)
        except ValidationError as exc:
            raise UnprocessableEntityError(exc.errors()) from exc

        logger.info(
            "video_predict_in",
            patient_id=payload.patient_id,
            schedule_id=payload.schedule_id,
            video_filename=video_file.filename,
        )

        # 执行检测
        response_model = asyncio.run(_process_video_detection(payload, video_bytes))

        logger.info(
            "video_predict_out",
            patient_id=payload.patient_id,
            schedule_id=payload.schedule_id,
            status=response_model.status,
            action_detected=response_model.action_detected,
        )

        return jsonify(response_model.model_dump(by_alias=True))

    except WerkzeugBadRequest as exc:
        raise BadRequestError(f"Invalid request: {exc}") from exc
    except Exception as exc:
        logger.exception("video_predict_error", error=str(exc))
        raise

