import http, { type Result } from './http'

export interface AdminMessageFeedbackVO {
  id: number
  workspaceId: number
  conversationId: number
  messageId: number
  userId: number
  userEmail?: string
  userNickname?: string
  rating: string
  content?: string
  status?: string
  messageExcerpt?: string
  adminReply?: string
  adminReplyBy?: number
  adminRepliedAt?: string
  createdAt: string
}

export interface MessageFeedbackPage {
  records: AdminMessageFeedbackVO[]
  total: number
  page: number
  pageSize: number
}

export function listMessageFeedbacks(params: {
  rating?: string
  status?: string
  page?: number
  pageSize?: number
}) {
  return http.get<Result<MessageFeedbackPage>>('/message-feedbacks', { params })
}

export interface AdminMessageFeedbackDetailVO extends AdminMessageFeedbackVO {
  messageContent?: string
}

export function getMessageFeedbackDetail(id: number) {
  return http.get<Result<AdminMessageFeedbackDetailVO>>(`/message-feedbacks/${id}`)
}

export function replyMessageFeedback(id: number, reply: string) {
  return http.post<Result<void>>(`/message-feedbacks/${id}/reply`, { reply })
}
