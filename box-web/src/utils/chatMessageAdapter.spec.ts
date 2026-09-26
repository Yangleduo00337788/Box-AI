import { describe, expect, it } from 'vitest'
import {
  isLocalLoadingBubble,
  toChatUiContent,
  toChatUiRole,
  toChatUiStatus,
  toLocalChatUiContent,
  toLocalChatUiStatus,
} from './chatMessageAdapter'
import type { MessageVO } from '@/api/conversation'

function message(partial: Partial<MessageVO> & Pick<MessageVO, 'role' | 'content'>): MessageVO {
  return {
    id: 1,
    contentType: 'text',
    sequenceNo: 1,
    createdAt: '2026-09-26T15:00:00',
    ...partial,
  }
}

describe('chatMessageAdapter', () => {
  it('maps roles and stop status', () => {
    expect(toChatUiRole('USER')).toBe('user')
    expect(toChatUiRole('ASSISTANT')).toBe('assistant')
    expect(toChatUiRole('SYSTEM')).toBe('system')
    const stopped = message({ role: 'ASSISTANT', content: '先这样（已停止生成）' })
    expect(toChatUiStatus(stopped, 0, [stopped], false)).toBe('stop')
  })

  it('marks last empty assistant as pending while chatting', () => {
    const user = message({ id: 1, role: 'USER', content: 'hi' })
    const assistant = message({ id: 2, role: 'ASSISTANT', content: '' })
    const messages = [user, assistant]
    expect(toChatUiStatus(assistant, 1, messages, true)).toBe('pending')
    expect(toChatUiContent(assistant, 1, messages, true)).toEqual([{ type: 'markdown', data: '' }])
  })

  it('renders empty assistant as fallback when not chatting', () => {
    const assistant = message({ role: 'ASSISTANT', content: '' })
    expect(toChatUiContent(assistant, 0, [assistant], false)).toEqual([{ type: 'markdown', data: '（无回复）' }])
  })

  it('handles local loading and streaming bubbles', () => {
    const messages = [
      { role: 'user' as const, content: 'hi' },
      { role: 'assistant' as const, content: '' },
    ]
    expect(isLocalLoadingBubble(messages[1], 1, messages, true)).toBe(true)
    expect(toLocalChatUiStatus(messages[1], 1, messages, true)).toBe('pending')
    expect(toLocalChatUiContent(messages[1], 1, messages, true)).toEqual([{ type: 'markdown', data: '' }])
    const streaming = [
      { role: 'user' as const, content: 'hi' },
      { role: 'assistant' as const, content: '正在' },
    ]
    expect(toLocalChatUiStatus(streaming[1], 1, streaming, true)).toBe('streaming')
  })
})
