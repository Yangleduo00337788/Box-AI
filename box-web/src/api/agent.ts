import { assertStreamResponseOk } from './apiError'
import http, { type Result } from './http'

export type ModelSource = 'PLATFORM' | 'BYOK'

export interface AgentVO {
  id: number
  name: string
  description?: string
  avatarUrl?: string
  status: string
  draftVersion?: number
  publishedVersion?: number
  modelSource?: ModelSource
  modelId?: number
  modelName?: string
  platformModelId?: number
  platformModelName?: string
  systemPrompt?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  streamEnabled?: boolean
  memoryEnabled?: boolean
  memoryWindowSize?: number
  longTermMemoryEnabled?: boolean
  configJson?: string
  createdBy: number
  createdAt: string
  updatedAt: string
}

export function listAgents() {
  return http.get<Result<AgentVO[]>>('/agents')
}

export function getAgent(id: number) {
  return http.get<Result<AgentVO>>(`/agents/${id}`)
}

export function createAgent(payload: {
  name: string
  description?: string
  avatarUrl?: string
  platformModelId?: number
  modelId?: number
}) {
  return http.post<Result<AgentVO>>('/agents', payload)
}

export function updateAgent(
  id: number,
  payload: { name: string; description?: string; avatarUrl?: string; modelId?: number },
) {
  return http.put<Result<AgentVO>>(`/agents/${id}`, payload)
}

export function updateAgentPrompt(id: number, payload: { systemPrompt?: string }) {
  return http.put<Result<AgentVO>>(`/agents/${id}/prompt`, payload)
}

export function updateAgentModel(
  id: number,
  payload: {
    modelSource: ModelSource
    platformModelId?: number
    modelId?: number
    temperature?: number
    topP?: number
    maxTokens?: number
    streamEnabled?: boolean
  },
) {
  return http.put<Result<AgentVO>>(`/agents/${id}/model`, payload)
}

export function updateAgentMemory(
  id: number,
  payload: { memoryEnabled?: boolean; memoryWindowSize?: number; longTermMemoryEnabled?: boolean },
) {
  return http.put<Result<AgentVO>>(`/agents/${id}/memory`, payload)
}

export interface AgentLongTermMemoryVO {
  id: number
  content: string
  createdAt: string
  updatedAt: string
}

export function listAgentLongTermMemories(id: number) {
  return http.get<Result<AgentLongTermMemoryVO[]>>(`/agents/${id}/long-term-memories`)
}

export function deleteAgentLongTermMemory(agentId: number, memoryId: number) {
  return http.delete<Result<void>>(`/agents/${agentId}/long-term-memories/${memoryId}`)
}

export function updateAgentConfig(id: number, payload: { configJson?: string }) {
  return http.put<Result<AgentVO>>(`/agents/${id}/config`, payload)
}

export interface AgentChatHistoryItem {
  role: 'USER' | 'ASSISTANT'
  content: string
}

export interface KnowledgeCitation {
  index: number
  chunkId: number
  documentId: number
  documentName: string
  pageNumber?: number
  chunkIndex?: number
  content: string
  score: number
}

export interface AgentChatResult {
  content: string
  citations?: KnowledgeCitation[]
  executionId?: number
}

function buildChatHistory(messages: AgentChatHistoryItem[]) {
  return messages
    .filter((item) => item.content?.trim())
    .map((item) => ({
      role: item.role,
      content: item.content.trim(),
    }))
}

export function chatAgent(id: number, message: string, history: AgentChatHistoryItem[] = []) {
  return http.post<Result<AgentChatResult>>(
    `/agents/${id}/chat`,
    { message, stream: false, history: buildChatHistory(history) },
    { timeout: 60000 },
  )
}

export interface ChatStreamEvent {
  type: 'delta' | 'done' | 'error' | 'citations'
  content?: string
  message?: string
  executionId?: number
}

