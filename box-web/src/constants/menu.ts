import type { MenuGroup } from '@box/ui/types/menu'

/** C 端侧栏菜单 */
export const CONSUMER_MENU_GROUPS: MenuGroup[] = [
  {
    title: '工作台',
    items: [
      { value: '/chat', label: '新任务', icon: 'chat-bubble-add', desc: '开启新对话' },
      { value: '/plugin-market', label: '插件市场', icon: 'ai-tool', desc: '工作流、知识库、工具与 MCP' },
      { value: '/models', label: '模型', icon: 'ai-1', desc: '自定义模型（BYOK）' },
      { value: '/executions', label: '执行记录', icon: 'history', desc: 'Trace 与运行历史' },
      { value: '/debug', label: 'Debug Console', icon: 'bug', desc: '统一调试与 Trace 树' },
    ],
  },
]
