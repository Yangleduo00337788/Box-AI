export interface PluginCatalogBundleDoc {
  storageKey: string
  fileName: string
  size?: number
}

export interface PluginManifestDraft {
  method: string
  url: string
  timeoutMs: number
  headersJson: string
  queryParamsJson: string
  bodyType: string
  bodyTemplate: string
  mcpInputMode: 'form' | 'json'
  transportType: string
  endpointUrl: string
  authType: string
  toolCatalogJson: string
  mcpJson: string
  definitionJson: string
  instructions: string
  skillMdStorageKey: string
  skillMdFileName: string
  skillPackageStorageKey: string
  skillPackageFileName: string
  bundleDocuments: PluginCatalogBundleDoc[]
}

export function emptyManifestDraft(): PluginManifestDraft {
  return {
    method: 'GET',
    url: '',
    timeoutMs: 10000,
    headersJson: '',
    queryParamsJson: '',
    bodyType: 'NONE',
    bodyTemplate: '',
    mcpInputMode: 'form',
    transportType: 'HTTP',
    endpointUrl: '',
    authType: 'NONE',
    toolCatalogJson: '[]',
    mcpJson: '',
    definitionJson: '',
    instructions: '',
    skillMdStorageKey: '',
    skillMdFileName: '',
    skillPackageStorageKey: '',
    skillPackageFileName: '',
    bundleDocuments: [],
  }
}

export function applyManifestJsonToDraft(raw?: string, draft?: PluginManifestDraft): PluginManifestDraft {
  const target = draft ?? emptyManifestDraft()
  const data = parseManifest(raw)
  target.method = String(data.method || 'GET')
  target.url = String(data.url || '')
  target.timeoutMs = Number(data.timeoutMs || 10000)
  target.headersJson = jsonFieldToText(data.headers)
  target.queryParamsJson = jsonFieldToText(data.queryParams)
  target.bodyType = String(data.bodyType || 'NONE')
  target.bodyTemplate = String(data.bodyTemplate || '')
  if (data.mcpServers && typeof data.mcpServers === 'object') {
    target.mcpInputMode = 'json'
    target.mcpJson = JSON.stringify(data, null, 2)
  } else {
    target.mcpInputMode = 'form'
    target.transportType = String(data.transportType || 'HTTP')
    target.endpointUrl = String(data.endpointUrl || '')
    target.authType = String(data.authType || 'NONE')
    target.toolCatalogJson =
      typeof data.toolCatalogJson === 'string'
        ? data.toolCatalogJson
        : data.toolCatalogJson
          ? JSON.stringify(data.toolCatalogJson)
          : '[]'
    target.mcpJson = ''
  }
  const definition = data.definitionJson ?? data.definition
  target.definitionJson =
    typeof definition === 'string' ? definition : definition ? JSON.stringify(definition, null, 2) : ''
  target.instructions = String(data.instructions || '')
  target.skillMdStorageKey = String(data.skillMdStorageKey || '')
  target.skillMdFileName = String(data.skillMdFileName || '')
  target.skillPackageStorageKey = String(data.skillPackageStorageKey || '')
  target.skillPackageFileName = String(data.skillPackageFileName || '')
  const bundle = data.bundleDocuments
  target.bundleDocuments = Array.isArray(bundle)
    ? bundle.map((item: Record<string, unknown>) => ({
        storageKey: String(item.storageKey || ''),
        fileName: String(item.fileName || ''),
        size: item.size != null ? Number(item.size) : undefined,
      }))
    : []
  return target
}

function parseManifest(raw?: string): Record<string, unknown> {
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return {}
  }
}

function jsonFieldToText(value: unknown): string {
  if (value == null) return ''
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return ''
  }
}

function parseJsonObject(text: string, label: string): Record<string, unknown> | undefined {
  const trimmed = text.trim()
  if (!trimmed) return undefined
  try {
    const parsed = JSON.parse(trimmed)
    if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error(`${label} 必须是 JSON 对象`)
    }
    return parsed as Record<string, unknown>
  } catch (e) {
    throw new Error(e instanceof Error ? e.message : `${label} JSON 无效`)
  }
}

