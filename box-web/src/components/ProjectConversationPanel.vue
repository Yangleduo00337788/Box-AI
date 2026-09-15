<template>
  <div class="project-chat-panel">
    <div class="project-chat-panel__head">
      <span>{{ title }}</span>
    </div>
    <t-loading :loading="loading" size="small">
      <div v-if="conversations.length" class="project-chat-panel__list">
        <div
          v-for="item in conversations"
          :key="item.id"
          class="project-chat-item"
          :class="{ 'project-chat-item--active': item.id === activeId }"
        >
          <button type="button" class="project-chat-item__main" @click="emit('open', item)">
            <t-icon name="chat-bubble-history" class="project-chat-item__icon" />
            <span class="project-chat-item__title">{{ item.title || item.agentName || '新会话' }}</span>
          </button>
          <div class="project-chat-item__actions">
            <t-tooltip
              :content="isPinned(item.id) ? '取消置顶' : '置顶'"
              placement="top"
              theme="light"
              :show-arrow="false"
              attach="body"
            >
              <button
                type="button"
                class="project-chat-item__action"
                :aria-label="isPinned(item.id) ? '取消置顶' : '置顶'"
                @click.stop="emit('pin', item.id)"
              >
                <t-icon name="pin" />
              </button>
            </t-tooltip>
            <t-tooltip content="改名" placement="top" theme="light" :show-arrow="false" attach="body">
              <button type="button" class="project-chat-item__action" aria-label="改名" @click.stop="emit('rename', item)">
                <t-icon name="edit-1" />
              </button>
            </t-tooltip>
            <t-tooltip content="删除" placement="top" theme="light" :show-arrow="false" attach="body">
              <button type="button" class="project-chat-item__action" aria-label="删除" @click.stop="emit('delete', item)">
                <t-icon name="delete" />
              </button>
            </t-tooltip>
          </div>
        </div>
      </div>
      <p v-else-if="!loading" class="project-chat-panel__empty">进入项目后发起的对话会显示在这里</p>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import type { ConversationVO } from '@/api/conversation'

const props = defineProps<{
  title: string
  loading?: boolean
  conversations: ConversationVO[]
  activeId?: number | null
  pinnedIds?: number[]
}>()

const emit = defineEmits<{
  open: [conversation: ConversationVO]
  pin: [conversationId: number]
  rename: [conversation: ConversationVO]
  delete: [conversation: ConversationVO]
}>()

function isPinned(id: number) {
  return (props.pinnedIds || []).includes(id)
}
</script>

<style scoped>
.project-chat-panel {
  width: 300px;
  max-height: 360px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 0 4px;
  scrollbar-width: none;
}

.project-chat-panel::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.project-chat-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 12px 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2329;
}

.project-chat-panel__head-actions {
  display: inline-flex;
  align-items: center;
}

.project-chat-panel :deep(.t-loading),
.project-chat-panel :deep(.t-loading__parent) {
  width: 100%;
}

.project-chat-panel__empty {
  margin: 0;
  padding: 16px;
  font-size: 13px;
  color: var(--box-muted);
  text-align: center;
}

.project-chat-panel__list {
  display: flex;
  flex-direction: column;
}

.project-chat-item {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  border-bottom: 1px solid #f0f1f2;
}

.project-chat-item:last-child {
  border-bottom: none;
}

.project-chat-item:hover,
.project-chat-item--active {
  background: #f7f8fa;
}

.project-chat-item__main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  padding: 10px 72px 10px 16px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: var(--box-ink);
}

.project-chat-item__icon {
  flex-shrink: 0;
  font-size: 16px;
  color: var(--box-muted);
}

.project-chat-item__title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  line-height: 20px;
}

.project-chat-item__actions {
  position: absolute;
  right: 8px;
  top: 50%;
  display: inline-flex;
  align-items: center;
  opacity: 0;
  transform: translateY(-50%);
  pointer-events: none;
}

.project-chat-item:hover .project-chat-item__actions {
  opacity: 1;
  pointer-events: auto;
}

.project-chat-item__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--box-muted);
  cursor: pointer;
}

.project-chat-item__action :deep(.t-icon) {
  font-size: 14px;
}

.project-chat-item__action:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}
</style>
