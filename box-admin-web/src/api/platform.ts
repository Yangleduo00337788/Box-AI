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

export function fetchPlatformModels() {
  return http.get<Result<PlatformModelVO[]>>('/platform/models')
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

export function fetchPlatformCredentials(providerId: number) {
  return http.get<Result<PlatformCredentialVO[]>>(`/platform/providers/${providerId}/credentials`)
}

export function createPlatformCredential(payload: {
  providerId: number
  credentialName: string
  apiKey: string
}) {
  return http.post<Result<PlatformCredentialVO>>('/platform/credentials', payload)
}
