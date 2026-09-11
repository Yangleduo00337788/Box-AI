import { ref } from 'vue'
import { listAgents } from '@/api/agent'
import { listMarketTemplates } from '@/api/market'

export interface ChatSuggestion {
  id: string
  title: string
  subtitle?: string
  prompt: string
  icon: string
}

const SUGGESTION_ICONS = ['user', 'view-module', 'filter', 'edit', 'chart', 'task-checked']

const suggestions = ref<ChatSuggestion[]>([])
const loading = ref(false)

function pickIcon(index: number, category?: string) {
  if (category) {
    const key = category.toLowerCase()
    if (key.includes('写') || key.includes('文案')) return 'edit'
    if (key.includes('分析') || key.includes('数据')) return 'chart'
    if (key.includes('调研') || key.includes('搜索')) return 'filter'
  }
  return SUGGESTION_ICONS[index % SUGGESTION_ICONS.length]
}

function buildPrompt(title: string, description?: string) {
  if (description?.trim()) return description.trim()
  return `请帮我完成：${title}`
}

function buildSubtitle(title: string, description?: string) {
  if (description?.trim()) {
    const text = description.trim()
    return text.length > 42 ? `${text.slice(0, 42)}…` : text
  }
  return `请帮我完成：${title}`
}

export function useChatSuggestions() {
  async function refresh() {
    loading.value = true
    try {
      const [templateRes, agentRes] = await Promise.all([
        listMarketTemplates().catch(() => ({ data: { data: [] as never[] } })),
        listAgents().catch(() => ({ data: { data: [] as never[] } })),
      ])

      const fromTemplates = (templateRes.data.data || []).slice(0, 3).map((item, index) => ({
        id: `template-${item.id}`,
        title: item.name,
        subtitle: buildSubtitle(item.name, item.description),
        prompt: buildPrompt(item.name, item.description),
        icon: pickIcon(index, item.category),
      }))

      if (fromTemplates.length >= 3) {
        suggestions.value = fromTemplates
        return
      }

      const fromAgents = (agentRes.data.data || [])
        .filter((item) => !fromTemplates.some((tpl) => tpl.title === item.name))
        .slice(0, 3 - fromTemplates.length)
        .map((item, index) => ({
          id: `agent-${item.id}`,
          title: item.name,
          subtitle: item.description?.trim()
            ? buildSubtitle(item.name, item.description)
            : `和 ${item.name} 聊聊`,
          prompt: buildPrompt(item.name, item.description),
          icon: pickIcon(fromTemplates.length + index),
        }))

      const merged = [...fromTemplates, ...fromAgents]
      suggestions.value = merged.slice(0, 3)
    } finally {
      loading.value = false
    }
  }

  return { suggestions, loading, refresh }
}
