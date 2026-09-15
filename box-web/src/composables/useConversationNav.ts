import { ref } from 'vue'
import { listConversations, type ConversationVO } from '@/api/conversation'

const conversations = ref<ConversationVO[]>([])
const projectConversations = ref<ConversationVO[]>([])
const catalog = ref<ConversationVO[]>([])
const loading = ref(false)
const projectLoading = ref(false)

export function useConversationNav() {
  async function refresh(options?: { projectId?: number; unassigned?: boolean; all?: boolean }) {
    if (options?.projectId != null) {
      projectLoading.value = true
      try {
        const { data } = await listConversations({ projectId: options.projectId })
        projectConversations.value = data.data || []
      } finally {
        projectLoading.value = false
      }
      return
    }
    loading.value = true
    try {
      const query = options?.all ? undefined : { unassigned: true as const }
      const { data } = await listConversations(query)
      conversations.value = data.data || []
    } finally {
      loading.value = false
    }
  }

  async function refreshProjectConversations(projectId: number | null) {
    if (projectId == null) {
      projectConversations.value = []
      return
    }
    await refresh({ projectId })
  }

  async function refreshCatalog() {
    const { data } = await listConversations()
    catalog.value = data.data || []
  }

  return {
    conversations,
    projectConversations,
    catalog,
    loading,
    projectLoading,
    refresh,
    refreshProjectConversations,
    refreshCatalog,
  }
}
