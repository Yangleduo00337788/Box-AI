export type PortalType = 'personal' | 'enterprise'

export const PORTAL_OPTIONS: { value: PortalType; label: string; desc: string }[] = [
  { value: 'personal', label: '个人端', desc: '适合个人创作者与独立开发者' },
  { value: 'enterprise', label: '企业端', desc: '适合团队与企业协作使用' },
]

export function portalLabel(type: PortalType) {
  return type === 'enterprise' ? '企业端' : '个人端'
}

export function portalTheme(type: PortalType): 'personal' | 'enterprise' {
  return type === 'enterprise' ? 'enterprise' : 'personal'
}
