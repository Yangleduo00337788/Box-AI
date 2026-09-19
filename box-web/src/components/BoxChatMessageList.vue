<template>
  <ChatList
    ref="listRef"
    class="box-chat-message-list box-hide-scrollbar"
    :class="[rootClass, { 'box-chat-message-list--share': shareMode }]"
    :clear-history="false"
    :reverse="false"
    :auto-scroll="autoScroll"
    :show-scroll-button="showScrollButton && !shareMode"
    layout="single"
  >
    <div v-if="emptyText && !items.length" class="box-chat-message-list__empty">
      {{ emptyText }}
    </div>
    <div
      v-for="(item, index) in items"
      :key="item.key"
      class="box-chat-message-list__wrap"
    >
      <div
        class="box-chat-message-list__row"
        :class="{ 'box-chat-message-list__row--selectable': shareMode && item.messageId }"
        @click="shareMode && item.messageId ? onRowClick(item.messageId!) : undefined"
      >
        <div v-if="shareMode" class="box-chat-message-list__checkbox" @click.stop>
          <t-checkbox
            :checked="item.messageId ? selectedShareIds.includes(item.messageId) : false"
            :disabled="!item.messageId"
            @change="(checked: boolean) => item.messageId && onToggleSelect(item.messageId, checked)"
          />
        </div>
        <ChatMessage
          :role="item.uiRole"
          :content="item.uiContent"
          :status="item.uiStatus"
          placement="left"
          variant="text"
          :animation="item.animation"
          class="box-chat-message-list__message"
        >
          <template #name>
            <span class="box-chat-message-list__name-row">
              <span class="box-chat-message-list__name">{{ item.displayName }}</span>
              <t-tag
                v-if="item.uiRole === 'assistant'"
                size="small"
                variant="light"
                class="box-chat-message-list__ai-tag"
              >
                AI
              </t-tag>
              <span v-if="item.sentTimeLabel" class="box-chat-message-list__time-meta">
                <span class="box-chat-message-list__time-dot" aria-hidden="true" />
                <time class="box-chat-message-list__time" :datetime="item.sentAt">{{ item.sentTimeLabel }}</time>
              </span>
            </span>
          </template>
          <template #avatar>
            <t-avatar
              size="28px"
              shape="circle"
              :image="item.avatarUrl"
              :style="{ background: item.avatarColor }"
            >
              {{ item.avatarText }}
            </t-avatar>
          </template>
          <template #content>
            <box-chat-thinking-status v-if="item.thinkingActive" />
            <div
              v-else
              class="box-chat-bubble"
              :class="`box-chat-bubble--${item.uiRole}`"
            >
              <ChatThinking
                v-if="item.reasoning"
                status="complete"
                animation="gradient"
                layout="border"
                :collapsed="true"
                class="box-chat-message-list__thinking"
              >
                {{ item.reasoning }}
              </ChatThinking>
              <div v-if="item.replyQuote" class="box-chat-reply-quote">
                <span class="box-chat-reply-quote__label">回复 {{ item.replyQuote.authorName }}:</span>
                {{ item.replyQuote.excerpt }}
              </div>
              <div v-if="item.userImages.length" class="box-chat-message-list__images">
                <a
                  v-for="(image, imageIndex) in item.userImages"
                  :key="`${image.url}-${imageIndex}`"
                  class="box-chat-message-list__image"
                  :href="image.url"
                  target="_blank"
                  rel="noreferrer"
                  @click.stop
                >
                  <t-image :src="image.url" :alt="image.name" fit="cover" shape="round" />
                </a>
              </div>
              <span v-if="item.userText && item.uiRole === 'user'">{{ item.userText }}</span>
              <ChatContent
                v-else-if="item.uiRole === 'assistant'"
                role="assistant"
                :content="{ type: 'markdown', data: item.plainText || '' }"
              />
            </div>
          </template>
        </ChatMessage>
        <div
          v-if="!readonly && !shareMode && item.showActions"
          class="box-chat-message-list__actions"
        >
          <ChatActionbar
            :content="item.copyText"
            :disabled="actionsDisabled"
            :action-bar="item.actionBar"
            :comment="feedbackMap[String(item.key)] || ''"
            @actions="(type) => onActionBar(type, index, item)"
          />
        </div>
      </div>
      <BoxChatCitations v-if="item.citations.length" :citations="item.citations" />
    </div>
    <slot />
  </ChatList>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  ChatActionbar,
  ChatContent,
  ChatList,
  ChatMessage,
  ChatThinking,
} from '@tdesign-vue-next/chat'
import type { BoxChatListItem } from '@/utils/boxChatListItems'
import BoxChatCitations from '@/components/BoxChatCitations.vue'
import BoxChatThinkingStatus from '@/components/BoxChatThinkingStatus.vue'
import '@/styles/box-chat-bubble.css'

