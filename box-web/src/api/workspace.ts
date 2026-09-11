import http, { type Result } from './http'
import type { WorkspaceVO } from './auth'

export function listWorkspaces() {
  return http.get<Result<WorkspaceVO[]>>('/workspaces')
}

export function createWorkspace(payload: { name: string; description?: string }) {
  return http.post<Result<WorkspaceVO>>('/workspaces', payload)
}
