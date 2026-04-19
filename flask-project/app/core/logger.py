"""结构化日志配置：统一注入 trace_id 并输出 JSON。"""

from __future__ import annotations

import logging

import structlog

from .config import Settings
from .tracing import get_trace_id


def configure_logging(settings: Settings) -> None:
    """初始化 structlog，设置日志级别和处理器链。"""

    logging.basicConfig(
        level=getattr(logging, settings.log_level),
        format="%(message)s",
    )

    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.StackInfoRenderer(),
            structlog.processors.TimeStamper(fmt="iso"),
            _inject_trace_id,  # 在 JSON 中注入 trace_id 字段
            structlog.processors.JSONRenderer(),
        ],
        wrapper_class=structlog.make_filtering_bound_logger(
            getattr(logging, settings.log_level)
        ),
        cache_logger_on_first_use=True,
    )


def _inject_trace_id(
    logger: structlog.types.BindableLogger,
    method_name: str,
    event_dict: dict,
) -> dict:
    """structlog processor：若上下文有 trace id 则写入 event。"""

    trace_id = get_trace_id()
    if trace_id:
        event_dict["trace_id"] = trace_id
    return event_dict


def get_logger() -> structlog.types.BindableLogger:
    """返回默认 logger，供各模块复用。"""

    return structlog.get_logger()
