import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchCaregiverDashboard } from './dashboardService'

const { getMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
}))

vi.mock('@/lib/http', () => ({
  default: {
    get: getMock,
  },
}))

describe('fetchCaregiverDashboard', () => {
  beforeEach(() => {
    getMock.mockReset()
  })

  it('preserves completionRate from dashboard payload', async () => {
    getMock.mockResolvedValue({
      patients: [],
      activePatient: null,
      recentEvents: [],
      activeAlerts: [],
      completionRate: 50,
    })

    const dashboard = await fetchCaregiverDashboard()

    expect(dashboard.completionRate).toBe(50)
  })
})
