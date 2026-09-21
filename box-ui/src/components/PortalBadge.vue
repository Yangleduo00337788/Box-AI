<template>
  <span class="portal-badge" :class="`portal-badge--${props.theme}`">{{ props.label }}</span>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    label: string
    theme?: 'consumer' | 'enterprise' | 'personal' | 'admin'
  }>(),
  {
    theme: 'consumer',
  },
)
</script>

<style scoped>
.portal-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.portal-badge--consumer,
.portal-badge--personal,
.portal-badge--enterprise {
  background: var(--td-gray-color-2);
  color: var(--box-ink);
}

.portal-badge--personal {
  background: linear-gradient(135deg, #f3f4f6 0%, #e8eaed 100%);
}

.portal-badge--enterprise {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
}

.portal-badge--admin {
  background: var(--box-ink);
  color: #fff;
}
</style>
