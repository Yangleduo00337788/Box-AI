import { describe, expect, it } from 'vitest'
import {
  canAccessAdminRoute,
  firstAccessibleAdminRoute,
  normalizePlatformRole,
  PLATFORM_ADMIN_ROLES,
} from './rbac'

describe('admin rbac', () => {
  it('normalizes unknown roles to super admin', () => {
    expect(normalizePlatformRole('ops')).toBe(PLATFORM_ADMIN_ROLES.OPS)
    expect(normalizePlatformRole('nope')).toBe(PLATFORM_ADMIN_ROLES.SUPER_ADMIN)
  })

  it('finance cannot open object storage but ops can', () => {
    expect(canAccessAdminRoute(PLATFORM_ADMIN_ROLES.FINANCE, '/platform-object-storage')).toBe(false)
    expect(canAccessAdminRoute(PLATFORM_ADMIN_ROLES.OPS, '/platform-object-storage')).toBe(true)
    expect(canAccessAdminRoute(PLATFORM_ADMIN_ROLES.SUPER_ADMIN, '/system-config')).toBe(true)
  })

  it('unregistered routes are allowed so 404 can render', () => {
    expect(canAccessAdminRoute(PLATFORM_ADMIN_ROLES.FINANCE, '/not-a-real-page')).toBe(true)
  })

  it('firstAccessibleAdminRoute prefers dashboard', () => {
    expect(firstAccessibleAdminRoute(PLATFORM_ADMIN_ROLES.CONTENT)).toBe('/dashboard')
  })
})
