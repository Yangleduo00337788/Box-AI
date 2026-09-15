<template>
  <div>
    <page-header title="智能体市场" desc="选用平台模板，一键启用到你的工作空间">
      <template #actions>
        <t-button variant="outline" @click="router.push('/plugin-market')">插件市场</t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <div v-if="templates.length" class="market-grid">
        <article v-for="item in templates" :key="item.id" class="market-card">
          <div class="market-card__head">
            <t-avatar size="48px" shape="round" class="market-card__avatar">
              {{ item.name.slice(0, 1).toUpperCase() }}
            </t-avatar>
            <t-tag v-if="item.category" variant="light" size="small">{{ item.category }}</t-tag>
          </div>
          <h3 class="market-card__title">{{ item.name }}</h3>
          <p class="market-card__desc">{{ item.description || '暂无描述' }}</p>
          <div class="market-card__meta">
            <span>{{ item.platformModelName || '平台模型' }}</span>
            <span>{{ item.installCount ?? 0 }} 次启用</span>
          </div>
          <t-button theme="primary" block :loading="enablingId === item.id" @click="enable(item)">
            一键启用
          </t-button>
        </article>
      </div>
      <div v-else class="market-empty">
        <t-empty description="暂无上架模板" />
      </div>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { enableMarketTemplate, listMarketTemplates, type AgentTemplateVO } from '@/api/market'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'

const router = useRouter()
const loading = ref(false)
const enablingId = ref<number | null>(null)
const templates = ref<AgentTemplateVO[]>([])

async function loadTemplates() {
  loading.value = true
  try {
    const { data } = await listMarketTemplates()
    templates.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function enable(item: AgentTemplateVO) {
  enablingId.value = item.id
  try {
    const { data } = await enableMarketTemplate(item.id)
    MessagePlugin.success(`「${item.name}」已启用`)
    if (data.data?.id) {
      router.push(`/agents/${data.data.id}/builder`)
    }
  } finally {
    enablingId.value = null
  }
}

onMounted(loadTemplates)
useReloadOnWorkspaceChange(loadTemplates)
</script>

<style scoped>
.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.market-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.market-card:hover {
  border-color: var(--td-gray-color-4);
  box-shadow: var(--box-shadow-soft);
  transform: translateY(-2px);
}

.market-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.market-card__avatar {
  background: var(--td-gray-color-2);
  color: var(--box-ink);
  font-weight: 600;
}

.market-card__title {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.market-card__desc {
  margin: 0;
  min-height: 44px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.market-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.market-empty {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}
</style>
