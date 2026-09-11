<template>
  <div class="page-header">
    <div class="page-header__main">
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="desc" class="page-header__desc">{{ desc }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  desc?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.page-header__main {
  min-width: 0;
}

.page-header__title {
  margin: 0;
  font: var(--td-font-headline-small);
  color: var(--box-ink);
}

.page-header__desc {
  margin: 8px 0 0;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
}

.page-header__actions {
  flex-shrink: 0;
}
</style>
