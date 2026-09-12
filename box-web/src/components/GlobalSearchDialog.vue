<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="搜索"
    :footer="false"
    width="560px"
    class="global-search-dialog"
    @opened="focusInput"
  >
    <t-input
      ref="inputRef"
      v-model="keyword"
      placeholder="搜索智能体、对话、工作流、知识库、工具…"
      clearable
      autofocus
      @keydown.down.prevent="moveSelection(1)"
      @keydown.up.prevent="moveSelection(-1)"
      @keydown.enter.prevent="openSelected"
    >
      <template #prefix-icon>
        <t-icon name="search" />
      </template>
    </t-input>

    <div v-if="loading" class="global-search__loading">
      <t-loading size="small" />
    </div>

    <div v-else-if="!keyword.trim()" class="global-search__hint">
      输入关键词搜索，或按 <kbd>Ctrl</kbd> + <kbd>K</kbd> 快速打开
    </div>

    <div v-else-if="!results.length" class="global-search__empty">
      <t-empty description="未找到相关内容" />
    </div>

    <ul v-else class="global-search__list" role="listbox">
      <li
        v-for="(item, index) in results"
        :key="`${item.type}-${item.id}`"
        class="global-search__item"
        :class="{ 'global-search__item--active': index === selectedIndex }"
        role="option"
        @mouseenter="selectedIndex = index"
        @click="openResult(item)"
      >
        <t-icon :name="iconForType(item.type)" class="global-search__icon" />
        <div class="global-search__meta">
          <span class="global-search__label">{{ item.title }}</span>
          <span class="global-search__type">{{ typeLabel(item.type) }}</span>
        </div>
        <span v-if="item.subtitle" class="global-search__hint-text">{{ item.subtitle }}</span>
      </li>
    </ul>
  </t-dialog>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { searchWorkspace, type SearchResultVO } from '@/api/search'
import { useAgentSelection } from '@/composables/useAgentSelection'

const visible = defineModel<boolean>('visible', { default: false })

const router = useRouter()
const { selectAgent } = useAgentSelection()
const keyword = ref('')
const loading = ref(false)
const selectedIndex = ref(0)
const results = ref<SearchResultVO[]>([])
const inputRef = ref<{ $el?: HTMLElement } | null>(null)

let searchTimer: ReturnType<typeof setTimeout> | null = null

watch(keyword, (value) => {
  selectedIndex.value = 0
  if (searchTimer) clearTimeout(searchTimer)
  if (!value.trim()) {
    results.value = []
    return
  }
  searchTimer = setTimeout(() => {
    runSearch(value.trim())
  }, 200)
})

watch(visible, (open) => {
  if (!open) {
    keyword.value = ''
    results.value = []
    selectedIndex.value = 0
  }
})

async function runSearch(q: string) {
  loading.value = true
  try {
    const { data } = await searchWorkspace(q)
    results.value = data.data || []
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    AGENT: '智能体',
    CONVERSATION: '对话',
    PLUGIN: '插件',
    WORKFLOW: '工作流',
    KNOWLEDGE: '知识库',
    TOOL: '工具',
  }
  return map[type] || type
}

function iconForType(type: string) {
  if (type === 'AGENT') return 'gesture-applause'
  if (type === 'CONVERSATION') return 'chat'
  if (type === 'PLUGIN') return 'shop'
  if (type === 'WORKFLOW') return 'tree-square-dot-vertical'
  if (type === 'KNOWLEDGE') return 'book'
  if (type === 'TOOL') return 'tools'
  return 'search'
}

function focusInput() {
  nextTick(() => {
    const el = inputRef.value?.$el?.querySelector('input') as HTMLInputElement | null
    el?.focus()
  })
}

function moveSelection(delta: number) {
  if (!results.value.length) return
  const next = selectedIndex.value + delta
  if (next < 0) {
    selectedIndex.value = results.value.length - 1
    return
  }
  selectedIndex.value = next % results.value.length
}

function openResult(item: SearchResultVO) {
  visible.value = false
  if (item.type === 'AGENT' && item.id) {
    selectAgent(item.id)
  }
  router.push(item.route)
}

function openSelected() {
  const item = results.value[selectedIndex.value]
  if (item) {
    openResult(item)
  }
}

function onGlobalKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    visible.value = true
  }
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
})
</script>

<style scoped>
.global-search__loading,
.global-search__hint,
.global-search__empty {
  margin-top: 16px;
}

.global-search__hint {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.global-search__list {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  max-height: 320px;
  overflow-y: auto;
}

.global-search__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.global-search__item:hover,
.global-search__item--active {
  background: rgba(0, 0, 0, 0.04);
}

.global-search__icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--box-muted);
}

.global-search__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.global-search__label {
  font-size: 14px;
  color: var(--box-ink);
}

.global-search__type {
  font-size: 12px;
  color: var(--box-muted);
}

.global-search__hint-text {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--box-muted);
}

kbd {
  display: inline-block;
  padding: 1px 5px;
  border: 1px solid var(--box-border);
  border-radius: 4px;
  background: #f5f6f7;
  font-size: 11px;
}
</style>
