<template>
  <article class="resource-item" :class="{ 'is-clickable': clickable }" @click="onClick">
    <span class="resource-item__icon" :class="`resource-item__icon--${tone}`">
      <t-icon :name="icon" size="22px" />
    </span>
    <div class="resource-item__body">
      <div class="resource-item__title-row">
        <h3>{{ title }}</h3>
        <slot name="tags" />
      </div>
      <p>{{ description || '暂无描述' }}</p>
      <div v-if="$slots.meta" class="resource-item__meta">
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.actions" class="resource-item__actions" @click.stop>
      <slot name="actions" />
    </div>
  </article>
</template>

<script setup lang="ts">
const props = defineProps<{
  title: string
  description?: string
  icon: string
  tone: string
  clickable?: boolean
}>()

const emit = defineEmits<{
  click: []
}>()

function onClick() {
  if (props.clickable) emit('click')
}
</script>
