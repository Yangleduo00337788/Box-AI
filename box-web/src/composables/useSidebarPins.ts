import { ref } from 'vue'
import { fetchSidebarPins, replaceSidebarPins } from '@/api/sidebar'

const pinnedAgentIds = ref<number[]>([])
const pinnedConversationIds = ref<number[]>([])
const loading = ref(false)

async function persist() {
  await replaceSidebarPins({
    pinnedAgentIds: pinnedAgentIds.value,
    pinnedConversationIds: pinnedConversationIds.value,
  })
}

export function useSidebarPins() {
  async function refresh() {
    loading.value = true
    try {
      const { data } = await fetchSidebarPins()
      pinnedAgentIds.value = data.data.pinnedAgentIds || []
      pinnedConversationIds.value = data.data.pinnedConversationIds || []
    } catch {
      pinnedAgentIds.value = []
      pinnedConversationIds.value = []
    } finally {
      loading.value = false
    }
  }

  function isAgentPinned(id: number) {
    return pinnedAgentIds.value.includes(id)
  }

  function isConversationPinned(id: number) {
    return pinnedConversationIds.value.includes(id)
  }

  async function toggleAgentPin(id: number) {
    if (isAgentPinned(id)) {
      pinnedAgentIds.value = pinnedAgentIds.value.filter((item) => item !== id)
    } else {
      pinnedAgentIds.value = [id, ...pinnedAgentIds.value.filter((item) => item !== id)]
    }
    await persist()
  }

  async function toggleConversationPin(id: number) {
    if (isConversationPinned(id)) {
      pinnedConversationIds.value = pinnedConversationIds.value.filter((item) => item !== id)
    } else {
      pinnedConversationIds.value = [id, ...pinnedConversationIds.value.filter((item) => item !== id)]
    }
    await persist()
  }

  return {
    pinnedAgentIds,
    pinnedConversationIds,
    loading,
    refresh,
    isAgentPinned,
    isConversationPinned,
    toggleAgentPin,
    toggleConversationPin,
  }
}
