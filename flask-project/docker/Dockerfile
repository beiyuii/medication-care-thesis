FROM python:3.11-slim AS builder
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /opt/app
COPY pyproject.toml ./
RUN pip install --upgrade pip && \
    pip install --no-cache-dir build
COPY app ./app
RUN python -m build --wheel --outdir /tmp/wheels

FROM python:3.11-slim AS runtime
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /opt/app
COPY --from=builder /tmp/wheels /tmp/wheels
RUN pip install --no-cache-dir /tmp/wheels/*.whl && rm -rf /tmp/wheels
COPY app ./app
RUN addgroup --system app && adduser --system --ingroup app app
USER app
EXPOSE 8000
CMD ["gunicorn", "--workers", "4", "--bind", "0.0.0.0:8000", "app.main:app"]
