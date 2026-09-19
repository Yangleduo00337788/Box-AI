<template>
  <div class="notification-center">
    <div class="notification-center__head">
      <span>站内信</span>
      <button
        type="button"
        class="notification-center__read"
        :disabled="!items.length"
        @click="markAllRead"
      >
        全部已读
      </button>
    </div>
    <t-loading :loading="loading" size="small">
      <div v-if="items.length" class="notification-center__list">
        <button
          v-for="item in items"
          :key="item.id"
          type="button"
          class="notification-item"
          :class="{ 'notification-item--unread': isNotificationUnread(item) }"
          @click="openItem(item)"
        >
          <div class="notification-item__main">
            <div class="notification-item__title">{{ item.title }}</div>
            <div class="notification-item__content">{{ item.content }}</div>
            <div class="notification-item__time">{{ item.createdAt }}</div>
          </div>
          <span v-if="isNotificationUnread(item)" class="notification-item__dot" aria-label="未读" />
        </button>
      </div>
      <p v-else-if="!loading" class="notification-center__empty">暂无站内信</p>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  isNotificationUnread,
  type NotificationVO,
} from '@/api/notification'
import { useNotificationUnread } from '@/composables/useNotificationUnread'

const props = defineProps<{
  active?: boolean
}>()

const router = useRouter()
const { refreshUnread } = useNotificationUnread()
const loading = ref(false)
const items = ref<NotificationVO[]>([])

async function loadItems() {
  loading.value = true
  try {
    const { data } = await listNotifications(30)
    items.value = data.data || []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

async function refreshPanel() {
  await Promise.all([loadItems(), refreshUnread()])
}

watch(
  () => props.active,
  (open) => {
    if (open) {
      void refreshPanel()
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (props.active) {
    void refreshPanel()
  }
})

async function openItem(item: NotificationVO) {
  if (isNotificationUnread(item)) {
    await markNotificationRead(item.id)
    item.read = true
    await refreshUnread()
  }
  if (item.linkUrl) {
    router.push(item.linkUrl)
  }
}

async function markAllRead() {
  await markAllNotificationsRead()
  items.value = items.value.map((item) => ({ ...item, read: true }))
  await refreshUnread()
}
</script>

<style scoped>
.notification-center {
  width: 320px;
  max-height: 360px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 0 4px;
  scrollbar-width: none;
}

.notification-center::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.notification-center__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 16px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
}

.notification-center__read {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--td-brand-color);
  font-size: 12px;
  font-weight: 400;
  cursor: pointer;
}

.notification-center__read:disabled {
  color: #c0c4cc;
  cursor: default;
}

.notification-center :deep(.t-loading),
.notification-center :deep(.t-loading__parent) {
  width: 100%;
}

.notification-center__empty {
  margin: 0;
  padding: 16px;
  font-size: 13px;
  color: var(--box-muted);
  text-align: center;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  border: none;
  border-bottom: 1px solid #f0f1f2;
  border-radius: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notification-item__main {
  flex: 1;
  min-width: 0;
}

.notification-item__dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #e34d59;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: #f7f8fa;
}

.notification-item--unread .notification-item__title {
  font-weight: 600;
}

.notification-item__title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
}

.notification-item__content {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
}

.notification-item__time {
  margin-top: 4px;
  font-size: 11px;
  color: var(--td-text-color-placeholder);
}
</style>
