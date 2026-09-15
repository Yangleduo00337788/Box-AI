export interface SettingsNavItem {
  value: string
  label: string
  icon: string
  permission?: string
}

export interface SettingsNavGroup {
  title: string
  items: SettingsNavItem[]
}

export const SETTINGS_NAV: SettingsNavGroup[] = [
  {
    title: '账号',
    items: [
      { value: '/settings/profile', label: '个人信息', icon: 'user' },
      { value: '/settings/security', label: '账号与安全', icon: 'lock-on' },
      { value: '/settings/preferences', label: '偏好设置', icon: 'setting' },
    ],
  },
  {
    title: '工作空间',
    items: [
      { value: '/settings/api-keys', label: 'API 密钥', icon: 'key', permission: 'api_key:manage' },
      { value: '/settings/roles', label: '角色与权限', icon: 'usergroup', permission: 'role:manage' },
      { value: '/settings/audit-logs', label: '审计日志', icon: 'history', permission: 'audit:read' },
    ],
  },
  {
    title: '套餐',
    items: [
      { value: '/settings/plan', label: '套餐与额度', icon: 'chart' },
    ],
  },
  {
    title: '关于',
    items: [
      { value: '/settings/about', label: '关于盒子', icon: 'help-circle' },
      { value: '/settings/legal', label: '隐私与协议', icon: 'file' },
    ],
  },
]
