import { ref } from 'vue'
import { listProjects, type ChatProjectVO } from '@/api/project'

const projects = ref<ChatProjectVO[]>([])
const loading = ref(false)

export function useProjectNav() {
  async function refresh() {
    loading.value = true
    try {
      const { data } = await listProjects()
      projects.value = data.data || []
    } finally {
      loading.value = false
    }
  }

  return { projects, loading, refresh }
}
