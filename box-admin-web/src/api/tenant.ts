import http, { type Result } from './http'

export interface TenantVO {
  id: number
  name: string
  slug: string
  tenantType?: 'PERSONAL' | 'ENTERPRISE'
  planId?: number
  planName?: string
  contactEmail?: string
  status: number
  ownerId?: number
  createdAt?: string
}

export interface QuotaSnapshotVO {
  tenantId: number
  planId: number
  planName: string
  period: string
  quotaAiCalls: number
  quotaTokens: number
  quotaMembers: number
  quotaWorkspaces: number
  usedAiCalls: number
  usedTokens: number
  usedMembers: number
  usedWorkspaces: number
  remainingAiCalls: number | null
  remainingTokens: number | null
  remainingMembers: number | null
  remainingWorkspaces: number | null
}

export interface CreateTenantRequest {
  name: string
  slug?: string
  contactEmail?: string
}

export function fetchTenants() {
  return http.get<Result<TenantVO[]>>('/tenants')
}

export function createTenant(payload: CreateTenantRequest) {
  return http.post<Result<TenantVO>>('/tenants', payload)
}

export function updateTenantStatus(id: number, status: number) {
  return http.put<Result<TenantVO>>(`/tenants/${id}/status`, { status })
}

export function assignTenantPlan(id: number, planId: number) {
  return http.put<Result<TenantVO>>(`/tenants/${id}/plan`, { planId })
}

export function fetchTenantQuota(id: number) {
  return http.get<Result<QuotaSnapshotVO>>(`/tenants/${id}/quota`)
}

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

export function fetchTenantMembers(tenantId: number) {
  return http.get<Result<TenantMemberVO[]>>(`/tenants/${tenantId}/members`)
}

export function addTenantMember(tenantId: number, payload: AddTenantMemberRequest) {
  return http.post<Result<TenantMemberVO>>(`/tenants/${tenantId}/members`, payload)
}

export function updateTenantMemberStatus(tenantId: number, userId: number, status: number) {
  return http.put<Result<TenantMemberVO>>(`/tenants/${tenantId}/members/${userId}/status`, { status })
}
