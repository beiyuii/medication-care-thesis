# Example cURL

```bash
curl -X POST http://localhost:8000/v1/detections/predict \
  -H 'Content-Type: application/json' \
  -H 'X-Trace-Id: demo-trace' \
  -d '{
        "patientId": "elder-001",
        "scheduleId": "schedule-123",
        "timestamp": "2024-05-01T08:00:00Z",
        "frameB64": "ZmFrZS1pbWFnZS1kYXRh"
      }'
```
