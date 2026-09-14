<template>
  <div
    class="ops-notice"
    :class="[
      `ops-notice--${item.theme || 'info'}`,
      `ops-notice--${item.kind === 'PROMO' ? 'promo' : 'announcement'}`,
      { 'ops-notice--clickable': !!item.linkUrl },
    ]"
    role="status"
    @click="onOpen"
  >
    <ops-placement-icon :item="item" />
    <div class="ops-notice__copy">
      <strong>{{ item.title }}</strong>
      <span v-if="item.body">{{ item.body }}</span>
    </div>
    <div v-if="total > 1" class="ops-notice__pager" @click.stop>
      <button type="button" class="ops-notice__nav" aria-label="上一条" @click="emit('page', wrap(index - 1))">
        <t-icon name="chevron-left" size="16px" />
      </button>
      <button
        v-for="page in total"
        :key="page"
        type="button"
        class="ops-notice__dot"
        :class="{ 'is-active': page - 1 === index }"
        :aria-label="`第 ${page} 条`"
        @click="emit('page', page - 1)"
      >
        {{ page }}
      </button>
      <button type="button" class="ops-notice__nav" aria-label="下一条" @click="emit('page', wrap(index + 1))">
        <t-icon name="chevron-right" size="16px" />
      </button>
    </div>
    <button
      v-if="item.dismissible"
      type="button"
      class="ops-notice__close"
      aria-label="关闭"
      @click.stop="emit('dismiss', item.id)"
    >
      <t-icon name="close" size="16px" />
    </button>
  </div>
</template>

<script setup lang="ts">
import type { OpsPlacementVO } from '@/api/ops'
import OpsPlacementIcon from '@/components/OpsPlacementIcon.vue'

const props = withDefaults(
  defineProps<{
    item: OpsPlacementVO
    index?: number
    total?: number
  }>(),
  { index: 0, total: 1 },
)

const emit = defineEmits<{
  dismiss: [id: number]
  open: [url?: string]
  page: [index: number]
}>()

function wrap(next: number) {
  if (props.total <= 0) return 0
  return (next + props.total) % props.total
}

function onOpen() {
  if (!props.item.linkUrl) return
  emit('open', props.item.linkUrl)
}
</script>

<style scoped>
.ops-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 10px 12px 10px 14px;
  border: 1px solid #eceef2;
  border-radius: 18px;
  background: #f7f8fa;
  color: #1f2329;
}

.ops-notice--clickable {
  cursor: pointer;
}

.ops-notice--success {
  background: #f3fbf6;
  border-color: #d9f0e3;
}

.ops-notice--warning,
.ops-notice--promo {
  background: #fff8f0;
  border-color: #f3e2cc;
}

.ops-notice--error {
  background: #fff5f5;
  border-color: #f0d4d4;
}

.ops-notice :deep(.ops-icon) {
  color: #5b8def;
}

.ops-notice--promo :deep(.ops-icon),
.ops-notice--warning :deep(.ops-icon) {
  color: #e37318;
}

.ops-notice--success :deep(.ops-icon) {
  color: #2ba471;
}

.ops-notice--error :deep(.ops-icon) {
  color: #d54941;
}

.ops-notice__copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-wrap: nowrap;
  align-items: baseline;
  gap: 8px;
  line-height: 1.45;
  overflow: hidden;
}

.ops-notice__copy strong {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
}

.ops-notice__copy span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #8b8f97;
}

.ops-notice__pager {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
}

.ops-notice__nav,
.ops-notice__dot,
.ops-notice__close {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #8b8f97;
  cursor: pointer;
}

.ops-notice__dot {
  width: auto;
  min-width: 18px;
  font-size: 12px;
}

.ops-notice__dot.is-active {
  color: #1f2329;
  font-weight: 700;
}

.ops-notice__nav:hover,
.ops-notice__dot:hover,
.ops-notice__close:hover {
  background: rgba(0, 0, 0, 0.04);
  color: #1f2329;
}
</style>
