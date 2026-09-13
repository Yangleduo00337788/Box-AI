<template>
  <div>
    <page-header v-if="!compact" title="概览" desc="Box 工作台 · 创建、编排、调试并发布 AI Agent">
      <template #actions>
        <t-radio-group v-model="periodDays" variant="default-filled" size="small" @change="loadOverview">
          <t-radio-button :value="1">今天</t-radio-button>
          <t-radio-button :value="7">7 天</t-radio-button>
          <t-radio-button :value="30">30 天</t-radio-button>
        </t-radio-group>
      </template>
    </page-header>
    <div v-else class="dialog-toolbar">
      <t-radio-group v-model="periodDays" variant="default-filled" size="small" @change="loadOverview">
        <t-radio-button :value="1">今天</t-radio-button>
        <t-radio-button :value="7">7 天</t-radio-button>
        <t-radio-button :value="30">30 天</t-radio-button>
      </t-radio-group>
    </div>

    <t-loading :loading="loading" size="small">
      <div v-if="overview" class="stats-grid">
        <article v-for="item in stats" :key="item.label" class="stat-card">
          <span class="stat-card__label">{{ item.label }}</span>
          <strong class="stat-card__value">{{ item.value }}</strong>
        </article>
      </div>
    </t-loading>

    <section class="section">
      <h3 class="section__title">快速操作</h3>
      <div class="quick-actions">
        <t-button v-for="action in quickActions" :key="action.label" theme="default" @click="onQuickAction(action)">
          <template #icon><t-icon :name="action.icon" /></template>
          {{ action.label }}
        </t-button>
      </div>
    </section>

    <section v-if="overview" class="section recent-grid">
      <article class="recent-card">
        <h3 class="section__title">最近智能体</h3>
        <t-list v-if="overview.recentAgents?.length" :split="true">
          <t-list-item v-for="item in overview.recentAgents" :key="item.id" @click="router.push(`/agents/${item.id}/builder`)">
            <div class="recent-item">
              <span>{{ item.name }}</span>
              <t-tag size="small" variant="light">{{ item.status }}</t-tag>
            </div>
          </t-list-item>
        </t-list>
        <t-empty v-else description="暂无智能体" />
      </article>

      <article class="recent-card">
        <h3 class="section__title">最近对话</h3>
        <t-list v-if="overview.recentConversations?.length" :split="true">
          <t-list-item v-for="item in overview.recentConversations" :key="item.id" @click="router.push(`/chat/${item.id}`)">
            <div class="recent-item">
              <span>{{ item.title || `对话 #${item.id}` }}</span>
            </div>
          </t-list-item>
        </t-list>
        <t-empty v-else description="暂无对话" />
      </article>

      <article class="recent-card">
        <h3 class="section__title">最近工作流</h3>
        <t-list v-if="overview.recentWorkflows?.length" :split="true">
          <t-list-item v-for="item in overview.recentWorkflows" :key="item.id" @click="router.push(`/workflows/${item.id}/editor`)">
            <div class="recent-item">
              <span>{{ item.name }}</span>
              <t-tag size="small" variant="light">{{ item.status }}</t-tag>
            </div>
          </t-list-item>
        </t-list>
        <t-empty v-else description="暂无工作流" />
      </article>
    </section>

    <section class="section">
      <h3 class="section__title">开始使用</h3>
      <div class="welcome-card">
        <p>创建 Agent → 配置 Prompt / 模型 → 绑定知识库与工具 → 调试 → 发布 → API 调用。</p>
        <t-space>
          <t-button theme="primary" @click="openCreateAgentDialog">创建智能体</t-button>
          <t-button variant="outline" @click="router.push('/chat')">开始对话</t-button>
        </t-space>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { fetchAnalyticsOverview, type AnalyticsOverviewVO } from '@/api/analytics'

const router = useRouter()
const { openCreateAgentDialog } = useCreateAgentDialog()
defineProps<{ compact?: boolean }>()
const loading = ref(false)
const overview = ref<AnalyticsOverviewVO | null>(null)
const periodDays = ref(7)

const stats = computed(() => {
  if (!overview.value) return []
  const quota = overview.value.quota
  return [
    { label: '智能体', value: overview.value.agentCount },
    { label: '对话', value: overview.value.conversationCount },
    { label: '执行次数', value: overview.value.periodExecutionCount },
    { label: '成功率', value: `${overview.value.successRate.toFixed(1)}%` },
    { label: '平均延迟', value: overview.value.avgLatencyMs ? `${overview.value.avgLatencyMs} ms` : '—' },
    {
      label: 'Token',
      value: quota ? `${quota.usedTokens.toLocaleString()} / ${quota.quotaTokens.toLocaleString()}` : '—',
    },
  ]
})

const quickActions = [
  { label: '创建智能体', action: 'create-agent' as const, icon: 'gesture-applause' },
  { label: '创建工作流', path: '/workflows', icon: 'tree-square-dot-vertical' },
  { label: '创建知识库', path: '/knowledge', icon: 'book' },
  { label: '添加模型', path: '/models', icon: 'cpu' },
]

function onQuickAction(action: { label: string; path?: string; action?: 'create-agent' }) {
  if (action.action === 'create-agent') {
    openCreateAgentDialog()
    return
  }
  if (action.path) {
    router.push(action.path)
  }
}

async function loadOverview() {
  loading.value = true
  try {
    const { data } = await fetchAnalyticsOverview(periodDays.value)
    overview.value = data.data
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  padding: 20px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.stat-card__label {
  display: block;
  margin-bottom: 8px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.stat-card__value {
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.section {
  margin-bottom: 28px;
}

.section__title {
  margin: 0 0 12px;
  font: var(--td-font-title-medium);
  color: var(--box-ink);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.recent-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.recent-card {
  padding: 16px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  cursor: pointer;
}

.welcome-card {
  padding: 24px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.welcome-card p {
  margin: 0 0 16px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
}

.dialog-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

@media (max-width: 960px) {
  .stats-grid,
  .recent-grid {
    grid-template-columns: 1fr;
  }
}
</style>
