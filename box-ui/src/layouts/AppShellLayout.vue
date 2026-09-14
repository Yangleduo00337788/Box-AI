<template>
  <t-layout class="app-layout" :class="`app-layout--${shellVariant}`">
    <t-aside
      v-if="!hideSidebar"
      :width="asideWidth"
      class="app-aside"
      :class="{ 'app-aside--consumer-collapsed': isConsumerCollapsed }"
    >
      <div
        class="sidebar"
        :class="{ 'sidebar--collapsed': isAdminIconRail }"
      >
        <div class="sidebar-header">
          <div
            class="sidebar-header__inner"
            :class="{ 'sidebar-header__inner--collapsed': isAdminIconRail }"
          >
            <router-link
              v-if="shellVariant === 'consumer'"
              to="/chat"
              class="sidebar-brand-link"
              aria-label="返回对话"
            >
              <brand-wordmark
                size="sm"
                align="left"
                mode="mascot"
                :collapsed="isAdminIconRail"
                class="sidebar-brand"
              />
            </router-link>
            <brand-wordmark
              v-else
              size="sm"
              align="center"
              mode="image"
              :collapsed="isAdminIconRail"
              class="sidebar-brand"
            />
            <div
              v-if="shellVariant === 'consumer'"
              class="sidebar-header__actions"
            >
              <t-tooltip content="折叠导航" placement="bottom" theme="light" :show-arrow="false" attach="body">
                <button
                  type="button"
                  class="sidebar-header-btn sidebar-collapse"
                  aria-label="折叠导航"
                  @click="collapsed = true"
                >
                  <sidebar-collapse-icon :collapsed="false" />
                </button>
              </t-tooltip>
              <t-tooltip content="搜索" placement="bottom" theme="light" :show-arrow="false" attach="body">
                <button
                  type="button"
                  class="sidebar-header-btn"
                  aria-label="搜索"
                  @click="onSearch"
                >
                  <t-icon name="search" />
                </button>
              </t-tooltip>
            </div>
            <t-tooltip
              v-else
              :content="isAdminIconRail ? '展开导航' : '折叠导航'"
              placement="bottom"
              theme="light"
              :show-arrow="false"
              attach="body"
            >
              <button
                type="button"
                class="sidebar-header-btn sidebar-collapse"
                :aria-label="isAdminIconRail ? '展开导航' : '折叠导航'"
                @click="collapsed = !collapsed"
              >
                <sidebar-collapse-icon :collapsed="isAdminIconRail" />
              </button>
            </t-tooltip>
          </div>
        </div>

        <nav class="sidebar-nav">
          <slot name="sidebar-nav" :collapsed="isAdminIconRail">
            <section v-for="group in menuGroups" :key="group.title" class="nav-group">
              <h3 v-if="!isAdminIconRail" class="nav-group__title">{{ group.title }}</h3>
              <router-link
                v-for="item in group.items"
                :key="item.value"
                :to="item.value"
                class="nav-item"
                :class="{ 'nav-item--active': active === item.value }"
                :title="isAdminIconRail ? item.label : undefined"
              >
                <t-icon :name="item.icon" class="nav-item__icon" />
                <span v-if="!isAdminIconRail" class="nav-item__label">{{ item.label }}</span>
              </router-link>
            </section>
          </slot>
        </nav>

        <div class="sidebar-footer">
          <slot name="footer-extra" :collapsed="isAdminIconRail" />
          <slot
            name="sidebar-footer"
            :collapsed="isAdminIconRail"
            :user-name="userName"
            :user-hint="userHint"
            :avatar-text="avatarText"
          >
            <t-dropdown :options="resolvedUserMenu" @click="onUserMenu">
              <div class="sidebar-user" :class="{ 'sidebar-user--collapsed': isAdminIconRail }">
                <t-avatar size="small" shape="round">{{ avatarText }}</t-avatar>
                <div v-if="!isAdminIconRail" class="sidebar-user__meta">
                  <span class="sidebar-user__name">{{ userName }}</span>
                  <span class="sidebar-user__hint">{{ userHint }}</span>
                </div>
                <t-icon v-if="!isAdminIconRail" name="chevron-down" class="sidebar-user__arrow" />
              </div>
            </t-dropdown>
          </slot>
        </div>
      </div>
    </t-aside>

    <t-content
      class="app-content"
      :class="{
        'app-content--panel': contentPanel,
        'app-content--panel-padded': contentPanel && contentPadded,
      }"
    >
      <div
        class="page-container"
        :class="{
          'page-container--panel': contentPanel,
          'page-container--panel-padded': contentPanel && contentPadded,
        }"
      >
        <Transition name="sidebar-expand">
          <t-tooltip
            v-if="isConsumerCollapsed && !hideSidebar"
            content="展开导航"
            placement="right"
            theme="light"
            :show-arrow="false"
            attach="body"
          >
            <button
              type="button"
              class="sidebar-expand-trigger"
              aria-label="展开导航"
              @click="collapsed = false"
            >
              <sidebar-collapse-icon :collapsed="true" />
            </button>
          </t-tooltip>
        </Transition>
        <router-view />
      </div>
    </t-content>
  </t-layout>
