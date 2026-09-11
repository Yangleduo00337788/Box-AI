<template>
  <div class="settings-page">
    <h1 class="settings-page__title">额度管理</h1>
    <t-loading :loading="loading" size="small">
      <div v-if="quota" class="settings-card">
        <div class="quota-head">
          <div>
            <p class="quota-head__label">当前套餐</p>
            <h2 class="quota-head__plan">{{ quota.planName }}</h2>
          </div>
          <t-tag theme="primary" variant="light">{{ quota.period }}</t-tag>
        </div>

        <div class="quota-grid">
          <article v-for="item in quotaItems" :key="item.label" class="quota-item">
            <div class="quota-item__head">
              <span>{{ item.label }}</span>
              <span>{{ item.used }} / {{ item.total }}</span>
            </div>
            <t-progress :percentage="item.percent" :status="item.status" />
          </article>
        </div>
      </div>
      <t-empty v-else description="暂无额度数据" />
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchQuota, type QuotaSnapshotVO } from '@/api/quota'

const loading = ref(false)
const quota = ref<QuotaSnapshotVO | null>(null)

function calcPercent(used: number, total: number) {
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

function calcStatus(percent: number) {
  if (percent >= 90) return 'error'
  if (percent >= 75) return 'warning'
  return 'active'
}

const quotaItems = computed(() => {
  if (!quota.value) return []
  const q = quota.value
  return [
    { label: 'AI 调用', used: q.usedAiCalls, total: q.quotaAiCalls },
    { label: 'Token', used: q.usedTokens, total: q.quotaTokens },
    { label: '成员', used: q.usedMembers, total: q.quotaMembers },
    { label: '工作空间', used: q.usedWorkspaces, total: q.quotaWorkspaces },
  ].map((item) => {
    const percent = calcPercent(item.used, item.total)
    return { ...item, percent, status: calcStatus(percent) }
  })
})

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await fetchQuota()
    quota.value = data.data
  } catch {
    quota.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.settings-page__title {
  margin: 0 0 24px;
  font: var(--td-font-title-large);
}

.settings-card {
  max-width: 720px;
  padding: 24px;
  border-radius: 16px;
  background: #f7f8fa;
}

.quota-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.quota-head__label {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--box-muted);
}

.quota-head__plan {
  margin: 0;
  font: var(--td-font-title-medium);
}

.quota-grid {
  display: grid;
  gap: 16px;
}

.quota-item__head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--box-ink);
}
</style>
