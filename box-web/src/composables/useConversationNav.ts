import { ref } from 'vue'
import { listConversations, type ConversationVO } from '@/api/conversation'

const conversations = ref<ConversationVO[]>([])
const loading = ref(false)

export function useConversationNav() {
  async function refresh() {
    loading.value = true
    try {
      const { data } = await listConversations()
      conversations.value = data.data || []
    } finally {
      loading.value = false
    }
  }

  return { conversations, loading, refresh }
}
