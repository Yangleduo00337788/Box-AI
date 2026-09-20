/** 插件市场分类 → 工作空间资源管理路由 */
export const PLUGIN_CATEGORY_MANAGE_PATHS: Record<string, string> = {
  workflows: '/workflows',
  knowledge: '/knowledge',
  tools: '/tools',
  mcp: '/mcp',
}

export const PLUGIN_CATEGORY_CREATE_LABELS: Record<string, string> = {
  workflows: '新建工作流',
  knowledge: '新建知识库',
  tools: '新建工具',
  mcp: '添加 MCP',
}

export function createPathForCategory(category: string): string | null {
  const path = PLUGIN_CATEGORY_MANAGE_PATHS[category]
  return path ? `${path}?create=1` : null
}

const MANAGE_PATH_ENTRIES = Object.entries(PLUGIN_CATEGORY_MANAGE_PATHS)

export function managePathForCategory(category: string): string | null {
  return PLUGIN_CATEGORY_MANAGE_PATHS[category] ?? null
}

export function categoryForManageRoute(path: string): string | null {
  for (const [category, managePath] of MANAGE_PATH_ENTRIES) {
    if (path === managePath || path.startsWith(`${managePath}/`)) {
      return category
    }
  }
  return null
}

export function pluginMarketPathForCategory(category: string): string {
  return `/plugin-market?category=${encodeURIComponent(category)}`
}

export function pluginMarketPathForManageRoute(path: string): string {
  const category = categoryForManageRoute(path)
  return category ? pluginMarketPathForCategory(category) : '/plugin-market'
}

/** 已安装插件「打开」：工作流优先进编辑器，其余走 API targetPath 或资源列表 */
export function resolvePluginOpenPath(item: {
  category: string
  resourceId?: number
  targetPath?: string
}): string | null {
  const category = item.category?.trim().toLowerCase()
  if ((category === 'workflows' || category === 'workflow') && item.resourceId) {
    return `/workflows/${item.resourceId}/editor`
  }
  if (item.targetPath) {
    return item.targetPath
  }
  return managePathForCategory(item.category)
}
