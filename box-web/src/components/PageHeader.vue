<template>
  <div class="page-header">
    <div class="page-header__main">
      <router-link v-if="backTo" :to="backTo" class="page-header__back">
        <t-icon name="chevron-left" />
        <span>{{ backLabel || '返回' }}</span>
      </router-link>
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
  backTo?: string
  backLabel?: string
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

.page-header__back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 8px;
  color: var(--box-muted);
  font: var(--td-font-body-small);
  text-decoration: none;
  transition: color 0.2s;
}

.page-header__back:hover {
  color: var(--box-ink);
}

.page-header__back :deep(.t-icon) {
  font-size: 16px;
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
