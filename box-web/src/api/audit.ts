import http, { type Result } from './http'

export interface AuditLogVO {
  id: number
  workspaceId: number
  userId?: number
  action: string
  resourceType: string
  resourceId?: string
  resourceName?: string
  result: string
  ipAddress?: string
  traceId?: string
  detail?: string
  createdAt: string
}

export interface AuditLogPage {
  records: AuditLogVO[]
  total: number
  page: number
  pageSize: number
}

export function listAuditLogs(params: {
  action?: string
  resourceType?: string
  userId?: number
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}) {
  return http.get<Result<AuditLogPage>>('/audit-logs', { params })
}
