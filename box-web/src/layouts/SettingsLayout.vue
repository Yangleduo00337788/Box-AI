<template>
  <div class="settings-shell">
    <aside class="settings-nav">
      <t-button variant="text" class="settings-nav__back" @click="router.push('/chat')">
        <template #icon><t-icon name="chevron-left" /></template>
        返回对话
      </t-button>

      <section v-for="group in SETTINGS_NAV" :key="group.title" class="settings-nav__group">
        <p class="settings-nav__title">{{ group.title }}</p>
        <router-link
          v-for="item in group.items"
          :key="item.value"
          :to="item.value"
          class="settings-nav__item"
          :class="{ 'settings-nav__item--active': route.path === item.value }"
        >
          <t-icon :name="item.icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </section>
    </aside>
    <main class="settings-main">
      <div class="settings-main__inner">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { SETTINGS_NAV } from '@/constants/settings'

const route = useRoute()
const router = useRouter()
</script>

<style scoped>
.settings-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  background: var(--box-surface);
}

.settings-nav {
  width: 220px;
  flex-shrink: 0;
  padding: 24px 12px;
  border-right: 1px solid var(--box-border);
  overflow-y: auto;
}

.settings-nav__back {
  margin-bottom: 16px;
  justify-content: flex-start;
  color: var(--box-ink);
}

.settings-nav__group {
  margin-bottom: 18px;
}

.settings-nav__title {
  margin: 0 8px 6px;
  font-size: 12px;
  color: var(--box-muted);
}

.settings-nav__item {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 8px;
  border-radius: 8px;
  color: var(--box-ink);
  text-decoration: none;
  font-size: 14px;
  transition: background 0.2s;
}

.settings-nav__item:hover {
  background: var(--box-hover);
}

.settings-nav__item--active {
  background: rgba(0, 0, 0, 0.04);
  font-weight: 500;
}

.settings-main {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: center;
  padding: 32px 40px;
  overflow: auto;
}

.settings-main__inner {
  width: 100%;
  max-width: 720px;
}
</style>
