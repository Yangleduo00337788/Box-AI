/** 插件市场分类 → 工作空间资源管理路由 */
export const PLUGIN_CATEGORY_MANAGE_PATHS: Record<string, string> = {
  workflows: '/workflows',
  knowledge: '/knowledge',
  tools: '/tools',
  mcp: '/mcp',
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
