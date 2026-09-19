import { onMounted, onUnmounted, ref, type Ref } from 'vue'
import { getUnreadNotificationCount } from '@/api/notification'

/** 轮询间隔：非 WebSocket，窗口聚焦时会立即刷新 */
const POLL_MS = 20_000

let pollTimer: ReturnType<typeof setInterval> | undefined
let subscriberCount = 0
let fetchGeneration = 0

const unreadCount = ref(0)

async function fetchUnread() {
  const token = localStorage.getItem('box.token')
  if (!token) {
    unreadCount.value = 0
    return
  }

  const generation = ++fetchGeneration
  try {
    const { data } = await getUnreadNotificationCount()
    if (generation !== fetchGeneration) {
      return
    }
    unreadCount.value = data.data?.count || 0
  } catch {
    /* 未登录或网络异常时忽略 */
  }
}

function onWindowFocus() {
  void fetchUnread()
}

function startPolling() {
  subscriberCount += 1
  if (subscriberCount > 1) return
  void fetchUnread()
  pollTimer = setInterval(() => void fetchUnread(), POLL_MS)
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

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    void fetchUnread()
  }
}

/** C 端站内信未读数（轮询 + 聚焦刷新，全站单一 ref） */
export function useNotificationUnread(): { unreadCount: Ref<number>; refreshUnread: () => Promise<void> } {
  onMounted(startPolling)
  onUnmounted(stopPolling)

  return {
    unreadCount,
    refreshUnread: fetchUnread,
  }
}
