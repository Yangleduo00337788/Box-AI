<template>
  <div ref="rootRef" class="box-chat-sender">
    <div
      class="box-chat-sender__composer"
      :class="{ 'box-chat-sender__composer--with-plugins': selectedPlugins.length }"
    >
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
      <template v-if="selectedPlugins.length" #input-prefix>
        <span
          v-for="plugin in selectedPlugins"
          :key="plugin.id"
          class="box-chat-sender__plugin-pill"
          @mouseenter="hoveredPluginId = plugin.id"
          @mouseleave="hoveredPluginId = null"
        >
          <t-icon :name="pluginComposerIcon()" class="box-chat-sender__plugin-pill-icon" />
          <span class="box-chat-sender__plugin-pill-text">{{ pluginPillLabel(plugin) }}</span>
          <button
            v-show="hoveredPluginId === plugin.id"
            type="button"
            class="box-chat-sender__plugin-pill-close"
            aria-label="取消启用插件"
            @click.stop="onRemovePlugin(plugin.id)"
          >
            <t-icon name="close" />
          </button>
        </span>
      </template>
      <template v-if="showAttach || showCloud" #footer-prefix>
        <div ref="attachAnchorRef" class="box-chat-sender__prefix">
          <t-tooltip v-if="showAttach" content="添加附件或插件" placement="top" theme="light" :show-arrow="false">
            <button
              type="button"
              class="box-chat-sender__tool"
              :class="{ 'box-chat-sender__tool--active': attachMenuOpen }"
              aria-label="添加附件或插件"
              aria-expanded="attachMenuOpen"
              @click="toggleAttachMenu"
            >
              <t-icon name="add" />
            </button>
          </t-tooltip>
          <chat-composer-plus-menu
            v-if="showAttach && attachMenuOpen && showPluginPicker"
            :plugins="installedPlugins"
            :selected-ids="selectedPluginIdList"
            :loading="pluginsLoading"
            @upload="onUploadFromMenu"
            @pick-plugin="onPickPlugin"
            @open-market="onOpenPluginMarket"
          />
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
    </div>
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
import { useRouter } from 'vue-router'
import type { TextareaProps } from 'tdesign-vue-next'
import { ChatSender, type TdAttachmentItem } from '@tdesign-vue-next/chat'
import type { PlatformModelVO } from '@/api/platform'
import type { PluginCatalogVO } from '@/api/plugin'
import { appPreferences } from '@/composables/useAppPreferences'
import { useWorkspaceInstalledPlugins } from '@/composables/useWorkspaceInstalledPlugins'
import ChatComposerPlusMenu from '@/components/ChatComposerPlusMenu.vue'
import ChatModelPicker from '@/components/ChatModelPicker.vue'
import { pluginComposerIcon, pluginPillLabel } from '@/constants/pluginCatalogMeta'

const props = withDefaults(
  defineProps<{
    modelValue: string
    loading?: boolean
    disabled?: boolean
    canSend?: boolean
    placeholder?: string
    maxRows?: number
    minRows?: number
    showCloud?: boolean
    showAttach?: boolean
    /** 加号菜单中展示本空间已安装插件 */
    showPluginPicker?: boolean
    showVoice?: boolean
    showModelPicker?: boolean
    listening?: boolean
    models?: PlatformModelVO[]
    modelPickerDisabled?: boolean
    autoHint?: string
    selectedModelKey?: string
    attachmentItems?: TdAttachmentItem[]
    selectedPlugins?: PluginCatalogVO[]
  }>(),
  {
    loading: false,
    disabled: false,
    canSend: false,
    placeholder: '说说你想聊什么...',
    maxRows: 8,
    minRows: 2,
    showCloud: true,
    showAttach: true,
    showPluginPicker: true,
    showVoice: false,
    showModelPicker: false,
    listening: false,
    models: () => [],
    modelPickerDisabled: false,
    autoHint: '',
    selectedModelKey: 'auto',
    attachmentItems: () => [],
    selectedPlugins: () => [],
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
  'pick-plugin': [plugin: PluginCatalogVO]
  'remove-plugin': [pluginId: number]
}>()

const router = useRouter()
const rootRef = ref<HTMLElement | null>(null)
const attachAnchorRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const attachMenuOpen = ref(false)
const hoveredPluginId = ref<number | null>(null)
const { installed: installedPlugins, loading: pluginsLoading, load: loadInstalledPlugins } =
  useWorkspaceInstalledPlugins()
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
  autosize: { minRows: props.minRows, maxRows: props.maxRows },
}))

