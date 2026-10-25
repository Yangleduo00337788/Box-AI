<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DemoBanner from '@/components/DemoBanner.vue'
import { adminNav, consumerNav } from '@/data/demoRegistry'

const route = useRoute()
const router = useRouter()

const realm = computed(() => (route.path.startsWith('/demo/admin') ? 'admin' : 'web'))

const navItems = computed(() => (realm.value === 'admin' ? adminNav : consumerNav))

function switchRealm(next: 'web' | 'admin') {
  router.push(next === 'admin' ? '/demo/admin/tenants' : '/demo/web/chat')
}

function onRealmChange(value: string) {
  switchRealm(value as 'web' | 'admin')
}
</script>

<template>
  <div class="demo-shell">
    <demo-banner />
    <header class="demo-shell__top">
      <router-link to="/" class="demo-shell__brand">Box AI 演示</router-link>
      <t-radio-group :value="realm" variant="default-filled" @change="onRealmChange">
        <t-radio-button value="web">用户工作台</t-radio-button>
        <t-radio-button value="admin">平台管理台</t-radio-button>
      </t-radio-group>
      <t-button theme="default" variant="outline" @click="router.push('/')">返回首页</t-button>
    </header>
    <div class="demo-shell__body">
      <aside class="demo-shell__aside">
        <t-menu :value="route.path" theme="light" width="240px">
          <t-menu-item
            v-for="item in navItems"
            :key="item.id"
            :value="item.path"
            :class="{ 't-is-active': route.path === item.path }"
            @click="router.push(item.path)"
          >
            {{ item.title }}
          </t-menu-item>
        </t-menu>
      </aside>
      <main class="demo-shell__main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.demo-shell__top {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--td-component-border);
  background: var(--td-bg-color-container);
  flex-wrap: wrap;
}

.demo-shell__brand {
  font-weight: 600;
  color: inherit;
  text-decoration: none;
  margin-right: auto;
}

.demo-shell__brand:hover {
  color: var(--td-brand-color);
}
</style>
