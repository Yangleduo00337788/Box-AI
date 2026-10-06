<template>
  <div class="auth-agreement">
    <t-checkbox v-model="agreed">
      <span class="auth-agreement__text">
        {{ actionLabel }}即表示同意
        <router-link to="/legal/terms" target="_blank" @click.stop>《用户协议》</router-link>
        和
        <router-link to="/legal/privacy" target="_blank" @click.stop>《隐私政策》</router-link>
      </span>
    </t-checkbox>
  </div>
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
  display: inline-flex;
  align-items: center;
}

.auth-agreement :deep(.t-checkbox__input) {
  flex-shrink: 0;
  margin-top: 0;
}

.auth-agreement__text {
  display: inline;
  line-height: 1.4;
  vertical-align: middle;
}
</style>
