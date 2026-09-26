import { describe, expect, it } from 'vitest'
import { ref } from 'vue'
import { useWorkflowEditorHistory } from './useWorkflowEditorHistory'

describe('useWorkflowEditorHistory', () => {
  it('undo and redo restore snapshots', () => {
    const nodes = ref([{ id: 'a' }])
    const edges = ref<{ id: string }[]>([])
    const history = useWorkflowEditorHistory(nodes, edges)

    history.recordBeforeChange()
    nodes.value = [{ id: 'b' }]
    expect(history.canUndo.value).toBe(true)

    expect(history.undo()).toBe(true)
    expect(nodes.value).toEqual([{ id: 'a' }])
    expect(history.canRedo.value).toBe(true)

    expect(history.redo()).toBe(true)
    expect(nodes.value).toEqual([{ id: 'b' }])
  })

  it('resetHistory clears stacks', () => {
    const nodes = ref([{ id: 'a' }])
    const edges = ref([])
    const history = useWorkflowEditorHistory(nodes, edges)
    history.recordBeforeChange()
    history.resetHistory()
    expect(history.undo()).toBe(false)
    expect(history.redo()).toBe(false)
  })
})

