<template>
  <t-popup placement="bottom-right" trigger="click" @visible-change="onVisibleChange">
    <t-badge :count="unreadCount" :max-count="99">
      <t-button variant="text" shape="square" size="small" aria-label="通知中心">
        <template #icon><t-icon name="notification" /></template>
      </t-button>
    </t-badge>
    <template #content>
      <div class="notification-center">
        <div class="notification-center__head">
          <strong>通知中心</strong>
          <t-button variant="text" size="small" :disabled="!items.length" @click="markAllRead">全部已读</t-button>
        </div>
        <t-loading :loading="loading" size="small">
          <div v-if="items.length" class="notification-center__list">
            <button
              v-for="item in items"
              :key="item.id"
              type="button"
              class="notification-item"
              :class="{ 'notification-item--unread': !item.read }"
              @click="openItem(item)"
            >
              <div class="notification-item__title">{{ item.title }}</div>
              <div class="notification-item__content">{{ item.content }}</div>
              <div class="notification-item__time">{{ item.createdAt }}</div>
            </button>
          </div>
          <t-empty v-else description="暂无通知" />
        </t-loading>
      </div>
    </template>
  </t-popup>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getUnreadNotificationCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationVO,
} from '@/api/notification'

const router = useRouter()
const loading = ref(false)
const unreadCount = ref(0)
const items = ref<NotificationVO[]>([])

async function refreshUnread() {
  const { data } = await getUnreadNotificationCount()
  unreadCount.value = data.data?.count || 0
}

async function loadItems() {
  loading.value = true
  try {
    const { data } = await listNotifications(30)
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function onVisibleChange(visible: boolean) {
  if (!visible) return
  await Promise.all([loadItems(), refreshUnread()])
}

async function openItem(item: NotificationVO) {
  if (!item.read) {
    await markNotificationRead(item.id)
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  items.value = items.value.map((item) => ({ ...item, read: true }))
  unreadCount.value = 0
}

onMounted(refreshUnread)
</script>

<style scoped>
.notification-center {
  width: 320px;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
}

.notification-center__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.notification-center__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item {
  width: 100%;
  text-align: left;
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
  padding: 10px;
  background: var(--td-bg-color-container);
  cursor: pointer;
}

.notification-item--unread {
  border-color: var(--td-brand-color);
  background: var(--td-brand-color-light);
}

.notification-item__title {
  font-weight: 600;
  margin-bottom: 4px;
}

.notification-item__content {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.notification-item__time {
  margin-top: 6px;
  font-size: 11px;
  color: var(--td-text-color-placeholder);
}
</style>
