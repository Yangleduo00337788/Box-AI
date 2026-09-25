import { onUnmounted, ref } from 'vue'
import { flowToDefinition } from '../workflow/workflowFlow'
import type { WorkflowDefinition } from '../types/workflow'

export type AutoSaveStatus = 'idle' | 'pending' | 'saving' | 'saved' | 'error'

export function useWorkflowEditorPersistence(
  getWorkflowId: () => number | null | undefined,
  getNodes: () => unknown[],
  getEdges: () => unknown[],
  getVariables: () => WorkflowDefinition['variables'],
  saveDefinition?: (workflowId: number, definitionJson: string) => Promise<void>,
  delayMs = 1500,
) {
  const status = ref<AutoSaveStatus>('idle')
  let timer: ReturnType<typeof setTimeout> | null = null
  let saving = false

  function scheduleAutoSave() {
    const workflowId = getWorkflowId()
    if (!workflowId || !saveDefinition) {
      status.value = 'idle'
      return
    }
    status.value = 'pending'
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      void performAutoSave()
    }, delayMs)
  }

  async function performAutoSave() {
    const workflowId = getWorkflowId()
    if (!workflowId || !saveDefinition) return false
    if (saving) return false
    saving = true
    status.value = 'saving'
    try {
      const definition = flowToDefinition(getNodes() as never[], getEdges() as never[], getVariables())
      await saveDefinition(workflowId, JSON.stringify(definition))
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
