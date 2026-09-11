import http, { type Result } from './http'

export interface SearchResultVO {
  type: string
  id: number
  title: string
  subtitle?: string
  route: string
}

export function searchWorkspace(keyword: string) {
  return http.get<Result<SearchResultVO[]>>('/search', {
    params: { q: keyword.trim() },
  })
}
