/** 插件市场「我的」资源分类 */
export const PLUGIN_MINE_CATEGORIES = ['workflows', 'knowledge', 'tools', 'mcp'] as const

export type PluginMineCategory = (typeof PLUGIN_MINE_CATEGORIES)[number]

export function isPluginMineCategory(value: string): value is PluginMineCategory {
  return (PLUGIN_MINE_CATEGORIES as readonly string[]).includes(value)
}

export function pluginMarketMinePath(category: string, extra: Record<string, string> = {}): string {
  const query = new URLSearchParams({ mine: category, ...extra })
  return `/plugin-market?${query.toString()}`
}

/** 插件市场分类 → 工作空间「我的」资源 */
export const PLUGIN_CATEGORY_MANAGE_PATHS: Record<string, string> = {
  workflows: pluginMarketMinePath('workflows'),
  knowledge: pluginMarketMinePath('knowledge'),
  tools: pluginMarketMinePath('tools'),
  mcp: pluginMarketMinePath('mcp'),
}

export const PLUGIN_CATEGORY_CREATE_LABELS: Record<string, string> = {
  workflows: '新建工作流',
  knowledge: '新建知识库',
  tools: '新建工具',
  mcp: '添加 MCP',
}

export function createPathForCategory(category: string): string | null {
  if (!isPluginMineCategory(category)) return null
  return pluginMarketMinePath(category, { create: '1' })
}

export function managePathForCategory(category: string): string | null {
  return PLUGIN_CATEGORY_MANAGE_PATHS[category] ?? null
}

export function categoryForManageRoute(path: string): string | null {
  const normalized = path.split('?')[0] || path
  try {
    const url = new URL(path, 'http://local.invalid')
    const mine = url.searchParams.get('mine')
    if (mine && isPluginMineCategory(mine)) return mine
  } catch {
    /* ignore */
  }
  if (normalized === '/workflows' || normalized.startsWith('/workflows/')) return 'workflows'
  if (normalized === '/knowledge' || normalized.startsWith('/knowledge/')) return 'knowledge'
  if (normalized === '/tools' || normalized.startsWith('/tools/')) return 'tools'
  if (normalized === '/mcp' || normalized.startsWith('/mcp/')) return 'mcp'
  return null
}

export function pluginMarketPathForCategory(category: string): string {
  return `/plugin-market?category=${encodeURIComponent(category)}`
}

export function pluginMarketPathForManageRoute(path: string): string {
  const category = categoryForManageRoute(path)
  return category ? pluginMarketMinePath(category) : '/plugin-market'
}

/** 已安装插件「打开」：工作流优先进编辑器，其余进插件市场「我的」 */
export function resolvePluginOpenPath(item: {
  category: string
  resourceId?: number
  targetPath?: string
}): string | null {
  const category = item.category?.trim().toLowerCase()
  if ((category === 'workflows' || category === 'workflow') && item.resourceId) {
    return `/workflows/${item.resourceId}/editor`
  }
  const minePath = managePathForCategory(item.category)
  if (minePath) return minePath
  if (item.targetPath) return item.targetPath
  return null
}
