import http, { type Result } from './http'

export interface ApiKeyVO {
  id: number
  name: string
  keyPrefix: string
  status: number
  expiresAt?: string
  lastUsedAt?: string
  createdAt: string
}

export interface CreateApiKeyResponse {
  id: number
  name: string
  apiKey: string
}

export function listApiKeys() {
  return http.get<Result<ApiKeyVO[]>>('/api-keys')
}

export function createApiKey(payload: { name: string }) {
  return http.post<Result<CreateApiKeyResponse>>('/api-keys', payload)
}

export function deleteApiKey(id: number) {
  return http.delete<Result<void>>(`/api-keys/${id}`)
}

export function disableApiKey(id: number) {
  return http.post<Result<ApiKeyVO>>(`/api-keys/${id}/disable`)
}

export function enableApiKey(id: number) {
  return http.post<Result<ApiKeyVO>>(`/api-keys/${id}/enable`)
}

export function rotateApiKey(id: number) {
  return http.post<Result<CreateApiKeyResponse>>(`/api-keys/${id}/rotate`)
}
