# Error Cases

1. `ValidationError` (400/422)
   - Missing frame payloads, malformed Base64, timestamp drift > configured tolerance.
2. `UnauthorizedAccessError` (401)
   - Reserved for future token validation hook; responds with error envelope.
3. `ForbiddenError` (403)
   - Role mismatch when non-elder tries to submit writable event.
4. `ImageFetchError` (400)
   - Remote frame URL returns non-2xx/invalid content-type.
5. `ModelNotReadyError` (503)
   - Called before lazy model load finishes.
6. `ModelInferenceError` (503)
   - Underlying runtime raised (ONNX, GPU, etc.) and service falls back to CPU.
7. `UpstreamTimeoutError` (504)
   - httpx timed out while fetching dependent resources.
8. `InternalServerError` (500)
   - Any uncaught exception; returns traceable envelope for observability.
