import { describe, expect, it } from 'vitest'
import { parseMessagePlugins, parseMessageReasoning, stripLegacyPluginSuffix } from './messageMetadata'

describe('messageMetadata', () => {
  it('parses plugins and reasoning from metadata json', () => {
    expect(
      parseMessagePlugins('{"plugins":[{"id":1,"title":"KB","category":"knowledge"},{"id":"bad"}]}'),
    ).toEqual([{ id: 1, title: 'KB', category: 'knowledge' }])
    expect(parseMessageReasoning('{"reasoning":" 想了想 "}')).toBe('想了想')
    expect(parseMessagePlugins('not-json')).toEqual([])
  })

  it('strips legacy plugin footer', () => {
    expect(stripLegacyPluginSuffix('正文\n\n---\n本次消息启用的插件：知识库')).toBe('正文')
    expect(stripLegacyPluginSuffix('')).toBe('')
  })
})
