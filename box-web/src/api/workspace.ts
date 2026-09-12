import http, { type Result } from './http'
import type { WorkspaceVO } from './auth'

export function createWorkspace(payload: { name: string; description?: string }) {
  return http.post<Result<WorkspaceVO>>('/workspaces', payload)
}

export interface WorkspaceMemberVO {
  id: number
  userId: number
  email: string
  nickname?: string
  roleCode: string
  status: number
  joinedAt?: string
}

export function listWorkspaceMembers() {
  return http.get<Result<WorkspaceMemberVO[]>>('/workspace/members')
}

export function inviteWorkspaceMember(payload: { email: string; roleCode?: string }) {
  return http.post<Result<WorkspaceMemberVO>>('/workspace/members', payload)
}

export function updateWorkspaceMemberRole(userId: number, payload: { roleCode: string }) {
  return http.put<Result<WorkspaceMemberVO>>(`/workspace/members/${userId}/role`, payload)
}

export function removeWorkspaceMember(userId: number) {
  return http.delete<Result<void>>(`/workspace/members/${userId}`)
}
