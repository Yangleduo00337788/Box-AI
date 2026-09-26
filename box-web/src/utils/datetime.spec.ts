import { describe, expect, it } from 'vitest'
import { formatDateTime } from './datetime'

describe('formatDateTime', () => {
  it('returns dash for empty values', () => {
    expect(formatDateTime()).toBe('-')
    expect(formatDateTime(null)).toBe('-')
    expect(formatDateTime('')).toBe('-')
  })

  it('replaces iso T and trims millis', () => {
    expect(formatDateTime('2026-09-26T15:08:11.123')).toBe('2026-09-26 15:08:11')
  })
})
