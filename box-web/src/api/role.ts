import http, { type Result } from './http'

export interface PermissionVO {
  id: number
  permissionCode: string
  permissionName: string
  resourceType?: string
  action?: string
}

export interface RoleVO {
  id: number
  roleCode: string
  roleName: string
  description?: string
  builtIn: boolean
  permissionCodes: string[]
}

export function listRoles() {
  return http.get<Result<RoleVO[]>>('/roles')
}

export function listPermissions() {
  return http.get<Result<PermissionVO[]>>('/roles/permissions')
}

export function createRole(payload: { roleName: string; description?: string; permissionCodes: string[] }) {
  return http.post<Result<RoleVO>>('/roles', payload)
}

export function updateRole(
  id: number,
  payload: { roleName: string; description?: string; permissionCodes: string[] },
) {
  return http.put<Result<RoleVO>>(`/roles/${id}`, payload)
}

export function deleteRole(id: number) {
  return http.delete<Result<void>>(`/roles/${id}`)
}