</template>

<script setup lang="ts">
import { computed, ref, withDefaults } from 'vue'
import type { DropdownOption } from 'tdesign-vue-next'
import BrandWordmark from '../components/BrandWordmark.vue'
import SidebarCollapseIcon from '../components/SidebarCollapseIcon.vue'
import type { MenuGroup } from '../types/menu'

const props = withDefaults(
  defineProps<{
    menuGroups: MenuGroup[]
    active: string
    userName: string
    userHint: string
    shellVariant?: 'consumer' | 'admin'
    contentPanel?: boolean
    contentPadded?: boolean
    hideSidebar?: boolean
    userMenuOptions?: DropdownOption[]
  }>(),
  {
    shellVariant: 'consumer',
    contentPanel: false,
    contentPadded: true,
    hideSidebar: false,
  },
)

const emit = defineEmits<{
  logout: []
  'user-menu': [value: string | number]
  search: []
}>()

const collapsed = ref(false)

const isConsumerCollapsed = computed(
  () => props.shellVariant === 'consumer' && collapsed.value,
)

const isAdminIconRail = computed(
  () => props.shellVariant === 'admin' && collapsed.value,
)

const asideWidth = computed(() => {
  if (props.shellVariant === 'consumer') {
    return isConsumerCollapsed.value ? '0px' : '260px'
  }
  return collapsed.value ? '72px' : '260px'
})

const avatarText = computed(() => props.userName.slice(0, 1).toUpperCase())

const resolvedUserMenu = computed<DropdownOption[]>(() =>
  props.userMenuOptions?.length
    ? props.userMenuOptions
    : [{ content: '退出登录', value: 'logout' }],
)

function onUserMenu(data: { value?: string | number }) {
  if (data?.value === undefined) return
  if (data.value === 'logout') {
    emit('logout')
    return
  }
  emit('user-menu', data.value)
}

function onSearch() {
  emit('search')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: row;
  height: 100vh;
  overflow: hidden;
  background: var(--box-bg);
  box-sizing: border-box;
}

.app-layout--consumer {
  padding: 6px;
  background: var(--box-sidebar-bg);
}

