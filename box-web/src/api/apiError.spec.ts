import { describe, expect, it } from 'vitest'
import { isAuthApiPath, isQuotaExceeded, UNAUTHORIZED_CODE, QUOTA_EXCEEDED_CODE } from './apiError'

describe('apiError auth and quota helpers', () => {
  it('isAuthApiPath detects login and register', () => {
    expect(isAuthApiPath('/auth/login')).toBe(true)
    expect(isAuthApiPath('/auth/register')).toBe(true)
    expect(isAuthApiPath('/agents')).toBe(false)
  })

  it('isQuotaExceeded reads business code', () => {
    expect(isQuotaExceeded({ code: QUOTA_EXCEEDED_CODE, message: '超限', data: null })).toBe(true)
    expect(isQuotaExceeded({ code: UNAUTHORIZED_CODE, message: '未登录', data: null })).toBe(false)
  })
})
