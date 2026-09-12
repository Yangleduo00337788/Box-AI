import { assertStreamResponseOk } from './apiError'
import http, { type Result } from './http'
import type { ChatStreamEvent, KnowledgeCitation } from './agent'

export interface ConversationVO {
  id: number
  agentId: number
  agentName?: string
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
  onDone?: (executionId?: number) => void
  onCitations?: (citations: KnowledgeCitation[]) => void
}

function parseSseEvent(rawEvent: string, handlers: StreamEventHandlers): 'continue' | 'done' {
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
      handlers.onDone?.(event.executionId)
      return 'done'
    }
  }
  return 'continue'
}

function drainSseBuffer(
  buffer: string,
  handlers: StreamEventHandlers,
): { rest: string; finished: boolean } {
  let rest = buffer
  let boundary = rest.indexOf('\n\n')
  while (boundary >= 0) {
    const rawEvent = rest.slice(0, boundary)
    rest = rest.slice(boundary + 2)
    if (parseSseEvent(rawEvent, handlers) === 'done') {
      return { rest, finished: true }
    }
    boundary = rest.indexOf('\n\n')
  }
  return { rest, finished: false }
}

export function listConversations() {
  return http.get<Result<ConversationVO[]>>('/conversations')
}

export function createConversation(payload: { agentId: number; title?: string }) {
  return http.post<Result<ConversationVO>>('/conversations', payload)
}

export function getConversation(id: number) {
  return http.get<Result<ConversationVO>>(`/conversations/${id}`)
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

async function consumeSseStream(
  response: Response,
  onDelta: (chunk: string) => void,
  options?: {
    signal?: AbortSignal
    onDone?: (executionId?: number) => void
    onCitations?: (citations: KnowledgeCitation[]) => void
  },
): Promise<void> {
  const signal = options?.signal
  const handlers: StreamEventHandlers = {
    onDelta,
    onDone: options?.onDone,
    onCitations: options?.onCitations,
  }
  await assertStreamResponseOk(response)

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取流式响应')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  const abort = () => {
    try {
      reader.cancel()
    } catch {
      // ignore
    }
  }
  signal?.addEventListener('abort', abort, { once: true })

  try {
    while (true) {
      if (signal?.aborted) {
        throw new DOMException('Aborted', 'AbortError')
      }
      const { done, value } = await reader.read()
      if (value) {
        buffer += decoder.decode(value, { stream: true })
      }
      const drained = drainSseBuffer(buffer, handlers)
      buffer = drained.rest
      if (drained.finished) {
        return
      }
      if (done) {
        if (buffer.trim()) {
          parseSseEvent(buffer, handlers)
        }
        return
      }
    }
  } finally {
    signal?.removeEventListener('abort', abort)
    try {
      await reader.cancel()
    } catch {
      // ignore
    }
  }
}

export async function sendMessageStream(
  id: number,
  message: string,
  onDelta: (chunk: string) => void,
  options?: {
    signal?: AbortSignal
    onDone?: (executionId?: number) => void
    onCitations?: (citations: KnowledgeCitation[]) => void
    platformModelId?: number
  },
): Promise<void> {
  const payload: { message: string; stream: boolean; platformModelId?: number } = {
    message,
    stream: true,
  }
  if (options?.platformModelId != null) {
    payload.platformModelId = options.platformModelId
  }
  const response = await fetch(`/api/v1/conversations/${id}/messages`, {
    method: 'POST',
    headers: authHeaders('text/event-stream, application/json'),
    body: JSON.stringify(payload),
    signal: options?.signal,
  })
  await consumeSseStream(response, onDelta, options)
}

export async function regenerateMessageStream(
  id: number,
  onDelta: (chunk: string) => void,
  options?: {
    signal?: AbortSignal
    onDone?: (executionId?: number) => void
    onCitations?: (citations: KnowledgeCitation[]) => void
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
  await consumeSseStream(response, onDelta, options)
}

export function deleteMessage(conversationId: number, messageId: number) {
  return http.delete<Result<void>>(`/conversations/${conversationId}/messages/${messageId}`)
}