export function buildManifestJsonFromDraft(category: string, draft: PluginManifestDraft): string {
  if (category === 'tools') {
    const payload: Record<string, unknown> = {
      type: 'HTTP',
      method: draft.method,
      url: draft.url.trim(),
      timeoutMs: draft.timeoutMs,
    }
    const headers = parseJsonObject(draft.headersJson, 'Headers')
    if (headers) payload.headers = headers
    const queryParams = parseJsonObject(draft.queryParamsJson, 'Query')
    if (queryParams) payload.queryParams = queryParams
    if (draft.bodyType && draft.bodyType !== 'NONE') {
      payload.bodyType = draft.bodyType
      if (draft.bodyTemplate.trim()) payload.bodyTemplate = draft.bodyTemplate.trim()
    }
    return JSON.stringify(payload)
  }
  if (category === 'mcp') {
    if (draft.mcpInputMode === 'json') {
      const trimmed = draft.mcpJson.trim()
      if (!trimmed) throw new Error('请填写 MCP JSON 配置')
      const parsed = JSON.parse(trimmed)
      if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('MCP JSON 必须是对象')
      }
      return JSON.stringify(parsed)
    }
    const payload: Record<string, unknown> = {
      transportType: draft.transportType,
      endpointUrl: draft.endpointUrl.trim(),
      authType: draft.authType,
    }
    const tools = draft.toolCatalogJson.trim()
    if (tools) payload.toolCatalogJson = tools
    return JSON.stringify(payload)
  }
  if (category === 'workflows') {
    const definition = draft.definitionJson.trim()
    return JSON.stringify(definition ? { definitionJson: definition } : {})
  }
  if (category === 'skills') {
    const payload: Record<string, unknown> = {}
    if (draft.instructions.trim()) payload.instructions = draft.instructions.trim()
    if (draft.skillMdStorageKey) {
      payload.skillMdStorageKey = draft.skillMdStorageKey
      payload.skillMdFileName = draft.skillMdFileName
    }
    if (draft.skillPackageStorageKey) {
      payload.skillPackageStorageKey = draft.skillPackageStorageKey
      payload.skillPackageFileName = draft.skillPackageFileName
    }
    return JSON.stringify(payload)
  }
  if (category === 'knowledge') {
    return JSON.stringify({
      bundleDocuments: draft.bundleDocuments.map((d) => ({
        storageKey: d.storageKey,
        fileName: d.fileName,
        size: d.size,
      })),
    })
  }
  return JSON.stringify({})
}

export function validateManifestDraft(category: string, draft: PluginManifestDraft): string | null {
  if (category === 'tools') {
    if (!draft.url.trim()) return '工具插件需填写请求地址'
    return null
  }
  if (category === 'mcp') {
    if (draft.mcpInputMode === 'json') {
      if (!draft.mcpJson.trim()) return '请填写 MCP JSON 配置'
      try {
        JSON.parse(draft.mcpJson)
      } catch {
        return 'MCP JSON 格式无效'
      }
    } else if (!draft.endpointUrl.trim()) {
      return 'MCP 插件需填写服务地址，或切换到 JSON 模式'
    }
    return null
  }
  if (category === 'skills') {
    if (!draft.instructions.trim() && !draft.skillMdStorageKey && !draft.skillPackageStorageKey) {
      return 'Skill 需填写说明，或上传 SKILL.md / 压缩包'
    }
    return null
  }
  if (category === 'knowledge') {
    if (!draft.bundleDocuments.length) return '知识库插件需至少上传 1 个文档'
    return null
  }
  if (category === 'workflows') {
    if (!draft.definitionJson.trim()) return '工作流插件需画布编排或导入 JSON'
    try {
      const parsed = JSON.parse(draft.definitionJson)
      if (!parsed || typeof parsed !== 'object') return '工作流定义必须是 JSON 对象'
    } catch {
      return '工作流 JSON 格式无效'
    }
    return null
  }
  return null
}

export function workflowNodeSummary(definitionJson: string): string {
  try {
    const parsed = JSON.parse(definitionJson) as { nodes?: unknown[] }
    const count = Array.isArray(parsed.nodes) ? parsed.nodes.length : 0
    return count ? `已解析 ${count} 个节点` : '未识别到 nodes 数组'
  } catch {
    return 'JSON 无效'
  }
}
