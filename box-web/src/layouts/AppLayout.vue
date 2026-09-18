<template>
  <div class="consumer-frame">
    <ops-global-alert />
    <app-shell-layout
      shell-variant="consumer"
      :menu-groups="CONSUMER_MENU_GROUPS"
      :active="active"
      content-panel
      :content-padded="contentPadded"
      :hide-sidebar="isSettingsRoute"
      :user-name="userName"
      user-hint="Box"
      @search="searchVisible = true"
    >
    <template #sidebar-nav>
      <consumer-sidebar-workspace :active="active" />
    </template>
    <template #sidebar-footer="{ collapsed, userName: slotUserName, avatarText }">
      <consumer-sidebar-footer
        :collapsed="collapsed"
        :user-name="slotUserName"
        :avatar-text="avatarText"
        :avatar-url="auth.user?.avatarUrl"
        @logout="onLogout"
      />
    </template>
  </app-shell-layout>
    <ops-corner-promo />
  </div>

  <global-search-dialog v-model:visible="searchVisible" />
  <create-agent-dialog @created="onAgentCreated" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import AppShellLayout from '@box/ui/layouts/AppShellLayout.vue'
import ConsumerSidebarFooter from '@/components/ConsumerSidebarFooter.vue'
import ConsumerSidebarWorkspace from '@/components/ConsumerSidebarWorkspace.vue'
import CreateAgentDialog from '@/components/CreateAgentDialog.vue'
import OpsCornerPromo from '@/components/OpsCornerPromo.vue'
import OpsGlobalAlert from '@/components/OpsGlobalAlert.vue'
import GlobalSearchDialog from '@/components/GlobalSearchDialog.vue'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { CONSUMER_MENU_GROUPS } from '@/constants/menu'
import { loadAppPreferencesFromServer } from '@/composables/useAppPreferences'
import { useAuthStore } from '@/stores/auth'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const permissionStore = usePermissionStore()
const searchVisible = ref(false)
const { refresh: refreshAgents } = useAgentSelection()

async function onAgentCreated() {
  await refreshAgents()
}

const userName = computed(() => auth.user?.nickname || auth.user?.username || auth.user?.email || '用户')
const isSettingsRoute = computed(() => route.path.startsWith('/settings'))

const active = computed(() => {
  const path = route.path
  if (path === '/chat') {
    return '/chat'
  }
  if (path.startsWith('/chat/')) {
    return ''
  }
  if (path === '/agents' || path.startsWith('/agents/')) {
    return ''
  }
  for (const group of CONSUMER_MENU_GROUPS) {
    for (const item of group.items) {
      if (path === item.value || path.startsWith(`${item.value}/`)) {
        return item.value
      }
    }
  }
  if (path.startsWith('/dashboard')) return ''
  if (path.startsWith('/analytics')) return ''
  if (path.startsWith('/settings')) return ''
  return path
})

const contentPadded = computed(() => {
  if (route.path === '/chat' || route.path.startsWith('/chat/')) {
    return false
  }
  if (route.path.startsWith('/settings')) {
    return false
  }
  return route.name !== 'agent-builder'
})

function onLogout() {
  auth.logout()
  MessagePlugin.success('已退出登录')
  router.push('/login')
}

onMounted(() => {
  if (auth.token) {
    loadAppPreferencesFromServer()
    void permissionStore.load()
  }
})

watch(
  () => route.fullPath,
  (path) => {
    if (!path.startsWith('/settings')) {
      sessionStorage.setItem('box.settingsReturnPath', route.path)
    }
  },
  { immediate: true },
)

watch(
  () => auth.currentWorkspaceId,
  (workspaceId, previousId) => {
    if (!auth.token || !workspaceId || workspaceId === previousId) {
      return
    }
    void permissionStore.load(true)
    void refreshAgents()
    const path = route.path
    if (/^\/chat\/\d+/.test(path)) {
      void router.replace('/chat')
    } else if (/^\/agents\/\d+/.test(path)) {
      void router.replace('/agents')
    } else if (/^\/workflows\/\d+/.test(path)) {
      void router.replace('/workflows')
    }
  },
)
</script>

<style scoped>
.consumer-frame {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.consumer-frame :deep(.app-layout) {
  flex: 1;
  min-height: 0;
  height: auto;
}
</style>

