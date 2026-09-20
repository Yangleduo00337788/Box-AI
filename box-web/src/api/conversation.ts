import http, { type Result } from './http'
import type { KnowledgeCitation } from './agent'
import {
  consumeSseStream,
  type ChatToolConfirmPayload,
  type ChatToolEventPayload,
  type StreamEventHandlers,
} from './chatStream'

export interface ConversationVO {
  id: number
  agentId: number
  agentName?: string
  projectId?: number | null
  title?: string
  status: string
  messageCount?: number
  lastMessageAt?: string
  createdAt: string
  updatedAt: string
}

export interface MessageVO {
  id: number
  role: string
  content: string
  contentType: string
  sequenceNo: number
  createdAt: string
  metadataJson?: string
  citations?: KnowledgeCitation[]
}

export function parseMessageCitations(metadataJson?: string): KnowledgeCitation[] {
  if (!metadataJson) return []
  try {
    const parsed = JSON.parse(metadataJson) as { citations?: KnowledgeCitation[] }
    return parsed.citations || []
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

export function listConversations(options?: { projectId?: number; unassigned?: boolean }) {
  const params: Record<string, string | number | boolean> = {}
  if (options?.projectId != null) {
    params.projectId = options.projectId
  }
  if (options?.unassigned) {
    params.unassigned = true
  }
  return http.get<Result<ConversationVO[]>>('/conversations', { params })
}

export function createConversation(payload: { agentId: number; title?: string; projectId?: number | null }) {
  return http.post<Result<ConversationVO>>('/conversations', payload)
}

export function moveConversationToProject(id: number, projectId: number | null) {
  return http.put<Result<ConversationVO>>(`/conversations/${id}/project`, { projectId })
}

export function getConversation(id: number) {
  return http.get<Result<ConversationVO>>(`/conversations/${id}`)
}

export function renameConversation(id: number, title: string) {
  return http.put<Result<ConversationVO>>(`/conversations/${id}`, { title })
}

export function deleteConversation(id: number) {
  return http.delete<Result<void>>(`/conversations/${id}`)
}

export function listMessages(id: number) {
  return http.get<Result<MessageVO[]>>(`/conversations/${id}/messages`)
}

export function sendMessage(id: number, message: string) {
  return http.post<Result<{ userMessage: MessageVO; assistantMessage: MessageVO }>>(
    `/conversations/${id}/messages`,
    { message, stream: false },
    { timeout: 60000 },
  )
}

export async function sendMessageStream(
  id: number,
  message: string,
  onDelta: (chunk: string) => void,
  options?: {
    signal?: AbortSignal
    onDone?: (executionId?: number) => void
    onCitations?: (citations: KnowledgeCitation[]) => void
    onToolStart?: (payload: ChatToolEventPayload) => void
    onToolDelta?: (payload: ChatToolEventPayload) => void
    onToolEnd?: (payload: ChatToolEventPayload) => void
    onToolConfirm?: (payload: ChatToolConfirmPayload) => void
    platformModelId?: number
    toolConfirmationToken?: string
    pluginIds?: number[]
  },
): Promise<void> {
  const payload: {
    message: string
    stream: boolean
    platformModelId?: number
    toolConfirmationToken?: string
    pluginIds?: number[]
  } = {
    message,
    stream: true,
  }
  if (options?.platformModelId != null) {
    payload.platformModelId = options.platformModelId
  }
  if (options?.toolConfirmationToken) {
    payload.toolConfirmationToken = options.toolConfirmationToken
  }
  if (options?.pluginIds?.length) {
    payload.pluginIds = options.pluginIds
  }
  const response = await fetch(`/api/v1/conversations/${id}/messages`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify(payload),
    signal: options?.signal,
  })
  const handlers: StreamEventHandlers = {
    onDelta,
    onDone: options?.onDone,
    onCitations: options?.onCitations,
    onToolStart: options?.onToolStart,
    onToolDelta: options?.onToolDelta,
    onToolEnd: options?.onToolEnd,
    onToolConfirm: options?.onToolConfirm,
  }
  await consumeSseStream(response, handlers, options?.signal)
}

export async function regenerateMessageStream(
  id: number,
  onDelta: (chunk: string) => void,
  options?: {
    signal?: AbortSignal
    onDone?: (executionId?: number) => void
    onCitations?: (citations: KnowledgeCitation[]) => void
    onToolStart?: (payload: ChatToolEventPayload) => void
    onToolDelta?: (payload: ChatToolEventPayload) => void
    onToolEnd?: (payload: ChatToolEventPayload) => void
    platformModelId?: number
  },
): Promise<void> {
  const payload: { platformModelId?: number } = {}
  if (options?.platformModelId != null) {
    payload.platformModelId = options.platformModelId
  }
  const response = await fetch(`/api/v1/conversations/${id}/messages/regenerate`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify(payload),
    signal: options?.signal,
  })
  const handlers: StreamEventHandlers = {
    onDelta,
    onDone: options?.onDone,
    onCitations: options?.onCitations,
    onToolStart: options?.onToolStart,
    onToolDelta: options?.onToolDelta,
    onToolEnd: options?.onToolEnd,
  }
  await consumeSseStream(response, handlers, options?.signal)
}

export function deleteMessage(conversationId: number, messageId: number) {
  return http.delete<Result<void>>(`/conversations/${conversationId}/messages/${messageId}`)
}

export interface ConversationShareCreatedVO {
  token: string
  sharePath: string
}

export interface SharedMessageVO {
  role: string
  content: string
}

export interface SharedConversationVO {
  title: string
  sharedAt: string
  userName: string
  agentName: string
  messages: SharedMessageVO[]
}

export function createConversationShare(
  conversationId: number,
  payload: { title?: string; messageIds: number[] },
) {
  return http.post<Result<ConversationShareCreatedVO>>(`/conversations/${conversationId}/shares`, payload)
}

export function getPublicConversationShare(token: string) {
  return http.get<Result<SharedConversationVO>>(`/public/conversation-shares/${token}`)
}

export function submitMessageFeedback(
  conversationId: number,
  messageId: number,
  payload: { rating: 'good' | 'bad'; content?: string },
) {
  return http.post<Result<void>>(`/conversations/${conversationId}/messages/${messageId}/feedback`, payload)
}