const selectedPluginIdList = computed(() => props.selectedPlugins.map((item) => item.id))

const attachmentsProps = computed(() => ({
  items: props.attachmentItems,
  overflow: 'scrollX' as const,
  removable: true,
}))

function onValueChange(value: string) {
  emit('update:modelValue', value)
}

function onSend(value: string) {
  emit('send', value)
  emit('update:modelValue', '')
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

function toggleAttachMenu() {
  if (!props.showPluginPicker) {
    fileInputRef.value?.click()
    return
  }
  attachMenuOpen.value = !attachMenuOpen.value
  if (attachMenuOpen.value) {
    void loadInstalledPlugins()
  }
}

function closeAttachMenu() {
  attachMenuOpen.value = false
}

function onUploadFromMenu() {
  closeAttachMenu()
  fileInputRef.value?.click()
}

function onPickPlugin(plugin: PluginCatalogVO) {
  closeAttachMenu()
  emit('pick-plugin', plugin)
}

function onRemovePlugin(pluginId: number) {
  hoveredPluginId.value = null
  emit('remove-plugin', pluginId)
}

function onOpenPluginMarket() {
  closeAttachMenu()
  void router.push('/plugin-market')
}

function onDocumentPointerDown(event: MouseEvent) {
  if (!attachMenuOpen.value) return
  const target = event.target as Node | null
  if (attachAnchorRef.value?.contains(target)) return
  closeAttachMenu()
}

onMounted(() => {
  textareaEl = rootRef.value?.querySelector('textarea') ?? null
  textareaEl?.addEventListener('keydown', onTextareaKeydownCapture, true)
  document.addEventListener('pointerdown', onDocumentPointerDown)
  if (props.showAttach && props.showPluginPicker) {
    void loadInstalledPlugins()
  }
})

onBeforeUnmount(() => {
  textareaEl?.removeEventListener('keydown', onTextareaKeydownCapture, true)
  textareaEl = null
  document.removeEventListener('pointerdown', onDocumentPointerDown)
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
  padding: 0;
  background: transparent;
  box-shadow: none;
}

.box-chat-sender__composer:not(.box-chat-sender__composer--with-plugins) .box-chat-sender__input :deep(.t-chat-sender__textarea) {
  border: 1px solid var(--box-composer-border, var(--box-border));
  border-radius: var(--box-composer-radius);
  background: var(--box-surface);
  box-shadow: var(--box-composer-shadow, var(--box-shadow-card));
}

.box-chat-sender__composer:not(.box-chat-sender__composer--with-plugins) .box-chat-sender__input :deep(.t-chat-sender__textarea:hover),
.box-chat-sender__composer:not(.box-chat-sender__composer--with-plugins) .box-chat-sender__input :deep(.t-chat-sender__textarea--focus),
.box-chat-sender__composer:not(.box-chat-sender__composer--with-plugins) .box-chat-sender__input :deep(.t-chat-sender__textarea--focus:hover) {
  border-color: var(--box-composer-border, var(--box-border));
  background: var(--box-surface);
  box-shadow: var(--box-composer-shadow, var(--box-shadow-card));
}

.box-chat-sender__input :deep(.t-textarea__inner) {
  font-size: 15px;
}

.box-chat-sender__file-input {
  display: none;
}

.box-chat-sender__prefix {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

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

.box-chat-sender__tool:hover,
.box-chat-sender__tool--active {
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

.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea),
.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea:hover),
.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea--focus),
.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea--focus:hover) {
  border: none;
  box-shadow: none;
  border-radius: 0;
  background: transparent;
}

.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea__wrapper) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
}

.box-chat-sender__composer--with-plugins .box-chat-sender__input :deep(.t-chat-sender__textarea) {
  flex: 1 1 140px;
  min-width: 0;
}

.box-chat-sender__plugin-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  color: #2563eb;
  font-size: 15px;
  line-height: 1.5;
  user-select: none;
  flex-shrink: 0;
}

.box-chat-sender__plugin-pill-text {
  white-space: nowrap;
}

.box-chat-sender__plugin-pill-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.box-chat-sender__plugin-pill-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.box-chat-sender__plugin-pill-close:hover {
  background: rgba(37, 99, 235, 0.2);
}
</style>
