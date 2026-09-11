import http, { type Result } from './http'

export interface PluginCategoryVO {
  value: string
  label: string
  desc: string
}

export interface PluginCatalogVO {
  id: number
  pluginCode: string
  category: string
  title: string
  description?: string
  installCount?: number
  installed: boolean
}

export function fetchPluginCategories() {
  return http.get<Result<PluginCategoryVO[]>>('/market/plugin-categories')
}

export function listPlugins(category?: string) {
  return http.get<Result<PluginCatalogVO[]>>('/market/plugins', {
    params: category ? { category } : undefined,
  })
}

export function installPlugin(id: number) {
  return http.post<Result<null>>(`/market/plugins/${id}/install`)
}

export function uninstallPlugin(id: number) {
  return http.delete<Result<null>>(`/market/plugins/${id}/install`)
}
