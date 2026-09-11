import { onUnmounted, ref } from 'vue'
import { flowToDefinition } from '@/utils/workflowFlow'
import { updateWorkflowDefinition, type WorkflowDefinition } from '@/api/workflow'

export type AutoSaveStatus = 'idle' | 'pending' | 'saving' | 'saved' | 'error'

export function useWorkflowAutoSave(
  getWorkflowId: () => number,
  getNodes: () => any[],
  getEdges: () => any[],
  getVariables: () => WorkflowDefinition['variables'],
  delayMs = 1500,
) {
  const status = ref<AutoSaveStatus>('idle')
  let timer: ReturnType<typeof setTimeout> | null = null
  let saving = false

  function scheduleAutoSave() {
    status.value = 'pending'
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      void performAutoSave()
    }, delayMs)
  }

  async function performAutoSave() {
    if (saving) return
    saving = true
    status.value = 'saving'
    try {
      const definition = flowToDefinition(getNodes(), getEdges(), getVariables())
      await updateWorkflowDefinition(getWorkflowId(), JSON.stringify(definition))
      status.value = 'saved'
      return true
    } catch {
      status.value = 'error'
      return false
    } finally {
      saving = false
    }
  }

  function cancelAutoSave() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  onUnmounted(() => {
    cancelAutoSave()
  })

  return {
    status,
    scheduleAutoSave,
    performAutoSave,
    cancelAutoSave,
  }
}