function parseCitationsJson(raw?: string): KnowledgeCitation[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function authHeaders(accept = 'application/json') {
  const token = localStorage.getItem('box.token')
  const workspaceId = localStorage.getItem('box.workspaceId')
  const headers: Record<string, string> = {
    Accept: accept,
    'Content-Type': 'application/json',
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (token && workspaceId) {
    headers['X-Workspace-Id'] = workspaceId
  }
  return headers
}

interface StreamEventHandlers {
  onDelta: (chunk: string) => void
  onCitations?: (citations: KnowledgeCitation[]) => void
}

function parseSseEvent(rawEvent: string, handlers: StreamEventHandlers): 'continue' | 'done' | 'error' {
  for (const line of rawEvent.split('\n')) {
    if (!line.startsWith('data:')) continue
    const payload = line.slice(5).trim()
    if (!payload) continue
    const event = JSON.parse(payload) as ChatStreamEvent
    if (event.type === 'delta' && event.content) {
      handlers.onDelta(event.content)
    }
    if (event.type === 'citations' && event.content) {
      handlers.onCitations?.(parseCitationsJson(event.content))
    }
    if (event.type === 'error') {
      throw new Error(event.message || '流式对话失败')
    }
    if (event.type === 'done') {
      return 'done'
    }
  }
  return 'continue'
}

function drainSseBuffer(buffer: string, handlers: StreamEventHandlers): { rest: string; finished: boolean } {
  let rest = buffer
  let boundary = rest.indexOf('\n\n')
  while (boundary >= 0) {
    const rawEvent = rest.slice(0, boundary)
    rest = rest.slice(boundary + 2)
    const status = parseSseEvent(rawEvent, handlers)
    if (status === 'done') {
      return { rest, finished: true }
    }
    boundary = rest.indexOf('\n\n')
  }
  return { rest, finished: false }
}

export async function chatAgentStream(
  id: number,
  message: string,
  onDelta: (chunk: string) => void,
  history: AgentChatHistoryItem[] = [],
  options?: { signal?: AbortSignal; onCitations?: (citations: KnowledgeCitation[]) => void },
): Promise<void> {
  const response = await fetch(`/api/v1/agents/${id}/chat`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify({ message, stream: true, history: buildChatHistory(history) }),
    signal: options?.signal,
  })

  await assertStreamResponseOk(response)

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取流式响应')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (value) {
        buffer += decoder.decode(value, { stream: true })
      }
      const handlers: StreamEventHandlers = {
        onDelta,
        onCitations: options?.onCitations,
      }
      const drained = drainSseBuffer(buffer, handlers)
      buffer = drained.rest
      if (drained.finished) {
        return
      }
      if (done) {
        if (buffer.trim()) {
          const status = parseSseEvent(buffer, handlers)
          if (status === 'done') return
        }
        return
      }
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // ignore cancel errors
    }
  }
}

export function deleteAgent(id: number) {
  return http.delete<Result<void>>(`/agents/${id}`)
}

export interface AgentKnowledgeBindingVO {
  id: number
  knowledgeBaseId: number
  topK?: number
  retrievalMode?: string
}

export interface AgentToolBindingVO {
  id: number
  toolId: number
  enabled?: boolean
}

export interface AgentMcpBindingVO {
  id: number
  mcpServerId: number
  mcpServerName?: string
  serverKey?: string
  enabled?: boolean
  toolCatalogJson?: string
}

export interface AgentPublishVO {
  agentId: number
  status: string
  publishedVersionId?: number
  publishedVersionNo?: number
  publishedVersionName?: string
  publishedAt?: string
}

export function listAgentKnowledge(agentId: number) {
  return http.get<Result<AgentKnowledgeBindingVO[]>>(`/agents/${agentId}/knowledge`)
}

export function bindAgentKnowledge(agentId: number, payload: { knowledgeBaseId: number; topK?: number }) {
  return http.post<Result<AgentKnowledgeBindingVO>>(`/agents/${agentId}/knowledge`, payload)
}

export function unbindAgentKnowledge(agentId: number, knowledgeBaseId: number) {
  return http.delete<Result<void>>(`/agents/${agentId}/knowledge/${knowledgeBaseId}`)
}

