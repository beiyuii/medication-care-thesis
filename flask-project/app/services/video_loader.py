"""视频加载器：负责视频文件读取、格式验证与帧提取。"""

from __future__ import annotations

import io
from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np
from werkzeug.datastructures import FileStorage

from app.core.config import Settings
from app.core.exceptions import BadRequestError
from app.core.logger import get_logger

logger = get_logger()


@dataclass
class VideoMetadata:
    """视频元数据信息。"""

    duration: float  # 视频时长（秒）
    fps: float  # 帧率
    total_frames: int  # 总帧数
    width: int  # 视频宽度
    height: int  # 视频高度


class VideoLoader:
    """视频加载器：支持多种视频格式的读取与帧提取。"""

    # 支持的视频格式（包括浏览器常用的 webm 格式）
    SUPPORTED_FORMATS = {".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".webm"}

    def __init__(self, settings: Settings) -> None:
        """
        初始化视频加载器。

        Args:
            settings: 应用配置对象
        """
        self.settings = settings

    def validate_video_file(self, file: FileStorage) -> None:
        """
        验证视频文件格式。

        Args:
            file: 上传的文件对象

        Raises:
            BadRequestError: 文件格式不支持或文件无效
        """
        if not file.filename:
            raise BadRequestError("Video file name is required")

        # 检查文件扩展名
        filename_lower = file.filename.lower()
        if not any(filename_lower.endswith(ext) for ext in self.SUPPORTED_FORMATS):
            supported = ", ".join(self.SUPPORTED_FORMATS)
            raise BadRequestError(f"Unsupported video format. Supported formats: {supported}")

        # 检查文件大小（可选，这里不限制，由业务层决定）
        file.seek(0, io.SEEK_END)
        file_size = file.tell()
        file.seek(0)

        if file_size == 0:
            raise BadRequestError("Video file is empty")

        logger.debug("video_file_validated", filename=file.filename, size_bytes=file_size)

    def load_video_metadata(self, video_bytes: bytes) -> VideoMetadata:
        """
        从视频字节数据中提取元数据。

        Args:
            video_bytes: 视频文件的字节数据

        Returns:
            VideoMetadata: 视频元数据对象

        Raises:
            BadRequestError: 无法读取视频文件或视频格式无效
        """
        # cv2.VideoCapture不支持直接从内存读取，需要使用临时文件
        import tempfile
        import os

        # 根据视频字节数据的前几个字节判断格式，或使用通用后缀
        # 为了兼容性，使用 .mp4 后缀（OpenCV 通常能处理多种格式）
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as tmp_file:
            tmp_file.write(video_bytes)
            tmp_path = tmp_file.name

        try:
            cap = cv2.VideoCapture(tmp_path)
            if not cap.isOpened():
                raise BadRequestError("Cannot open video file or invalid video format")

            fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
            frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
            width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
            height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
            duration = frame_count / fps if fps > 0 else 0.0

            cap.release()

            metadata = VideoMetadata(
                duration=duration,
                fps=fps,
                total_frames=frame_count,
                width=width,
                height=height,
            )

            logger.debug(
                "video_metadata_extracted",
                duration=duration,
                fps=fps,
                total_frames=frame_count,
                width=width,
                height=height,
            )

            return metadata
        finally:
            # 清理临时文件
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)

    def extract_frames(
        self,
        video_bytes: bytes,
        sampling_rate: int = 30,
        max_frames: Optional[int] = None,
    ) -> list[np.ndarray]:
        """
        从视频中提取帧。

        Args:
            video_bytes: 视频文件的字节数据
            sampling_rate: 每秒采样帧数（默认30帧/秒）
            max_frames: 最大提取帧数（None表示不限制）

        Returns:
            list[np.ndarray]: 提取的帧列表（BGR格式）

        Raises:
            BadRequestError: 无法读取视频文件
        """
        import tempfile
        import os

        # 创建临时文件（使用 .mp4 后缀，OpenCV 通常能处理多种格式）
        with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as tmp_file:
            tmp_file.write(video_bytes)
            tmp_path = tmp_file.name

        try:
            cap = cv2.VideoCapture(tmp_path)
            if not cap.isOpened():
                raise BadRequestError("Cannot open video file for frame extraction")

            fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

            # 计算采样间隔
            frame_interval = max(1, int(fps / sampling_rate))

            frames = []
            frame_index = 0
            extracted_count = 0

            while True:
                ret, frame = cap.read()
                if not ret:
                    break

                # 按采样率提取帧
                if frame_index % frame_interval == 0:
                    frames.append(frame.copy())
                    extracted_count += 1

                    # 检查是否达到最大帧数限制
                    if max_frames and extracted_count >= max_frames:
                        break

                frame_index += 1

            cap.release()

            logger.info(
                "frames_extracted",
                total_frames=total_frames,
                extracted_count=len(frames),
                sampling_rate=sampling_rate,
            )

            return frames
        finally:
            # 清理临时文件
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)

