import { computed, ref, type Ref } from 'vue'

interface FlowSnapshot {
  nodes: any[]
  edges: any[]
}

export function useWorkflowEditorHistory(nodes: Ref<any[]>, edges: Ref<any[]>, maxDepth = 50) {
  const past = ref<FlowSnapshot[]>([])
  const future = ref<FlowSnapshot[]>([])

  function snapshot(): FlowSnapshot {
    return {
      nodes: JSON.parse(JSON.stringify(nodes.value)),
      edges: JSON.parse(JSON.stringify(edges.value)),
    }
  }

  function applySnapshot(state: FlowSnapshot) {
    nodes.value = state.nodes
    edges.value = state.edges
  }

  function recordBeforeChange() {
    past.value.push(snapshot())
    if (past.value.length > maxDepth) {
      past.value.shift()
    }
    future.value = []
  }

  function resetHistory() {
    past.value = []
    future.value = []
  }

  function undo() {
    if (!past.value.length) return false
    future.value.unshift(snapshot())
    const previous = past.value.pop()!
    applySnapshot(previous)
    return true
  }

  function redo() {
    if (!future.value.length) return false
    past.value.push(snapshot())
    const next = future.value.shift()!
    applySnapshot(next)
    return true
  }

  const canUndo = computed(() => past.value.length > 0)
  const canRedo = computed(() => future.value.length > 0)

  return {
    recordBeforeChange,
    resetHistory,
    undo,
    redo,
    canUndo,
    canRedo,
  }
}
