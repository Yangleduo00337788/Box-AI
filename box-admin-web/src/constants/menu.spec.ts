import { describe, expect, it } from 'vitest'
import { ADMIN_MENU_GROUPS, adminMenuLeafPaths } from './menu'
import { ADMIN_ROUTE_ROLES } from './rbac'

describe('admin menu', () => {
  it('registers every sidebar path in ADMIN_ROUTE_ROLES', () => {
    const paths = adminMenuLeafPaths()
    expect(paths.length).toBeGreaterThan(0)
    for (const path of paths) {
      expect(ADMIN_ROUTE_ROLES[path], path).toBeDefined()
    }
  })

  it('uses unique submenu keys for nested groups', () => {
    const keys = ADMIN_MENU_GROUPS.map((group) => group.value)
    expect(keys.every(Boolean)).toBe(true)
    expect(new Set(keys).size).toBe(keys.length)
  })
})
