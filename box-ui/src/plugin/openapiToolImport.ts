import yaml from 'js-yaml'

export interface OpenApiOperationOption {
  id: string
  method: string
  path: string
  summary: string
}

export interface OpenApiImportedTool {
  method: string
  url: string
  headersJson: string
  queryParamsJson: string
  bodyType: string
  bodyTemplate: string
}

export function parseOpenApiDocument(raw: string): unknown {
  const trimmed = raw.trim()
  if (!trimmed) {
    throw new Error('请粘贴或上传 OpenAPI 文档')
  }
  if (trimmed.startsWith('{')) {
    try {
      return JSON.parse(trimmed) as unknown
    } catch {
      throw new Error('OpenAPI JSON 格式无效')
    }
  }
  try {
    return yaml.load(trimmed) as unknown
  } catch {
    throw new Error('OpenAPI YAML 解析失败，请检查格式')
  }
}

export function resolveOpenApiServerUrl(doc: unknown, override?: string): string {
  const trimmedOverride = override?.trim().replace(/\/$/, '')
  if (trimmedOverride) return trimmedOverride
  if (!doc || typeof doc !== 'object') return ''
  const root = doc as Record<string, unknown>
  const servers = Array.isArray(root.servers) ? root.servers : []
  if (servers.length && typeof servers[0] === 'object' && servers[0] && 'url' in (servers[0] as object)) {
    return String((servers[0] as { url?: string }).url || '').replace(/\/$/, '')
  }
  return ''
}

export function listOpenApiOperations(doc: unknown): OpenApiOperationOption[] {
  if (!doc || typeof doc !== 'object') {
    throw new Error('OpenAPI 文档格式无效')
  }
  const root = doc as Record<string, unknown>
  const paths = root.paths as Record<string, Record<string, unknown>> | undefined
  if (!paths) {
    throw new Error('缺少 paths 字段')
  }

  const ops: OpenApiOperationOption[] = []
  for (const [path, methods] of Object.entries(paths)) {
    if (!methods || typeof methods !== 'object') continue
    for (const [method, detail] of Object.entries(methods)) {
      const m = method.toLowerCase()
      if (!['get', 'post', 'put', 'delete', 'patch'].includes(m)) continue
      const summary =
        detail && typeof detail === 'object' && 'summary' in detail
          ? String((detail as { summary?: string }).summary || '')
          : ''
      const operationId =
        detail && typeof detail === 'object' && 'operationId' in detail
          ? String((detail as { operationId?: string }).operationId || '')
          : ''
      ops.push({
        id: `${m.toUpperCase()} ${path}`,
        method: m.toUpperCase(),
        path,
        summary: summary || operationId || `${m.toUpperCase()} ${path}`,
      })
    }
  }
  if (!ops.length) {
    throw new Error('未解析到任何 HTTP 操作')
  }
  return ops.sort((a, b) => a.id.localeCompare(b.id))
}

export function buildToolFromOpenApiOperation(
  doc: unknown,
  operationId: string,
  serverUrlOverride?: string,
): OpenApiImportedTool {
  const [method, path] = operationId.split(' ', 2)
  if (!method || !path) {
    throw new Error('操作无效')
  }
  const serverUrl = resolveOpenApiServerUrl(doc, serverUrlOverride)
  const fullUrl = `${serverUrl}${path}`.replace(/([^:]\/)\/+/g, '$1')

  const root = doc as Record<string, unknown>
  const pathItem = (root.paths as Record<string, Record<string, unknown>>)?.[path]
  const op = pathItem?.[method.toLowerCase()] as Record<string, unknown> | undefined

  const query: Record<string, string> = {}
  const parameters = Array.isArray(op?.parameters) ? op.parameters : []
  for (const param of parameters) {
    if (!param || typeof param !== 'object') continue
    const p = param as { in?: string; name?: string; example?: unknown; schema?: { default?: unknown } }
    if (p.in === 'query' && p.name) {
      const example = p.example ?? p.schema?.default
      query[p.name] = example != null ? String(example) : ''
    }
  }

  let bodyType = 'NONE'
  let bodyTemplate = ''
  const requestBody = op?.requestBody as { content?: Record<string, unknown> } | undefined
  const jsonSchema = requestBody?.content?.['application/json']
  if (jsonSchema) {
    bodyType = 'JSON'
    bodyTemplate = '{\n  \n}'
  }

  const headers: Record<string, string> = {}
  if (bodyType === 'JSON') {
    headers['Content-Type'] = 'application/json'
  }

  return {
    method: method.toUpperCase(),
    url: fullUrl,
    headersJson: Object.keys(headers).length ? JSON.stringify(headers, null, 2) : '',
    queryParamsJson: Object.keys(query).length ? JSON.stringify(query, null, 2) : '',
    bodyType,
    bodyTemplate,
  }
}
