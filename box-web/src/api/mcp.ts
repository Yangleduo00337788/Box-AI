import http, { type Result } from './http'

export interface McpServerVO {
  id: number
  name: string
  serverKey: string
  description?: string
  transportType: string
  endpointUrl: string
  authType: string
  toolCatalogJson?: string
  status: number
  lastSyncAt?: string
  createdAt: string
  updatedAt: string
}

export function listMcpServers() {
  return http.get<Result<McpServerVO[]>>('/mcp-servers')
}

export function createMcpServer(payload: {
  name: string
  serverKey: string
  description?: string
  endpointUrl: string
  transportType?: string
}) {
  return http.post<Result<McpServerVO>>('/mcp-servers', payload)
}

export function deleteMcpServer(id: number) {
  return http.delete<Result<void>>(`/mcp-servers/${id}`)
}

export function syncMcpServer(id: number) {
  return http.post<Result<McpServerVO>>(`/mcp-servers/${id}/sync`)
}
