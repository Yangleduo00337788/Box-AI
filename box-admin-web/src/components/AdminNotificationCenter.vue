<template>
  <t-popup
    v-model:visible="visible"
    trigger="click"
    placement="bottom-right"
    :overlay-inner-style="{ padding: 0 }"
    destroy-on-close
  >
    <template #content>
      <div class="admin-inbox">
        <div class="admin-inbox__head">
          <span>消息通知</span>
          <t-space :size="0">
            <t-button
              v-if="unreadCount > 0"
              variant="text"
              size="small"
              @click="onMarkAllRead"
            >
              全部已读
            </t-button>
            <t-button variant="text" size="small" @click="refreshInbox">刷新</t-button>
          </t-space>
        </div>
        <div v-if="!items.length" class="admin-inbox__empty">
          <t-empty description="暂无新通知" />
        </div>
        <ul v-else class="admin-inbox__list">
          <li
            v-for="item in items"
            :key="item.key"
            class="admin-inbox__item"
            :class="{ 'admin-inbox__item--read': item.read }"
          >
            <t-divider align="left" class="admin-inbox__type-divider">
              {{ item.type === 'TASK' ? '待处理' : '公告' }}
            </t-divider>
            <div class="admin-inbox__item-main">
              <strong>{{ item.title }}</strong>
              <p v-if="item.body">{{ item.body }}</p>
            </div>
            <div class="admin-inbox__item-actions">
              <t-button
                v-if="!item.read"
                size="small"
                variant="text"
                theme="default"
                @click="onMarkRead(item.key)"
              >
                标为已读
              </t-button>
              <t-button
                v-if="item.linkUrl"
                size="small"
                variant="text"
                theme="primary"
                @click="go(item)"
              >
                {{ item.linkLabel || '查看' }}
              </t-button>
              <t-button
                v-if="item.dismissible"
                size="small"
                variant="text"
                theme="default"
                @click="onDismiss(item.key)"
              >
                不再提醒
              </t-button>
            </div>
          </li>
        </ul>
      </div>
    </template>
    <t-badge :count="unreadCount" :max-count="99" :offset="[4, 4]">
      <t-button variant="text" shape="square" aria-label="消息通知">
        <template #icon>
          <t-icon name="notification" />
        </template>
      </t-button>
    </t-badge>
  </t-popup>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { AdminInboxItem } from '@/api/adminNotification'
import { useAdminNotifications } from '@/composables/useAdminNotifications'

const router = useRouter()
const visible = ref(false)
const { unreadCount, items, refreshInbox, markItemRead, markAllRead, dismissItem } = useAdminNotifications()

async function go(item: AdminInboxItem) {
  if (!item.read) {
    await markItemRead(item.key)
  }
  visible.value = false
  if (!item.linkUrl) return
  if (item.linkUrl.startsWith('/')) {
    void router.push(item.linkUrl)
    return
  }
  window.open(item.linkUrl, '_blank', 'noopener,noreferrer')
}

async function onMarkRead(key: string) {
  await markItemRead(key)
}

async function onMarkAllRead() {
  await markAllRead()
}

async function onDismiss(key: string) {
  await dismissItem(key)
}
</script>

<style scoped>
.admin-inbox {
  width: 360px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
}

.admin-inbox__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--td-component-stroke);
  font-weight: 600;
}

.admin-inbox__empty {
  padding: 24px 16px;
}

.admin-inbox__list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow: auto;
}

.admin-inbox__item {
  padding: 4px 16px 12px;
  border-bottom: 1px solid var(--td-component-stroke);
}

.admin-inbox__item--read .admin-inbox__item-main strong {
  color: var(--td-text-color-secondary);
  font-weight: 500;
}

.admin-inbox__type-divider {
  margin: 8px 0 4px;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.admin-inbox__item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-inbox__item-main p {
  margin: 0;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.admin-inbox__item-actions {
  margin-top: 8px;
  display: flex;
  gap: 4px;
}
</style>
