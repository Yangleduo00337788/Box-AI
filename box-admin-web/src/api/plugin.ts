import http, { type Result } from './http'

export interface PluginCategoryVO {
  value: string
  label: string
  desc: string
}

export interface AdminPluginCatalogVO {
  id: number
  pluginCode: string
  category: string
  title: string
  description?: string
  manifestJson?: string
  status: string
  reviewStatus?: string
  sourceType?: string
  visibility?: string
  tenantIdsJson?: string | null
  rolloutPercent?: number
  sortOrder: number
  installCount: number
}

export function fetchPluginCategories() {
  return http.get<Result<PluginCategoryVO[]>>('/plugin-categories')
}

export function createPluginCategory(payload: {
  categoryCode: string
  label: string
  description?: string
  sortOrder?: number
}) {
  return http.post<Result<PluginCategoryVO>>('/plugin-categories', payload)
}

export function updatePluginCategory(
  categoryCode: string,
  payload: { label?: string; description?: string; sortOrder?: number; status?: string },
) {
  return http.put<Result<PluginCategoryVO>>(`/plugin-categories/${categoryCode}`, payload)
}

export function deletePluginCategory(categoryCode: string) {
  return http.delete<Result<void>>(`/plugin-categories/${categoryCode}`)
}

export function fetchPlugins(category?: string) {
  return http.get<Result<AdminPluginCatalogVO[]>>('/plugins', { params: category ? { category } : undefined })
}

export function createPlugin(payload: {
  pluginCode: string
  category: string
  title: string
  description?: string
  manifestJson?: string
  sortOrder?: number
}) {
  return http.post<Result<AdminPluginCatalogVO>>('/plugins', payload)
}

export function updatePlugin(
  id: number,
  payload: {
    category?: string
    title?: string
    description?: string
    manifestJson?: string
    sortOrder?: number
    status?: string
  },
) {
  return http.put<Result<AdminPluginCatalogVO>>(`/plugins/${id}`, payload)
}

export function updatePluginReview(id: number, reviewStatus: string) {
  return http.put<Result<AdminPluginCatalogVO>>(`/plugins/${id}/review`, { reviewStatus })
}

export function updatePluginRollout(
  id: number,
  payload: { visibility: string; tenantIds?: string; rolloutPercent?: number },
) {
  return http.put<Result<AdminPluginCatalogVO>>(`/plugins/${id}/rollout`, payload)
}

export function deletePlugin(id: number) {
  return http.delete<Result<void>>(`/plugins/${id}`)
}

export interface PluginCatalogAssetVO {
  storageKey: string
  fileName: string
  size: number
  contentType?: string
}

export function uploadPluginCatalogAsset(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<PluginCatalogAssetVO>>('/plugins/catalog-assets', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
