<template>
  <div>
    <page-header title="概览" desc="Box 工作台 · 创建、编排、调试并发布 AI Agent" />

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
        <t-button v-for="action in quickActions" :key="action.path" theme="default" @click="router.push(action.path)">
          <template #icon><t-icon :name="action.icon" /></template>
          {{ action.label }}
        </t-button>
      </div>
    </section>

    <section class="section">
      <h3 class="section__title">开始使用</h3>
      <div class="welcome-card">
        <p>创建 Agent → 配置 Prompt / 模型 → 绑定知识库与工具 → 调试 → 发布 → API 调用。</p>
        <t-space>
          <t-button theme="primary" @click="router.push('/agents')">创建智能体</t-button>
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
import { fetchAnalyticsOverview, type AnalyticsOverviewVO } from '@/api/analytics'

const router = useRouter()
const loading = ref(false)
const overview = ref<AnalyticsOverviewVO | null>(null)

const stats = computed(() => {
  if (!overview.value) return []
  const quota = overview.value.quota
  return [
    { label: '智能体', value: overview.value.agentCount },
    { label: '对话', value: overview.value.conversationCount },
    {
      label: 'Token',
      value: quota ? `${quota.usedTokens.toLocaleString()} / ${quota.quotaTokens.toLocaleString()}` : '—',
    },
    {
      label: 'AI 调用',
      value: quota ? `${quota.usedAiCalls} / ${quota.quotaAiCalls}` : '—',
    },
  ]
})

const quickActions = [
  { label: '创建智能体', path: '/agents', icon: 'gesture-applause' },
  { label: '插件市场', path: '/plugin-market', icon: 'shop' },
  { label: '添加模型', path: '/models', icon: 'cpu' },
]

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await fetchAnalyticsOverview()
    overview.value = data.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

@media (max-width: 960px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
