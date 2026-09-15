import http, { type Result } from './http'
import type { WorkspaceVO } from './auth'

export function fetchWorkspaces() {
  return http.get<Result<WorkspaceVO[]>>('/workspaces')
}

export function createWorkspace(payload: { name: string; description?: string; avatarUrl?: string }) {
  return http.post<Result<WorkspaceVO>>('/workspaces', payload)
}

export function updateWorkspace(
  id: number,
  payload: { name: string; description?: string; avatarUrl?: string; status?: number },
) {
  return http.put<Result<WorkspaceVO>>(`/workspaces/${id}`, payload)
}

export function deleteWorkspace(id: number) {
  return http.delete<Result<void>>(`/workspaces/${id}`)
}

export function fetchCurrentPermissions() {
  return http.get<Result<string[]>>('/workspaces/current-permissions')
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

export interface WorkspaceInvitationVO {
  id: number
  email: string
  roleCode: string
  token?: string
  status: string
  expiresAt?: string
  acceptedAt?: string
  createdAt?: string
}

export function listInvitations() {
  return http.get<Result<WorkspaceInvitationVO[]>>('/workspace/invitations')
}

export function createInvitation(payload: { email: string; roleCode?: string }) {
  return http.post<Result<WorkspaceInvitationVO>>('/workspace/invitations', payload)
}

export function revokeInvitation(id: number) {
  return http.post<Result<void>>(`/workspace/invitations/${id}/revoke`)
}

export function previewInvitation(token: string) {
  return http.get<Result<WorkspaceInvitationVO>>(`/public/invitations/${token}`)
}

export function acceptInvitation(token: string) {
  return http.post<Result<WorkspaceMemberVO>>(`/public/invitations/${token}/accept`)
}
