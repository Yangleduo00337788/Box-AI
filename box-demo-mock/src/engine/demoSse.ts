import { demoToolEvents } from './chatSimulator'

const encoder = new TextEncoder()

export interface ChatSseOptions {
  text: string
  withTools?: boolean
  executionId?: number
  chunkChars?: number
  chunkDelayMs?: number
  onFinished?: () => void
}

function sleep(ms: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

function chatSseLines(text: string, withTools: boolean, executionId: number, step = 3): string[] {
  const lines: string[] = []
  for (let i = 0; i < text.length; i += step) {
    lines.push(`data: ${JSON.stringify({ type: 'delta', content: text.slice(i, i + step) })}\n\n`)
  }
  if (withTools) {
    for (const tool of demoToolEvents()) {
      lines.push(`data: ${JSON.stringify(tool)}\n\n`)
    }
  }
  lines.push(
    `data: ${JSON.stringify({
      type: 'citations',
      content: JSON.stringify([
        {
          index: 1,
          chunkId: 1,
          documentId: 8001,
          documentName: '产品 FAQ.pdf',
          content: '企业版治理能力（演示引用）',
          score: 0.9,
        },
      ]),
    })}\n\n`,
  )
  lines.push(`data: ${JSON.stringify({ type: 'done', executionId })}\n\n`)
  return lines
}

/** 分片推送 SSE，模拟真实打字机流式 */
export function createChatSseStream(options: ChatSseOptions): ReadableStream<Uint8Array> {
  const {
    text,
    withTools = false,
    executionId = 99001,
    chunkChars = 3,
    chunkDelayMs = 22,
    onFinished,
  } = options

  const events = chatSseLines(text, withTools, executionId, chunkChars)
  let index = 0
  let closed = false

  return new ReadableStream({
    async pull(controller) {
      if (index >= events.length) {
        if (!closed) {
          closed = true
          onFinished?.()
          controller.close()
        }
        return
      }
      if (index > 0) {
        await sleep(chunkDelayMs)
      }
      controller.enqueue(encoder.encode(events[index]))
      index += 1
    },
    cancel() {
      if (!closed) {
        closed = true
        onFinished?.()
      }
    },
  })
}

export function createChatSseResponse(options: ChatSseOptions): Response {
  return new Response(createChatSseStream(options), {
    status: 200,
    headers: {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
      'X-Request-Id': 'box-demo-mock',
    },
  })
}

export function createWorkflowDebugSseStream(workflowId: number): ReadableStream<Uint8Array> {
  const nodes = [
    { id: 'start', type: 'START' },
    { id: 'kb', type: 'KNOWLEDGE_RETRIEVAL' },
    { id: 'llm', type: 'LLM' },
    { id: 'end', type: 'END' },
  ]
  const lines: string[] = []
  for (const node of nodes) {
    lines.push(`data: ${JSON.stringify({ type: 'node.start', nodeId: node.id, nodeType: node.type })}\n\n`)
    if (node.type === 'LLM') {
      const answer = `工作流 #${workflowId} 调试输出：已检索知识库并完成 LLM 汇总（演示）。`
      for (let i = 0; i < answer.length; i += 4) {
        lines.push(
          `data: ${JSON.stringify({
            type: 'node.delta',
            nodeId: node.id,
            nodeType: node.type,
            payload: answer.slice(i, i + 4),
          })}\n\n`,
        )
      }
    }
    lines.push(
      `data: ${JSON.stringify({
        type: 'node.end',
        nodeId: node.id,
        nodeType: node.type,
        payload: { status: 'SUCCEEDED', durationMs: 180 + nodes.indexOf(node) * 90, output: { ok: true } },
      })}\n\n`,
    )
  }
  lines.push(
    `data: ${JSON.stringify({
      type: 'done',
      payload: {
        status: 'SUCCEEDED',
        output: { answer: '工作流调试完成（演示）' },
        durationMs: 1200,
        nodeTraces: nodes.map((n) => ({
          nodeId: n.id,
          nodeType: n.type,
          status: 'SUCCEEDED',
          durationMs: 200,
        })),
      },
    })}\n\n`,
  )

  let index = 0
  return new ReadableStream({
    async pull(controller) {
      if (index >= lines.length) {
        controller.close()
        return
      }
      if (index > 0) {
        await sleep(28)
      }
      controller.enqueue(encoder.encode(lines[index]))
      index += 1
    },
  })
}

export function createWorkflowDebugSseResponse(workflowId: number): Response {
  return new Response(createWorkflowDebugSseStream(workflowId), {
    status: 200,
    headers: {
      'Content-Type': 'text/event-stream; charset=utf-8',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
      'X-Request-Id': 'box-demo-mock',
    },
  })
}
