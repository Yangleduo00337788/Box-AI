import http, { type Result } from './http'
import type { EntityId } from '@/utils/entityId'

export interface TenantMemberVO {
  id: number
  userId: EntityId
  loginName?: string
  email: string
  nickname: string
  roleCode: string
  status: number
  joinedAt?: string
}

export interface EnterpriseOrgAccessVO {
  tenantId: number
  name: string
  orgId: string
  inviteCode: string
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

export function provisionEmployee(payload: {
  account: string
  password: string
  nickname?: string
  email?: string
  roleCode?: string
}) {
  return http.post<Result<TenantMemberVO>>('/tenant/members/accounts', payload)
}

export function resetEmployeePassword(userId: EntityId, password: string) {
  return http.put<Result<void>>(`/tenant/members/${userId}/password`, { password })
}

export function fetchOrgAccess() {
  return http.get<Result<EnterpriseOrgAccessVO>>('/tenant/org-access')
}

export function rotateInviteCode() {
  return http.post<Result<EnterpriseOrgAccessVO>>('/tenant/invite-code/rotate')
}

export function updateOrgId(orgId: string) {
  return http.put<Result<EnterpriseOrgAccessVO>>('/tenant/org-id', { orgId })
}

export function updateTenantMemberStatus(userId: EntityId, status: number) {
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