export function listAgentTools(agentId: number) {
  return http.get<Result<AgentToolBindingVO[]>>(`/agents/${agentId}/tools`)
}

export function bindAgentTool(agentId: number, payload: { toolId: number; enabled?: boolean }) {
  return http.post<Result<AgentToolBindingVO>>(`/agents/${agentId}/tools`, payload)
}

export function unbindAgentTool(agentId: number, toolId: number) {
  return http.delete<Result<void>>(`/agents/${agentId}/tools/${toolId}`)
}

export function listAgentMcp(agentId: number) {
  return http.get<Result<AgentMcpBindingVO[]>>(`/agents/${agentId}/mcp`)
}

export function bindAgentMcp(agentId: number, payload: { mcpServerId: number; enabled?: boolean }) {
  return http.post<Result<AgentMcpBindingVO>>(`/agents/${agentId}/mcp`, payload)
}

export function unbindAgentMcp(agentId: number, mcpServerId: number) {
  return http.delete<Result<void>>(`/agents/${agentId}/mcp/${mcpServerId}`)
}

export interface AgentSubAgentBindingVO {
  id: number
  subAgentId: number
  subAgentName?: string
  enabled?: boolean
  sortOrder?: number
}

export function listAgentSubAgents(agentId: number) {
  return http.get<Result<AgentSubAgentBindingVO[]>>(`/agents/${agentId}/sub-agents`)
}

export function bindAgentSubAgent(
  agentId: number,
  payload: { subAgentId: number; enabled?: boolean; sortOrder?: number },
) {
  return http.post<Result<AgentSubAgentBindingVO>>(`/agents/${agentId}/sub-agents`, payload)
}

export function unbindAgentSubAgent(agentId: number, subAgentId: number) {
  return http.delete<Result<void>>(`/agents/${agentId}/sub-agents/${subAgentId}`)
}

export function getAgentPublishStatus(agentId: number) {
  return http.get<Result<AgentPublishVO>>(`/agents/${agentId}/publish`)
}

export function publishAgent(agentId: number) {
  return http.post<Result<AgentPublishVO>>(`/agents/${agentId}/publish`)
}

export function unpublishAgent(agentId: number) {
  return http.post<Result<AgentPublishVO>>(`/agents/${agentId}/unpublish`)
}

export interface AgentVersionVO {
  id: number
  versionNo: number
  versionName: string
  status: string
  publishedAt?: string
  createdAt: string
  updatedAt: string
  currentDraft: boolean
  published: boolean
}

export interface AgentVersionDiffVO {
  field: string
  label: string
  baseValue: string
  targetValue: string
  changed: boolean
}

export interface AgentVersionCompareVO {
  baseVersionId: number
  targetVersionId: number
  diffs: AgentVersionDiffVO[]
}

export function listAgentVersions(agentId: number) {
  return http.get<Result<AgentVersionVO[]>>(`/agents/${agentId}/versions`)
}

export function compareAgentVersions(agentId: number, baseId: number, targetId: number) {
  return http.get<Result<AgentVersionCompareVO>>(`/agents/${agentId}/versions/compare`, {
    params: { baseId, targetId },
  })
}

export function createAgentVersion(agentId: number, sourceVersionId?: number) {
  return http.post<Result<AgentVersionVO>>(`/agents/${agentId}/versions`, { sourceVersionId })
}

export function restoreAgentVersion(agentId: number, versionId: number) {
  return http.post<Result<void>>(`/agents/${agentId}/versions/${versionId}/restore`)
}

export function archiveAgentVersion(agentId: number, versionId: number) {
  return http.post<Result<AgentVersionVO>>(`/agents/${agentId}/versions/${versionId}/archive`)
}

export function duplicateAgent(agentId: number) {
  return http.post<Result<AgentVO>>(`/agents/${agentId}/duplicate`)
}

export function archiveAgent(agentId: number) {
  return http.post<Result<AgentVO>>(`/agents/${agentId}/archive`)
}
