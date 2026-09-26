import { describe, expect, it } from 'vitest'
import { roleName } from './role'

describe('roleName', () => {
  it('maps known role codes', () => {
    expect(roleName('TENANT_ADMIN')).toBe('工作空间管理员')
    expect(roleName('DEVELOPER')).toBe('开发者')
    expect(roleName('MEMBER')).toBe('成员')
  })

  it('falls back for missing or unknown codes', () => {
    expect(roleName()).toBe('-')
    expect(roleName('GUEST')).toBe('GUEST')
  })
})
