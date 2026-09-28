import http, { type Result } from './http'

export interface NotificationVO {
  id: number | null
  key?: string
  title: string
  content: string
  category: string
  linkUrl?: string
  linkLabel?: string
  dismissible?: boolean
  /** 未读为 false；缺字段时按已读处理，避免 Jackson 省略 false 导致误判 */
  read?: boolean
  createdAt: string
}

export function notificationItemKey(item: NotificationVO) {
  return item.key || `id:${item.id}`
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

export function markNotificationReadByKey(key: string) {
  return http.post<Result<void>>('/notifications/read-by-key', { key })
}

export function dismissNotificationByKey(key: string) {
  return http.post<Result<void>>('/notifications/dismiss', { key })
}

export function markAllNotificationsRead() {
  return http.post<Result<void>>('/notifications/read-all')
}
