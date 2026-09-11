<template>
  <div class="chat-workspace">
    <div v-if="!conversationId" class="chat-new">
      <div class="chat-stage">
      <h1 class="chat-new__title">
        有什么想交给
        <span v-if="selectedAgent" class="chat-new__agent">
          <t-avatar size="24px" class="chat-new__agent-avatar" :style="{ background: agentAvatarColor }">
            {{ selectedAgent.name.slice(0, 1) }}
          </t-avatar>
          {{ selectedAgent.name }}
        </span>
        <template v-else> Box </template>
        ？
      </h1>

      <div class="composer">
        <t-textarea
          v-model="composerText"
          :placeholder="composerPlaceholder"
          :autosize="{ minRows: 2, maxRows: 8 }"
          class="composer__input"
          :disabled="chatting"
          @keydown="onComposerKeydown"
        />
        <input
          ref="fileInputRef"
          type="file"
          class="composer__file-input"
          accept=".txt,.md,.json,.csv,.log,text/plain"
          @change="onFileSelected"
        />
        <div class="composer__toolbar">
          <div class="composer__left">
            <t-tooltip content="添加文本文件" placement="top" theme="light" :show-arrow="false">
              <t-button variant="text" shape="square" size="small" @click="openFilePicker">
                <template #icon><t-icon name="add" /></template>
              </t-button>
            </t-tooltip>
            <t-tooltip content="任务运行环境：云端" placement="top" theme="light" :show-arrow="false">
              <t-button variant="text" size="small" tag="div">
                <template #icon><t-icon name="cloud" /></template>
                云端
              </t-button>
            </t-tooltip>
          </div>
          <div class="composer__right">
            <chat-model-picker v-model="selectedModelKey" :models="platformModels" />
            <t-tooltip :content="listening ? '停止语音输入' : '语音输入'" placement="top" theme="light" :show-arrow="false">
              <t-button
                variant="text"
                shape="square"
                size="small"
                :class="{ 'composer__voice--active': listening }"
                @click="toggleVoiceInput"
              >
                <template #icon><t-icon name="microphone-1" /></template>
              </t-button>
            </t-tooltip>
            <t-button
              class="composer__send"
              :class="{ 'composer__send--ready': canSendNewChat }"
              shape="circle"
              :loading="chatting"
              :disabled="!canSendNewChat"
              @click="startNewChat(composerText)"
            >
              <template #icon><t-icon name="chevron-up" /></template>
            </t-button>
          </div>
        </div>
      </div>

      <div v-if="suggestionsLoading" class="suggestions-loading">
        <t-loading size="small" />
      </div>
      <div v-else-if="displaySuggestions.length" class="suggestions">
        <button
          v-for="item in displaySuggestions"
          :key="item.id"
          type="button"
          class="suggestion-card"
          @click="startNewChat(item.prompt)"
        >
          <div class="suggestion-card__head">
            <t-icon :name="item.icon" />
            <span class="suggestion-card__title">{{ item.title }}</span>
          </div>
          <p v-if="item.subtitle" class="suggestion-card__subtitle">{{ item.subtitle }}</p>
        </button>
      </div>
      </div>
    </div>

    <div v-else class="chat-active">
      <div class="chat-stage chat-stage--active">
      <div class="chat-active__header">
        <t-dropdown v-if="selectedAgent" :options="agentOptions" trigger="click" @click="onAgentDropdown">
          <button type="button" class="agent-pill agent-pill--compact">
            <t-avatar size="22px" class="agent-pill__avatar" :style="{ background: agentAvatarColor }">
              {{ selectedAgent.name.slice(0, 1) }}
            </t-avatar>
            <span>{{ selectedAgent.name }}</span>
            <t-icon name="chevron-down" class="agent-pill__arrow" />
          </button>
        </t-dropdown>
        <span v-if="activeConversationTitle" class="chat-active__title">{{ activeConversationTitle }}</span>
      </div>

      <div ref="chatListRef" class="chat-messages">
        <div
          v-for="(item, index) in messages"
          :key="item.id ?? index"
          class="chat-message"
          :class="`chat-message--${item.role.toLowerCase()}`"
        >
          <div class="chat-message__content" :class="{ 'chat-message__content--loading': isLoadingBubble(item, index) }">
            <span v-if="isLoadingBubble(item, index)" class="typing-dots" aria-label="思考中">
              <i /><i /><i />
            </span>
            <template v-else>{{ displayContent(item, index) }}</template>
          </div>
        </div>
      </div>

      <div class="composer composer--bottom">
        <t-textarea
          v-model="composerText"
          placeholder="继续对话..."
          :autosize="{ minRows: 2, maxRows: 6 }"
          class="composer__input"
          :disabled="chatting"
          @keydown="onComposerKeydown"
        />
        <input
          ref="activeFileInputRef"
          type="file"
          class="composer__file-input"
          accept=".txt,.md,.json,.csv,.log,text/plain"
          @change="onFileSelected"
        />
        <div class="composer__toolbar">
          <div class="composer__left">
            <t-tooltip content="添加文本文件" placement="top" theme="light" :show-arrow="false">
              <t-button variant="text" shape="square" size="small" @click="openActiveFilePicker">
                <template #icon><t-icon name="add" /></template>
              </t-button>
            </t-tooltip>
          </div>
          <div class="composer__right">
            <t-button
              class="composer__send"
              :class="{ 'composer__send--ready': canSendMessage }"
              shape="circle"
              :loading="chatting"
              :disabled="!canSendMessage"
              @click="sendChat()"
            >
              <template #icon><t-icon name="chevron-up" /></template>
            </t-button>
          </div>
        </div>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
