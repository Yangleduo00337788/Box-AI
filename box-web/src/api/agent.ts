import http, { type Result } from './http'
import {
  consumeSseStream,
  type ChatStreamEvent,
  type ChatToolEventPayload,
  type StreamEventHandlers,
} from './chatStream'

export type { ChatStreamEvent, ChatToolEventPayload }

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

export interface AgentEmbedConfigVO {
  themeColor?: string
  logoUrl?: string
  welcomeMessage?: string
  suggestedQuestions?: string[]
  agentName?: string
}

export function getAgentEmbedConfig(id: number) {
  return http.get<Result<AgentEmbedConfigVO>>(`/agents/${id}/embed-config`)
}

export function updateAgentEmbedConfig(id: number, payload: Partial<AgentEmbedConfigVO>) {
  return http.put<Result<AgentEmbedConfigVO>>(`/agents/${id}/embed-config`, payload)
}

export function getPublishedAgentEmbedConfig(id: number) {
  return http.get<Result<AgentEmbedConfigVO>>(`/published/agents/${id}/embed-config`)
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

export async function chatAgentStream(
  id: number,
  message: string,
  onDelta: (chunk: string) => void,
  history: AgentChatHistoryItem[] = [],
  options?: {
    signal?: AbortSignal
    toolConfirmationToken?: string
    onCitations?: (citations: KnowledgeCitation[]) => void
    onToolStart?: (payload: ChatToolEventPayload) => void
    onToolDelta?: (payload: ChatToolEventPayload) => void
    onToolEnd?: (payload: ChatToolEventPayload) => void
    onToolConfirm?: (payload: import('./chatStream').ChatToolConfirmPayload) => void
  },
): Promise<void> {
  const response = await fetch(`/api/v1/agents/${id}/chat`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify({
      message,
      stream: true,
      history: buildChatHistory(history),
      toolConfirmationToken: options?.toolConfirmationToken,
    }),
    signal: options?.signal,
  })

  const handlers: StreamEventHandlers = {
    onDelta,
    onCitations: options?.onCitations,
    onToolStart: options?.onToolStart,
    onToolDelta: options?.onToolDelta,
    onToolEnd: options?.onToolEnd,
    onToolConfirm: options?.onToolConfirm,
  }
  await consumeSseStream(response, handlers, options?.signal)
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
  requireConfirmation?: boolean
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

export function bindAgentTool(
  agentId: number,
  payload: { toolId: number; enabled?: boolean; requireConfirmation?: boolean },
) {
  return http.post<Result<AgentToolBindingVO>>(`/agents/${agentId}/tools`, payload)
}

export function updateAgentToolBinding(
  agentId: number,
  toolId: number,
  payload: { enabled?: boolean; requireConfirmation?: boolean },
) {
  return http.put<Result<AgentToolBindingVO>>(`/agents/${agentId}/tools/${toolId}`, payload)
}

export function confirmAgentTool(agentId: number, confirmationToken: string) {
  return http.post<Result<{ toolKey: string; toolName: string; output: string }>>(
    `/agents/${agentId}/tools/confirm`,
    { confirmationToken },
  )
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
