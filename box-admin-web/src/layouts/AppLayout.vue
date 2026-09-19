<template>
  <t-layout
    class="admin-layout"
    :class="{
      'admin-layout--dark-aside': appearance.menuTheme === 'dark',
      'admin-layout--collapsed': collapsed,
    }"
    :style="appearance.brandStyle"
  >
    <t-aside :width="collapsed ? '64px' : '232px'" class="admin-aside">
      <t-menu
        :theme="appearance.menuTheme"
        :value="active"
        :collapsed="collapsed"
        :width="['232px', '64px']"
        @change="onMenuChange"
      >
        <template #logo>
          <div class="admin-logo" :class="{ 'admin-logo--collapsed': collapsed }">
            <img
              :src="collapsed ? logoCollapsed : logoWordmark"
              :alt="collapsed ? '盒子' : '盒子管理后台'"
              class="admin-logo__img"
              :class="{ 'admin-logo__img--collapsed': collapsed }"
            />
          </div>
        </template>
        <t-menu-group v-for="group in visibleMenuGroups" :key="group.title" :title="group.title">
          <t-menu-item
            v-for="item in group.items"
            :key="item.value"
            :value="item.value"
            :to="item.value"
          >
            <template #icon>
              <t-icon :name="item.icon" />
            </template>
            {{ item.label }}
          </t-menu-item>
        </t-menu-group>
      </t-menu>
    </t-aside>

    <t-layout class="admin-main">
      <t-header height="56px" class="admin-header">
        <t-button variant="text" shape="square" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="collapsed = !collapsed">
          <template #icon>
            <t-icon :name="collapsed ? 'view-list' : 'menu-fold'" />
          </template>
        </t-button>
        <t-breadcrumb class="admin-header__crumb">
          <t-breadcrumb-item>平台管理</t-breadcrumb-item>
          <t-breadcrumb-item>{{ currentTitle }}</t-breadcrumb-item>
        </t-breadcrumb>
        <t-space class="admin-header__ops" align="center" :size="4">
          <admin-notification-center />
          <t-tooltip content="主题配置" theme="light" :show-arrow="false">
            <t-button variant="text" shape="square" aria-label="主题配置" @click="themeVisible = true">
              <template #icon>
                <t-icon name="setting" />
              </template>
            </t-button>
          </t-tooltip>
          <t-dropdown :options="userMenu" @click="onUserMenu">
            <div class="admin-user">
              <t-avatar size="32px">{{ avatarText }}</t-avatar>
              <span class="admin-user__name">{{ userName }}</span>
              <t-icon name="chevron-down" size="16px" />
            </div>
          </t-dropdown>
        </t-space>
      </t-header>
      <admin-ops-header-alert />
      <t-content class="admin-content">
        <router-view />
      </t-content>
    </t-layout>
  </t-layout>

  <theme-setting-drawer v-model:visible="themeVisible" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption, MenuValue } from 'tdesign-vue-next'
import logoWordmark from '@/assets/logo.png'
import logoCollapsed from '@/assets/logo-collapsed.png'
import AdminNotificationCenter from '@/components/AdminNotificationCenter.vue'
import AdminOpsHeaderAlert from '@/components/AdminOpsHeaderAlert.vue'
import ThemeSettingDrawer from '@/components/ThemeSettingDrawer.vue'
import { ADMIN_MENU_GROUPS } from '@/constants/menu'
import { canAccessAdminRoute } from '@/constants/rbac'
import { useAppearanceStore } from '@/stores/appearance'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const appearance = useAppearanceStore()
const collapsed = ref(false)
const themeVisible = ref(false)

const active = computed(() => route.path)
const userName = computed(() => auth.user?.nickname || auth.user?.email || '管理员')
const avatarText = computed(() => userName.value.slice(0, 1).toUpperCase())

const visibleMenuGroups = computed(() =>
  ADMIN_MENU_GROUPS.map((group) => ({
    ...group,
    items: group.items.filter((item) => canAccessAdminRoute(auth.platformRole, item.value)),
  })).filter((group) => group.items.length),
)

const currentTitle = computed(() => {
  for (const group of visibleMenuGroups.value) {
    const hit = group.items.find((item) => item.value === route.path)
    if (hit) return hit.label
  }
  return String(route.meta.title || '控制台')
})

const userMenu: DropdownOption[] = [{ content: '退出登录', value: 'logout' }]

function onMenuChange(value: MenuValue) {
  if (typeof value === 'string' && value !== route.path) {
    void router.push(value)
  }
}

function onUserMenu(data: { value?: string | number }) {
  if (data?.value !== 'logout') return
  auth.logout()
  MessagePlugin.success('已退出登录')
  void router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  flex-direction: row;
  height: 100vh;
  overflow: hidden;
}

