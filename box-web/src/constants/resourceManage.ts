import { PLUGIN_CATEGORY_CREATE_LABELS, PLUGIN_CATEGORY_MANAGE_PATHS } from './resourceRoutes'

export interface ResourceManageCategory {
  value: string
  path: string
  label: string
  title: string
  desc: string
  icon: string
  tone: string
  createLabel: string
  searchPlaceholder: string
}

export const RESOURCE_MANAGE_CATEGORIES: ResourceManageCategory[] = [
  {
    value: 'workflows',
    path: PLUGIN_CATEGORY_MANAGE_PATHS.workflows,
    label: '工作流',
    title: '我的工作流',
    desc: '编排本空间的 Agent 工作流。创建后全体成员共用，可调试与发布。',
    icon: 'tree-square-dot',
    tone: 'ink',
    createLabel: PLUGIN_CATEGORY_CREATE_LABELS.workflows,
    searchPlaceholder: '搜索工作流名称或描述',
  },
  {
    value: 'knowledge',
    path: PLUGIN_CATEGORY_MANAGE_PATHS.knowledge,
    label: '知识库',
    title: '我的知识库',
    desc: '上传文档、分块并建立索引。创建后全体成员共用，供 Agent RAG 检索。',
    icon: 'folder',
    tone: 'sky',
    createLabel: PLUGIN_CATEGORY_CREATE_LABELS.knowledge,
    searchPlaceholder: '搜索知识库名称或描述',
  },
  {
    value: 'tools',
    path: PLUGIN_CATEGORY_MANAGE_PATHS.tools,
    label: '工具',
    title: '我的工具',
    desc: '配置 HTTP 工具。创建后全体成员共用，绑定到 Agent 后可调用。',
    icon: 'tools',
    tone: 'stone',
    createLabel: PLUGIN_CATEGORY_CREATE_LABELS.tools,
    searchPlaceholder: '搜索工具名称、Key 或描述',
  },
  {
    value: 'mcp',
    path: PLUGIN_CATEGORY_MANAGE_PATHS.mcp,
    label: 'MCP',
    title: '我的 MCP',
    desc: '连接 MCP Server。创建后全体成员共用，同步后即可使用外部工具。',
    icon: 'server',
    tone: 'violet',
    createLabel: PLUGIN_CATEGORY_CREATE_LABELS.mcp,
    searchPlaceholder: '搜索 MCP 名称、Key 或地址',
  },
]

export function resourceManageCategory(value: string): ResourceManageCategory | undefined {
  return RESOURCE_MANAGE_CATEGORIES.find((item) => item.value === value)
}

export function filterResourcesByKeyword<T>(
  items: T[],
  keyword: string,
  fields: (item: T) => Array<string | number | undefined | null>,
): T[] {
  const query = keyword.trim().toLowerCase()
  if (!query) return items
  return items.filter((item) =>
    fields(item)
      .map((field) => String(field ?? '').toLowerCase())
      .join(' ')
      .includes(query),
  )
}
