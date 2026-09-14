import http, { type Result } from './http'

export interface ImageAssetVO {
  url: string
}

export function uploadAdminImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<ImageAssetVO>>('/assets/images', form)
}
