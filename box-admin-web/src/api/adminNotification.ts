import http, { type Result } from './http'

export interface AdminInboxItem {
  key: string
  type: 'TASK' | 'ANNOUNCEMENT'
  title: string
  body?: string
  linkUrl?: string
  linkLabel?: string
  theme?: string
  dismissible: boolean
  read: boolean
  createdAt?: string
}

export interface AdminInbox {
  unreadCount: number
  items: AdminInboxItem[]
}

export function fetchAdminInbox() {
  return http.get<Result<AdminInbox>>('/notifications/inbox')
}

export function markAdminNotificationRead(key: string) {
  return http.post<Result<void>>('/notifications/read', { key })
}

export function markAllAdminNotificationsRead() {
  return http.post<Result<void>>('/notifications/read-all')
}

export function dismissAdminNotification(key: string) {
  return http.post<Result<void>>('/notifications/dismiss', { key })
}
