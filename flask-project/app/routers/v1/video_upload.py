"""v1 视频上传路由：负责接收前端视频文件并保存到指定目录。"""

from __future__ import annotations

import uuid
from pathlib import Path

from flask import Blueprint, jsonify, request
from werkzeug.datastructures import FileStorage
from werkzeug.exceptions import BadRequest as WerkzeugBadRequest

from app.core.config import get_settings
from app.core.exceptions import BadRequestError
from app.core.logger import get_logger
from app.core.tracing import get_trace_id
from app.services.video_loader import VideoLoader

bp = Blueprint("video_upload_v1", __name__, url_prefix="/v1/videos")
logger = get_logger()
settings = get_settings()
video_loader = VideoLoader(settings)


@bp.post("/upload")
def upload():
    """
    视频上传接口：接收multipart/form-data格式的视频文件并保存到video目录。

    请求格式：
    - Content-Type: multipart/form-data
    - 字段名: videoFile (必需)
    - 可选字段: patientId, scheduleId, customFilename

    返回格式：
    {
        "success": true,
        "filename": "保存的文件名",
        "path": "相对路径",
        "fullPath": "完整路径",
        "size": 文件大小（字节）,
        "traceId": "追踪ID"
    }

    Returns:
        JSON响应：包含文件保存信息
    """
    trace_id = get_trace_id()

    try:
        # 检查Content-Type
        if not request.content_type or "multipart/form-data" not in request.content_type:
            raise BadRequestError("Content-Type must be multipart/form-data")

        # 获取视频文件
        if "videoFile" not in request.files:
            raise BadRequestError("videoFile is required in multipart/form-data")

        video_file: FileStorage = request.files["videoFile"]
        if not video_file or video_file.filename == "":
            raise BadRequestError("videoFile cannot be empty")

        # 验证视频文件格式
        video_loader.validate_video_file(video_file)

        # 获取可选参数
        patient_id = request.form.get("patientId")
        schedule_id = request.form.get("scheduleId")
        custom_filename = request.form.get("customFilename")

        # 确定保存的文件名
        original_filename = video_file.filename or "video.mp4"
        file_ext = Path(original_filename).suffix.lower()

        if custom_filename:
            # 使用自定义文件名（确保有扩展名）
            if not custom_filename.endswith(file_ext):
                save_filename = f"{custom_filename}{file_ext}"
            else:
                save_filename = custom_filename
        elif patient_id and schedule_id:
            # 使用患者ID和计划ID生成文件名
            save_filename = f"patient_{patient_id}_schedule_{schedule_id}_{uuid.uuid4().hex[:8]}{file_ext}"
        else:
            # 使用UUID生成唯一文件名
            save_filename = f"{uuid.uuid4().hex}{file_ext}"

        # 获取视频存储目录（相对于项目根目录）
        app_dir = Path(__file__).parent.parent.parent.parent
        video_dir = app_dir / settings.video_storage_path

        # 确保目录存在
        video_dir.mkdir(parents=True, exist_ok=True)

        # 构建完整保存路径
        save_path = video_dir / save_filename

        # 检查文件是否已存在（如果存在，添加序号）
        counter = 1
        base_name = save_path.stem
        while save_path.exists():
            save_filename = f"{base_name}_{counter}{file_ext}"
            save_path = video_dir / save_filename
            counter += 1

        # 保存文件
        video_file.seek(0)
        video_file.save(str(save_path))

        # 获取文件大小
        file_size = save_path.stat().st_size

        logger.info(
            "video_uploaded",
            original_filename=original_filename,
            save_filename=save_filename,
            file_size=file_size,
            patient_id=patient_id,
            schedule_id=schedule_id,
        )

        # 返回成功响应
        return jsonify(
            {
                "success": True,
                "filename": save_filename,
                "path": f"{settings.video_storage_path}/{save_filename}",
                "fullPath": str(save_path),
                "size": file_size,
                "traceId": trace_id,
            }
        )

    except WerkzeugBadRequest as exc:
        raise BadRequestError(f"Invalid request: {exc}") from exc
    except OSError as exc:
        logger.exception("video_upload_os_error", error=str(exc))
        raise BadRequestError(f"Failed to save video file: {exc}") from exc
    except Exception as exc:
        logger.exception("video_upload_error", error=str(exc))
        raise


@bp.get("/list")
def list_videos():
    """
    列出已上传的视频文件列表。

    查询参数：
    - patientId (可选): 过滤特定患者的视频
    - scheduleId (可选): 过滤特定计划的视频
    - limit (可选): 返回数量限制，默认100

    Returns:
        JSON响应：包含视频文件列表
    """
    trace_id = get_trace_id()

    try:
        # 获取查询参数
        patient_id = request.args.get("patientId")
        schedule_id = request.args.get("scheduleId")
        limit = request.args.get("limit", type=int, default=100)

        # 获取视频存储目录
        app_dir = Path(__file__).parent.parent.parent.parent
        video_dir = app_dir / settings.video_storage_path

        if not video_dir.exists():
            return jsonify(
                {
                    "success": True,
                    "videos": [],
                    "count": 0,
                    "traceId": trace_id,
                }
            )

        # 获取所有视频文件
        video_files = []
        for file_path in video_dir.iterdir():
            if file_path.is_file() and file_path.suffix.lower() in video_loader.SUPPORTED_FORMATS:
                # 如果指定了patientId或scheduleId，检查文件名
                if patient_id and f"patient_{patient_id}" not in file_path.name:
                    continue
                if schedule_id and f"schedule_{schedule_id}" not in file_path.name:
                    continue

                stat = file_path.stat()
                video_files.append(
                    {
                        "filename": file_path.name,
                        "path": f"{settings.video_storage_path}/{file_path.name}",
                        "fullPath": str(file_path),
                        "size": stat.st_size,
                        "createdAt": stat.st_ctime,
                        "modifiedAt": stat.st_mtime,
                    }
                )

        # 按创建时间倒序排序
        video_files.sort(key=lambda x: x["createdAt"], reverse=True)

        # 应用限制
        if limit > 0:
            video_files = video_files[:limit]

        logger.info(
            "video_list_requested",
            count=len(video_files),
            patient_id=patient_id,
            schedule_id=schedule_id,
        )

        return jsonify(
            {
                "success": True,
                "videos": video_files,
                "count": len(video_files),
                "traceId": trace_id,
            }
        )

    except Exception as exc:
        logger.exception("video_list_error", error=str(exc))
        raise

