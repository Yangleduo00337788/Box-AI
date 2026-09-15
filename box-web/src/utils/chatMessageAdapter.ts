import type { MessageVO } from '@/api/conversation'
import { parseUserContent } from '@/utils/chatContent'

export type ChatUiRole = 'user' | 'assistant' | 'system'
export type ChatUiStatus = 'pending' | 'streaming' | 'complete' | 'stop' | 'error'

export interface ChatUiContentBlock {
  type: 'text' | 'markdown'
  data: string
}

export function toChatUiRole(role: MessageVO['role']): ChatUiRole {
  if (role === 'USER') return 'user'
  if (role === 'ASSISTANT') return 'assistant'
  return 'system'
}

export function toChatUiStatus(
  item: MessageVO,
  index: number,
  messages: MessageVO[],
  chatting: boolean,
): ChatUiStatus {
  const loading = chatting && index === messages.length - 1 && item.role === 'ASSISTANT' && !item.content
  if (loading) return 'pending'
  if (item.content?.includes('（已停止生成）')) return 'stop'
  return 'complete'
}

export function toChatUiContent(
  item: MessageVO,
  index: number,
  messages: MessageVO[],
  chatting: boolean,
): ChatUiContentBlock[] {
  const text = item.content || ''
  const loading = chatting && index === messages.length - 1 && item.role === 'ASSISTANT' && !text

  if (item.role === 'USER') {
    const parts = parseUserContent(text)
    const blocks: ChatUiContentBlock[] = []
    if (parts.text) {
      blocks.push({ type: 'text', data: parts.text })
    }
    if (!blocks.length && text) {
      blocks.push({ type: 'text', data: text })
    }
    return blocks.length ? blocks : [{ type: 'text', data: '' }]
  }

  if (loading) {
    return [{ type: 'markdown', data: '' }]
  }

  return [{ type: 'markdown', data: text || '（无回复）' }]
}

export function userMessageImages(item: MessageVO) {
  if (item.role !== 'USER') return []
  return parseUserContent(item.content || '').images
}