import ChatModelPicker from '@/components/ChatModelPicker.vue'
import { extractApiError } from '@/api/apiError'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'
import { appPreferences } from '@/composables/useAppPreferences'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useChatSuggestions } from '@/composables/useChatSuggestions'
import { useConversationNav } from '@/composables/useConversationNav'
import { getAvatarColor } from '@/utils/format'
import {
  createConversation,
  getConversation,
  listMessages,
  sendMessageStream,
  type MessageVO,
} from '@/api/conversation'

const route = useRoute()
const router = useRouter()
const { refresh: refreshConversations, conversations } = useConversationNav()
const { agents, selectedAgent, refresh: refreshAgents, selectAgent, selectAgentByConversation } = useAgentSelection()
const { suggestions, loading: suggestionsLoading, refresh: refreshSuggestions } = useChatSuggestions()

const chatting = ref(false)
const composerText = ref('')
const messages = ref<MessageVO[]>([])
const chatListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const activeFileInputRef = ref<HTMLInputElement | null>(null)
const listening = ref(false)

interface BrowserSpeechRecognition {
  lang: string
  interimResults: boolean
  onstart: (() => void) | null
  onend: (() => void) | null
  onerror: (() => void) | null
  onresult: ((event: { results: Array<Array<{ transcript: string }>> }) => void) | null
  start: () => void
  stop: () => void
}

let speechRecognition: BrowserSpeechRecognition | null = null
const selectedModelKey = ref('auto')
const platformModels = ref<PlatformModelVO[]>([])
const activeConversationTitle = ref('')
/** 新会话首条消息发送中，避免路由切换时 loadMessages 覆盖乐观更新 */
const suppressMessagesReload = ref(false)

const agentAvatarColor = computed(() => getAvatarColor(selectedAgent.value?.name || 'Box'))

const canSendNewChat = computed(
  () => !!composerText.value.trim() && !chatting.value && !!selectedAgent.value,
)

const canSendMessage = computed(
  () => !!composerText.value.trim() && !chatting.value,
)

const BRAND_SUGGESTION_TITLES = new Set(['box', 'coze', '扣子', '盒子'])

const displaySuggestions = computed(() =>
  suggestions.value.filter((item) => !BRAND_SUGGESTION_TITLES.has(item.title.trim().toLowerCase())),
)

const agentOptions = computed<DropdownOption[]>(() =>
  agents.value.map((agent) => ({
    content: agent.name,
    value: agent.id,
  })),
)

const composerPlaceholder = computed(() =>
  selectedAgent.value ? `告诉${selectedAgent.value.name}，你想先从哪件事开始...` : '告诉我 Box，你想先从哪件事开始...',
)

const conversationId = computed(() => {
  const id = route.params.id
  if (!id || Array.isArray(id)) return null
  const num = Number(id)
  return Number.isFinite(num) ? num : null
})

function displayContent(item: MessageVO, index: number) {
  if (item.content) return item.content
  if (chatting.value && index === messages.value.length - 1 && item.role === 'ASSISTANT') {
    return '思考中…'
  }
  return ''
}

function isLoadingBubble(item: MessageVO, index: number) {
  return chatting.value && index === messages.value.length - 1 && item.role === 'ASSISTANT' && !item.content
}

function onAgentDropdown(data: { value?: string | number }) {
  if (typeof data?.value === 'number') {
    selectAgent(data.value)
  }
}

async function loadPlatformModels() {
  try {
    const { data } = await listPlatformModels()
    platformModels.value = (data.data || []).filter((item) => item.status === 1)
  } catch {
    platformModels.value = []
  }
}

async function loadConversationMeta(id: number) {
  try {
    const { data } = await getConversation(id)
    activeConversationTitle.value = data.data?.title || ''
    selectAgentByConversation(data.data?.agentId)
  } catch {
    const cached = conversations.value.find((item) => item.id === id)
    activeConversationTitle.value = cached?.title || ''
    selectAgentByConversation(cached?.agentId)
  }
}

