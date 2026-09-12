import http, { type Result } from './http'

export interface ExecutionVO {
  id: number
  executionNo: string
  executionType: string
  agentId?: number
  agentVersionId?: number
  conversationId?: number
  userId?: number
  status: string
  inputJson?: string
  outputJson?: string
  errorMessage?: string
  startedAt?: string
  finishedAt?: string
  durationMs?: number
  totalTokens?: number
  createdAt: string
}

export function listExecutions(params: {
  limit?: number
  executionType?: string
  status?: string
  agentId?: number
  conversationId?: number
} = {}) {
  return http.get<Result<ExecutionVO[]>>('/executions', {
    params: {
      limit: params.limit ?? 50,
      executionType: params.executionType,
      status: params.status,
      agentId: params.agentId,
      conversationId: params.conversationId,
    },
  })
}

export function getExecution(id: number) {
  return http.get<Result<ExecutionVO>>(`/executions/${id}`)
}

export interface TraceSpanVO {
  spanId: string
  parentSpanId?: string
  spanType: string
  name: string
  status: string
  inputJson?: string
  outputJson?: string
  durationMs?: number
  errorMessage?: string
  startTime?: string
  endTime?: string
}

export interface TraceDetailVO {
  traceId: string
  executionId: number
  status: string
  spans: TraceSpanVO[]
}

export function getExecutionTrace(id: number) {
  return http.get<Result<TraceDetailVO>>(`/executions/${id}/trace`)
}

export function getLatestExecutionByConversation(conversationId: number) {
  return http.get<Result<ExecutionVO>>('/executions/latest', { params: { conversationId } })
}
