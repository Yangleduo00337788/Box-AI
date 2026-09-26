import { describe, expect, it } from 'vitest'
import { ADMIN_MENU_GROUPS } from './menu'
import { ADMIN_ROUTE_ROLES } from './rbac'

describe('admin menu', () => {
  it('registers every sidebar path in ADMIN_ROUTE_ROLES', () => {
    const paths = ADMIN_MENU_GROUPS.flatMap((group) => group.items.map((item) => item.value))
    expect(paths.length).toBeGreaterThan(0)
    for (const path of paths) {
      expect(ADMIN_ROUTE_ROLES[path], path).toBeDefined()
    }
  })
})
