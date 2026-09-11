import type { MenuGroup } from '@box/ui/types/menu'

export const ADMIN_MENU_GROUPS: MenuGroup[] = [
  {
    title: '平台',
    items: [
      { value: '/tenants', label: '租户管理', icon: 'city', desc: '租户创建、启停与套餐分配' },
      { value: '/plans', label: '套餐管理', icon: 'wallet', desc: '套餐方案与配额配置' },
      { value: '/platform-models', label: '平台模型池', icon: 'cpu', desc: '接入模型与平台密钥' },
      { value: '/agent-templates', label: '智能体市场', icon: 'shop', desc: '上架模板供 C 端启用' },
      { value: '/plugin-catalog', label: '插件市场', icon: 'layers', desc: '插件分类与上架管理' },
      { value: '/system-config', label: '系统配置', icon: 'setting', desc: '关于、协议与客服信息' },
    ],
  },
]
