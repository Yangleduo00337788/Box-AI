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
        <div
          v-for="item in items"
          :key="notificationItemKey(item)"
          class="notification-item"
          :class="{ 'notification-item--unread': isNotificationUnread(item) }"
        >
          <button type="button" class="notification-item__open" @click="openItem(item)">
            <div class="notification-item__main">
              <div class="notification-item__title">
                <span v-if="item.category === 'OPS_ANNOUNCEMENT'" class="notification-item__tag">公告</span>
                {{ item.title }}
              </div>
              <div v-if="item.content" class="notification-item__content">{{ item.content }}</div>
              <div class="notification-item__time">{{ item.createdAt }}</div>
            </div>
            <span v-if="isNotificationUnread(item)" class="notification-item__dot" aria-label="未读" />
          </button>
          <div v-if="item.key && item.dismissible" class="notification-item__actions">
            <button type="button" class="notification-item__link" @click="dismissItem(item)">不再提醒</button>
          </div>
        </div>
      </div>
      <p v-else-if="!loading" class="notification-center__empty">暂无站内信</p>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  dismissNotificationByKey,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationReadByKey,
  isNotificationUnread,
  notificationItemKey,
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

function navigateLink(url: string) {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  void router.push(url)
}

async function openItem(item: NotificationVO) {
  if (isNotificationUnread(item)) {
    if (item.key) {
      await markNotificationReadByKey(item.key)
    } else if (item.id != null) {
      await markNotificationRead(item.id)
    }
    item.read = true
    await refreshUnread()
  }
  if (item.linkUrl) {
    navigateLink(item.linkUrl)
  }
}

async function dismissItem(item: NotificationVO) {
  if (!item.key) return
  await dismissNotificationByKey(item.key)
  items.value = items.value.filter((row) => notificationItemKey(row) !== notificationItemKey(item))
  await refreshUnread()
}

async function markAllRead() {
  await markAllNotificationsRead()
  items.value.forEach((item) => {
    item.read = true
  })
  await refreshUnread()
}
</script>

<style scoped>
.notification-center {
  width: 320px;
  max-height: 400px;
  overflow: auto;
  padding: 8px 0;
}

.notification-center::-webkit-scrollbar {
  width: 6px;
}

.notification-center__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 12px 8px;
  font-size: 13px;
  font-weight: 600;
}

.notification-center__read {
  border: none;
  background: transparent;
  color: var(--td-brand-color);
  font-size: 12px;
  cursor: pointer;
}

.notification-center__read:disabled {
  color: var(--td-text-color-disabled);
  cursor: not-allowed;
}

.notification-center :deep(.t-loading),
.notification-center :deep(.t-loading__parent) {
  min-height: 80px;
}

.notification-center__empty {
  margin: 0;
  padding: 24px 12px;
  text-align: center;
  color: var(--td-text-color-placeholder);
  font-size: 13px;
}

.notification-item {
  border-bottom: 1px solid var(--td-component-stroke);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item__open {
  display: flex;
  width: 100%;
  gap: 8px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.notification-item:hover .notification-item__open {
  background: var(--td-bg-color-container-hover);
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
  background: var(--td-error-color);
}

.notification-item--unread .notification-item__title {
  font-weight: 600;
}

.notification-item__title {
  font-size: 13px;
  line-height: 1.4;
}

.notification-item__tag {
  display: inline-block;
  margin-right: 6px;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--td-brand-color-light);
  color: var(--td-brand-color);
  font-size: 11px;
  font-weight: 500;
}

.notification-item__content {
  margin-top: 4px;
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-item__time {
  margin-top: 6px;
  color: var(--td-text-color-placeholder);
  font-size: 11px;
}

.notification-item__actions {
  padding: 0 12px 8px;
}

.notification-item__link {
  border: none;
  background: transparent;
  color: var(--td-text-color-secondary);
  font-size: 12px;
  cursor: pointer;
}

.notification-item__link:hover {
  color: var(--td-brand-color);
}
</style>
