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

export function listExecutions(limit = 50) {
  return http.get<Result<ExecutionVO[]>>('/executions', { params: { limit } })
}

export function getExecution(id: number) {
  return http.get<Result<ExecutionVO>>(`/executions/${id}`)
}
