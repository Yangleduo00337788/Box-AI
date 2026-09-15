import http, { type Result } from './http'

export interface KnowledgeBaseVO {
  id: number
  name: string
  description?: string
  icon?: string
  embeddingModelId?: number
  rerankModelId?: number
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

export function importKnowledgeUrl(knowledgeBaseId: number, url: string, syncCron?: string) {
  return http.post<Result<KnowledgeDocumentVO>>(`/knowledge-bases/${knowledgeBaseId}/documents/import-url`, {
    url,
    syncCron,
  })
}

export interface KnowledgeSearchHit {
  chunkId: number
  documentId: number
  chunkIndex: number
  content: string
  score: number
}

export function searchKnowledge(knowledgeBaseId: number, query: string, topK = 5) {
  return http.post<Result<KnowledgeSearchHit[]>>(
    `/knowledge-bases/${knowledgeBaseId}/search`,
    { query, topK },
  )
}

export interface KnowledgeTestAnswerVO {
  answer: string
  citations: KnowledgeSearchHit[]
}

export function testKnowledgeAnswer(knowledgeBaseId: number, query: string, topK = 5) {
  return http.post<Result<KnowledgeTestAnswerVO>>(
    `/knowledge-bases/${knowledgeBaseId}/test-answer`,
    { query, topK },
  )
}

export function retryKnowledgeDocument(documentId: number) {
  return http.post<Result<KnowledgeDocumentVO>>(`/documents/${documentId}/retry`)
}

export interface KnowledgeChunkVO {
  id: number
  chunkIndex: number
  content: string
  tokenCount?: number
  status?: string
}

export function listDocumentChunks(documentId: number) {
  return http.get<Result<KnowledgeChunkVO[]>>(`/documents/${documentId}/chunks`)
}
