import http, { type Result } from './http'

export interface ChatProjectVO {
  id: number
  name: string
  conversationCount?: number
  createdAt: string
  updatedAt: string
}

export function listProjects() {
  return http.get<Result<ChatProjectVO[]>>('/projects')
}

export function createProject(name: string) {
  return http.post<Result<ChatProjectVO>>('/projects', { name })
}

export function renameProject(id: number, name: string) {
  return http.put<Result<ChatProjectVO>>(`/projects/${id}`, { name })
}

export function deleteProject(id: number) {
  return http.delete<Result<void>>(`/projects/${id}`)
}
