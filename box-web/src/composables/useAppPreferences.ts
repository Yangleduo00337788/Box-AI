import { reactive, watch } from 'vue'
import { fetchPreferences, updatePreferences } from '@/api/auth'

export type ThemeMode = 'light' | 'dark' | 'system'

export interface AppPreferences {
  theme: ThemeMode
  sendWithEnter: boolean
}

const defaults: AppPreferences = {
  theme: 'light',
  sendWithEnter: true,
}

function resolveTheme(mode: ThemeMode) {
  if (mode === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return mode
}

function applyTheme(mode: ThemeMode) {
  const resolved = resolveTheme(mode)
  document.documentElement.dataset.boxTheme = resolved
  document.documentElement.setAttribute('theme-mode', resolved)
  document.documentElement.style.colorScheme = resolved
}

function normalizeTheme(theme?: string): ThemeMode {
  if (theme === 'dark' || theme === 'system') return theme
  return 'light'
}

export const appPreferences = reactive<AppPreferences>({ ...defaults })

let syncing = false
let saveTimer: ReturnType<typeof setTimeout> | null = null

async function persistPreferences() {
  if (syncing) return
  await updatePreferences({
    theme: appPreferences.theme,
    sendWithEnter: appPreferences.sendWithEnter,
  })
}

function schedulePersist() {
  if (syncing) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    persistPreferences().catch(() => {})
  }, 300)
}

export async function loadAppPreferencesFromServer() {
  syncing = true
  try {
    const { data } = await fetchPreferences()
    const prefs = data.data
    appPreferences.theme = normalizeTheme(prefs.theme)
    appPreferences.sendWithEnter = prefs.sendWithEnter !== false
    applyTheme(appPreferences.theme)
  } catch {
    applyTheme(appPreferences.theme)
  } finally {
    syncing = false
  }
}

export function initAppPreferences() {
  applyTheme(appPreferences.theme)

  watch(
    () => appPreferences.theme,
    (theme) => {
      applyTheme(theme)
      schedulePersist()
    },
  )

  watch(
    () => appPreferences.sendWithEnter,
    () => {
      schedulePersist()
    },
  )

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (appPreferences.theme === 'system') {
      applyTheme('system')
    }
  })
}
