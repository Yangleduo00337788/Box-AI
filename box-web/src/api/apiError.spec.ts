import { describe, expect, it, vi, beforeEach } from 'vitest'

vi.mock('tdesign-vue-next', () => ({
  MessagePlugin: { error: vi.fn() },
}))

import { MessagePlugin } from 'tdesign-vue-next'
import {
  isAuthApiPath,
  isQuotaExceeded,
  notifyError,
  UNAUTHORIZED_CODE,
  QUOTA_EXCEEDED_CODE,
} from './apiError'

describe('apiError auth and quota helpers', () => {
  beforeEach(() => {
    vi.mocked(MessagePlugin.error).mockClear()
  })

  it('isAuthApiPath detects auth endpoints', () => {
    expect(isAuthApiPath('/auth/login')).toBe(true)
    expect(isAuthApiPath('/auth/register')).toBe(true)
    expect(isAuthApiPath('/auth/enterprise/org')).toBe(true)
    expect(isAuthApiPath('/agents')).toBe(false)
  })

  it('notifyError suppresses duplicate messages', () => {
    notifyError('企业标识不存在')
    notifyError('企业标识不存在')
    expect(MessagePlugin.error).toHaveBeenCalledTimes(1)
  })

  it('isQuotaExceeded reads business code', () => {
    expect(isQuotaExceeded({ code: QUOTA_EXCEEDED_CODE, message: '超限', data: null })).toBe(true)
    expect(isQuotaExceeded({ code: UNAUTHORIZED_CODE, message: '未登录', data: null })).toBe(false)
  })
})
