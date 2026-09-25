import http, { type Result } from './http'

export interface PluginCatalogAssetVO {
  storageKey: string
  fileName: string
  size: number
  contentType?: string
}

export function uploadPluginCatalogAsset(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<PluginCatalogAssetVO>>('/market/plugin-catalog-assets', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
