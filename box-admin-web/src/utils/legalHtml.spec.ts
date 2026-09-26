import { describe, expect, it } from 'vitest'
import { legalSourceToHtml, legalValueToSource, sanitizeLegalHtml } from './legalHtml'

describe('legalHtml', () => {
  it('strips script handlers and javascript urls', () => {
    const html = '<p onclick="alert(1)">ok</p><script>alert(2)</script><a href="javascript:void(0)">x</a>'
    const cleaned = sanitizeLegalHtml(html)
    expect(cleaned).not.toContain('script')
    expect(cleaned).not.toContain('onclick')
    expect(cleaned).not.toMatch(/javascript:/i)
  })

  it('converts json paragraphs to escaped html', () => {
    const source = legalValueToSource('["Hello <b>x</b>","Next"]')
    expect(source).toContain('<p>Hello &lt;b&gt;x&lt;/b&gt;</p>')
    expect(source).toContain('<p>Next</p>')
  })

  it('legalSourceToHtml uses empty placeholder', () => {
    expect(legalSourceToHtml('')).toContain('暂无内容')
  })
})
