import { describe, expect, it } from 'vitest'
import { parseUserContent } from './chatContent'

describe('parseUserContent', () => {
  it('extracts public-asset image blocks and leftover text', () => {
    const parsed = parseUserContent(
      '看看这张图\n\n[图片: 发票.png]\n\n/api/v1/public-assets/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png\n\n谢谢',
    )
    expect(parsed.images).toEqual([
      {
        name: '发票.png',
        url: '/api/v1/public-assets/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png',
      },
    ])
    expect(parsed.text).toContain('看看这张图')
    expect(parsed.text).toContain('谢谢')
    expect(parsed.text).not.toContain('[图片:')
  })

  it('returns original text when there is no image block', () => {
    expect(parseUserContent('你好')).toEqual({ text: '你好', images: [] })
  })
})
