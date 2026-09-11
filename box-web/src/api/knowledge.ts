import http, { type Result } from './http'

export interface KnowledgeBaseVO {
  id: number
  name: string
  description?: string
  icon?: string
  documentCount: number
  chunkCount: number
  status: string
  createdAt: string
  updatedAt: string
}

export interface KnowledgeDocumentVO {
  id: number
  knowledgeBaseId: number
  name: string
  fileName: string
  fileType: string
  fileSize: number
  chunkCount: number
  status: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export function listKnowledgeBases() {
  return http.get<Result<KnowledgeBaseVO[]>>('/knowledge-bases')
}

export function createKnowledgeBase(payload: { name: string; description?: string }) {
  return http.post<Result<KnowledgeBaseVO>>('/knowledge-bases', payload)
}

export function deleteKnowledgeBase(id: number) {
  return http.delete<Result<void>>(`/knowledge-bases/${id}`)
}

export function listKnowledgeDocuments(knowledgeBaseId: number) {
  return http.get<Result<KnowledgeDocumentVO[]>>(`/knowledge-bases/${knowledgeBaseId}/documents`)
}

export function uploadKnowledgeDocument(knowledgeBaseId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<Result<KnowledgeDocumentVO>>(`/knowledge-bases/${knowledgeBaseId}/documents`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function searchKnowledge(knowledgeBaseId: number, query: string, topK = 5) {
  return http.post<Result<Array<{ chunkId: number; content: string; score: number }>>>(
    `/knowledge-bases/${knowledgeBaseId}/search`,
    { query, topK },
  )
}
