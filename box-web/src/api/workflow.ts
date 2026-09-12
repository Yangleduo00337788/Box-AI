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
