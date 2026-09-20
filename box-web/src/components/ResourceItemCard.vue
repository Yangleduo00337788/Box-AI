<template>
  <article class="plugin-card" :class="{ 'is-clickable': clickable }" @click="onClick">
    <span class="plugin-card__icon" :class="`plugin-card__icon--${tone}`">
      <t-icon :name="icon" size="22px" />
    </span>
    <div class="plugin-card__body">
      <div class="plugin-card__title-row">
        <h3 class="plugin-card__title">{{ title }}</h3>
        <div v-if="$slots.tags" class="plugin-card__tags">
          <slot name="tags" />
        </div>
      </div>
      <p class="plugin-card__desc">{{ description || '暂无描述' }}</p>
      <div v-if="$slots.meta" class="plugin-card__meta">
        <slot name="meta" />
      </div>
    </div>
    <div v-if="$slots.actions" class="plugin-card__actions" @click.stop>
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
