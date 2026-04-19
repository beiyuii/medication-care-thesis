import { describe, expect, it, vi } from 'vitest'
import { validateStoredSession } from './sessionValidation'

describe('validateStoredSession', () => {
  it('keeps session when profile loading fails with server error', async () => {
    const clearSession = vi.fn()
    const setSession = vi.fn()

    await validateStoredSession(
      {
        isAuthenticated: true,
        token: 'valid-token',
        clearSession,
        setSession,
      },
      async () => {
        throw {
          status: 500,
          message: 'Unknown column medicine_name',
        }
      },
    )

    expect(clearSession).not.toHaveBeenCalled()
    expect(setSession).not.toHaveBeenCalled()
  })

  it('clears session when profile loading fails with 401', async () => {
    const clearSession = vi.fn()

    await validateStoredSession(
      {
        isAuthenticated: true,
        token: 'expired-token',
        clearSession,
        setSession: vi.fn(),
      },
      async () => {
        throw {
          status: 401,
          message: '未认证或令牌无效',
        }
      },
    )

    expect(clearSession).toHaveBeenCalledTimes(1)
  })
})
