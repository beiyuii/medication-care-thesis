import { describe, expect, it } from 'vitest'
import { resolveDashboardCompletionRate } from './dashboardCompletion'

describe('resolveDashboardCompletionRate', () => {
  it('prefers backend completionRate over local recent-event ratio', () => {
    const completionRate = resolveDashboardCompletionRate(50, [
      { status: 'confirmed' },
    ])

    expect(completionRate).toBe(50)
  })
})
