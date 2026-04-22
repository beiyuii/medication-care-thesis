"""云端视觉复核客户端：负责关键帧复核与结构化 JSON 解析。"""

from __future__ import annotations

import base64
import json
from dataclasses import dataclass
from typing import Any

import cv2
import httpx
import numpy as np

from app.core.config import Settings, get_settings
from app.core.logger import get_logger

logger = get_logger()


@dataclass(slots=True)
class VisionReviewResult:
    status: str
    target_confidence: float
    action_confidence: float
    final_confidence: float
    reason_code: str
    reason_text: str
    risk_tag: str
    medication_visible: bool
    hand_to_mouth_action: bool
    swallowing_likely: bool
    frame_summary: str
    llm_provider: str
    llm_model: str
    llm_frame_count: int
    llm_decision_source: str


class VisionReviewClient:
    def __init__(self, settings: Settings | None = None) -> None:
        self.settings = settings or get_settings()

    def is_enabled(self) -> bool:
        return bool(
            self.settings.deepseek_enabled
            and self.settings.deepseek_base_url
            and self.settings.deepseek_api_key
            and self.settings.deepseek_model
        )

    def review_frames(
        self,
        frames: list[np.ndarray],
        *,
        context: dict[str, Any],
    ) -> VisionReviewResult:
        if not self.is_enabled():
            raise RuntimeError("Cloud VLM is not configured")

        encoded_frames = [self._encode_frame(frame) for frame in frames]
        payload = self._build_payload(encoded_frames, context)
        response = self._post(payload)
        parsed = self._parse_response(response)
        normalized_status = self._normalize_choice(
            parsed.get("status"),
            {"confirmed", "suspected", "abnormal"},
            fallback="suspected",
        )
        normalized_reason_code = self._normalize_choice(
            parsed.get("reasonCode"),
            {
                "clear_intake",
                "target_only",
                "action_only",
                "possible_fake_intake",
                "insufficient_evidence",
                "no_medication_detected",
            },
            fallback="insufficient_evidence",
        )
        normalized_risk_tag = self._normalize_choice(
            parsed.get("riskTag"),
            {
                "clear_intake",
                "possible_fake_intake",
                "insufficient_evidence",
                "no_target",
            },
            fallback=self._fallback_risk_tag(normalized_status, normalized_reason_code),
        )

        return VisionReviewResult(
            status=normalized_status,
            target_confidence=self._normalize_score(parsed.get("targetConfidence")),
            action_confidence=self._normalize_score(parsed.get("actionConfidence")),
            final_confidence=self._normalize_score(parsed.get("finalConfidence")),
            reason_code=normalized_reason_code,
            reason_text=str(parsed["reasonText"]),
            risk_tag=normalized_risk_tag,
            medication_visible=bool(parsed.get("medicationVisible")),
            hand_to_mouth_action=bool(parsed.get("handToMouthAction")),
            swallowing_likely=bool(parsed.get("swallowingLikely")),
            frame_summary=str(parsed.get("frameSummary") or parsed.get("reasonText") or ""),
            llm_provider="dashscope",
            llm_model=self.settings.deepseek_model or "unknown",
            llm_frame_count=len(frames),
            llm_decision_source="cloud_vlm",
        )

    def _encode_frame(self, frame: np.ndarray) -> str:
        height, width = frame.shape[:2]
        max_edge = max(height, width)
        if max_edge > 960:
            scale = 960 / max_edge
            frame = cv2.resize(frame, (int(width * scale), int(height * scale)))
        ok, buffer = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 82])
        if not ok:
            raise RuntimeError("Failed to encode frame for cloud VLM")
        return base64.b64encode(buffer.tobytes()).decode("utf-8")

    def _build_payload(self, encoded_frames: list[str], context: dict[str, Any]) -> dict[str, Any]:
        user_parts: list[dict[str, Any]] = [
            {
                "type": "text",
                "text": (
                    "请作为服药行为复核器，只根据关键帧和辅助信号判断是否真实完成服药。"
                    "禁止医学建议，禁止输出 Markdown，禁止补充解释，只输出 JSON。"
                ),
            },
            {
                "type": "text",
                "text": (
                    "输出字段必须为："
                    '{"status":"confirmed|suspected|abnormal","targetConfidence":0.0,'
                    '"actionConfidence":0.0,"finalConfidence":0.0,'
                    '"reasonCode":"clear_intake|target_only|action_only|possible_fake_intake|insufficient_evidence|no_medication_detected",'
                    '"reasonText":"string","riskTag":"clear_intake|possible_fake_intake|insufficient_evidence|no_target",'
                    '"medicationVisible":true,"handToMouthAction":true,"swallowingLikely":false,"frameSummary":"string"}'
                ),
            },
            {
                "type": "text",
                "text": json.dumps(context, ensure_ascii=False),
            },
        ]
        for index, frame_b64 in enumerate(encoded_frames, start=1):
            user_parts.append(
                {
                    "type": "text",
                    "text": f"关键帧 {index}",
                }
            )
            user_parts.append(
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{frame_b64}",
                    },
                }
            )
        return {
            "model": self.settings.deepseek_model,
            "temperature": 0,
            "response_format": {"type": "json_object"},
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是服药视频复核器。你只能输出严格 JSON。"
                        "如果证据不足，必须返回 suspected 或 abnormal，不能猜测。"
                    ),
                },
                {"role": "user", "content": user_parts},
            ],
        }

    def _post(self, payload: dict[str, Any]) -> dict[str, Any]:
        base_url = (self.settings.deepseek_base_url or "").rstrip("/")
        url = f"{base_url}/chat/completions"
        timeout = self.settings.deepseek_timeout_ms / 1000
        with httpx.Client(timeout=timeout) as client:
            response = client.post(
                url,
                headers={
                    "Authorization": f"Bearer {self.settings.deepseek_api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
            response.raise_for_status()
            return response.json()

    def _parse_response(self, response: dict[str, Any]) -> dict[str, Any]:
        try:
            content = response["choices"][0]["message"]["content"]
        except (KeyError, IndexError, TypeError) as exc:
            raise RuntimeError("Cloud VLM returned an unexpected payload") from exc
        if isinstance(content, list):
            content = "".join(str(item.get("text", "")) for item in content if isinstance(item, dict))
        if not isinstance(content, str):
            raise RuntimeError("Cloud VLM content is not a string")
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError as exc:
            logger.warning("cloud_vlm_invalid_json", content=content)
            raise RuntimeError("Cloud VLM did not return valid JSON") from exc
        return parsed

    def _normalize_choice(self, value: Any, allowed: set[str], fallback: str) -> str:
        if isinstance(value, str):
            candidate = value.strip()
            if candidate in allowed:
                return candidate
            for token in candidate.replace("/", "|").replace(",", "|").split("|"):
                normalized = token.strip()
                if normalized in allowed:
                    logger.warning(
                        "cloud_vlm_choice_normalized",
                        raw=value,
                        normalized=normalized,
                    )
                    return normalized
        return fallback

    def _normalize_score(self, value: Any) -> float:
        try:
            score = float(value)
        except (TypeError, ValueError):
            return 0.0
        return max(0.0, min(score, 1.0))

    def _fallback_risk_tag(self, status: str, reason_code: str) -> str:
        if status == "confirmed":
            return "clear_intake"
        if reason_code == "possible_fake_intake":
            return "possible_fake_intake"
        if reason_code == "no_medication_detected":
            return "no_target"
        return "insufficient_evidence"


vision_review_client = VisionReviewClient()
