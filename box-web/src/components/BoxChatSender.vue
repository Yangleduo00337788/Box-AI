<template>
  <div ref="rootRef" class="box-chat-sender">
    <ChatSender
      :value="modelValue"
      :loading="loading"
      :disabled="disabled"
      :send-btn-disabled="!canSend"
      :textarea-props="textareaProps"
      :attachments-props="attachmentsProps"
      class="box-chat-sender__input"
      @update:value="onValueChange"
      @send="onSend"
      @stop="emit('stop')"
      @remove="onRemoveAttachment"
    >
      <template v-if="showAttach || showCloud" #footer-prefix>
        <div class="box-chat-sender__prefix">
          <t-tooltip v-if="showAttach" content="添加文件" placement="top" theme="light" :show-arrow="false">
            <button type="button" class="box-chat-sender__tool" aria-label="添加文件" @click="fileInputRef?.click()">
              <t-icon name="add" />
            </button>
          </t-tooltip>
          <t-tooltip v-if="showCloud" content="任务运行环境：云端" placement="top" theme="light" :show-arrow="false">
            <span class="box-chat-sender__env" aria-label="云端运行">
              <span class="box-chat-sender__env-icon">
                <t-icon name="cloud" />
                <i class="box-chat-sender__env-dot" />
              </span>
              云端
            </span>
          </t-tooltip>
        </div>
      </template>
      <template #suffix="{ renderPresets }">
        <div class="box-chat-sender__suffix">
          <chat-model-picker
            v-if="showModelPicker"
            v-model="modelKey"
            :models="models"
            :disabled="modelPickerDisabled"
            :auto-hint="autoHint"
          />
          <t-tooltip
            v-if="showVoice"
            :content="listening ? '停止语音输入' : '语音输入'"
            placement="top"
            theme="light"
            :show-arrow="false"
          >
            <button
              type="button"
              class="box-chat-sender__tool"
              :class="{ 'composer__voice--active': listening }"
              :aria-label="listening ? '停止语音输入' : '语音输入'"
              @click="emit('toggle-voice')"
            >
              <t-icon name="microphone-1" />
            </button>
          </t-tooltip>
          <component :is="renderPresets([])" />
        </div>
      </template>
    </ChatSender>
    <input
      v-if="showAttach"
      ref="fileInputRef"
      type="file"
      class="box-chat-sender__file-input"
      accept=".txt,.md,.json,.csv,.log,.png,.jpg,.jpeg,text/plain,image/png,image/jpeg"
      @change="onFileChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { TextareaProps } from 'tdesign-vue-next'
import { ChatSender, type TdAttachmentItem } from '@tdesign-vue-next/chat'
import type { PlatformModelVO } from '@/api/platform'
import { appPreferences } from '@/composables/useAppPreferences'
import ChatModelPicker from '@/components/ChatModelPicker.vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    loading?: boolean
    disabled?: boolean
    canSend?: boolean
    placeholder?: string
    maxRows?: number
    showCloud?: boolean
    showAttach?: boolean
    showVoice?: boolean
    showModelPicker?: boolean
    listening?: boolean
    models?: PlatformModelVO[]
    modelPickerDisabled?: boolean
    autoHint?: string
    selectedModelKey?: string
    attachmentItems?: TdAttachmentItem[]
  }>(),
  {
    loading: false,
    disabled: false,
    canSend: false,
    placeholder: '说说你想聊什么...',
    maxRows: 8,
    showCloud: true,
    showAttach: true,
    showVoice: false,
    showModelPicker: false,
    listening: false,
    models: () => [],
    modelPickerDisabled: false,
    autoHint: '',
    selectedModelKey: 'auto',
    attachmentItems: () => [],
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'update:selectedModelKey': [value: string]
  send: [value: string]
  stop: []
  'toggle-voice': []
  'remove-attachment': [key: string]
  'file-change': [event: Event]
}>()

const rootRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
let textareaEl: HTMLTextAreaElement | null = null
const modelKey = ref(props.selectedModelKey)

watch(
  () => props.selectedModelKey,
  (value) => {
    modelKey.value = value
  },
)

watch(modelKey, (value) => {
  emit('update:selectedModelKey', value)
})

const textareaProps = computed<TextareaProps>(() => ({
  placeholder: props.placeholder,
  autosize: { minRows: 2, maxRows: props.maxRows },
}))

const attachmentsProps = computed(() => ({
  items: props.attachmentItems,
  overflow: 'scrollX' as const,
  removable: true,
}))

function onValueChange(value: string) {
  emit('update:modelValue', value)
}

function onSend(value: string) {
  emit('update:modelValue', value)
  emit('send', value)
}

function shouldSendOnEnter(event: KeyboardEvent) {
  const modifier = event.ctrlKey || event.metaKey
  return appPreferences.sendWithEnter
    ? !event.shiftKey && !modifier
    : modifier && !event.shiftKey
}

function onTextareaKeydownCapture(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.isComposing || event.keyCode === 229) return
  if (!shouldSendOnEnter(event)) {
    event.stopPropagation()
  }
}

onMounted(() => {
  textareaEl = rootRef.value?.querySelector('textarea') ?? null
  textareaEl?.addEventListener('keydown', onTextareaKeydownCapture, true)
})

onBeforeUnmount(() => {
  textareaEl?.removeEventListener('keydown', onTextareaKeydownCapture, true)
  textareaEl = null
})

function onRemoveAttachment(event: CustomEvent<TdAttachmentItem>) {
  const key = event.detail?.key
  if (key != null) {
    emit('remove-attachment', String(key))
  }
}

function onFileChange(event: Event) {
  emit('file-change', event)
}
</script>

<style scoped>
.box-chat-sender {
  width: 100%;
}

.box-chat-sender__input :deep(.t-chat-sender) {
  border-radius: 18px;
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.box-chat-sender__input :deep(.t-textarea__inner) {
  font-size: 15px;
}

.box-chat-sender__file-input {
  display: none;
}

.box-chat-sender__prefix,
.box-chat-sender__suffix {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.box-chat-sender__tool {
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
  font-size: 18px;
  cursor: pointer;
}

.box-chat-sender__tool:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.box-chat-sender__env {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 6px;
  border-radius: 8px;
  color: var(--box-muted);
  font-size: 13px;
  line-height: 20px;
  user-select: none;
}

.box-chat-sender__env-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.box-chat-sender__env-dot {
  position: absolute;
  right: -1px;
  bottom: 0;
  width: 6px;
  height: 6px;
  border: 1.5px solid var(--box-surface);
  border-radius: 50%;
  background: #2ba471;
}
</style>
