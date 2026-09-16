<template>
  <div class="not-found-page">
    <t-empty description="你访问的页面不存在，或已被移除。">
      <template #title>
        <span class="not-found-page__title">404 页面不存在</span>
      </template>
      <template #action>
        <t-button theme="primary" @click="goHome">回到工作台</t-button>
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
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 24px;
}

.not-found-page__title {
  font: var(--td-font-title-large);
  color: var(--td-text-color-primary);
}
</style>
