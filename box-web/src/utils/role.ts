const ROLE_NAMES: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  TENANT_ADMIN: '工作空间管理员',
  DEVELOPER: '开发者',
  MEMBER: '成员',
}

export function roleName(code?: string) {
  if (!code) return '-'
  return ROLE_NAMES[code] || code
}
