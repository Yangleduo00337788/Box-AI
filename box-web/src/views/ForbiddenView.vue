<template>
  <div class="forbidden-page">
    <t-empty description="当前账号在此工作空间没有访问该页面的权限。">
      <p v-if="traceId" class="forbidden-page__trace">Request ID: {{ traceId }}</p>
      <template #title>
        <span class="forbidden-page__title">403 无访问权限</span>
      </template>
      <template #action>
        <t-space>
          <t-button theme="primary" @click="goBack">返回上一页</t-button>
          <t-button variant="outline" @click="goHome">回到对话</t-button>
        </t-space>
      </template>
    </t-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { readLastTraceId } from '@/api/apiError'

const router = useRouter()
const traceId = computed(() => readLastTraceId())

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/chat')
}

function goHome() {
  router.push('/chat')
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

.forbidden-page__trace {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  word-break: break-all;
}
</style>
