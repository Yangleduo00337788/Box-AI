<template>
  <t-layout class="settings-shell">
    <t-aside class="settings-aside" width="240px">
      <t-button variant="text" class="settings-aside__back" @click="goBack">
        <template #icon><t-icon name="chevron-left" /></template>
        返回工作台
      </t-button>
      <t-menu
        :value="activeMenu"
        :theme="menuTheme"
        width="240px"
        class="settings-menu"
        @change="onMenuChange"
      >
        <t-menu-group v-for="group in settingsNav" :key="group.title" :title="group.title">
          <t-menu-item v-for="item in group.items" :key="item.value" :value="item.value">
            <template #icon>
              <t-icon :name="item.icon" />
            </template>
            {{ item.label }}
          </t-menu-item>
        </t-menu-group>
      </t-menu>
    </t-aside>
    <t-content class="settings-content">
      <div class="settings-content__inner">
        <router-view />
      </div>
    </t-content>
  </t-layout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuValue } from 'tdesign-vue-next'
import { SETTINGS_NAV } from '@/constants/settings'
import { appPreferences } from '@/composables/useAppPreferences'
import { usePermissionStore } from '@/stores/permission'

const RETURN_PATH_KEY = 'box.settingsReturnPath'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const settingsNav = computed(() => {
  return SETTINGS_NAV.map((group) => ({
    ...group,
    items: group.items.filter((item) => !item.permission || permissionStore.can(item.permission)),
  })).filter((group) => group.items.length > 0)
})

const activeMenu = computed(() => {
  if (route.path === '/settings/quota' || route.path === '/settings/capacity' || route.path === '/settings/billing') {
    return '/settings/plan'
  }
  if (route.path === '/settings/appearance' || route.path === '/settings/general') {
    return '/settings/preferences'
  }
  return route.path
})

const menuTheme = computed(() => {
  if (appPreferences.theme === 'dark') return 'dark'
  if (appPreferences.theme === 'light') return 'light'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
})

function onMenuChange(value: MenuValue) {
  const path = String(value)
  if (path && path !== route.path) {
    void router.push(path)
  }
}

function goBack() {
  const fallback = sessionStorage.getItem(RETURN_PATH_KEY) || '/chat'
  void router.push(fallback.startsWith('/settings') ? '/chat' : fallback)
}
</script>

<style scoped>
.settings-shell {
  height: 100%;
  min-height: 0;
  background: var(--box-surface);
}

.settings-aside {
  display: flex;
  flex-direction: column;
  padding: 16px 0 24px;
  border-right: 1px solid var(--box-border);
  background: var(--box-sidebar);
  overflow: hidden;
}

.settings-aside__back {
  margin: 0 12px 8px;
  justify-content: flex-start;
  color: var(--box-ink);
}

.settings-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background: transparent;
}

.settings-menu :deep(.t-default-menu),
.settings-menu :deep(.t-default-menu__inner) {
  width: 100%;
  height: auto;
  background: transparent !important;
  border-right: none;
}

.settings-menu :deep(.t-menu__item) {
  color: var(--box-ink);
}

.settings-menu :deep(.t-menu__item.t-is-active:not(.t-is-opened)) {
  background: var(--box-active) !important;
  color: var(--box-ink) !important;
}

.settings-menu :deep(.t-menu__item:hover:not(.t-is-active):not(.t-is-disabled)) {
  background: var(--box-hover) !important;
}

.settings-content {
  min-width: 0;
  padding: 32px 40px 48px;
  overflow: auto;
  background: var(--box-surface);
}

.settings-content__inner {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

@media (max-width: 900px) {
  .settings-content {
    padding: 24px 20px 40px;
  }
}
</style>
