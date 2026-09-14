import type { MenuGroup } from '@box/ui/types/menu'

export const ADMIN_MENU_GROUPS: MenuGroup[] = [
  {
    title: '工作台',
    items: [
      { value: '/dashboard', label: '概览', icon: 'dashboard', desc: '平台资源与快捷入口' },
      { value: '/analytics', label: '平台分析', icon: 'chart', desc: '执行量、Token 与租户用量' },
    ],
  },
  {
    title: '运营',
    items: [
      { value: '/tenants', label: '租户管理', icon: 'city', desc: '租户创建、启停与套餐分配' },
      { value: '/users', label: '用户管理', icon: 'user', desc: '新建平台管理员、启停账号' },
      { value: '/plans', label: '套餐管理', icon: 'wallet', desc: '套餐方案与配额配置' },
      { value: '/audit-logs', label: '审计日志', icon: 'history', desc: '全平台操作与安全审计' },
    ],
  },
  {
    title: '资源',
    items: [
      { value: '/platform-models', label: '平台模型池', icon: 'cpu', desc: '接入模型与平台密钥' },
      { value: '/agent-templates', label: '智能体市场', icon: 'shop', desc: '上架模板供 C 端启用' },
      { value: '/plugin-catalog', label: '插件市场', icon: 'layers', desc: '插件分类与上架管理' },
      { value: '/platform-tools', label: '官方工具', icon: 'tools', desc: '上架平台 HTTP 工具供租户安装' },
      { value: '/platform-mcp', label: '官方 MCP', icon: 'server', desc: '上架平台 MCP 服务供租户安装' },
    ],
  },
  {
    title: '系统',
    items: [{ value: '/system-config', label: '系统配置', icon: 'setting', desc: '关于、协议与客服信息' }],
  },
]
