import { describe, expect, it } from 'vitest'
import { parseMessageCitations } from './conversation'

describe('parseMessageCitations', () => {
  it('reads citations array and ignores invalid json', () => {
    expect(parseMessageCitations('{"citations":[{"chunkId":1,"content":"a","score":0.9}]}')[0]).toMatchObject({
      chunkId: 1,
      content: 'a',
      score: 0.9,
    })
    expect(parseMessageCitations('{"citations":[]}')).toEqual([])
    expect(parseMessageCitations('not-json')).toEqual([])
    expect(parseMessageCitations()).toEqual([])
  })
})
