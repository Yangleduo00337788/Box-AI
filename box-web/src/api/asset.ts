import http, { type Result } from './http'
import type { AuthVO } from './auth'

export interface ImageAssetVO {
  url: string
}

export function uploadImageAsset(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<ImageAssetVO>>('/assets/images', form)
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<AuthVO>>('/auth/avatar', form)
}
