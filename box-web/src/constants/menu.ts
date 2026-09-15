import type { MenuGroup } from '@box/ui/types/menu'

/** C 端侧栏菜单 */
export const CONSUMER_MENU_GROUPS: MenuGroup[] = [
  {
    title: '工作台',
    items: [
      { value: '/chat', label: '新任务', icon: 'chat-bubble-add', desc: '开启新对话' },
      { value: '/plugin-market', label: '插件市场', icon: 'ai-tool', desc: '工作流、知识库、工具、MCP 与 Skills' },
      { value: '/models', label: '模型', icon: 'ai-1', desc: '自定义模型（BYOK）' },
    ],
  },
  {
    title: '资源',
    items: [
      { value: '/knowledge', label: '知识库', icon: 'book', desc: '文档与检索' },
      { value: '/tools', label: '工具', icon: 'tools', desc: 'HTTP 工具' },
      { value: '/mcp', label: 'MCP', icon: 'server', desc: 'MCP 服务' },
      { value: '/workflows', label: '工作流', icon: 'tree-square-dot', desc: '自动化流程' },
      { value: '/market', label: '市场', icon: 'shop', desc: '智能体模板' },
    ],
  },
]
