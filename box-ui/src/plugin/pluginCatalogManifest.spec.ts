import { describe, expect, it } from 'vitest'
import {
  applyManifestJsonToDraft,
  buildManifestJsonFromDraft,
  emptyManifestDraft,
} from './pluginCatalogManifest'

describe('pluginCatalogManifest', () => {
  it('starts with http get defaults', () => {
    const draft = emptyManifestDraft()
    expect(draft.method).toBe('GET')
    expect(draft.bodyType).toBe('NONE')
    expect(draft.toolCatalogJson).toBe('[]')
  })

  it('round-trips a tools manifest', () => {
    const draft = emptyManifestDraft()
    draft.method = 'POST'
    draft.url = 'https://example.com/run'
    draft.headersJson = '{"Authorization":"Bearer x"}'
    const json = buildManifestJsonFromDraft('tools', draft)
    const next = applyManifestJsonToDraft(json)
    expect(next.method).toBe('POST')
    expect(next.url).toBe('https://example.com/run')
    expect(JSON.parse(next.headersJson)).toEqual({ Authorization: 'Bearer x' })
  })

  it('requires mcp json when input mode is json', () => {
    const draft = emptyManifestDraft()
    draft.mcpInputMode = 'json'
    expect(() => buildManifestJsonFromDraft('mcp', draft)).toThrow('请填写 MCP JSON 配置')
  })
})
