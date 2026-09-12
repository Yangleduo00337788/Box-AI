import http, { type Result } from './http'

export interface NotificationVO {
  id: number
  title: string
  content: string
  category: string
  linkUrl?: string
  read: boolean
  createdAt: string
}

export function listNotifications(limit = 20) {
  return http.get<Result<NotificationVO[]>>('/notifications', { params: { limit } })
}

export function getUnreadNotificationCount() {
  return http.get<Result<{ count: number }>>('/notifications/unread-count')
}

export function markNotificationRead(id: number) {
  return http.post<Result<void>>(`/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return http.post<Result<void>>('/notifications/read-all')
}
