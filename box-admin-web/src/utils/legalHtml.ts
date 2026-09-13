export function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function sanitizeLegalHtml(html: string) {
  return html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
    .replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)/gi, '')
    .replace(/javascript:/gi, '')
}

export function legalValueToSource(raw: string) {
  const trimmed = (raw || '').trim()
  if (!trimmed) return ''
  if (trimmed.startsWith('<')) return trimmed
  if (trimmed.startsWith('[')) {
    try {
      const paragraphs = JSON.parse(trimmed) as unknown
      if (Array.isArray(paragraphs)) {
        return paragraphs
          .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
          .map((item) => `<p>${escapeHtml(item)}</p>`)
          .join('\n')
      }
    } catch {
      return trimmed
    }
  }
  return trimmed
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter(Boolean)
    .map((block) => `<p>${escapeHtml(block).replace(/\n/g, '<br/>')}</p>`)
    .join('\n')
}

export function legalSourceToHtml(raw: string) {
  const trimmed = (raw || '').trim()
  if (!trimmed) return '<p class="legal-empty">暂无内容</p>'
  if (trimmed.startsWith('<')) return sanitizeLegalHtml(trimmed)
  return sanitizeLegalHtml(legalValueToSource(trimmed))
}
