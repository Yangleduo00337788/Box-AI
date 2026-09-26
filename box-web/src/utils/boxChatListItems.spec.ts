import { describe, expect, it } from 'vitest'
import { buildConversationListItems, buildLocalListItems } from './boxChatListItems'
import type { MessageVO } from '@/api/conversation'

const identity = { userName: 'Ada', agentName: '助手' }

function vo(partial: Partial<MessageVO> & Pick<MessageVO, 'id' | 'role' | 'content'>): MessageVO {
  return {
    contentType: 'text',
    sequenceNo: 1,
    createdAt: '2026-09-26T14:39:00',
    ...partial,
  }
}

describe('boxChatListItems', () => {
  it('builds conversation rows with reply quote and action bars', () => {
    const messages = [
      vo({ id: 1, role: 'USER', content: '怎么退款' }),
      vo({ id: 2, role: 'ASSISTANT', content: '提交工单即可' }),
    ]
    const items = buildConversationListItems(messages, false, { enableFullActions: true }, identity)
    expect(items).toHaveLength(2)
    expect(items[0].uiRole).toBe('user')
    expect(items[0].actionBar).toEqual(['copy', 'share'])
    expect(items[0].displayName).toBe('Ada')
    expect(items[1].uiRole).toBe('assistant')
    expect(items[1].replyQuote).toEqual({ authorName: 'Ada', excerpt: '怎么退款' })
    expect(items[1].actionBar).toEqual(['copy', 'replay', 'good', 'bad', 'share'])
    expect(items[1].sentTimeLabel).toBe('14:39')
  })

  it('keeps thinking bubble without actions while chatting', () => {
    const messages = [
      vo({ id: 1, role: 'USER', content: 'hi' }),
      vo({ id: 2, role: 'ASSISTANT', content: '' }),
    ]
    const items = buildConversationListItems(messages, true, {}, identity)
    expect(items[1].thinkingActive).toBe(true)
    expect(items[1].showActions).toBe(false)
    expect(items[1].uiStatus).toBe('complete')
  })

  it('builds compact local chat items', () => {
    const items = buildLocalListItems(
      [
        { role: 'user', content: 'hi' },
        { role: 'assistant', content: 'hello' },
      ],
      false,
      { enableFullActions: false },
      identity,
    )
    expect(items[0].actionBar).toEqual(['copy'])
    expect(items[1].actionBar).toEqual(['copy'])
    expect(items[1].replyQuote?.excerpt).toBe('hi')
  })
})
