import http, { type Result } from './http'

export interface NotificationVO {
  id: number
  title: string
  content: string
  category: string
  linkUrl?: string
  /** 未读为 false；缺字段时按已读处理，避免 Jackson 省略 false 导致误判 */
  read?: boolean
  createdAt: string
}

export function isNotificationUnread(item: NotificationVO) {
  return item.read === false
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
