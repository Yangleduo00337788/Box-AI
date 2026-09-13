import DOMPurify from 'dompurify'
import { marked } from 'marked'

export function renderMarkdown(source: string): string {
  try {
    const html = marked.parse(source || '', { async: false, gfm: true, breaks: true }) as string
    if (typeof DOMPurify?.sanitize !== 'function') return html
    return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
  } catch {
    return source || ''
  }
}
