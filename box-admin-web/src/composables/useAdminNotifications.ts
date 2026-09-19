import { onMounted, onUnmounted, ref, type Ref } from 'vue'
import {
  dismissAdminNotification,
  fetchAdminInbox,
  markAdminNotificationRead,
  markAllAdminNotificationsRead,
  type AdminInboxItem,
} from '@/api/adminNotification'

const POLL_MS = 20_000

let pollTimer: ReturnType<typeof setInterval> | undefined
let subscriberCount = 0
const unreadCount = ref(0)
const items = ref<AdminInboxItem[]>([])

async function fetchInbox() {
  try {
    const { data } = await fetchAdminInbox()
    unreadCount.value = data.data?.unreadCount || 0
    items.value = data.data?.items || []
  } catch {
    /* 未登录或网络异常时忽略 */
  }
}

function onWindowFocus() {
  void fetchInbox()
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void fetchInbox()
  }
}

function startPolling() {
  subscriberCount += 1
  if (subscriberCount > 1) return
  void fetchInbox()
  pollTimer = setInterval(() => void fetchInbox(), POLL_MS)
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)
}

function stopPolling() {
  subscriberCount = Math.max(0, subscriberCount - 1)
  if (subscriberCount > 0) return
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
  window.removeEventListener('focus', onWindowFocus)
  document.removeEventListener('visibilitychange', onVisibilityChange)
}

export function useAdminNotifications(): {
  unreadCount: Ref<number>
  items: Ref<AdminInboxItem[]>
  refreshInbox: () => Promise<void>
  markItemRead: (key: string) => Promise<void>
  markAllRead: () => Promise<void>
  dismissItem: (key: string) => Promise<void>
} {
  onMounted(startPolling)
  onUnmounted(stopPolling)

  async function markItemRead(key: string) {
    await markAdminNotificationRead(key)
    await fetchInbox()
  }

  async function markAllRead() {
    await markAllAdminNotificationsRead()
    await fetchInbox()
  }

  async function dismissItem(key: string) {
    await dismissAdminNotification(key)
    await fetchInbox()
  }

  return {
    unreadCount,
    items,
    refreshInbox: fetchInbox,
    markItemRead,
    markAllRead,
    dismissItem,
  }
}
