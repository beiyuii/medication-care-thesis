"""简单的 trace_id ContextVar 工具，贯穿请求链路。"""

from __future__ import annotations

import contextvars
import uuid
from typing import Iterator

trace_id_var: contextvars.ContextVar[str] = contextvars.ContextVar("trace_id", default="")


def generate_trace_id() -> str:
    """生成全局唯一的 32 位 trace id。"""

    return uuid.uuid4().hex


def set_trace_id(trace_id: str) -> None:
    """写入当前上下文的 trace id。"""

    trace_id_var.set(trace_id)


def get_trace_id() -> str:
    """读取当前上下文中的 trace id，若不存在返回空串。"""

    return trace_id_var.get()


def trace_context(trace_id: str) -> Iterator[None]:
    """上下文管理器：在 with 块内使用指定 trace id。"""

    token = trace_id_var.set(trace_id)
    try:
        yield
    finally:
        trace_id_var.reset(token)
