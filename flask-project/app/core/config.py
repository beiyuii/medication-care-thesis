"""配置模块：基于 Pydantic Settings 统一读取环境变量。"""

from __future__ import annotations

from functools import cache
from typing import Literal

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """所有运行参数集中于此，方便注入和测试替换。"""

    model_config = SettingsConfigDict(
        env_prefix="ALGO_",
        env_file=".env",
        env_file_encoding="utf-8",
        frozen=True,
        protected_namespaces=("settings_",),
    )

    app_name: str = "algo-service"
    env: Literal["local", "staging", "prod"] = "local"
    log_level: Literal["DEBUG", "INFO", "WARNING", "ERROR"] = "INFO"
    cors_origins: list[str] = Field(default_factory=lambda: ["*"])

    # 模型运行相关配置
    model_path: str = "models/medication-intake.onnx"
    device_preference: Literal["auto", "gpu", "cpu"] = "auto"
    fallback_to_cpu: bool = True
    confidence_threshold: float = 0.35
    action_distance_threshold_cm: float = 8.0
    action_frames_threshold: int = 45

    # 并发与资源控制
    max_workers: int = 4
    semaphore_slots: int = 2

    # 外部 IO 相关
    image_fetch_timeout: float = 30.0
    image_fetch_retries: int = 3

    # 视频存储相关配置
    video_storage_path: str = "video"

    readiness_wait_secs: float = 5.0


@cache
def get_settings() -> Settings:
    """通过 functools.cache 保证 Settings 单例。"""

    return Settings()
