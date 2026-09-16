<template>
  <div class="forbidden-page">
    <t-empty description="当前平台角色没有访问该页面的权限。">
      <template #title>
        <span class="forbidden-page__title">403 无访问权限</span>
      </template>
      <template #action>
        <t-space>
          <t-button theme="primary" @click="goHome">回到工作台</t-button>
        </t-space>
      </template>
    </t-empty>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

function goHome() {
  void router.push(auth.homePath)
}
</script>

<style scoped>
.forbidden-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 24px;
}

.forbidden-page__title {
  font: var(--td-font-title-large);
  color: var(--td-text-color-primary);
}
</style>
