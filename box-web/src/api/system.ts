import http, { type Result } from './http'

export interface SystemContentVO {
  supportEmail: string
  about: {
    slogan: string
    productName: string
    positioning: string
    version: string
  }
  legal: {
    privacy: string[]
    terms: string[]
    updatedAt: string
  }
}

export function fetchSystemContent() {
  return http.get<Result<SystemContentVO>>('/system/content')
}
