import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'

const activeProjectId = ref<number | null>(null)

function storageKey(workspaceId: number | null) {
  return workspaceId ? `box.activeProject.${workspaceId}` : null
}

function loadFromStorage(workspaceId: number | null) {
  const key = storageKey(workspaceId)
  if (!key) {
    activeProjectId.value = null
    return
  }
  const raw = localStorage.getItem(key)
  if (!raw) {
    activeProjectId.value = null
    return
  }
  const id = Number(raw)
  activeProjectId.value = Number.isFinite(id) ? id : null
}

function persist(workspaceId: number | null) {
  const key = storageKey(workspaceId)
  if (!key) return
  if (activeProjectId.value == null) {
    localStorage.removeItem(key)
  } else {
    localStorage.setItem(key, String(activeProjectId.value))
  }
}

export function useActiveProject() {
  const auth = useAuthStore()

  watch(
    () => auth.currentWorkspaceId,
    (workspaceId) => {
      loadFromStorage(workspaceId)
    },
    { immediate: true },
  )

  const hasActiveProject = computed(() => activeProjectId.value != null)

  function selectProject(projectId: number | null) {
    activeProjectId.value = projectId
    persist(auth.currentWorkspaceId)
  }

  function clearProject() {
    selectProject(null)
  }

  function syncActiveProject(projectIds: number[]) {
    if (activeProjectId.value == null) {
      return
    }
    if (!projectIds.includes(activeProjectId.value)) {
      clearProject()
    }
  }

  return {
    activeProjectId,
    hasActiveProject,
    selectProject,
    clearProject,
    syncActiveProject,
  }
}
