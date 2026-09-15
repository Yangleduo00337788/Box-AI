<template>
  <div class="invite-page">
    <t-card :bordered="false" class="invite-card">
      <t-loading :loading="loading">
        <template v-if="invitation">
          <h1 class="invite-title">工作空间邀请</h1>
          <p class="invite-desc">
            你受邀以 <strong>{{ invitation.roleCode }}</strong> 身份加入协作（{{ invitation.email }}）。
          </p>
          <p v-if="invitation.expiresAt" class="invite-meta">有效期至 {{ formatTime(invitation.expiresAt) }}</p>
          <div class="invite-actions">
            <t-button v-if="!auth.token" theme="primary" @click="goLogin">登录后接受</t-button>
            <t-button v-else theme="primary" :loading="accepting" @click="onAccept">接受邀请</t-button>
          </div>
        </template>
        <t-empty v-else-if="!loading" description="邀请无效或已过期" />
      </t-loading>
    </t-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { acceptInvitation, previewInvitation, type WorkspaceInvitationVO } from '@/api/workspace'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/datetime'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const accepting = ref(false)
const invitation = ref<WorkspaceInvitationVO | null>(null)

function formatTime(value?: string) {
  return value ? formatDateTime(value) : '-'
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function load() {
  const token = String(route.params.token || '')
  if (!token) return
  loading.value = true
  try {
    const { data } = await previewInvitation(token)
    invitation.value = data.data
  } catch {
    invitation.value = null
  } finally {
    loading.value = false
  }
}

async function onAccept() {
  const token = String(route.params.token || '')
  accepting.value = true
  try {
    await acceptInvitation(token)
    MessagePlugin.success('已加入工作空间')
    await auth.hydrate()
    router.replace('/team')
  } catch {
    // http interceptor handles message
  } finally {
    accepting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.invite-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--td-bg-color-page);
}
.invite-card {
  width: min(480px, 100%);
  padding: 8px;
}
.invite-title {
  margin: 0 0 12px;
  font-size: 22px;
}
.invite-desc {
  margin: 0 0 8px;
  color: var(--td-text-color-secondary);
}
.invite-meta {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}
.invite-actions {
  display: flex;
  gap: 12px;
}
</style>
