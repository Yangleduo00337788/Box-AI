<template>
  <div v-if="current" class="ops-top-banner" :class="`ops-top-banner--${current.theme || 'info'}`">
    <div
      class="ops-top-banner__main"
      :class="{ 'ops-top-banner__main--clickable': !!current.linkUrl }"
      @click="openLink(current.linkUrl)"
    >
      <ops-placement-icon :item="current" />
      <strong>{{ current.title }}</strong>
      <span v-if="current.body">{{ current.body }}</span>
      <em v-if="current.linkUrl">{{ current.linkLabel || '立即查看' }}</em>
    </div>
    <div class="ops-top-banner__tools">
      <button type="button" class="ops-top-banner__nav" aria-label="上一条" @click.stop="prev">
        <t-icon name="chevron-left" />
      </button>
      <button
        v-for="(item, i) in items"
        :key="item.id"
        type="button"
        class="ops-top-banner__num"
        :class="{ 'is-active': i === index }"
        :aria-label="`第 ${i + 1} 条`"
        @click.stop="index = i"
      >
        {{ i + 1 }}
      </button>
      <button type="button" class="ops-top-banner__nav" aria-label="下一条" @click.stop="next">
        <t-icon name="chevron-right" />
      </button>
      <button
        v-if="current.dismissible"
        type="button"
        class="ops-top-banner__close"
        aria-label="关闭"
        @click.stop="dismiss(current.id)"
      >
        <t-icon name="close" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import OpsPlacementIcon from '@/components/OpsPlacementIcon.vue'
import { useOpsPlacements } from '@/composables/useOpsPlacements'

const { items, dismiss, openLink } = useOpsPlacements('GLOBAL_ALERT')
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
</script>

<style scoped>
.ops-top-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 36px;
  padding: 6px 16px;
  color: #fff;
  background: #5b6cff;
}

.ops-top-banner--success {
  background: #2ba471;
}

.ops-top-banner--warning {
  background: #e37318;
}

.ops-top-banner--error {
  background: #d54941;
}

.ops-top-banner__main {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 8px;
  overflow: hidden;
  line-height: 1.4;
}

.ops-top-banner__main--clickable {
  cursor: pointer;
}

.ops-top-banner__main strong {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
}

.ops-top-banner__main span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  opacity: 0.92;
}

.ops-top-banner__main em {
  flex-shrink: 0;
  font-size: 13px;
  font-style: normal;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.ops-top-banner :deep(.ops-icon) {
  color: #fff;
}

.ops-top-banner :deep(.ops-icon svg),
.ops-top-banner :deep(.ops-icon img) {
  filter: brightness(0) invert(1);
}

.ops-top-banner__tools {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
}

.ops-top-banner__page,
.ops-top-banner__nav,
.ops-top-banner__num,
.ops-top-banner__close {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 4px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  font-size: 12px;
  cursor: pointer;
}

.ops-top-banner__num.is-active {
  font-weight: 700;
  background: rgba(255, 255, 255, 0.2);
}

.ops-top-banner__nav:hover,
.ops-top-banner__num:hover,
.ops-top-banner__close:hover {
  background: rgba(255, 255, 255, 0.16);
}
</style>
