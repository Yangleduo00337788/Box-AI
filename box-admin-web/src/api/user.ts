import http, { type Result } from './http'

export interface PlatformUserVO {
  id: number
  username?: string
  email?: string
  nickname?: string
  userType?: string
  platformAdminRole?: string
  status: number
  lastLoginAt?: string
  createdAt?: string
}

export interface PlatformUserContextVO {
  userId: number
  email?: string
  nickname?: string
  primaryTenantName?: string
  tenantType?: string
  workspaceNames: string[]
  monthAiCalls: number
  monthTokens: number
  recentFailureRate: number
}

export interface PlatformUserPage {
  records: PlatformUserVO[]
  total: number
  page: number
  pageSize: number
}

export function listPlatformUsers(params: {
  keyword?: string
  userType?: string
  status?: number
  page?: number
  pageSize?: number
}) {
  return http.get<Result<PlatformUserPage>>('/users', { params })
}

export function updatePlatformUserStatus(id: number, status: number) {
  return http.put<Result<PlatformUserVO>>(`/users/${id}/status`, { status })
}

export function createPlatformAdmin(payload: {
  email: string
  password: string
  nickname?: string
  platformAdminRole?: string
}) {
  return http.post<Result<PlatformUserVO>>('/users', payload)
}

export function fetchUserContext(id: number) {
  return http.get<Result<PlatformUserContextVO>>(`/users/${id}/context`)
}
