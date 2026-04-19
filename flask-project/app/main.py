"""应用入口：负责构建 Flask 实例、注册路由、中间件和统一错误处理。"""

from __future__ import annotations

import asyncio
from pathlib import Path

from flask import Flask, jsonify, render_template, request
from flask_cors import CORS
from werkzeug.exceptions import HTTPException, RequestEntityTooLarge

from app.core.config import Settings, get_settings
from app.core.exceptions import AlgoServiceError, ModelNotReadyError
from app.core.logger import configure_logging, get_logger
from app.core.tracing import generate_trace_id, get_trace_id, set_trace_id
from app.routers.v1.detection import bp as detection_router
from app.routers.v1.video_detection import bp as video_detection_router
from app.routers.v1.video_upload import bp as video_upload_router
from app.schemas import ErrorResponse
from app.services.detector import detector_service
from app.services.video_detector import video_detector_service

logger = get_logger()


def create_app() -> Flask:
    """创建并返回 Flask 应用对象，便于单元测试或 WSGI 服务器复用。"""

    settings = get_settings()
    configure_logging(settings)

    # 获取应用根目录和模板目录路径
    # __file__ 指向 app/main.py，所以 parent 是 app/ 目录
    app_dir = Path(__file__).parent
    template_dir = app_dir / "templates"
    
    # 确保模板目录存在
    if not template_dir.exists():
        logger.warning(f"Template directory not found: {template_dir}")
    
    app = Flask(__name__, template_folder=str(template_dir))
    logger.debug(f"Flask app initialized with template folder: {app.template_folder}")
    app.config["settings"] = settings
    
    # 设置最大文件上传大小为 100MB（视频文件可能较大）
    app.config["MAX_CONTENT_LENGTH"] = 100 * 1024 * 1024  # 100MB

    # 允许业务前端跨域访问，origin 列表可在 env 中配置
    CORS(app, resources={r"*": {"origins": settings.cors_origins}})

    _register_middlewares(app)
    _register_routes(app, settings)
    _register_error_handlers(app)

    return app


def _register_middlewares(app: Flask) -> None:
    """统一在请求生命周期中注入 trace_id，便于跨服务追踪。"""

    @app.before_request
    def inject_trace_id() -> None:
        # 如果前端已经传入 trace id 则沿用，否则自动生成
        incoming = request.headers.get("X-Trace-Id") or generate_trace_id()
        set_trace_id(incoming)

    @app.after_request
    def append_trace_id(response):
        # 将 trace id 写回响应头，供前端或链路追踪使用
        trace_id = get_trace_id()
        if trace_id:
            response.headers["X-Trace-Id"] = trace_id
        response.headers.setdefault("Cache-Control", "no-store")
        return response


def _register_routes(app: Flask, settings: Settings) -> None:
    """注册健康检查与业务蓝图。"""

    @app.get("/")
    def index():
        """测试页面入口。"""
        try:
            return render_template("test_video_detection.html")
        except Exception as exc:
            logger.exception("failed_to_render_template", error=str(exc), template_folder=app.template_folder)
            return jsonify({"error": "Template not found", "detail": str(exc), "template_folder": app.template_folder}), 500

    @app.get("/test")
    def test_page():
        """测试页面（别名）。"""
        try:
            return render_template("test_video_detection.html")
        except Exception as exc:
            logger.exception("failed_to_render_template", error=str(exc), template_folder=app.template_folder)
            return jsonify({"error": "Template not found", "detail": str(exc), "template_folder": app.template_folder}), 500

    @app.get("/health")
    def health():
        return jsonify({"status": "ok", "app": settings.app_name})

    @app.get("/ready")
    def ready():
        # 只有在模型加载完成后才对外宣称 ready，用于 K8s ReadinessProbe
        try:
            asyncio.run(asyncio.wait_for(detector_service.load(), timeout=settings.readiness_wait_secs))
            # 同时检查视频检测服务的模型是否就绪
            asyncio.run(asyncio.wait_for(video_detector_service.load_yolo_model(), timeout=settings.readiness_wait_secs))
        except Exception as exc:
            raise ModelNotReadyError("Detector still warming up") from exc
        return jsonify({"status": "ready"})

    # 注册路由：保留原有图片检测接口（向后兼容）
    app.register_blueprint(detection_router)
    # 注册新的视频检测接口
    app.register_blueprint(video_detection_router)
    # 注册视频上传接口
    app.register_blueprint(video_upload_router)


def _register_error_handlers(app: Flask) -> None:
    """统一包裹错误输出，确保前端始终拿到 {error, detail, trace_id} 格式。"""

    @app.errorhandler(AlgoServiceError)
    def handle_algo_error(exc: AlgoServiceError):
        return _error_response(exc.error, exc.detail, exc.status_code)

    @app.errorhandler(RequestEntityTooLarge)
    def handle_file_too_large(exc: RequestEntityTooLarge):
        """处理文件过大错误。"""
        max_size_mb = app.config.get("MAX_CONTENT_LENGTH", 16 * 1024 * 1024) / (1024 * 1024)
        return _error_response(
            "file_too_large",
            f"文件大小超过限制（最大 {max_size_mb:.0f}MB），请压缩后重试",
            413,
        )

    @app.errorhandler(HTTPException)
    def handle_http_exception(exc: HTTPException):
        # 对于404错误，如果是HTML请求，返回测试页面
        if exc.code == 404 and request.accept_mimetypes.accept_html:
            return render_template("test_video_detection.html"), 404
        return _error_response(exc.name, exc.description, exc.code or 500)

    @app.errorhandler(Exception)
    def handle_generic(exc: Exception):
        logger.exception("unhandled", error=str(exc))
        return _error_response("internal_error", "Unexpected server error", 500)


def _error_response(error: str, detail, status: int):
    """生成通用错误响应结构。"""

    payload = ErrorResponse(error=error, detail=detail, trace_id=get_trace_id())
    return jsonify(payload.model_dump(by_alias=True)), status


app = create_app()


if __name__ == "__main__":
    # 开发场景下可以直接运行该文件启动服务
    app.run(host="0.0.0.0", port=8000)
