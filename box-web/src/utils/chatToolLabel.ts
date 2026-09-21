import type { MessagePluginMeta } from '@/utils/messageMetadata'
import { PLUGIN_CATEGORY_LABELS } from '@/constants/pluginCatalogMeta'

export function resolveToolInvokeLabel(toolKey: string, plugins: MessagePluginMeta[] = []): string {
  const key = toolKey?.trim() || 'unknown'
  for (const plugin of plugins) {
    const code = (plugin.title || '').toLowerCase()
    if (key.includes(String(plugin.id)) || (code && key.toLowerCase().includes(code))) {
      const cat = PLUGIN_CATEGORY_LABELS[plugin.category] || plugin.category || '插件'
      return `${cat} · ${plugin.title}`
    }
  }
  if (key.startsWith('mcp_')) {
    return `MCP · ${key.replace(/^mcp_/, '').replace(/_/g, ' ')}`
  }
  if (key.startsWith('workflow_')) {
    return `工作流 · ${key.replace(/^workflow_/, '')}`
  }
  if (key.startsWith('plugin-') || key.includes('plugin')) {
    return `工具 · ${key}`
  }
  return `工具 · ${key}`
}
