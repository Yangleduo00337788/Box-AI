import { describe, expect, it } from 'vitest'
import {
  buildToolFromOpenApiOperation,
  listOpenApiOperations,
  parseOpenApiDocument,
  resolveOpenApiServerUrl,
} from './openapiToolImport'

describe('parseOpenApiDocument', () => {
  it('parses json and yaml, rejects empty or invalid input', () => {
    expect(() => parseOpenApiDocument('  ')).toThrow('请粘贴或上传 OpenAPI 文档')
    expect(parseOpenApiDocument('{"openapi":"3.0.0"}')).toEqual({ openapi: '3.0.0' })
    expect(() => parseOpenApiDocument('{')).toThrow('OpenAPI JSON 格式无效')
    expect(parseOpenApiDocument('openapi: "3.0.0"\ninfo:\n  title: Demo')).toMatchObject({
      openapi: '3.0.0',
    })
    expect(() => parseOpenApiDocument(':\n  -')).toThrow('OpenAPI YAML 解析失败')
  })
})

describe('openApi operations', () => {
  const doc = {
    servers: [{ url: 'https://api.example.com/' }],
    paths: {
      '/pets': {
        get: { summary: 'List pets' },
        post: { operationId: 'createPet', requestBody: { content: { 'application/json': {} } } },
      },
    },
  }

  it('lists http operations and builds a tool from the selected one', () => {
    expect(resolveOpenApiServerUrl(doc)).toBe('https://api.example.com')
    expect(resolveOpenApiServerUrl(doc, 'https://override.example/')).toBe('https://override.example')
    const ops = listOpenApiOperations(doc)
    expect(ops.map((item) => item.id)).toEqual(['GET /pets', 'POST /pets'])
    const tool = buildToolFromOpenApiOperation(doc, 'POST /pets')
    expect(tool.method).toBe('POST')
    expect(tool.url).toBe('https://api.example.com/pets')
    expect(tool.bodyType).toBe('JSON')
    expect(JSON.parse(tool.headersJson)['Content-Type']).toBe('application/json')
  })

  it('rejects documents without paths', () => {
    expect(() => listOpenApiOperations({})).toThrow('缺少 paths 字段')
  })
})
