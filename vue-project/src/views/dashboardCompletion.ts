export function resolveDashboardCompletionRate(
  completionRate: number | null | undefined,
  recentEvents: Array<{ status: string }>,
): number {
  if (typeof completionRate === 'number' && Number.isFinite(completionRate)) {
    return Math.max(0, Math.min(100, Math.round(completionRate)))
  }

  const confirmed = recentEvents.filter(event => event.status === 'confirmed').length
  return recentEvents.length === 0 ? 0 : Math.round((confirmed / recentEvents.length) * 100)
}
