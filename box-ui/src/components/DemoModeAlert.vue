<template>
  <t-alert v-if="enabled" theme="error" title="演示模式" :class="['demo-mode-alert', `demo-mode-alert--${variant}`]">
    <slot>{{ message }}</slot>
  </t-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    variant?: 'login' | 'banner' | 'sidebar'
    message?: string
  }>(),
  {
    variant: 'login',
    message: '',
  },
)

const enabled = import.meta.env.VITE_BOX_DEMO === 'true'

const defaultMessages: Record<string, string> = {
  login: '任意邮箱与密码即可登录，界面与正式用户端一致，数据为前端模拟。',
  banner: '任意账号密码可登录，界面与正式管理台一致，数据为前端模拟。',
  sidebar: '全功能 Mock，数据保存在 sessionStorage。控制台可执行 __BOX_DEMO_RESET__() 恢复种子。',
}

const message = computed(() => props.message || defaultMessages[props.variant] || defaultMessages.login)
</script>

<style scoped>
.demo-mode-alert {
  margin: 0;
  font-weight: 600;
}

.demo-mode-alert--login {
  margin-bottom: 16px;
}

.demo-mode-alert--banner {
  margin: 0;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-top: none;
}

.demo-mode-alert--sidebar {
  margin: 0 8px 8px;
  font-size: 12px;
}

.demo-mode-alert--sidebar :deep(.t-alert__description) {
  font-size: 11px;
  line-height: 1.45;
}
</style>
