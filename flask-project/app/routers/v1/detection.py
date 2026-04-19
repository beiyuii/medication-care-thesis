"""v1 检测路由：负责接收前端帧数据并调用推理服务。"""

from __future__ import annotations

import asyncio

from flask import Blueprint, jsonify, request
from pydantic import ValidationError
from werkzeug.exceptions import BadRequest as WerkzeugBadRequest

from app.core.config import get_settings
from app.core.exceptions import BadRequestError, UnprocessableEntityError
from app.core.logger import get_logger
from app.core.tracing import get_trace_id
from app.schemas import DetectionRequest
from app.services.detector import detector_service
from app.services.image_loader import load_frame_bytes

bp = Blueprint("detections_v1", __name__, url_prefix="/v1/detections")
logger = get_logger()
settings = get_settings()


async def _resolve_and_predict(payload: DetectionRequest):
    """根据 payload 解析 frame，并调用 detector 输出结果。"""

    frame_bytes = await load_frame_bytes(payload.frame_b64, payload.frame_url, settings)
    return await detector_service.predict(payload, frame_bytes)


@bp.post("/predict")
def predict():
    """主要入口：对请求体做参数校验，并输出统一 JSON。"""

    trace_id = get_trace_id()
    try:
        body = request.get_json(force=True, silent=False)
    except WerkzeugBadRequest as exc:
        raise BadRequestError("Request body must be valid JSON") from exc

    try:
        payload = DetectionRequest.model_validate(body or {})
    except ValidationError as exc:
        raise UnprocessableEntityError(exc.errors()) from exc

    logger.info("predict_in", patient_id=payload.patient_id, schedule_id=payload.schedule_id)
    response_model = asyncio.run(_resolve_and_predict(payload))
    logger.info(
        "predict_out",
        patient_id=payload.patient_id,
        schedule_id=payload.schedule_id,
        status=response_model.status,
    )
    return jsonify(response_model.model_dump(by_alias=True))
