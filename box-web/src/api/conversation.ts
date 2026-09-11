import { assertStreamResponseOk } from './apiError'
import http, { type Result } from './http'
import type { ChatStreamEvent } from './agent'

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

function parseSseEvent(rawEvent: string, onDelta: (chunk: string) => void): 'continue' | 'done' {
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
    if (parseSseEvent(rawEvent, onDelta) === 'done') {
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

export async function sendMessageStream(
  id: number,
  message: string,
  onDelta: (chunk: string) => void,
): Promise<void> {
  const response = await fetch(`/api/v1/conversations/${id}/messages`, {
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
          parseSseEvent(buffer, onDelta)
        }
        return
      }
    }
  } finally {
    try {
      await reader.cancel()
    } catch {
      // ignore
    }
  }
}