async function loadMessages(id: number) {
  const { data } = await listMessages(id)
  messages.value = data.data || []
  await scrollChatToBottom()
}

async function scrollChatToBottom() {
  await nextTick()
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

async function startNewChat(text: string) {
  const prompt = text.trim()
  if (!prompt || chatting.value) return
  const agentId = selectedAgent.value?.id
  if (!agentId) {
    MessagePlugin.warning('请先创建智能体')
    router.push('/agents')
    return
  }
  try {
    const { data } = await createConversation({
      agentId,
      title: prompt.slice(0, 24),
    })
    const id = data.data?.id
    if (!id) return
    suppressMessagesReload.value = true
    await router.push(`/chat/${id}`)
    await loadConversationMeta(id)
    await sendChat(id, prompt)
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建对话失败'))
  } finally {
    suppressMessagesReload.value = false
  }
}

async function sendChat(id?: number, preset?: string) {
  const targetId = id ?? conversationId.value
  if (!targetId) return
  const text = (preset ?? composerText.value).trim()
  if (!text || chatting.value) return

  chatting.value = true
  let assistantIndex = -1
  const usedPreset = Boolean(preset)

  messages.value.push({
    id: Date.now(),
    role: 'USER',
    content: text,
    contentType: 'TEXT',
    sequenceNo: messages.value.length + 1,
    createdAt: new Date().toISOString(),
  })
  if (!usedPreset) {
    composerText.value = ''
  }
  await scrollChatToBottom()

  try {
    assistantIndex = messages.value.length
    messages.value.push({
      id: Date.now() + 1,
      role: 'ASSISTANT',
      content: '',
      contentType: 'TEXT',
      sequenceNo: messages.value.length + 1,
      createdAt: new Date().toISOString(),
    })
    await scrollChatToBottom()

    await sendMessageStream(targetId, text, (delta) => {
      messages.value[assistantIndex].content += delta
      scrollChatToBottom()
    })

    if (!messages.value[assistantIndex]?.content) {
      messages.value[assistantIndex].content = '（无回复）'
    }
    await refreshConversations()
    await loadMessages(targetId)
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '发送失败'))
    if (assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
      messages.value.splice(assistantIndex, 1)
    }
    await loadMessages(targetId)
  } finally {
    chatting.value = false
    if (usedPreset) {
      composerText.value = ''
    }
  }
}

function onComposerKeydown(_value: string, context: { e: KeyboardEvent }) {
  const event = context.e
  if (chatting.value || event.key !== 'Enter') return

  const modifier = event.ctrlKey || event.metaKey
  const shouldSend = appPreferences.sendWithEnter
    ? !event.shiftKey && !modifier
    : modifier && !event.shiftKey

  if (!shouldSend) return

  event.preventDefault()
  if (conversationId.value) {
    sendChat()
  } else {
    startNewChat(composerText.value)
  }
}

function openFilePicker() {
  fileInputRef.value?.click()
}

function openActiveFilePicker() {
  activeFileInputRef.value?.click()
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const prefix = composerText.value.trim() ? '\n\n' : ''
    composerText.value += `${prefix}--- ${file.name} ---\n${text.slice(0, 8000)}`
    MessagePlugin.success('已添加文件内容')
  } catch {
    MessagePlugin.error('读取文件失败')
  } finally {
    input.value = ''
  }
}

function getSpeechRecognitionCtor() {
  const win = window as Window & {
    SpeechRecognition?: new () => BrowserSpeechRecognition
    webkitSpeechRecognition?: new () => BrowserSpeechRecognition
  }
  return win.SpeechRecognition || win.webkitSpeechRecognition
}

function toggleVoiceInput() {
  const SpeechRecognitionCtor = getSpeechRecognitionCtor()
  if (!SpeechRecognitionCtor) {
    MessagePlugin.warning('当前浏览器不支持语音输入')
    return
  }

  if (listening.value) {
    speechRecognition?.stop()
    return
  }

  speechRecognition = new SpeechRecognitionCtor()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.interimResults = false
  speechRecognition.onstart = () => {
    listening.value = true
  }
  speechRecognition.onend = () => {
    listening.value = false
  }
  speechRecognition.onerror = () => {
    listening.value = false
    MessagePlugin.error('语音识别失败')
  }
  speechRecognition.onresult = (resultEvent) => {
    const text = resultEvent.results[0]?.[0]?.transcript
    if (text) {
      composerText.value = composerText.value ? `${composerText.value} ${text}` : text
    }
  }
  speechRecognition.start()
}

