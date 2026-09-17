import http, { type Result } from './http'

export interface WorkflowVO {
  id: number
  name: string
  description?: string
  status: string
  draftVersionId?: number
  publishedVersionId?: number
  draftVersionNo?: number
  publishedVersionNo?: number
  definitionJson?: string
  createdAt: string
  updatedAt: string
}

export interface WorkflowDefinition {
  nodes: Array<{
    id: string
    type: string
    label?: string
    position?: { x: number; y: number }
    config?: Record<string, unknown>
    /** @deprecated use config */
    data?: Record<string, unknown>
  }>
  edges: Array<{
    id?: string
    source: string
    target: string
    sourceHandle?: string
    targetHandle?: string
    label?: string
  }>
  variables: Array<{ name: string; type?: string; defaultValue?: string }>
}

export function listWorkflows() {
  return http.get<Result<WorkflowVO[]>>('/workflows')
}

export function getWorkflow(id: number) {
  return http.get<Result<WorkflowVO>>(`/workflows/${id}`)
}

export function createWorkflow(payload: { name: string; description?: string }) {
  return http.post<Result<WorkflowVO>>('/workflows', payload)
}

export function updateWorkflowDefinition(id: number, definitionJson: string) {
  return http.put<Result<WorkflowVO>>(`/workflows/${id}/definition`, { definitionJson })
}

export function validateWorkflow(id: number) {
  return http.post<Result<{ valid: boolean; errors: string[] }>>(`/workflows/${id}/validate`)
}

export interface WorkflowPublishVO {
  workflowId: number
  status: string
  publishedVersionId?: number
  publishedVersionNo?: number
  publishedAt?: string
  webhookToken?: string
  webhookUrl?: string
  webhookSecret?: string
}

export function getWorkflowPublishStatus(id: number) {
  return http.get<Result<WorkflowPublishVO>>(`/workflows/${id}/publish`)
}

export function publishWorkflow(id: number) {
  return http.post<Result<WorkflowPublishVO>>(`/workflows/${id}/publish`)
}

export function deleteWorkflow(id: number) {
  return http.delete<Result<void>>(`/workflows/${id}`)
}

export interface WorkflowNodeTrace {
  nodeId: string
  nodeType: string
  status: string
  durationMs: number
  output?: Record<string, unknown>
  errorMessage?: string
}

export interface WorkflowDebugResult {
  executionId?: number
  executionNo?: string
  status: string
  outputs?: Record<string, unknown>
  nodeTraces?: WorkflowNodeTrace[]
  errorMessage?: string
}

export function runWorkflowDebug(id: number, input: Record<string, unknown>) {
  return http.post<Result<WorkflowDebugResult>>(
    `/workflows/${id}/debug`,
    { inputs: input },
    { timeout: 120000 },
  )
}

export interface WorkflowStreamEvent {
  type: 'node.start' | 'node.delta' | 'node.end' | 'done' | 'error'
  nodeId?: string
  nodeType?: string
  payload?: unknown
  message?: string
}

function workflowAuthHeaders() {
  const token = localStorage.getItem('box.token')
  const workspaceId = localStorage.getItem('box.workspaceId')
  const headers: Record<string, string> = {
    Accept: 'text/event-stream, application/json',
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

export async function runWorkflowDebugStream(
  id: number,
  input: Record<string, unknown>,
  handlers: {
    onNodeStart?: (nodeId: string, nodeType: string) => void
    onNodeDelta?: (nodeId: string, nodeType: string, chunk: string) => void
    onNodeEnd?: (trace: WorkflowNodeTrace) => void
    onDone?: (result: WorkflowDebugResult) => void
  },
  signal?: AbortSignal,
): Promise<WorkflowDebugResult | null> {
  const response = await fetch(`/api/v1/workflows/${id}/debug/stream`, {
    method: 'POST',
    headers: workflowAuthHeaders(),
    body: JSON.stringify({ inputs: input }),
    signal,
  })
  if (!response.ok) {
    throw new Error(`调试失败 HTTP ${response.status}`)
  }
  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法读取调试流')
  }
  const decoder = new TextDecoder()
  let buffer = ''
  let result: WorkflowDebugResult | null = null
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
      const { done, value } = await reader.read()
      if (value) {
        buffer += decoder.decode(value, { stream: true })
      }
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const rawEvent = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        for (const line of rawEvent.split('\n')) {
          if (!line.startsWith('data:')) continue
          const payload = line.slice(5).trim()
          if (!payload) continue
          let event: WorkflowStreamEvent
          try {
            event = JSON.parse(payload) as WorkflowStreamEvent
          } catch {
            continue
          }
          if (event.type === 'node.start' && event.nodeId && event.nodeType) {
            handlers.onNodeStart?.(event.nodeId, event.nodeType)
          }
          if (event.type === 'node.delta' && event.nodeId && event.nodeType && typeof event.payload === 'string') {
            handlers.onNodeDelta?.(event.nodeId, event.nodeType, event.payload)
          }
          if (event.type === 'node.end' && event.nodeId && event.nodeType) {
            const body = (event.payload || {}) as {
              status?: string
              durationMs?: number
              output?: Record<string, unknown>
              errorMessage?: string
            }
            handlers.onNodeEnd?.({
              nodeId: event.nodeId,
              nodeType: event.nodeType,
              status: body.status || 'SUCCEEDED',
              durationMs: body.durationMs || 0,
              output: body.output,
              errorMessage: body.errorMessage,
            })
          }
          if (event.type === 'error') {
            throw new Error(event.message || '工作流调试失败')
          }
          if (event.type === 'done') {
            result = (event.payload || null) as WorkflowDebugResult | null
            handlers.onDone?.(result || { status: 'UNKNOWN' })
            return result
          }
        }
        boundary = buffer.indexOf('\n\n')
      }
      if (done) {
        return result
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
