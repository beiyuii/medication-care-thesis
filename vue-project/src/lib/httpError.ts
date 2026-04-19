export interface NormalizedHttpError {
  status: number
  code?: number
  message: string
  traceId?: string
  timestamp?: string
  data: Record<string, unknown>
}

export function isAuthFailureStatus(status?: number): boolean {
  return status === 401 || status === 403
}

export function normalizeHttpError(error: unknown): NormalizedHttpError {
  const axiosLike = error as {
    response?: {
      status?: number
      data?: Record<string, unknown>
    }
    message?: string
  }
  const data = axiosLike.response?.data ?? {}
  const message =
    asString(data.detail) ??
    asString(data.message) ??
    asString(data.msg) ??
    asString(data.error) ??
    axiosLike.message ??
    '网络异常，请稍后重试'

  return {
    status: axiosLike.response?.status ?? asNumber(data.code) ?? 0,
    code: asNumber(data.code),
    message,
    traceId: asString(data.traceId),
    timestamp: asString(data.timestamp),
    data,
  }
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() !== '' ? value : undefined
}

function asNumber(value: unknown): number | undefined {
  return typeof value === 'number' ? value : undefined
}
