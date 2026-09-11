import http, { type Result } from './http'

export interface PlatformModelVO {
  id: number
  providerId: number
  providerName: string
  modelCode: string
  modelName: string
  description?: string
  contextWindow?: number
  maxOutputTokens?: number
  supportStreaming?: boolean
  status: number
}

export interface PlatformCapabilities {
  byokEnabled: boolean
}

export function listPlatformModels() {
  return http.get<Result<PlatformModelVO[]>>('/platform/models')
}

export function fetchPlatformCapabilities() {
  return http.get<Result<PlatformCapabilities>>('/platform/capabilities')
}
