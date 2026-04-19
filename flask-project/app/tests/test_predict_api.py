from __future__ import annotations

import base64
import io
from typing import Any

from PIL import Image

from app.services.detector import detector_service

ENDPOINT = "/v1/detections/predict"


def _valid_image_b64() -> str:
    image = Image.new("RGB", (8, 8), color=(255, 255, 255))
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return base64.b64encode(buffer.getvalue()).decode()


def _payload(**overrides: Any) -> dict[str, Any]:
    data: dict[str, Any] = {
        "patientId": "elder-001",
        "scheduleId": "schedule-abc",
        "timestamp": "2024-05-01T08:00:00Z",
        "frameB64": _valid_image_b64(),
    }
    data.update(overrides)
    return data


def test_predict_success(client):
    res = client.post(ENDPOINT, json=_payload())
    assert res.status_code == 200
    body = res.get_json()
    assert body["status"] in {"suspected", "confirmed", "abnormal"}
    assert "traceId" in body


def test_predict_empty_payload(client):
    res = client.post(ENDPOINT, json={})
    assert res.status_code == 422


def test_predict_max_length_ids(client):
    long_id = "x" * 64
    payload = _payload(patientId=long_id, scheduleId=long_id)
    res = client.post(ENDPOINT, json=payload)
    assert res.status_code == 200


def test_predict_special_characters(client):
    payload = _payload(patientId="elder_测试!?", scheduleId="plan-✓-123")
    res = client.post(ENDPOINT, json=payload)
    assert res.status_code == 200


def test_predict_invalid_base64(client):
    payload = _payload(frameB64="###invalid###")
    res = client.post(ENDPOINT, json=payload)
    assert res.status_code == 400


def test_predict_model_failure(monkeypatch, client):
    def _explode(*args, **kwargs):
        raise RuntimeError("boom")

    monkeypatch.setattr(detector_service, "_run_inference", _explode)
    res = client.post(ENDPOINT, json=_payload())
    assert res.status_code == 503
