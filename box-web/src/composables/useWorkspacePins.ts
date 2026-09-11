import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'

const STORAGE_PREFIX = 'box.sidebar.pinnedWorkspaceIds'

const pinnedWorkspaceIds = ref<number[]>([])

function storageKey(userId: number) {
  return `${STORAGE_PREFIX}.${userId}`
}

function readPinnedWorkspaceIds(userId: number | undefined) {
  if (!userId) {
    return []
  }
  try {
    const raw = localStorage.getItem(storageKey(userId))
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter((id) => Number.isFinite(Number(id))).map(Number) : []
  } catch {
    return []
  }
}

function writePinnedWorkspaceIds(userId: number | undefined, ids: number[]) {
  if (!userId) return
  localStorage.setItem(storageKey(userId), JSON.stringify(ids))
}

export function useWorkspacePins() {
  const auth = useAuthStore()

  function syncFromStorage() {
    pinnedWorkspaceIds.value = readPinnedWorkspaceIds(auth.user?.id)
  }

  watch(
    () => auth.user?.id,
    () => syncFromStorage(),
    { immediate: true },
  )

  function isWorkspacePinned(id: number) {
    return pinnedWorkspaceIds.value.includes(id)
  }

  function toggleWorkspacePin(id: number) {
    if (isWorkspacePinned(id)) {
      pinnedWorkspaceIds.value = pinnedWorkspaceIds.value.filter((item) => item !== id)
    } else {
      pinnedWorkspaceIds.value = [id, ...pinnedWorkspaceIds.value.filter((item) => item !== id)]
    }
    writePinnedWorkspaceIds(auth.user?.id, pinnedWorkspaceIds.value)
  }

  return {
    pinnedWorkspaceIds,
    isWorkspacePinned,
    toggleWorkspacePin,
    refresh: syncFromStorage,
  }
}
