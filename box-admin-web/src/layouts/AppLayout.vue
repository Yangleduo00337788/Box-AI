<template>
  <app-shell-layout
    shell-variant="admin"
    :menu-groups="ADMIN_MENU_GROUPS"
    :active="active"
    :user-name="userName"
    user-hint="平台管理端"
    @logout="onLogout"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import AppShellLayout from '@box/ui/layouts/AppShellLayout.vue'
import { ADMIN_MENU_GROUPS } from '@/constants/menu'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const active = computed(() => route.path)
const userName = computed(() => auth.user?.nickname || auth.user?.email || '管理员')

function onLogout() {
  auth.logout()
  MessagePlugin.success('已退出登录')
  router.push('/login')
}
</script>
