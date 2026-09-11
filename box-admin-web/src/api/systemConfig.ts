import http, { type Result } from './http'

export interface SystemConfigVO {
  configKey: string
  configValue: string
  description?: string
}

export function fetchSystemConfigs() {
  return http.get<Result<SystemConfigVO[]>>('/system/config')
}

export function upsertSystemConfig(payload: {
  configKey: string
  configValue?: string
  description?: string
}) {
  return http.put<Result<SystemConfigVO>>('/system/config', payload)
}
