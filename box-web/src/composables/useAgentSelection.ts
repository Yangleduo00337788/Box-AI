import { computed, ref } from 'vue'
import { listAgents, type AgentVO } from '@/api/agent'
import { fetchSidebarSelection, updateSidebarSelection } from '@/api/sidebar'

const agents = ref<AgentVO[]>([])
const loading = ref(false)
const selectedAgentId = ref<number | null>(null)

export function useAgentSelection() {
  const selectedAgent = computed(() => {
    if (!agents.value.length) return null
    const found = agents.value.find((item) => item.id === selectedAgentId.value)
    return found ?? agents.value[0]
  })

  async function loadSelection() {
    try {
      const { data } = await fetchSidebarSelection()
      selectedAgentId.value = data.data.selectedAgentId
    } catch {
      selectedAgentId.value = null
    }
  }

  async function refresh() {
    loading.value = true
    try {
      await loadSelection()
      const { data } = await listAgents()
      agents.value = data.data || []
      if (!agents.value.length) {
        selectedAgentId.value = null
        await persistSelection(null)
        return
      }
      const exists = agents.value.some((item) => item.id === selectedAgentId.value)
      if (!exists) {
        await selectAgent(agents.value[0].id)
      }
    } finally {
      loading.value = false
    }
  }

  async function reloadSelection() {
    await loadSelection()
  }

  async function persistSelection(id: number | null) {
    await updateSidebarSelection(id)
  }

  async function selectAgent(id: number) {
    selectedAgentId.value = id
    await persistSelection(id)
  }

  function selectAgentByConversation(agentId?: number) {
    if (!agentId) return
    if (agents.value.some((item) => item.id === agentId)) {
      selectAgent(agentId)
    }
  }

  return {
    agents,
    loading,
    selectedAgentId,
    selectedAgent,
    refresh,
    reloadSelection,
    selectAgent,
    selectAgentByConversation,
  }
}