.app-aside {
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  background: var(--box-surface);
  box-shadow: 1px 0 0 var(--box-border);
  transition:
    width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    flex-basis 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    min-width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-layout--consumer .app-aside {
  flex: 0 0 auto;
  min-width: 0;
  background: transparent;
  box-shadow: none;
}

.app-layout--consumer .sidebar {
  width: 260px;
  min-width: 260px;
  transition: opacity 0.22s ease, transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-layout--consumer .app-aside--consumer-collapsed .sidebar {
  opacity: 0;
  transform: translateX(-12px);
  pointer-events: none;
}

.app-layout--consumer .app-aside:not(.app-aside--consumer-collapsed) .sidebar {
  opacity: 1;
  transform: translateX(0);
}

.app-layout--consumer .app-content--panel {
  transition: flex 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: inherit;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 72px;
  padding: 12px 8px;
  border-bottom: 1px solid var(--box-border);
  flex-shrink: 0;
  background: inherit;
}

.sidebar--collapsed .sidebar-header {
  min-height: 56px;
  padding: 8px 4px;
}

.sidebar-header__inner {
  display: flex;
  align-items: center;
  width: 100%;
}

.sidebar-header__inner:not(.sidebar-header__inner--collapsed) {
  position: relative;
  justify-content: space-between;
  gap: 8px;
}

.sidebar-header__inner--collapsed {
  justify-content: center;
  gap: 0;
}

.sidebar-header__actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.sidebar-header__actions :deep(.t-popup__reference) {
  display: inline-flex;
}

.sidebar-brand-link {
  display: flex;
  flex: 1;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.sidebar-brand {
  display: flex;
  justify-content: flex-start;
  min-width: 0;
}

.sidebar-header__inner:not(.sidebar-header__inner--collapsed) .sidebar-brand {
  flex: 1;
}

.sidebar-header__inner--collapsed .sidebar-brand {
  flex: none;
}

.sidebar-header-btn,
.sidebar-collapse {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid var(--box-border);
  border-radius: 8px;
  background: var(--box-surface);
  color: var(--box-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.sidebar-header-btn:hover,
.sidebar-collapse:hover {
  border-color: var(--td-gray-color-4);
  background: var(--td-gray-color-1);
  color: var(--box-ink);
}

.sidebar-header-btn :deep(.t-icon) {
  font-size: 18px;
}

.sidebar-header__inner--collapsed .sidebar-collapse {
  margin-left: -4px;
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px;
  scrollbar-width: none;
}

.sidebar-nav::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.nav-group {
  margin-bottom: 4px;
}

.nav-group__title {
  margin: 0;
  padding: 8px 12px 4px;
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
  color: var(--box-muted);
  text-align: left;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  margin-bottom: 2px;
  padding: 0 12px;
  border-radius: 8px;
  color: var(--box-muted);
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}

.sidebar--collapsed .nav-item {
  justify-content: center;
  padding: 0;
}

.nav-item:hover {
  background: var(--td-gray-color-1);
  color: var(--box-ink);
  opacity: 1;
}

.nav-item--active {
  background: var(--td-gray-color-2);
  color: var(--box-ink);
  font-weight: 500;
}

.nav-item__icon {
  flex-shrink: 0;
  font-size: 18px;
}

.nav-item__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font: var(--td-font-body-medium);
  text-align: left;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 12px 12px;
  flex-shrink: 0;
  background: inherit;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-user:hover {
  background: var(--td-gray-color-1);
}

.sidebar-user--collapsed {
  justify-content: center;
  padding: 8px;
}

.sidebar-user__meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  text-align: left;
}

.sidebar-user__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font: var(--td-font-body-medium);
  color: var(--box-ink);
}

.sidebar-user__hint {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.sidebar-user__arrow {
  color: var(--box-muted);
  font-size: 14px;
}

.app-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
  padding: 24px 32px 32px;
  overflow-x: hidden;
  overflow-y: auto;
}

.app-layout--consumer .app-content {
  background: transparent;
}

.app-layout--consumer .app-content--panel {
  flex: 1;
  min-width: 0;
  padding: 0;
  overflow: hidden;
}

.app-layout--consumer.t-layout {
  flex-direction: row;
}

.app-layout--consumer > .t-layout__content {
  flex: 1;
  min-width: 0;
}

.app-layout--consumer .page-container--panel {
  position: relative;
  width: 100%;
  max-width: none;
  margin: 0;
  height: 100%;
  min-height: 0;
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
  overflow: hidden;
  box-shadow: none;
}

.sidebar-expand-trigger {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.sidebar-expand-trigger:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--box-ink);
}

.sidebar-expand-enter-active,
.sidebar-expand-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.sidebar-expand-enter-from,
.sidebar-expand-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.app-layout--consumer .page-container--panel-padded {
  padding: 24px 32px 32px;
  overflow-x: hidden;
  overflow-y: auto;
}

.app-layout--consumer .sidebar-collapse,
.app-layout--consumer .sidebar-header-btn {
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
}

.app-layout--consumer .sidebar-collapse:hover,
.app-layout--consumer .sidebar-header-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--box-ink);
}

.app-layout--consumer .sidebar-header__inner:not(.sidebar-header__inner--collapsed) {
  justify-content: space-between;
  padding-left: 4px;
}

.app-layout--consumer .sidebar-header {
  min-height: 48px;
  padding: 8px 10px;
  border-bottom: none;
}

.app-layout--consumer .sidebar-header__actions {
  gap: 8px;
}

.app-layout--consumer .sidebar-brand {
  justify-content: flex-start;
}

.app-layout--consumer .nav-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.app-layout--consumer .nav-item--active {
  background: rgba(0, 0, 0, 0.04);
  color: var(--box-ink);
  font-weight: 500;
}

.app-layout--consumer .nav-item--active .nav-item__icon {
  color: var(--box-ink);
}

.app-layout--admin .app-aside {
  background: #fafbfc;
}

.app-layout--admin .nav-group__title {
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-size: 10px;
}

.app-layout--admin .app-content {
  background: #f3f4f6;
}

.app-layout--admin .page-container {
  max-width: 1280px;
}

@media (prefers-reduced-motion: reduce) {
  .app-aside,
  .app-layout--consumer .sidebar,
  .app-layout--consumer .app-content--panel,
  .sidebar-expand-enter-active,
  .sidebar-expand-leave-active {
    transition: none !important;
  }
}

</style>
