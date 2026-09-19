<template>
  <div class="quota-btn-wrap">
    <t-popup
      v-model:visible="visible"
      trigger="click"
      placement="top-right"
      attach="body"
      :overlay-inner-style="{ padding: 0, maxHeight: 'none', overflow: 'visible' }"
      @visible-change="onVisibleChange"
    >
      <button
        type="button"
        class="consumer-footer__icon-btn"
        :class="{
          'consumer-footer__icon-btn--warn': warnLevel === 'warning',
          'consumer-footer__icon-btn--danger': warnLevel === 'danger',
        }"
        title="额度"
        aria-label="查看套餐额度"
      >
        <t-icon name="dashboard" />
      </button>

      <template #content>
        <div class="quota-popover">
          <t-loading :loading="loading" size="small">
            <template v-if="quota">
              <div class="quota-popover__head">
                <div>
                  <p class="quota-popover__label">当前套餐</p>
                  <p class="quota-popover__plan">{{ quota.planName }}</p>
                </div>
                <t-tag size="small" theme="primary" variant="light">{{ quota.period }}</t-tag>
              </div>

              <ul class="quota-popover__list">
                <li v-for="item in quotaRows" :key="item.label" class="quota-popover__row">
                  <div class="quota-popover__row-head">
                    <span>{{ item.label }}</span>
                    <span class="quota-popover__nums">{{ item.summary }}</span>
                  </div>
                  <div class="quota-popover__track" aria-hidden="true">
                    <span
                      class="quota-popover__fill"
                      :class="{
                        'quota-popover__fill--warn': item.percent >= 75 && item.percent < 90,
                        'quota-popover__fill--danger': item.percent >= 90,
                      }"
                      :style="{ width: `${item.percent}%` }"
                    />
                  </div>
                </li>
              </ul>

              <button type="button" class="quota-popover__link" @click="goPlan">
                套餐与额度详情
                <t-icon name="chevron-right" />
              </button>
            </template>
            <p v-else class="quota-popover__empty">暂无额度数据</p>
          </t-loading>
        </div>
      </template>
    </t-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  formatQuotaNumber,
  quotaUsagePercent,
  useTenantQuota,
} from '@/composables/useTenantQuota'

const router = useRouter()
const visible = ref(false)
const { quota, loading, tokenPercent, refreshQuota } = useTenantQuota()

const warnLevel = computed(() => {
  const p = tokenPercent.value
  if (p >= 90) return 'danger'
  if (p >= 75) return 'warning'
  return 'normal'
})

const quotaRows = computed(() => {
  const q = quota.value
  if (!q) return []
  const rows = [
    { label: 'Token', used: q.usedTokens, total: q.quotaTokens },
    { label: 'AI 调用', used: q.usedAiCalls, total: q.quotaAiCalls },
    { label: '成员', used: q.usedMembers, total: q.quotaMembers },
    { label: '工作空间', used: q.usedWorkspaces, total: q.quotaWorkspaces },
    { label: '知识库', used: q.usedKnowledgeBases, total: q.quotaKnowledgeBases },
  ]
  return rows.map((row) => ({
    label: row.label,
    percent: quotaUsagePercent(row.used, row.total),
    summary: `${formatQuotaNumber(row.used)} / ${formatQuotaNumber(row.total)}`,
  }))
})

function onVisibleChange(open: boolean) {
  if (open) {
    void refreshQuota()
  }
}

function goPlan() {
  visible.value = false
  void router.push('/settings/plan')
}
</script>

<style scoped>
.quota-btn-wrap {
  display: inline-flex;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  overflow: hidden;
}

.quota-btn-wrap :deep(.t-popup),
.quota-btn-wrap :deep(.t-popup__reference) {
  display: inline-flex !important;
  width: 24px;
  height: 24px;
  overflow: hidden;
}

.consumer-footer__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.consumer-footer__icon-btn:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.consumer-footer__icon-btn--warn {
  color: var(--td-warning-color);
}

.consumer-footer__icon-btn--danger {
  color: var(--td-error-color);
}

.consumer-footer__icon-btn :deep(.t-icon) {
  font-size: 16px;
}

.quota-popover {
  width: 280px;
  padding: 14px 16px 12px;
  border-radius: 12px;
  background: var(--box-surface);
}

.quota-popover__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.quota-popover__label {
  margin: 0 0 2px;
  font-size: 11px;
  color: var(--box-muted);
}

.quota-popover__plan {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--box-ink);
}

.quota-popover__list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.quota-popover__row {
  margin-bottom: 10px;
}

.quota-popover__row-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--box-muted);
}

.quota-popover__nums {
  font-weight: 600;
  color: var(--box-ink);
  white-space: nowrap;
}

.quota-popover__track {
  height: 4px;
  border-radius: 999px;
  background: var(--box-border);
  overflow: hidden;
}

.quota-popover__fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--box-ink);
  transition: width 0.35s ease;
}

.quota-popover__fill--warn {
  background: var(--td-warning-color);
}

.quota-popover__fill--danger {
  background: var(--td-error-color);
}

.quota-popover__link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
  padding: 6px 0 0;
  border: none;
  background: transparent;
  color: var(--td-brand-color);
  font-size: 13px;
  cursor: pointer;
}

.quota-popover__link:hover {
  opacity: 0.85;
}

.quota-popover__empty {
  margin: 0;
  font-size: 13px;
  color: var(--box-muted);
  text-align: center;
}
</style>
