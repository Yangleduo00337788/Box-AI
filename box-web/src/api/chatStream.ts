import { assertStreamResponseOk } from './apiError'
import type { KnowledgeCitation } from './agent'

export interface ChatToolEventPayload {
  toolKey: string
  arguments?: Record<string, unknown>
  output?: string
  status?: string
}

export interface ChatToolConfirmPayload {
  confirmationToken: string
  toolKey: string
  toolName?: string
  arguments?: Record<string, unknown>
}

export interface ChatStreamEvent {
  type: 'delta' | 'done' | 'error' | 'citations' | 'tool.start' | 'tool.delta' | 'tool.end' | 'tool.confirm'
  content?: string
  message?: string
  executionId?: number
}

export interface StreamEventHandlers {
  onDelta: (chunk: string) => void
  onDone?: (executionId?: number) => void
  onCitations?: (citations: KnowledgeCitation[]) => void
  onToolStart?: (payload: ChatToolEventPayload) => void
  onToolDelta?: (payload: ChatToolEventPayload) => void
  onToolEnd?: (payload: ChatToolEventPayload) => void
  onToolConfirm?: (payload: ChatToolConfirmPayload) => void
}

export function parseToolConfirmPayload(raw?: string): ChatToolConfirmPayload | null {
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as ChatToolConfirmPayload
    if (!parsed.confirmationToken || !parsed.toolKey) return null
    return parsed
  } catch {
    return null
  }
}

export function parseToolPayload(raw?: string): ChatToolEventPayload | null {
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as ChatToolEventPayload
    if (!parsed.toolKey) return null
    return parsed
  } catch {
    return null
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

export function parseSseEvent(rawEvent: string, handlers: StreamEventHandlers): 'continue' | 'done' | 'error' {
  for (const line of rawEvent.split('\n')) {
    if (!line.startsWith('data:')) continue
    const payload = line.slice(5).trim()
    if (!payload) continue
    try {
      const event = JSON.parse(payload) as ChatStreamEvent
      if (event.type === 'delta' && event.content) {
        handlers.onDelta(event.content)
      }
      if (event.type === 'citations' && event.content) {
        handlers.onCitations?.(parseCitationsJson(event.content))
      }
      if (event.type === 'tool.start') {
        const toolPayload = parseToolPayload(event.content)
        if (toolPayload) handlers.onToolStart?.(toolPayload)
      }
      if (event.type === 'tool.delta') {
        const toolPayload = parseToolPayload(event.content)
        if (toolPayload) handlers.onToolDelta?.(toolPayload)
      }
      if (event.type === 'tool.end') {
        const toolPayload = parseToolPayload(event.content)
        if (toolPayload) handlers.onToolEnd?.(toolPayload)
      }
      if (event.type === 'tool.confirm') {
        const confirmPayload = parseToolConfirmPayload(event.content)
        if (confirmPayload) handlers.onToolConfirm?.(confirmPayload)
      }
      if (event.type === 'error') {
        throw new Error(event.message || '流式对话失败')
      }
      if (event.type === 'done') {
        handlers.onDone?.(event.executionId)
        return 'done'
      }
    } catch (error) {
      if (error instanceof Error && error.message !== '流式对话失败') {
        continue
      }
      throw error
    }
  }
  return 'continue'
}

export function drainSseBuffer(
  buffer: string,
  handlers: StreamEventHandlers,
): { rest: string; finished: boolean } {
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

export async function consumeSseStream(
  response: Response,
  handlers: StreamEventHandlers,
  signal?: AbortSignal,
): Promise<void> {
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
