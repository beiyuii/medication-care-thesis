from __future__ import annotations

import statistics
import time

from app.tests.test_predict_api import ENDPOINT, _payload


def test_perf_snippet(client, capsys):
    runs = 10
    latencies = []
    start = time.perf_counter()
    for _ in range(runs):
        lap = time.perf_counter()
        resp = client.post(ENDPOINT, json=_payload())
        assert resp.status_code == 200
        latencies.append((time.perf_counter() - lap) * 1000)
    elapsed = time.perf_counter() - start
    qps = runs / elapsed
    mean_latency = statistics.mean(latencies)
    capsys.readouterr()  # clear buffers
    print(f"perf_runs={runs} mean_ms={mean_latency:.2f} qps={qps:.2f}")
