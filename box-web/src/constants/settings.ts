export const SETTINGS_NAV = [
  {
    title: '账号',
    items: [
      { value: '/settings/profile', label: '个人信息', icon: 'user' },
      { value: '/settings/appearance', label: '外观设置', icon: 'palette' },
      { value: '/settings/general', label: '通用设置', icon: 'setting' },
      { value: '/settings/security', label: '账号与安全', icon: 'lock-on' },
      { value: '/settings/api-keys', label: 'API 密钥', icon: 'key' },
      { value: '/settings/roles', label: '角色与权限', icon: 'usergroup' },
    ],
  },
  {
    title: '资源与计费',
    items: [
      { value: '/settings/quota', label: '额度管理', icon: 'chart' },
      { value: '/settings/billing', label: '账单概览', icon: 'money' },
      { value: '/settings/capacity', label: '容量管理', icon: 'server' },
    ],
  },
  {
    title: '关于',
    items: [
      { value: '/settings/about', label: '关于盒子', icon: 'help-circle' },
      { value: '/settings/legal', label: '隐私与协议', icon: 'secured' },
    ],
  },
] as const
