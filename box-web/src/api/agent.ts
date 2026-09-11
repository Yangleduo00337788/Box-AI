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
  payload: { memoryEnabled?: boolean; memoryWindowSize?: number },
) {
  return http.put<Result<AgentVO>>(`/agents/${id}/memory`, payload)
}

export function chatAgent(id: number, message: string) {
  return http.post<Result<{ content: string }>>(`/agents/${id}/chat`, { message, stream: false }, { timeout: 60000 })
}

export interface ChatStreamEvent {
  type: 'delta' | 'done' | 'error'
  content?: string
  message?: string
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

function parseSseEvent(rawEvent: string, onDelta: (chunk: string) => void): 'continue' | 'done' | 'error' {
  for (const line of rawEvent.split('\n')) {
    if (!line.startsWith('data:')) continue
    const payload = line.slice(5).trim()
    if (!payload) continue
    const event = JSON.parse(payload) as ChatStreamEvent
    if (event.type === 'delta' && event.content) {
      onDelta(event.content)
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

function drainSseBuffer(buffer: string, onDelta: (chunk: string) => void): { rest: string; finished: boolean } {
  let rest = buffer
  let boundary = rest.indexOf('\n\n')
  while (boundary >= 0) {
    const rawEvent = rest.slice(0, boundary)
    rest = rest.slice(boundary + 2)
    const status = parseSseEvent(rawEvent, onDelta)
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
): Promise<void> {
  const response = await fetch(`/api/v1/agents/${id}/chat`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify({ message, stream: true }),
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
      const drained = drainSseBuffer(buffer, onDelta)
      buffer = drained.rest
      if (drained.finished) {
        return
      }
      if (done) {
        if (buffer.trim()) {
          const status = parseSseEvent(buffer, onDelta)
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

export function getAgentPublishStatus(agentId: number) {
  return http.get<Result<AgentPublishVO>>(`/agents/${agentId}/publish`)
}

export function publishAgent(agentId: number) {
  return http.post<Result<AgentPublishVO>>(`/agents/${agentId}/publish`)
}

export function unpublishAgent(agentId: number) {
  return http.post<Result<AgentPublishVO>>(`/agents/${agentId}/unpublish`)
}
