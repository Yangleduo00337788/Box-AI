import http, { type Result } from './http'
import type { AgentVO } from './agent'

export interface AgentTemplateVO {
  id: number
  templateCode: string
  name: string
  description?: string
  avatarUrl?: string
  category?: string
  platformModelId?: number
  platformModelName?: string
  installCount?: number
  status: string
}

export function listMarketTemplates() {
  return http.get<Result<AgentTemplateVO[]>>('/market/templates')
}

export function enableMarketTemplate(id: number) {
  return http.post<Result<AgentVO>>(`/market/templates/${id}/enable`)
}
