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
  status: string
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

export function fetchPlugins() {
  return http.get<Result<AdminPluginCatalogVO[]>>('/plugins')
}

export function createPlugin(payload: {
  pluginCode: string
  category: string
  title: string
  description?: string
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
    sortOrder?: number
    status?: string
  },
) {
  return http.put<Result<AdminPluginCatalogVO>>(`/plugins/${id}`, payload)
}
