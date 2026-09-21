import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { formatQuotaNumber, quotaUsagePercent, refreshTenantQuota } from './useTenantQuota'
import * as quotaApi from '@/api/quota'

describe('useTenantQuota', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('quotaUsagePercent caps at 100 and handles zero total', () => {
    expect(quotaUsagePercent(50, 100)).toBe(50)
    expect(quotaUsagePercent(200, 100)).toBe(100)
    expect(quotaUsagePercent(10, 0)).toBe(0)
  })

  it('formatQuotaNumber abbreviates large values', () => {
    expect(formatQuotaNumber(15_000)).toBe('15.0k')
    expect(formatQuotaNumber(2_500_000)).toMatch(/M/)
  })

  it('refreshTenantQuota skips API when not logged in', async () => {
    const fetchQuota = vi.spyOn(quotaApi, 'fetchQuota')
    await refreshTenantQuota()
    expect(fetchQuota).not.toHaveBeenCalled()
  })

  it('refreshTenantQuota loads snapshot when token exists', async () => {
    localStorage.setItem('box.token', 't')
    vi.spyOn(quotaApi, 'fetchQuota').mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: {
          quotaTokens: 100,
          usedTokens: 10,
          quotaAiCalls: 50,
          usedAiCalls: 5,
        },
      },
    } as Awaited<ReturnType<typeof quotaApi.fetchQuota>>)

    await refreshTenantQuota()
    expect(quotaApi.fetchQuota).toHaveBeenCalled()
  })
})
