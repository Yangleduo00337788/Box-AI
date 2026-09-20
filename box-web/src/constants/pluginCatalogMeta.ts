import type { PluginCatalogVO } from '@/api/plugin'

export const PLUGIN_CATEGORY_META: Record<string, { icon: string; tone: string }> = {
  workflows: { icon: 'tree-square-dot', tone: 'ink' },
  knowledge: { icon: 'folder', tone: 'sky' },
  tools: { icon: 'tools', tone: 'stone' },
  mcp: { icon: 'server', tone: 'violet' },
  skills: { icon: 'education', tone: 'teal' },
}

export const PLUGIN_CATEGORY_LABELS: Record<string, string> = {
  workflows: '工作流',
  knowledge: '知识库',
  tools: '工具',
  mcp: 'MCP',
  skills: 'skill',
}

export function pluginPillLabel(plugin: Pick<PluginCatalogVO, 'category' | 'title'>) {
  const category = PLUGIN_CATEGORY_LABELS[plugin.category] || plugin.category || '插件'
  return `${category} ${plugin.title}`.trim()
}

export function pluginCategoryIcon(category: string) {
  return PLUGIN_CATEGORY_META[category]?.icon || 'extension'
}

/** 输入框内插件标签统一用拼图图标 */
export function pluginComposerIcon() {
  return 'app'
}

export function pluginSourceLabel(sourceType?: string) {
  return sourceType === 'USER' ? '工作空间' : '官方'
}

export function pluginSourceTheme(sourceType?: string): 'primary' | 'success' | 'default' {
  return sourceType === 'USER' ? 'success' : 'primary'
}

export function isPluginInstalled(item: PluginCatalogVO) {
  return Boolean(item.installed)
}
