<template>
  <div>
    <page-header title="分析" desc="查看工作空间内的智能体、对话与资源使用情况" />

    <t-loading :loading="loading" size="small">
      <div v-if="overview" class="stats-grid">
        <article v-for="item in stats" :key="item.label" class="stat-card">
          <span class="stat-card__label">{{ item.label }}</span>
          <strong class="stat-card__value">{{ item.value }}</strong>
          <span v-if="item.hint" class="stat-card__hint">{{ item.hint }}</span>
        </article>
      </div>
    </t-loading>

    <section v-if="overview?.quota" class="section">
      <h3 class="section__title">资源使用</h3>
      <div class="usage-grid">
        <article v-for="item in usageItems" :key="item.label" class="usage-card">
          <div class="usage-card__head">
            <span>{{ item.label }}</span>
            <span>{{ item.percent }}%</span>
          </div>
          <t-progress :percentage="item.percent" :status="item.status" />
          <p class="usage-card__meta">{{ item.used }} / {{ item.total }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchAnalyticsOverview, type AnalyticsOverviewVO } from '@/api/analytics'

const loading = ref(false)
const overview = ref<AnalyticsOverviewVO | null>(null)

const stats = computed(() => {
  if (!overview.value) return []
  return [
    { label: '智能体', value: overview.value.agentCount, hint: '当前工作空间' },
    { label: '对话', value: overview.value.conversationCount, hint: '历史会话' },
    { label: '执行', value: overview.value.executionCount, hint: 'Agent/Workflow 运行' },
    { label: '知识库', value: overview.value.knowledgeBaseCount, hint: 'RAG 资源' },
    { label: '工具', value: overview.value.toolCount, hint: 'HTTP Tool' },
    { label: '工作流', value: overview.value.workflowCount, hint: '编排数量' },
    { label: 'MCP', value: overview.value.mcpServerCount, hint: '外部能力' },
    { label: '套餐', value: overview.value.quota?.planName || '—', hint: overview.value.quota?.period || '' },
  ]
})

function calcPercent(used: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

function calcStatus(percent: number) {
  if (percent >= 90) return 'error'
  if (percent >= 75) return 'warning'
  return 'active'
}

const usageItems = computed(() => {
  const quota = overview.value?.quota
  if (!quota) return []
  return [
    { label: 'Token', used: quota.usedTokens, total: quota.quotaTokens },
    { label: 'AI 调用', used: quota.usedAiCalls, total: quota.quotaAiCalls },
    { label: '成员', used: quota.usedMembers, total: quota.quotaMembers },
    { label: '工作空间', used: quota.usedWorkspaces, total: quota.quotaWorkspaces },
  ].map((item) => {
    const percent = calcPercent(item.used, item.total)
    return {
      ...item,
      percent,
      status: calcStatus(percent),
      used: item.used.toLocaleString(),
      total: item.total.toLocaleString(),
    }
  })
})

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

.stat-card__hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: var(--box-muted);
}

.section__title {
  margin: 0 0 12px;
  font: var(--td-font-title-medium);
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.usage-card {
  padding: 18px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}

.usage-card__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}

.usage-card__meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--box-muted);
}

@media (max-width: 960px) {
  .stats-grid,
  .usage-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
