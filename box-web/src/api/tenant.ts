import http, { type Result } from './http'

export interface TenantMemberVO {
  id: number
  userId: number
  email: string
  nickname: string
  roleCode: string
  status: number
  joinedAt?: string
}

export interface AddTenantMemberRequest {
  email: string
  roleCode?: string
}

export function fetchTenantMembers() {
  return http.get<Result<TenantMemberVO[]>>('/tenant/members')
}

export function addTenantMember(payload: AddTenantMemberRequest) {
  return http.post<Result<TenantMemberVO>>('/tenant/members', payload)
}

export function updateTenantMemberStatus(userId: number, status: number) {
  return http.put<Result<TenantMemberVO>>(`/tenant/members/${userId}/status`, { status })
}

export interface TenantVO {
  id: number
  name: string
  slug: string
  tenantType?: 'PERSONAL' | 'ENTERPRISE'
}

export function upgradeEnterprise(companyName: string) {
  return http.post<Result<TenantVO>>('/tenant/upgrade-enterprise', { companyName })
}
