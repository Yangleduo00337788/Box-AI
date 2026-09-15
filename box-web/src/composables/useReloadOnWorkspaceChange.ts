import { watch } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useReloadOnWorkspaceChange(reload: () => void | Promise<void>) {
  const auth = useAuthStore()
  watch(
    () => auth.currentWorkspaceId,
    (workspaceId, previousId) => {
      if (!workspaceId || workspaceId === previousId) {
        return
      }
      void reload()
    },
  )
}
