export const PLATFORM_ADMIN_ROLES = {
  SUPER_ADMIN: 'SUPER_ADMIN',
  OPS: 'OPS',
  FINANCE: 'FINANCE',
  CONTENT: 'CONTENT',
} as const

export type PlatformAdminRole = (typeof PLATFORM_ADMIN_ROLES)[keyof typeof PLATFORM_ADMIN_ROLES]

const ALL_ROLES: PlatformAdminRole[] = [
  PLATFORM_ADMIN_ROLES.SUPER_ADMIN,
  PLATFORM_ADMIN_ROLES.OPS,
  PLATFORM_ADMIN_ROLES.FINANCE,
  PLATFORM_ADMIN_ROLES.CONTENT,
]

/** 平台角色可见页面的唯一来源；侧栏与路由守卫都读这里。 */
export const ADMIN_ROUTE_ROLES: Record<string, PlatformAdminRole[]> = {
  '/dashboard': ALL_ROLES,
  '/analytics': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.FINANCE],
  '/tenants': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.FINANCE],
  '/users': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS],
  '/plans': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.FINANCE],
  '/billing-invoices': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.FINANCE],
  '/ops-placements': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/message-feedbacks': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/audit-logs': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.FINANCE],
  '/platform-models': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS],
  '/agent-templates': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/plugin-catalog': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/platform-tools': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/platform-mcp': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS, PLATFORM_ADMIN_ROLES.CONTENT],
  '/system-config': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN],
  '/platform-ocr': [PLATFORM_ADMIN_ROLES.SUPER_ADMIN, PLATFORM_ADMIN_ROLES.OPS],
}

export function normalizePlatformRole(role?: string | null): PlatformAdminRole {
  const value = role?.trim().toUpperCase()
  if (value === PLATFORM_ADMIN_ROLES.OPS
    || value === PLATFORM_ADMIN_ROLES.FINANCE
    || value === PLATFORM_ADMIN_ROLES.CONTENT
    || value === PLATFORM_ADMIN_ROLES.SUPER_ADMIN) {
    return value
  }
  return PLATFORM_ADMIN_ROLES.SUPER_ADMIN
}

export function canAccessAdminRoute(role: string | undefined | null, path: string): boolean {
  const normalized = normalizePlatformRole(role)
  if (normalized === PLATFORM_ADMIN_ROLES.SUPER_ADMIN) {
    return true
  }
  const matched = Object.keys(ADMIN_ROUTE_ROLES)
    .filter((route) => path === route || path.startsWith(`${route}/`))
    .sort((a, b) => b.length - a.length)[0]
  if (!matched) {
    // 未登记路由交给 catch-all 渲染 404，而不是一律当成无权限。
    return true
  }
  return ADMIN_ROUTE_ROLES[matched].includes(normalized)
}

export function firstAccessibleAdminRoute(role: string | undefined | null): string {
  const normalized = normalizePlatformRole(role)
  for (const [path, roles] of Object.entries(ADMIN_ROUTE_ROLES)) {
    if (roles.includes(normalized)) {
      return path
    }
  }
  return '/forbidden'
}
