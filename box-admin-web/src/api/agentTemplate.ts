import http, { type Result } from './http'

export interface AgentTemplateVO {
  id: number
  templateCode: string
  name: string
  description?: string
  avatarUrl?: string
  category?: string
  systemPrompt?: string
  platformModelId?: number
  platformModelName?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  streamEnabled?: boolean
  status: string
  sortOrder?: number
  installCount?: number
  createdAt?: string
  updatedAt?: string
}

export function fetchAgentTemplates() {
  return http.get<Result<AgentTemplateVO[]>>('/agent-templates')
}

export function createAgentTemplate(payload: {
  templateCode: string
  name: string
  description?: string
  category?: string
  systemPrompt?: string
  platformModelId: number
  temperature?: number
  topP?: number
  maxTokens?: number
  streamEnabled?: boolean
  sortOrder?: number
}) {
  return http.post<Result<AgentTemplateVO>>('/agent-templates', payload)
}

export function updateAgentTemplate(
  id: number,
  payload: {
    name: string
    description?: string
    category?: string
    systemPrompt?: string
    platformModelId: number
    temperature?: number
    topP?: number
    maxTokens?: number
    streamEnabled?: boolean
    sortOrder?: number
  },
) {
  return http.put<Result<AgentTemplateVO>>(`/agent-templates/${id}`, payload)
}

export function updateAgentTemplateStatus(id: number, status: string) {
  return http.put<Result<AgentTemplateVO>>(`/agent-templates/${id}/status`, { status })
}
