import { describe, expect, it } from 'vitest'
import { normalizeHttpError } from './httpError'

describe('normalizeHttpError', () => {
  it('prefers backend detail and preserves traceId', () => {
    const error = normalizeHttpError({
      response: {
        status: 500,
        data: {
          error: 'BadSqlGrammarException',
          detail: "Unknown column 'medicine_name'",
          traceId: 'trace-123',
          timestamp: '2026-03-07T10:00:00Z',
        },
      },
      message: 'Request failed',
    })

    expect(error.status).toBe(500)
    expect(error.message).toBe("Unknown column 'medicine_name'")
    expect(error.traceId).toBe('trace-123')
  })
})
