import {
  appendStreamConversationMessage,
  dispatchConsumerMock,
  finalizeRegenerateAssistantMessage,
} from './consumer/store'
import { buildAssistantReply } from './engine/chatSimulator'
import { createChatSseResponse, createWorkflowDebugSseResponse } from './engine/demoSse'

let fetchPatched = false

function isDemoChatUrl(url: string): boolean {
  return (
    (url.includes('/api/v1/conversations/') &&
      (url.includes('/messages') || url.includes('/messages/regenerate'))) ||
    (url.includes('/api/v1/agents/') && url.includes('/chat')) ||
    (url.includes('/api/v1/published/agents/') && url.includes('/chat'))
  )
}

function isWorkflowDebugStreamUrl(url: string): boolean {
  return url.includes('/api/v1/workflows/') && url.includes('/debug/stream')
}

async function readFetchBody(input: RequestInfo | URL, init?: RequestInit): Promise<unknown> {
  if (init?.body != null) {
    return readBodyValue(init.body)
  }
  if (typeof input !== 'string' && !(input instanceof URL) && input.body != null) {
    return readBodyValue(input.body)
  }
  return undefined
}

async function readBodyValue(body: BodyInit): Promise<unknown> {
  if (typeof body === 'string') {
    try {
      return JSON.parse(body)
    } catch {
      return body
    }
  }
  if (typeof Blob !== 'undefined' && body instanceof Blob) {
    const text = await body.text()
    try {
      return JSON.parse(text)
    } catch {
      return text
    }
  }
  return undefined
}

function userTextFromBody(body: unknown): string {
  if (!body || typeof body !== 'object') return '你好'
  const json = body as { content?: string; message?: string }
  return json.message || json.content || '你好'
}

function wantsStream(body: unknown): boolean {
  if (!body || typeof body !== 'object') return true
  const stream = (body as { stream?: boolean }).stream
  return stream !== false
}

function conversationIdFromUrl(url: string): number | undefined {
  const m = url.match(/\/conversations\/(\d+)\//)
  return m ? Number(m[1]) : undefined
}

function workflowIdFromUrl(url: string): number | undefined {
  const m = url.match(/\/workflows\/(\d+)\//)
  return m ? Number(m[1]) : undefined
}

function resolveUrl(input: RequestInfo | URL): string {
  if (typeof input === 'string') return input
  if (input instanceof URL) return input.href
  return input.url
}

function resolveMethod(input: RequestInfo | URL, init?: RequestInit): string {
  if (init?.method) return init.method.toUpperCase()
  if (typeof input !== 'string' && !(input instanceof URL) && input.method) {
    return input.method.toUpperCase()
  }
  return 'GET'
}

export function installDemoFetchPatch() {
  if (fetchPatched || typeof window === 'undefined') {
    return
  }
  fetchPatched = true

  const native = window.fetch.bind(window)
  window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = resolveUrl(input)
    const method = resolveMethod(input, init)

    if (!url.includes('/api/v1/') || url.includes('/api/v1/admin')) {
      return native(input, init)
    }

    if (isWorkflowDebugStreamUrl(url) && method === 'POST') {
      const wfId = workflowIdFromUrl(url) ?? 0
      return createWorkflowDebugSseResponse(wfId)
    }

    if (isDemoChatUrl(url) && method === 'POST') {
      const body = await readFetchBody(input, init)
      if (!wantsStream(body)) {
        const payload = dispatchConsumerMock(method, url, body)
        return new Response(JSON.stringify(payload), {
          status: 200,
          headers: {
            'Content-Type': 'application/json; charset=utf-8',
            'X-Request-Id': 'box-demo-mock',
          },
        })
      }

      const convId = conversationIdFromUrl(url)
      const userText = userTextFromBody(body)
      const reply = buildAssistantReply(userText)
      const withTools = /订单|工具|tool|mcp/i.test(userText)
      const isRegenerate = url.includes('/messages/regenerate')

      return createChatSseResponse({
        text: reply,
        withTools,
        onFinished: () => {
          if (!convId) return
          if (isRegenerate) {
            finalizeRegenerateAssistantMessage(convId, reply)
          } else {
            appendStreamConversationMessage(convId, userText, reply)
          }
        },
      })
    }

    const body = await readFetchBody(input, init)
    const payload = dispatchConsumerMock(method, url, body)
    return new Response(JSON.stringify(payload), {
      status: 200,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'X-Request-Id': 'box-demo-mock',
      },
    })
  }
}
