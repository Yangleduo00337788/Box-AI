import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatChatMessageTime, formatRelativeTime, getAvatarColor } from './format'

describe('format helpers', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-26T15:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('formatRelativeTime covers minute hour and day buckets', () => {
    expect(formatRelativeTime(undefined)).toBe('')
    expect(formatRelativeTime('not-a-date')).toBe('')
    expect(formatRelativeTime('2026-09-26T14:59:30')).toBe('刚刚')
    expect(formatRelativeTime('2026-09-26T14:45:00')).toBe('15 分钟')
    expect(formatRelativeTime('2026-09-26T12:00:00')).toBe('3 小时')
    expect(formatRelativeTime('2026-09-24T15:00:00')).toBe('2 天')
  })

  it('formatChatMessageTime uses local hours and minutes', () => {
    expect(formatChatMessageTime('2026-09-26T14:39:00')).toBe('14:39')
    expect(formatChatMessageTime('')).toBe('')
  })

  it('getAvatarColor is stable for the same seed', () => {
    expect(getAvatarColor('alice')).toBe(getAvatarColor('alice'))
    expect(['#f9a8d4', '#93c5fd', '#86efac', '#fcd34d', '#c4b5fd', '#fda4af']).toContain(
      getAvatarColor('alice'),
    )
  })
})
