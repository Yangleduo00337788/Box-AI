<template>
  <div v-if="current" class="admin-ops-banner" :class="`admin-ops-banner--${current.theme || 'info'}`">
    <div
      class="admin-ops-banner__main"
      :class="{ 'admin-ops-banner__main--clickable': !!current.linkUrl }"
      @click="openLink(current.linkUrl)"
    >
      <strong>{{ current.title }}</strong>
      <span v-if="current.body">{{ current.body }}</span>
      <em v-if="current.linkUrl">{{ current.linkLabel || '立即查看' }}</em>
    </div>
    <div class="admin-ops-banner__tools">
      <button type="button" class="admin-ops-banner__nav" aria-label="上一条" @click.stop="prev">
        <t-icon name="chevron-left" />
      </button>
      <button
        v-for="(item, i) in items"
        :key="item.id"
        type="button"
        class="admin-ops-banner__num"
        :class="{ 'is-active': i === index }"
        :aria-label="`第 ${i + 1} 条`"
        @click.stop="index = i"
      >
        {{ i + 1 }}
      </button>
      <button type="button" class="admin-ops-banner__nav" aria-label="下一条" @click.stop="next">
        <t-icon name="chevron-right" />
      </button>
      <button
        v-if="current.dismissible"
        type="button"
        class="admin-ops-banner__close"
        aria-label="关闭"
        @click.stop="onDismiss"
      >
        <t-icon name="close" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { dismissAdminNotification } from '@/api/adminNotification'
import { useAdminOpsPlacements } from '@/composables/useAdminOpsPlacements'

const { items, dismissLocal, openLink } = useAdminOpsPlacements('ADMIN_HEADER')

const index = ref(0)
const current = computed(() => items.value[index.value] || items.value[0])

watch(
  () => items.value.map((item) => item.id).join(','),
  () => {
    if (index.value >= items.value.length) index.value = 0
  },
)

function prev() {
  if (!items.value.length) return
  index.value = (index.value - 1 + items.value.length) % items.value.length
}

function next() {
  if (!items.value.length) return
  index.value = (index.value + 1) % items.value.length
}

async function onDismiss() {
  const item = current.value
  if (!item) return
  dismissLocal(item.id)
  if (item.dismissible) {
    try {
      await dismissAdminNotification(`ops:${item.id}`)
    } catch {
      /* 本地已关闭顶栏展示 */
    }
  }
}
</script>

<style scoped>
.admin-ops-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 40px;
  padding: 8px 16px;
  font-size: 13px;
  line-height: 1.4;
  border-bottom: 1px solid var(--td-component-stroke);
}

.admin-ops-banner--info {
  background: var(--td-brand-color-light);
  color: var(--td-text-color-primary);
}

.admin-ops-banner--success {
  background: var(--td-success-color-1);
}

.admin-ops-banner--warning {
  background: var(--td-warning-color-1);
}

.admin-ops-banner--error {
  background: var(--td-error-color-1);
}

.admin-ops-banner__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.admin-ops-banner__main--clickable {
  cursor: pointer;
}

.admin-ops-banner__main em {
  font-style: normal;
  text-decoration: underline;
  opacity: 0.85;
}

.admin-ops-banner__tools {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.admin-ops-banner__nav,
.admin-ops-banner__num,
.admin-ops-banner__close {
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  color: inherit;
}

.admin-ops-banner__num.is-active {
  font-weight: 600;
  text-decoration: underline;
}
</style>
