"""统一异常类型，方便控制 HTTP 状态码与错误标识。"""

from __future__ import annotations

from typing import Any


class AlgoServiceError(Exception):
    """默认服务器异常，子类可覆盖 status_code 与 error。"""

    status_code = 500
    error = "internal_error"

    def __init__(self, detail: Any = None) -> None:
        super().__init__(detail)
        self.detail = detail


class BadRequestError(AlgoServiceError):
    status_code = 400
    error = "bad_request"


class UnauthorizedError(AlgoServiceError):
    status_code = 401
    error = "unauthorized"


class ForbiddenError(AlgoServiceError):
    status_code = 403
    error = "forbidden"


class NotFoundError(AlgoServiceError):
    status_code = 404
    error = "not_found"


class UnprocessableEntityError(AlgoServiceError):
    status_code = 422
    error = "validation_error"


class ModelNotReadyError(AlgoServiceError):
    status_code = 503
    error = "model_not_ready"


class ModelInferenceError(AlgoServiceError):
    status_code = 503
    error = "model_inference_error"


class ImageFetchError(AlgoServiceError):
    status_code = 400
    error = "image_fetch_error"