watch(
  conversationId,
  async (id) => {
    if (id) {
      await loadConversationMeta(id)
      if (!suppressMessagesReload.value && !chatting.value) {
        await loadMessages(id)
      }
    } else {
      messages.value = []
      activeConversationTitle.value = ''
      composerText.value = ''
    }
  },
  { immediate: true },
)

watch(
  () => route.query.prompt,
  (prompt) => {
    if (typeof prompt === 'string' && prompt.trim() && !conversationId.value) {
      composerText.value = prompt.trim()
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await Promise.all([refreshAgents(), refreshSuggestions(), loadPlatformModels()])
})
</script>

<style scoped>
.chat-workspace {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #fff;
}

.chat-new {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 0;
  padding: 32px 40px 40px;
  box-sizing: border-box;
  overflow-y: auto;
}

.chat-stage {
  display: flex;
  flex-direction: column;
  width: min(720px, 100%);
  max-width: 720px;
  margin: 0 auto;
  flex-shrink: 0;
}

.chat-stage--active {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  width: min(720px, 100%);
  max-width: 720px;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
}

.chat-new__title {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  width: 100%;
  margin: 0 0 28px;
  font-size: 28px;
  font-weight: 500;
  line-height: 1.35;
  color: var(--box-ink);
  text-align: center;
}

.chat-new__agent {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.chat-new__agent-avatar {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 8px 10px 10px;
  border: 1px solid #d9dde3;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.composer:focus-within {
  border-color: #b8bec8;
  box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.04);
}

.composer--bottom {
  flex-shrink: 0;
  width: 100%;
  margin: 0 0 20px;
}

.composer__input :deep(.t-textarea__inner) {
  border: none;
  box-shadow: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  min-height: 56px;
  padding: 2px 6px 0;
  color: var(--box-ink);
}

.composer__input :deep(.t-textarea__inner::placeholder) {
  color: #b0b4bc;
}

.composer__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
}

.composer__left,
.composer__right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.agent-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px 4px 4px;
  border-radius: 999px;
  border: 1px solid #ebebeb;
  background: #fff;
  color: #646a73;
  font-size: 13px;
  cursor: pointer;
}

.agent-pill--compact {
  margin-top: 0;
}

.agent-pill:hover {
  border-color: #d0d3d6;
  color: #1f2329;
}

.agent-pill__avatar {
  flex-shrink: 0;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.agent-pill__arrow {
  font-size: 14px;
  color: #8f959e;
}

.composer__send {
  min-width: 32px;
  width: 32px;
  height: 32px;
  background: #c9cdd4 !important;
  border-color: #c9cdd4 !important;
  color: #fff !important;
}

.composer__send :deep(.t-icon) {
  color: #fff;
}

.composer__send--ready:not(:disabled) {
  background: #1f2329 !important;
  border-color: #1f2329 !important;
}

.composer__send:disabled {
  opacity: 1;
  cursor: not-allowed;
  background: #c9cdd4 !important;
  border-color: #c9cdd4 !important;
}

.suggestions-loading {
  margin-top: 20px;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
  margin-top: 20px;
}

.suggestion-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid #d9dde3;
  border-radius: 16px;
  background: #fff;
  color: #1f2329;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.suggestion-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.suggestion-card__title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
}

.suggestion-card__subtitle {
  margin: 0;
  font-size: 12px;
  line-height: 1.45;
  color: #8f959e;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.suggestion-card :deep(.t-icon) {
  flex-shrink: 0;
  color: #8f959e;
  font-size: 16px;
}

.suggestion-card:hover {
  border-color: #b8bec8;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.chat-active {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  width: 100%;
}

.chat-active__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 0 0;
  width: 100%;
  box-sizing: border-box;
}

.chat-active__title {
  font-size: 13px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px 0 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  box-sizing: border-box;
}

.chat-message {
  display: flex;
  max-width: 88%;
}

.chat-message--user {
  align-self: flex-end;
}

.chat-message--assistant {
  align-self: flex-start;
}

.chat-message__content {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-message--user .chat-message__content {
  background: var(--box-hover);
  color: var(--box-ink);
  border-bottom-right-radius: 6px;
}

.chat-message--assistant .chat-message__content {
  background: transparent;
  color: #1f2329;
  padding-left: 0;
}

.chat-message__content--loading {
  color: #8f959e;
}

.typing-dots {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
}

.typing-dots i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c9cdd4;
  animation: typing-bounce 1.1s infinite ease-in-out;
}

.typing-dots i:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dots i:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes typing-bounce {
  0%,
  80%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  40% {
    transform: translateY(-3px);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .suggestions {
    grid-template-columns: 1fr;
  }

  .chat-new__title {
    font-size: 22px;
  }
}
</style>
