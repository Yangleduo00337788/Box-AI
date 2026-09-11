import http, { type Result } from './http'

export interface HttpToolConfigVO {
  method: string
  url: string
  headersJson?: string
  queryParamsJson?: string
  bodyType?: string
  bodyTemplate?: string
  timeoutMs?: number
  allowRedirect?: boolean
}

export interface ToolVO {
  id: number
  name: string
  toolKey: string
  description?: string
  type: string
  inputSchemaJson?: string
  outputSchemaJson?: string
  status: number
  httpConfig?: HttpToolConfigVO
  createdAt: string
  updatedAt: string
}

export function listTools() {
  return http.get<Result<ToolVO[]>>('/tools')
}

export function createTool(payload: {
  name: string
  toolKey: string
  description?: string
  type: string
  httpConfig?: Partial<HttpToolConfigVO>
}) {
  return http.post<Result<ToolVO>>('/tools', payload)
}

export function deleteTool(id: number) {
  return http.delete<Result<void>>(`/tools/${id}`)
}

export function testTool(id: number) {
  return http.post<Result<{ statusCode: number; body: string; durationMs: number }>>(`/tools/${id}/test`)
}
