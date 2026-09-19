import http, { type Result } from './http'

export interface PlatformProviderVO {
  id: number
  providerCode: string
  providerName: string
  providerType: string
  baseUrl?: string
  status: number
}

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

export interface PlatformCredentialVO {
  id: number
  providerId: number
  credentialName: string
  apiKeyMasked: string
  status: number
  lastUsedAt?: string
}

export interface UpstreamModelVO {
  modelCode: string
  modelName: string
  imported: boolean
  contextWindow?: number
  maxOutputTokens?: number
}

export function fetchPlatformProviders() {
  return http.get<Result<PlatformProviderVO[]>>('/platform/providers')
}

export function createPlatformProvider(payload: {
  providerCode: string
  providerName: string
  providerType?: string
  baseUrl?: string
}) {
  return http.post<Result<PlatformProviderVO>>('/platform/providers', payload)
}

export function updatePlatformProvider(
  id: number,
  payload: { providerName?: string; providerType?: string; baseUrl?: string; status?: number },
) {
  return http.put<Result<PlatformProviderVO>>(`/platform/providers/${id}`, payload)
}

export function deletePlatformProvider(id: number) {
  return http.delete<Result<void>>(`/platform/providers/${id}`)
}

export function fetchPlatformModels(providerId?: number) {
  return http.get<Result<PlatformModelVO[]>>('/platform/models', {
    params: providerId ? { providerId } : undefined,
  })
}

export function createPlatformModel(payload: {
  providerId: number
  modelCode: string
  modelName: string
  description?: string
  contextWindow?: number
  maxOutputTokens?: number
  sortOrder?: number
}) {
  return http.post<Result<PlatformModelVO>>('/platform/models', payload)
}

export function updatePlatformModel(
  id: number,
  payload: {
    modelName?: string
    description?: string
    contextWindow?: number
    maxOutputTokens?: number
    sortOrder?: number
    status?: number
  },
) {
  return http.put<Result<PlatformModelVO>>(`/platform/models/${id}`, payload)
}

export function deletePlatformModel(id: number) {
  return http.delete<Result<void>>(`/platform/models/${id}`)
}

export function fetchUpstreamModels(providerId: number) {
  return http.get<Result<UpstreamModelVO[]>>(`/platform/providers/${providerId}/upstream-models`)
}

export function importUpstreamModels(providerId: number, modelCodes: string[]) {
  return http.post<Result<PlatformModelVO[]>>(`/platform/providers/${providerId}/models/import`, {
    modelCodes,
  })
}

export function syncMissingModelLimits(providerId: number) {
  return http.post<Result<number>>(`/platform/providers/${providerId}/models/sync-limits`)
}

export function fetchPlatformCredentials(providerId: number) {
  return http.get<Result<PlatformCredentialVO[]>>(`/platform/providers/${providerId}/credentials`)
}

export interface PlatformOcrDefaultVO {
  configuredPlatformModelId?: number | null
  platformModelId?: number | null
  modelCode?: string
  modelName?: string
  providerName?: string
  runnable: boolean
  resolutionMode: string
  resolutionHint: string
}

export function fetchPlatformOcrDefault() {
  return http.get<Result<PlatformOcrDefaultVO>>('/platform/ocr-default')
}

export function updatePlatformOcrDefault(platformModelId?: number | null) {
  return http.put<Result<PlatformOcrDefaultVO>>('/platform/ocr-default', { platformModelId })
}

export function createPlatformCredential(payload: {
  providerId: number
  credentialName: string
  apiKey: string
}) {
  return http.post<Result<PlatformCredentialVO>>('/platform/credentials', payload)
}

export function deletePlatformCredential(id: number) {
  return http.delete<Result<void>>(`/platform/credentials/${id}`)
}
