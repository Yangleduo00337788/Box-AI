<template>
  <span v-if="hasIcon" class="ops-icon" aria-hidden="true">
    <span v-if="item.iconSvg" class="ops-icon__svg" v-html="item.iconSvg" />
    <img v-else-if="item.iconUrl" :src="item.iconUrl" alt="" />
    <t-icon v-else-if="item.iconName" :name="item.iconName" />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { OpsPlacementVO } from '@/api/ops'

const props = defineProps<{
  item: OpsPlacementVO
}>()

const hasIcon = computed(() => !!(props.item.iconSvg || props.item.iconUrl || props.item.iconName))
</script>

<style scoped>
.ops-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
}

.ops-icon img,
.ops-icon__svg :deep(svg) {
  width: 22px;
  height: 22px;
  display: block;
  object-fit: contain;
}
</style>
