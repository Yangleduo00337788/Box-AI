<template>
  <p class="auth-agreement">
    <t-checkbox v-model="agreed">
      {{ actionLabel }}即表示同意
      <router-link to="/legal/terms" target="_blank" @click.stop>《用户协议》</router-link>
      和
      <router-link to="/legal/privacy" target="_blank" @click.stop>《隐私政策》</router-link>
    </t-checkbox>
  </p>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    actionLabel?: string
  }>(),
  { actionLabel: '登录' },
)

const agreed = defineModel<boolean>('agreed', { default: false })
</script>

<style scoped>
.auth-agreement {
  margin: 0 0 16px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.auth-agreement a {
  color: var(--td-brand-color);
  text-decoration: none;
}

.auth-agreement :deep(.t-checkbox) {
  align-items: flex-start;
}
</style>
