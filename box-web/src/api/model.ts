import http, { type Result } from './http'

export interface ProviderVO {
  id: number
  providerCode: string
  providerName: string
  providerType: string
  baseUrl?: string
  status: number
}

export interface ModelVO {
  id: number
  providerId: number
  providerName?: string
  modelCode: string
  modelName: string
  modelType: string
  supportStreaming: boolean
  supportToolCalling: boolean
  status: number
}

export interface CredentialVO {
  id: number
  providerId: number
  providerName?: string
  credentialName: string
  maskedApiKey: string
  status: number
}

export function listProviders() {
  return http.get<Result<ProviderVO[]>>('/model-providers')
}

export function createProvider(payload: Omit<ProviderVO, 'id' | 'status'>) {
  return http.post<Result<ProviderVO>>('/model-providers', payload)
}

export function deleteProvider(id: number) {
  return http.delete<Result<void>>(`/model-providers/${id}`)
}

export function listModels() {
  return http.get<Result<ModelVO[]>>('/models')
}

export function createModel(payload: {
  providerId: number
  modelCode: string
  modelName: string
  modelType?: string
  supportStreaming?: boolean
}) {
  return http.post<Result<ModelVO>>('/models', payload)
}

export function deleteModel(id: number) {
  return http.delete<Result<void>>(`/models/${id}`)
}

export function testChat(id: number, message: string) {
  return http.post<Result<{ content: string }>>(`/models/${id}/chat`, { message }, { timeout: 60000 })
}

export function listCredentials() {
  return http.get<Result<CredentialVO[]>>('/model-api-keys')
}

export function createCredential(payload: { providerId: number; credentialName: string; apiKey: string }) {
  return http.post<Result<CredentialVO>>('/model-api-keys', payload)
}

export function deleteCredential(id: number) {
  return http.delete<Result<void>>(`/model-api-keys/${id}`)
}
