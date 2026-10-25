const CONSUMER_KEY = 'box.demo.world.v1'
const ADMIN_KEY = 'box.demo.admin.v1'

export function loadJson<T>(key: string, fallback: T): T {
  if (typeof sessionStorage === 'undefined') {
    return fallback
  }
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

export function saveJson(key: string, value: unknown) {
  if (typeof sessionStorage === 'undefined') {
    return
  }
  try {
    sessionStorage.setItem(key, JSON.stringify(value))
  } catch {
    // quota exceeded — ignore
  }
}

export function resetDemoPersistence() {
  if (typeof sessionStorage === 'undefined') return
  sessionStorage.removeItem(CONSUMER_KEY)
  sessionStorage.removeItem(ADMIN_KEY)
}

export const persistKeys = { consumer: CONSUMER_KEY, admin: ADMIN_KEY }