.admin-aside {
  flex: 0 0 auto;
  align-self: stretch;
  height: 100%;
  overflow: hidden;
  border-right: none;
  background: var(--admin-candy-gradient);
  box-shadow: none;
}

.admin-aside :deep(.t-default-menu),
.admin-aside :deep(.t-default-menu__inner) {
  height: 100%;
  background: transparent !important;
}

.admin-aside :deep(.t-default-menu .t-menu__logo) {
  height: 56px;
  justify-content: flex-start;
  padding: 0;
  border-bottom: none !important;
  box-shadow: none !important;
}

.admin-aside :deep(.t-default-menu .t-menu__logo::after),
.admin-aside :deep(.t-default-menu .t-menu__logo::before) {
  display: none !important;
}

.admin-aside :deep(.t-default-menu__inner::before) {
  display: none;
}

.admin-aside :deep(.t-default-menu__inner .t-menu-group__title) {
  padding: 12px 16px 6px;
  color: var(--td-text-color-placeholder);
  text-align: left;
}

.admin-aside :deep(.t-default-menu .t-menu__item) {
  margin: 2px 8px;
  padding-left: 8px;
  justify-content: flex-start;
}

.admin-aside :deep(.t-default-menu .t-menu__item .t-icon) {
  margin-right: 8px;
}

.admin-layout:not(.admin-layout--dark-aside) .admin-aside :deep(.t-default-menu .t-menu__item:hover:not(.t-is-active):not(.t-is-disabled)) {
  background: rgba(255, 255, 255, 0.55) !important;
}

.admin-layout:not(.admin-layout--dark-aside) .admin-aside :deep(.t-default-menu .t-menu__item.t-is-active:not(.t-is-opened)),
.admin-layout:not(.admin-layout--dark-aside) .admin-aside :deep(.t-default-menu .t-submenu.t-is-active > .t-menu__item) {
  background: var(--admin-candy-active-bg) !important;
  box-shadow: inset 3px 0 0 var(--td-text-color-primary);
  color: var(--td-text-color-primary) !important;
  font-weight: 600;
}

.admin-layout:not(.admin-layout--dark-aside) .admin-aside :deep(.t-default-menu .t-menu__item.t-is-active:not(.t-is-opened) .t-icon),
.admin-layout:not(.admin-layout--dark-aside) .admin-aside :deep(.t-default-menu .t-submenu.t-is-active > .t-menu__item .t-icon) {
  color: var(--td-text-color-primary) !important;
}

.admin-layout--dark-aside .admin-aside {
  background: var(--td-gray-color-13);
  box-shadow: none;
}

.admin-layout--dark-aside .admin-aside :deep(.t-default-menu) {
  background: var(--td-gray-color-13);
}

.admin-layout--dark-aside .admin-aside :deep(.t-default-menu .t-menu__item.t-is-active:not(.t-is-opened)),
.admin-layout--dark-aside .admin-aside :deep(.t-default-menu .t-submenu.t-is-active > .t-menu__item) {
  background: var(--td-brand-color) !important;
  color: #fff !important;
}

.admin-layout--dark-aside .admin-aside :deep(.t-default-menu .t-menu__item.t-is-active:not(.t-is-opened) .t-icon),
.admin-layout--dark-aside .admin-aside :deep(.t-default-menu .t-submenu.t-is-active > .t-menu__item .t-icon) {
  color: #fff !important;
}

.admin-layout--dark-aside .admin-aside :deep(.t-default-menu .t-menu__item:hover:not(.t-is-active):not(.t-is-disabled)) {
  background: rgba(255, 255, 255, 0.08);
}

.admin-layout--dark-aside .admin-aside :deep(.t-menu-group__title) {
  color: rgba(255, 255, 255, 0.4);
}

.admin-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.admin-logo {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  width: 100%;
  height: 56px;
  padding: 0 16px;
  box-sizing: border-box;
}

.admin-logo--collapsed {
  justify-content: center;
  padding: 0;
}

.admin-logo__img {
  display: block;
  height: 32px;
  width: auto;
  max-width: 168px;
  object-fit: contain;
}

.admin-logo__img--collapsed {
  height: 36px;
  max-width: 36px;
}

.admin-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid var(--td-component-stroke);
}

.admin-header__crumb {
  flex: 1;
  min-width: 0;
}

.admin-header__ops {
  margin-left: auto;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
}

.admin-user:hover {
  background: var(--td-bg-color-container-hover);
}

.admin-user__name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--td-text-color-primary);
}

.admin-content {
  flex: 1;
  min-height: 0;
  padding: 16px 20px 24px;
  overflow: auto;
  background: #fff;
}
</style>
