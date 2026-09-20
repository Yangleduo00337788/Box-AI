import type { MenuGroup } from '@box/ui/types/menu'

/**
 * C 端侧栏固定入口。知识库 / 工具 / MCP / 工作流从插件市场「我的」进入。
 */
export const CONSUMER_MENU_GROUPS: MenuGroup[] = [
  {
    title: '工作台',
    items: [
      { value: '/chat', label: '新任务', icon: 'chat-bubble-add', desc: '开启新对话' },
      { value: '/plugin-market', label: '插件市场', icon: 'ai-tool', desc: '工作流、知识库、工具、MCP 与 Skills' },
      { value: '/models', label: '模型', icon: 'ai-1', desc: '自定义模型（BYOK）' },
    ],
  },
]
