/** 从消息 metadataJson 解析扩展字段（与 citations 并存） */

export interface MessagePluginMeta {
  id: number
  title: string
  category: string
  sourceType?: string
}

export interface ChatToolRun {
  toolKey: string
  label: string
  status: 'running' | 'success' | 'failed'
  output?: string
}

const LEGACY_PLUGIN_SUFFIX = /\n\n---\n本次消息启用的插件：[\s\S]*$/m

export function stripLegacyPluginSuffix(content: string): string {
  if (!content) return ''
  return content.replace(LEGACY_PLUGIN_SUFFIX, '').trim()
}

export function parseMessagePlugins(metadataJson?: string): MessagePluginMeta[] {
  if (!metadataJson) return []
  try {
    const parsed = JSON.parse(metadataJson) as { plugins?: MessagePluginMeta[] }
    if (!Array.isArray(parsed.plugins)) return []
    return parsed.plugins.filter((item) => item && typeof item.id === 'number')
  } catch {
    return []
  }
}

export function parseMessageReasoning(metadataJson?: string): string | undefined {
  if (!metadataJson) return undefined
  try {
    const parsed = JSON.parse(metadataJson) as { reasoning?: string; thinking?: string }
    const value = parsed.reasoning || parsed.thinking
    return value?.trim() || undefined
  } catch {
    return undefined
  }
}
