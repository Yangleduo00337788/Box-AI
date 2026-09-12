<template>
  <div>
    <page-header title="分析" desc="查看工作空间内的智能体、对话与资源使用情况">
      <template #actions>
        <t-radio-group v-model="periodDays" variant="default-filled" size="small" @change="loadOverview">
          <t-radio-button :value="1">今天</t-radio-button>
          <t-radio-button :value="7">7 天</t-radio-button>
          <t-radio-button :value="30">30 天</t-radio-button>
        </t-radio-group>
        <t-button variant="outline" @click="router.push('/executions')">查看执行记录</t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <div v-if="overview" class="stats-grid">
        <article v-for="item in stats" :key="item.label" class="stat-card">
          <span class="stat-card__label">{{ item.label }}</span>
          <strong class="stat-card__value">{{ item.value }}</strong>
          <span v-if="item.hint" class="stat-card__hint">{{ item.hint }}</span>
        </article>
      </div>
    </t-loading>

    <section v-if="overview?.topAgents?.length" class="section">
      <h3 class="section__title">热门智能体（近 {{ overview.periodDays }} 天）</h3>
      <t-table row-key="agentId" :data="overview.topAgents" :columns="topAgentColumns" bordered stripe size="small">
        <template #successRate="{ row }">{{ row.successRate.toFixed(1) }}%</template>
        <template #op="{ row }">
          <t-button variant="text" theme="primary" @click="router.push(`/agents/${row.agentId}/builder`)">打开</t-button>
        </template>
      </t-table>
    </section>

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
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { fetchAnalyticsOverview, type AnalyticsOverviewVO } from '@/api/analytics'

const router = useRouter()
const loading = ref(false)
const overview = ref<AnalyticsOverviewVO | null>(null)
const periodDays = ref(7)

const stats = computed(() => {
  if (!overview.value) return []
  return [
    { label: '智能体', value: overview.value.agentCount, hint: '当前工作空间' },
    { label: '对话', value: overview.value.conversationCount, hint: '历史会话' },
    { label: '执行', value: overview.value.periodExecutionCount, hint: `近 ${overview.value.periodDays} 天` },
    { label: '成功率', value: `${overview.value.successRate.toFixed(1)}%`, hint: '执行成功占比' },
    { label: '平均延迟', value: overview.value.avgLatencyMs ? `${overview.value.avgLatencyMs} ms` : '—', hint: '执行耗时' },
    { label: '总执行', value: overview.value.executionCount, hint: '累计记录' },
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

const topAgentColumns = [
  { colKey: 'agentName', title: '智能体' },
  { colKey: 'executionCount', title: '执行次数', width: 100 },
  { colKey: 'successRate', title: '成功率', width: 100 },
  { colKey: 'op', title: '操作', width: 80 },
]

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
