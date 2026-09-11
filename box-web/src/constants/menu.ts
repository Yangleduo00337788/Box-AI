import type { MenuGroup } from '@box/ui/types/menu'

/** C 端侧栏菜单 */
export const CONSUMER_MENU_GROUPS: MenuGroup[] = [
  {
    title: '工作台',
    items: [
      { value: '/chat', label: '新任务', icon: 'chat-bubble-add', desc: '开启新对话' },
      { value: '/agents', label: '智能体', icon: 'user-avatar', desc: '创建与管理 Agent' },
      { value: '/workflows', label: '工作流', icon: 'tree-square-dot-vertical', desc: '可视化编排' },
      { value: '/knowledge', label: '知识库', icon: 'book', desc: '文档与 RAG' },
      { value: '/tools', label: '工具', icon: 'tools', desc: 'HTTP Tool' },
      { value: '/mcp', label: 'MCP', icon: 'link', desc: 'MCP Server' },
      { value: '/plugin-market', label: '插件市场', icon: 'ai-tool', desc: '模板与插件' },
      { value: '/models', label: '模型', icon: 'ai-1', desc: '模型与 Provider' },
    ],
  },
]
