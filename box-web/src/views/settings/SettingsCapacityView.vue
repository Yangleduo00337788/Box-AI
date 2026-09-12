<template>
  <div class="settings-page">
    <h1 class="settings-page__title">容量管理</h1>
    <t-loading :loading="loading" size="small">
      <div v-if="quota" class="settings-card">
        <p class="settings-card__desc">查看当前企业资源占用与剩余容量。</p>
        <div class="capacity-list">
          <div v-for="item in capacityItems" :key="item.label" class="capacity-row">
            <div class="capacity-row__main">
              <t-icon :name="item.icon" />
              <div>
                <div class="capacity-row__label">{{ item.label }}</div>
                <div class="capacity-row__hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="capacity-row__value">{{ item.value }}</div>
          </div>
        </div>
      </div>
      <t-empty v-else description="暂无容量数据" />
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchQuota, type QuotaSnapshotVO } from '@/api/quota'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const loading = ref(false)
const quota = ref<QuotaSnapshotVO | null>(null)

const capacityItems = computed(() => {
  if (!quota.value) return []
  const q = quota.value
  return [
    {
      label: '工作空间',
      hint: '当前账号可创建的工作空间数量',
      icon: 'folder',
      value: `${q.usedWorkspaces} / ${q.quotaWorkspaces}`,
    },
    {
      label: '团队成员',
      hint: '企业内可邀请的成员数量',
      icon: 'usergroup',
      value: `${q.usedMembers} / ${q.quotaMembers}`,
    },
    {
      label: 'Token 用量',
      hint: '本周期累计 Token 消耗',
      icon: 'chart',
      value: `${q.usedTokens.toLocaleString()} / ${q.quotaTokens.toLocaleString()}`,
    },
    {
      label: '已加入工作空间',
      hint: '你当前可访问的工作空间',
      icon: 'layers',
      value: String(auth.workspaces.length),
    },
  ]
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

.settings-card__desc {
  margin: 0 0 20px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
}

.capacity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.capacity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: #fff;
}

.capacity-row__main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.capacity-row__label {
  font-size: 14px;
  color: var(--box-ink);
}

.capacity-row__hint {
  margin-top: 2px;
  font-size: 12px;
  color: var(--box-muted);
}

.capacity-row__value {
  flex-shrink: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--box-ink);
}
</style>
