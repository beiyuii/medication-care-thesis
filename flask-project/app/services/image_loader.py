"""帧加载工具：支持 Base64 字段或远程 URL。"""

from __future__ import annotations

import base64
from typing import Optional

import binascii
import httpx
from tenacity import RetryError, AsyncRetrying, stop_after_attempt, wait_exponential

from app.core.config import Settings
from app.core.exceptions import BadRequestError, ImageFetchError
from app.core.logger import get_logger

logger = get_logger()


async def load_frame_bytes(
    frame_b64: Optional[str],
    frame_url: Optional[str],
    settings: Settings,
) -> bytes:
    """根据前端传参选择本地解码或远程拉取。"""

    if frame_b64:
        return _decode_base64(frame_b64)
    if frame_url:
        return await _fetch_remote_frame(frame_url, settings)
    raise BadRequestError("Payload must include frameB64 or frameUrl")


def _decode_base64(data: str) -> bytes:
    """校验 Base64 输入格式，防止脏数据污染模型。"""

    try:
        return base64.b64decode(data, validate=True)
    except (ValueError, binascii.Error) as exc:
        raise BadRequestError("frameB64 is not valid Base64 data") from exc


async def _fetch_remote_frame(url: str, settings: Settings) -> bytes:
    """异步下载远程帧，带超时与重试机制。"""

    async def _request() -> bytes:
        async with httpx.AsyncClient(timeout=settings.image_fetch_timeout) as client:
            resp = await client.get(url)
            if resp.status_code >= 400:
                raise ImageFetchError(f"Image fetch failed: HTTP {resp.status_code}")
            ctype = resp.headers.get("content-type", "")
            if "image" not in ctype:
                raise ImageFetchError(f"Remote file is not an image: {ctype}")
            return resp.content

    retrying = AsyncRetrying(
        stop=stop_after_attempt(settings.image_fetch_retries),
        wait=wait_exponential(multiplier=0.5, max=5),
        reraise=True,
    )
    try:
        async for attempt in retrying:
            with attempt:
                return await _request()
    except RetryError as exc:
        logger.error("image_fetch_failed", url=url, error=str(exc))
        raise ImageFetchError("Image fetch exhausted retries") from exc
