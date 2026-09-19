/** 从消息 metadataJson 解析扩展字段（与 citations 并存） */

export function parseMessageReasoning(metadataJson?: string): string | undefined {
  if (!metadataJson) return undefined
  try {
    const parsed = JSON.parse(metadataJson) as { reasoning?: string; thinking?: string }
    const text = parsed.reasoning?.trim() || parsed.thinking?.trim()
    return text || undefined
  } catch {
    return undefined
  }
}