const props = withDefaults(
  defineProps<{
    items: BoxChatListItem[]
    actionsDisabled?: boolean
    readonly?: boolean
    shareMode?: boolean
    selectedShareIds?: number[]
    emptyText?: string
    autoScroll?: boolean
    showScrollButton?: boolean
    rootClass?: string
  }>(),
  {
    actionsDisabled: false,
    readonly: false,
    shareMode: false,
    selectedShareIds: () => [],
    emptyText: '',
    autoScroll: true,
    showScrollButton: false,
    rootClass: '',
  },
)

const emit = defineEmits<{
  regenerate: [index: number]
  share: [index: number]
  feedback: [index: number, rating: 'good' | 'bad']
  'toggle-share-select': [messageId: number, selected: boolean]
}>()

const listRef = ref<{ scrollToBottom?: (params?: { behavior?: 'auto' | 'smooth' }) => void } | null>(null)
const feedbackMap = reactive<Record<string, 'good' | 'bad' | ''>>({})

function onToggleSelect(messageId: number, selected: boolean) {
  emit('toggle-share-select', messageId, selected)
}

function onRowClick(messageId: number) {
  const selected = !props.selectedShareIds.includes(messageId)
  emit('toggle-share-select', messageId, selected)
}

function onActionBar(type: string, index: number, item: BoxChatListItem) {
  if (type === 'replay') {
    emit('regenerate', index)
    return
  }
  if (type === 'share') {
    emit('share', index)
    return
  }
  if (type === 'good' || type === 'bad') {
    const key = String(item.key)
    if (type === 'good') {
      feedbackMap[key] = feedbackMap[key] === 'good' ? '' : 'good'
      emit('feedback', index, 'good')
      return
    }
    emit('feedback', index, 'bad')
  }
}

function scrollToBottom(params?: { behavior?: 'auto' | 'smooth' }) {
  listRef.value?.scrollToBottom?.(params)
}

defineExpose({
  scrollToBottom,
})
</script>

<style scoped>
.box-chat-message-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.box-chat-message-list :deep(.t-chat__list) {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 0;
}

.box-chat-message-list :deep(.t-chat__scroll-button) {
  right: 20px;
  left: auto;
  bottom: 12px;
  transform: none;
  box-shadow: var(--box-shadow-soft);
}

.box-chat-message-list :deep(.t-chat-item) {
  max-width: 100%;
}

.box-chat-message-list :deep(.t-chat-item__inner) {
  align-items: flex-start;
}

.box-chat-message-list :deep(.t-chat-item__avatar) {
  align-self: flex-start;
  margin-top: 1px;
}

.box-chat-message-list :deep(.t-chat-item__avatar .t-avatar) {
  font-size: 12px;
}

.box-chat-message-list :deep(.t-chat-item__content) {
  background: transparent !important;
  padding: 0 !important;
  box-shadow: none !important;
}

.box-chat-message-list__wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding-right: 4px;
}

.box-chat-message-list__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.box-chat-message-list__row--selectable {
  cursor: pointer;
}

.box-chat-message-list--share .box-chat-message-list__row {
  padding: 4px 0;
  border-radius: var(--box-radius-md);
}

.box-chat-message-list--share .box-chat-message-list__row--selectable:hover {
  background: rgba(0, 0, 0, 0.03);
}

.box-chat-message-list__checkbox {
  flex-shrink: 0;
  padding-top: 28px;
}

.box-chat-message-list__message {
  flex: 1;
  min-width: 0;
}

.box-chat-message-list__name-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 22px;
}

.box-chat-message-list__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.box-chat-message-list__ai-tag {
  flex-shrink: 0;
}

.box-chat-message-list__ai-tag :deep(.t-tag) {
  border-radius: var(--box-radius-md);
  font-size: 11px;
  line-height: 18px;
  padding: 0 6px;
}

.box-chat-message-list__time-meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 400;
  line-height: 1;
  color: var(--td-text-color-placeholder);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.box-chat-message-list__row:hover .box-chat-message-list__time-meta,
.box-chat-message-list__row:focus-within .box-chat-message-list__time-meta {
  opacity: 1;
}

.box-chat-message-list__time-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.box-chat-message-list__time {
  font-variant-numeric: tabular-nums;
}

.box-chat-message-list__actions {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  margin-top: 22px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
}

.box-chat-message-list__row:hover .box-chat-message-list__actions,
.box-chat-message-list__row:focus-within .box-chat-message-list__actions {
  opacity: 1;
  pointer-events: auto;
}

.box-chat-message-list__empty {
  margin: auto;
  text-align: center;
  font: var(--td-font-body-small);
  color: var(--box-muted, var(--td-text-color-secondary));
  padding: 24px 16px;
}

.box-chat-reply-quote {
  border-left: 3px solid var(--td-component-border);
  padding: 2px 0 2px 12px;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--td-text-color-secondary);
  word-break: break-word;
}

.box-chat-reply-quote__label {
  color: var(--td-text-color-placeholder);
  margin-right: 4px;
}

.box-chat-message-list__thinking {
  margin-bottom: 8px;
}

.box-chat-message-list__images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.box-chat-message-list__image {
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  cursor: zoom-in;
}

.box-chat-message-list__image :deep(.t-image) {
  width: 168px;
  height: 168px;
  border-radius: var(--box-radius-md);
}
</style>
