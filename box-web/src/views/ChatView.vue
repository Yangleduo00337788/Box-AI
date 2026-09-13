<template>
  <div class="chat-workspace">
    <div v-if="!conversationId" class="chat-new">
      <div class="chat-stage">
      <h1 class="chat-new__title">
        有什么想交给
        <t-dropdown
          v-if="selectedAgent"
          :options="agentOptions"
          trigger="click"
          :min-column-width="180"
          :popup-props="{ overlayInnerClassName: 'chat-agent-switch' }"
          @click="onAgentDropdown"
        >
          <button type="button" class="chat-new__agent" aria-haspopup="listbox" aria-label="切换智能体">
            <t-avatar size="24px" class="chat-new__agent-avatar" :style="{ background: agentAvatarColor }">
              {{ selectedAgent.name.slice(0, 1) }}
            </t-avatar>
            {{ selectedAgent.name }}
            <t-icon name="chevron-down" class="chat-new__agent-arrow" />
          </button>
        </t-dropdown>
        <template v-else> Box </template>
        ？
      </h1>

      <div class="composer">
        <t-textarea
          v-model="composerText"
          data-testid="chat-composer-input"
          :placeholder="composerPlaceholder"
          :autosize="{ minRows: 2, maxRows: 8 }"
          class="composer__input"
          :disabled="chatting"
          @keydown="onComposerKeydown"
        />
        <div v-if="composerAttachments.length" class="composer__files">
          <div v-for="file in composerAttachments" :key="file.id" class="composer-file">
            <img v-if="file.previewUrl" :src="file.previewUrl" alt="" class="composer-file__thumb" />
            <t-icon v-else name="file-1" size="18px" />
            <span class="composer-file__name">{{ file.uploading ? '正在上传…' : file.name }}</span>
            <button type="button" class="composer-file__remove" aria-label="移除文件" @click="removeComposerAttachment(file.id)">
              <t-icon name="close" size="14px" />
            </button>
          </div>
        </div>
        <input
          ref="fileInputRef"
          type="file"
          class="composer__file-input"
          accept=".txt,.md,.json,.csv,.log,.png,.jpg,.jpeg,text/plain,image/png,image/jpeg"
          @change="onFileSelected"
        />
        <div class="composer__toolbar">
          <div class="composer__left">
            <t-tooltip content="添加文件" placement="top" theme="light" :show-arrow="false">
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
            <chat-model-picker
              v-model="selectedModelKey"
              :models="platformModels"
              :disabled="!modelPickerEnabled"
            />
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
      <div class="chat-active__layout">
      <div class="chat-stage chat-stage--active">
      <div class="chat-active__header">
        <div class="chat-active__header-main">
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
        <t-tooltip :content="tracePanelOpen ? '收起 Trace' : '展开 Trace'" placement="left" theme="light" :show-arrow="false">
          <t-button
            variant="text"
            shape="square"
            size="small"
            :class="{ 'trace-toggle--active': tracePanelOpen }"
            @click="toggleTracePanel"
          >
            <template #icon><t-icon name="chart-bubble" /></template>
          </t-button>
        </t-tooltip>
      </div>

      <div ref="chatListRef" class="chat-messages box-hide-scrollbar">
        <div
          v-for="(item, index) in messages"
          :key="item.id ?? index"
          class="chat-message"
          :class="`chat-message--${item.role.toLowerCase()}`"
        >
          <div class="chat-message__body">
            <div class="chat-message__content" :class="{ 'chat-message__content--loading': isLoadingBubble(item, index) }">
              <span v-if="isLoadingBubble(item, index)" class="typing-dots" aria-label="思考中">
                <i /><i /><i />
              </span>
              <template v-else-if="item.role === 'USER'">
                <div v-if="userMessageParts(item, index).images.length" class="chat-message__images">
                  <a
                    v-for="(image, imageIndex) in userMessageParts(item, index).images"
                    :key="`${image.url}-${imageIndex}`"
                    class="chat-message__image"
                    :href="image.url"
                    target="_blank"
                    rel="noreferrer"
                    @click.stop
                  >
                    <t-image :src="image.url" :alt="image.name" fit="cover" shape="round" />
                  </a>
                </div>
                <span v-if="userMessageParts(item, index).text">{{ userMessageParts(item, index).text }}</span>
              </template>
              <chat-markdown v-else :content="displayContent(item, index)" />
            </div>
            <div v-if="messageCitations(item).length" class="chat-citations">
              <span class="chat-citations__label">引用来源</span>
              <div class="chat-citations__list">
                <t-popup
                  v-for="cite in messageCitations(item)"
                  :key="cite.index"
                  placement="top"
                  trigger="hover"
                  show-arrow
                  destroy-on-close
                >
                  <template #content>
                    <div class="chat-citation-popup">
                      <div class="chat-citation-popup__title">{{ cite.documentName }}</div>
                      <div class="chat-citation-popup__body">{{ cite.content }}</div>
                    </div>
                  </template>
                  <button type="button" class="chat-citation-chip">
                    [{{ cite.index }}] {{ cite.documentName }}
                  </button>
                </t-popup>
              </div>
            </div>
            <div
              v-if="canShowMessageActions(item, index)"
              class="chat-message__actions"
            >
              <t-tooltip content="复制" placement="top" theme="light" :show-arrow="false">
                <t-button variant="text" shape="square" size="small" @click="copyMessage(item)">
                  <template #icon><t-icon name="file-copy" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip
                v-if="canRegenerate(item, index)"
                content="重新生成"
                placement="top"
                theme="light"
                :show-arrow="false"
              >
                <t-button
                  variant="text"
                  shape="square"
                  size="small"
                  data-testid="chat-regenerate"
                  :disabled="chatting"
                  @click="regenerateReply(index)"
                >
                  <template #icon><t-icon name="refresh" /></template>
                </t-button>
              </t-tooltip>
              <t-tooltip content="删除" placement="top" theme="light" :show-arrow="false">
                <t-button variant="text" shape="square" size="small" :disabled="chatting" @click="removeMessage(item, index)">
                  <template #icon><t-icon name="delete" /></template>
                </t-button>
              </t-tooltip>
            </div>
          </div>
        </div>
      </div>

      <div class="composer composer--bottom">
        <t-textarea
          v-model="composerText"
          data-testid="chat-composer-input"
          placeholder="继续对话..."
          :autosize="{ minRows: 2, maxRows: 6 }"
          class="composer__input"
          :disabled="chatting"
          @keydown="onComposerKeydown"
        />
        <div v-if="composerAttachments.length" class="composer__files">
          <div v-for="file in composerAttachments" :key="file.id" class="composer-file">
            <img v-if="file.previewUrl" :src="file.previewUrl" alt="" class="composer-file__thumb" />
            <t-icon v-else name="file-1" size="18px" />
            <span class="composer-file__name">{{ file.uploading ? '正在上传…' : file.name }}</span>
            <button type="button" class="composer-file__remove" aria-label="移除文件" @click="removeComposerAttachment(file.id)">
              <t-icon name="close" size="14px" />
            </button>
          </div>
        </div>
        <input
          ref="activeFileInputRef"
          type="file"
          class="composer__file-input"
          accept=".txt,.md,.json,.csv,.log,.png,.jpg,.jpeg,text/plain,image/png,image/jpeg"
          @change="onFileSelected"
        />
        <div class="composer__toolbar">
          <div class="composer__left">
            <t-tooltip content="添加文件" placement="top" theme="light" :show-arrow="false">
              <t-button variant="text" shape="square" size="small" @click="openActiveFilePicker">
                <template #icon><t-icon name="add" /></template>
              </t-button>
            </t-tooltip>
          </div>
          <div class="composer__right">
            <chat-model-picker
              v-model="selectedModelKey"
              :models="platformModels"
              :disabled="!modelPickerEnabled"
            />
            <t-button
              v-if="chatting"
              variant="outline"
              size="small"
              @click="stopGeneration"
            >
              停止生成
            </t-button>
            <t-button
              v-else
              class="composer__send"
              :class="{ 'composer__send--ready': canSendMessage }"
              shape="circle"
              :disabled="!canSendMessage"
              @click="sendChat()"
            >
              <template #icon><t-icon name="chevron-up" /></template>
            </t-button>
          </div>
        </div>
      </div>
      </div>

      <aside v-if="tracePanelOpen" class="chat-trace-panel">
        <div class="chat-trace-panel__head">
          <h3>执行 Trace</h3>
          <span v-if="latestExecution?.durationMs" class="chat-trace-panel__meta">
            {{ latestExecution.durationMs }} ms
            <template v-if="latestExecution.totalTokens"> · {{ latestExecution.totalTokens }} tokens</template>
          </span>
        </div>
        <t-loading :loading="traceLoading" size="small">
          <div v-if="traceSpans.length" class="chat-trace-panel__list">
            <div v-for="span in traceSpans" :key="span.spanId" class="trace-span">
              <div class="trace-span__head">
                <strong>{{ span.name }}</strong>
                <t-tag size="small" variant="light">{{ span.spanType }}</t-tag>
                <span>{{ span.durationMs ?? 0 }} ms</span>
              </div>
              <pre v-if="span.outputJson" class="trace-span__output">{{ formatTraceJson(span.outputJson) }}</pre>
            </div>
          </div>
          <t-empty v-else description="暂无 Trace 数据" />
        </t-loading>
      </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { DropdownOption } from 'tdesign-vue-next'
import ChatMarkdown from '@/components/ChatMarkdown.vue'
import ChatModelPicker from '@/components/ChatModelPicker.vue'
import { parseUserContent } from '@/utils/chatContent'
import { extractApiError } from '@/api/apiError'
import { promptToolConfirmation } from '@/composables/useToolConfirmation'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'
import { uploadImageAsset } from '@/api/asset'
import { appPreferences } from '@/composables/useAppPreferences'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useChatSuggestions } from '@/composables/useChatSuggestions'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { useConversationNav } from '@/composables/useConversationNav'
import { getAvatarColor } from '@/utils/format'
import {
  createConversation,
  deleteMessage,
  getConversation,
  listMessages,
  regenerateMessageStream,
  sendMessageStream,
  parseMessageCitations,
  type MessageVO,
} from '@/api/conversation'
import type { KnowledgeCitation } from '@/api/agent'
import {
  getExecutionTrace,
  getLatestExecutionByConversation,
  type ExecutionVO,
  type TraceSpanVO,
} from '@/api/execution'

const route = useRoute()
const router = useRouter()
const { refresh: refreshConversations, conversations } = useConversationNav()
const { openCreateAgentDialog } = useCreateAgentDialog()
const { agents, selectedAgent, refresh: refreshAgents, selectAgent, selectAgentByConversation } = useAgentSelection()
const { suggestions, loading: suggestionsLoading, refresh: refreshSuggestions } = useChatSuggestions()

const chatting = ref(false)
const composerText = ref('')
const composerAttachments = ref<ComposerAttachment[]>([])
const messages = ref<MessageVO[]>([])
const chatListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const activeFileInputRef = ref<HTMLInputElement | null>(null)
const listening = ref(false)

interface ComposerAttachment {
  id: string
  name: string
  kind: 'image' | 'text'
  uploading?: boolean
  previewUrl?: string
  remoteUrl?: string
  textContent?: string
}

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

const modelPickerEnabled = computed(() => {
  const agent = selectedAgent.value
  if (!agent) return false
  return agent.modelSource !== 'BYOK'
})

function resolvePlatformModelId(): number | undefined {
  if (!modelPickerEnabled.value || selectedModelKey.value === 'auto') {
    return undefined
  }
  const id = Number(selectedModelKey.value)
  return Number.isFinite(id) ? id : undefined
}

function syncModelPickerWithAgent() {
  const agent = selectedAgent.value
  if (!agent || agent.modelSource === 'BYOK') {
    selectedModelKey.value = 'auto'
    return
  }
  if (agent.platformModelId != null) {
    selectedModelKey.value = String(agent.platformModelId)
    return
  }
  selectedModelKey.value = 'auto'
}

watch(selectedAgent, () => {
  syncModelPickerWithAgent()
})
const activeConversationTitle = ref('')
/** 新会话首条消息发送中，避免路由切换时 loadMessages 覆盖乐观更新 */
const suppressMessagesReload = ref(false)
let streamAbortController: AbortController | null = null
const stoppedByUser = ref(false)
const tracePanelOpen = ref(false)
const traceLoading = ref(false)
const traceSpans = ref<TraceSpanVO[]>([])
const latestExecution = ref<ExecutionVO | null>(null)

const agentAvatarColor = computed(() => getAvatarColor(selectedAgent.value?.name || 'Box'))

const attachmentsReady = computed(() =>
  composerAttachments.value.some(
    (item) => !item.uploading && (item.kind === 'text' ? !!item.textContent : !!item.remoteUrl),
  ),
)
const attachmentsUploading = computed(() => composerAttachments.value.some((item) => item.uploading))

const canSendNewChat = computed(
  () =>
    (!!composerText.value.trim() || attachmentsReady.value) &&
    !chatting.value &&
    !attachmentsUploading.value &&
    !!selectedAgent.value,
)

const canSendMessage = computed(
  () => (!!composerText.value.trim() || attachmentsReady.value) && !chatting.value && !attachmentsUploading.value,
)

const BRAND_SUGGESTION_TITLES = new Set(['box', 'coze', '扣子', '盒子'])

const displaySuggestions = computed(() =>
  suggestions.value.filter((item) => !BRAND_SUGGESTION_TITLES.has(item.title.trim().toLowerCase())),
)

const agentOptions = computed<DropdownOption[]>(() =>
  agents.value.map((agent) => ({
    content: agent.name,
    value: agent.id,
    active: agent.id === selectedAgent.value?.id,
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

function messageCitations(item: MessageVO): KnowledgeCitation[] {
  if (item.citations?.length) return item.citations
  return parseMessageCitations(item.metadataJson)
}

function applyStreamCitations(index: number, citations: KnowledgeCitation[]) {
  if (index < 0 || !citations.length) return
  messages.value[index].citations = citations
}

function displayContent(item: MessageVO, index: number) {
  if (item.content) return item.content
  if (chatting.value && index === messages.value.length - 1 && item.role === 'ASSISTANT') {
    return '思考中…'
  }
  return ''
}

function userMessageParts(item: MessageVO, index: number) {
  return parseUserContent(displayContent(item, index))
}

function isLoadingBubble(item: MessageVO, index: number) {
  return chatting.value && index === messages.value.length - 1 && item.role === 'ASSISTANT' && !item.content
}

function canShowMessageActions(item: MessageVO, index: number) {
  if (isLoadingBubble(item, index)) return false
  return Boolean(item.content?.trim())
}

function canRegenerate(item: MessageVO, index: number) {
  return item.role === 'ASSISTANT' && index === messages.value.length - 1 && !chatting.value
}

function formatTraceJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function toggleTracePanel() {
  tracePanelOpen.value = !tracePanelOpen.value
  if (tracePanelOpen.value && conversationId.value) {
    loadLatestTrace(conversationId.value)
  }
}

async function loadLatestTrace(convId: number, executionId?: number) {
  traceLoading.value = true
  try {
    const { data } = await getLatestExecutionByConversation(convId)
    latestExecution.value = data.data || null
    const execId = executionId ?? data.data?.id
    if (!execId) {
      traceSpans.value = []
      return
    }
    const traceRes = await getExecutionTrace(execId)
    traceSpans.value = traceRes.data.data?.spans || []
  } catch {
    traceSpans.value = []
  } finally {
    traceLoading.value = false
  }
}

async function copyMessage(item: MessageVO) {
  if (!item.content?.trim()) return
  try {
    await navigator.clipboard.writeText(item.content)
    MessagePlugin.success('已复制')
  } catch {
    MessagePlugin.error('复制失败')
  }
}

async function removeMessage(item: MessageVO, index: number) {
  if (chatting.value) return
  const targetId = conversationId.value
  if (!targetId) return
  try {
    if (item.id && item.id > 0) {
      await deleteMessage(targetId, item.id)
    }
    messages.value.splice(index, 1)
    MessagePlugin.success('消息已删除')
    await refreshConversations()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '删除失败'))
  }
}

function stopGeneration() {
  stoppedByUser.value = true
  streamAbortController?.abort()
}

function beginStream() {
  stoppedByUser.value = false
  streamAbortController?.abort()
  streamAbortController = new AbortController()
  return streamAbortController
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
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
  messages.value = (data.data || []).map((item) => ({
    ...item,
    citations: parseMessageCitations(item.metadataJson),
  }))
  await scrollChatToBottom()
}

async function scrollChatToBottom() {
  await nextTick()
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

async function startNewChat(text: string) {
  const prompt = buildComposerPayload(text)
  if (!prompt || chatting.value) return
  const agentId = selectedAgent.value?.id
  if (!agentId) {
    MessagePlugin.warning('请先创建智能体')
    openCreateAgentDialog()
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
    composerText.value = ''
    clearComposerAttachments()
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建对话失败'))
  } finally {
    suppressMessagesReload.value = false
  }
}

async function sendChat(id?: number, preset?: string) {
  const targetId = id ?? conversationId.value
  if (!targetId) return
  const text = (preset ?? buildComposerPayload()).trim()
  if (!text || chatting.value) return

  chatting.value = true
  let assistantIndex = -1
  const usedPreset = Boolean(preset)
  const controller = beginStream()

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
    clearComposerAttachments()
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
    }, {
      signal: controller.signal,
      platformModelId: resolvePlatformModelId(),
      onCitations: (citations) => applyStreamCitations(assistantIndex, citations),
      onDone: (executionId) => {
        if (tracePanelOpen.value) {
          loadLatestTrace(targetId, executionId)
        }
      },
      onToolConfirm: async (payload) => {
        const agentId = selectedAgent.value?.id
        if (!agentId) return
        const confirmed = await promptToolConfirmation(agentId, payload)
        if (confirmed && assistantIndex >= 0) {
          messages.value[assistantIndex].content += `\n\n[已确认执行工具 ${payload.toolName || payload.toolKey}]`
        }
      },
    })

    if (stoppedByUser.value) {
      if (!messages.value[assistantIndex]?.content) {
        messages.value[assistantIndex].content = '（已停止生成）'
      }
    } else if (!messages.value[assistantIndex]?.content) {
      messages.value[assistantIndex].content = '（无回复）'
    }
    await refreshConversations()
    if (!stoppedByUser.value) {
      await loadMessages(targetId)
    }
  } catch (error) {
    if (isAbortError(error)) {
      if (assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
        messages.value[assistantIndex].content = '（已停止生成）'
      }
    } else {
      MessagePlugin.error(extractApiError(error, '发送失败'))
      if (assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
        messages.value.splice(assistantIndex, 1)
      }
      await loadMessages(targetId)
    }
  } finally {
    chatting.value = false
    streamAbortController = null
    if (usedPreset) {
      composerText.value = ''
    }
  }
}

async function regenerateReply(index: number) {
  const targetId = conversationId.value
  if (!targetId || chatting.value) return
  const item = messages.value[index]
  if (!item || item.role !== 'ASSISTANT' || index !== messages.value.length - 1) return

  chatting.value = true
  const controller = beginStream()
  item.content = ''
  await scrollChatToBottom()

  try {
    await regenerateMessageStream(targetId, (delta) => {
      messages.value[index].content += delta
      scrollChatToBottom()
    }, {
      signal: controller.signal,
      platformModelId: resolvePlatformModelId(),
      onCitations: (citations) => {
        messages.value[index].citations = citations
      },
      onDone: (executionId) => {
        if (tracePanelOpen.value) {
          loadLatestTrace(targetId, executionId)
        }
      },
    })

    if (stoppedByUser.value) {
      if (!messages.value[index]?.content) {
        messages.value[index].content = '（已停止生成）'
      }
    } else if (!messages.value[index]?.content) {
      messages.value[index].content = '（无回复）'
    }
    await refreshConversations()
    if (!stoppedByUser.value) {
      await loadMessages(targetId)
    }
  } catch (error) {
    if (isAbortError(error)) {
      if (!messages.value[index]?.content) {
        messages.value[index].content = '（已停止生成）'
      }
    } else {
      MessagePlugin.error(extractApiError(error, '重新生成失败'))
      await loadMessages(targetId)
    }
  } finally {
    chatting.value = false
    streamAbortController = null
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

function buildComposerPayload(raw?: string) {
  const parts: string[] = []
  const text = (raw ?? composerText.value).trim()
  if (text) parts.push(text)
  for (const file of composerAttachments.value) {
    if (file.uploading) continue
    if (file.kind === 'image' && file.remoteUrl) {
      parts.push(`[图片: ${file.name}]\n${file.remoteUrl}`)
    } else if (file.kind === 'text' && file.textContent) {
      parts.push(`--- ${file.name} ---\n${file.textContent.slice(0, 8000)}`)
    }
  }
  return parts.join('\n\n')
}

function clearComposerAttachments() {
  for (const file of composerAttachments.value) {
    if (file.previewUrl) URL.revokeObjectURL(file.previewUrl)
  }
  composerAttachments.value = []
}

function removeComposerAttachment(id: string) {
  const current = composerAttachments.value.find((item) => item.id === id)
  if (current?.previewUrl) URL.revokeObjectURL(current.previewUrl)
  composerAttachments.value = composerAttachments.value.filter((item) => item.id !== id)
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const name = file.name
  const id = `${Date.now()}-${name}`
  const isImage = /\.(png|jpe?g)$/i.test(name) || file.type.startsWith('image/')
  const isText = /\.(txt|md|json|csv|log)$/i.test(name) || file.type.startsWith('text/')
  try {
    if (isImage) {
      if (file.size > 2 * 1024 * 1024) {
        MessagePlugin.warning('图片不能超过 2MB')
        return
      }
      const previewUrl = URL.createObjectURL(file)
      composerAttachments.value.push({ id, name, kind: 'image', previewUrl, uploading: true })
      try {
        const { data } = await uploadImageAsset(file)
        const item = composerAttachments.value.find((entry) => entry.id === id)
        if (item) {
          item.remoteUrl = data.data.url
          item.uploading = false
        }
        MessagePlugin.success('已添加图片')
      } catch (error) {
        removeComposerAttachment(id)
        MessagePlugin.error(extractApiError(error, '图片上传失败'))
      }
      return
    }
    if (isText) {
      const textContent = await file.text()
      composerAttachments.value.push({ id, name, kind: 'text', textContent })
      MessagePlugin.success('已添加文本文件')
      return
    }
    MessagePlugin.warning('请上传 PNG/JPG 图片，或 TXT、MD、JSON、CSV 文本')
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
      if (tracePanelOpen.value) {
        await loadLatestTrace(id)
      }
    } else {
      messages.value = []
      activeConversationTitle.value = ''
      composerText.value = ''
      traceSpans.value = []
      latestExecution.value = null
    }
  },
  { immediate: true },
)

watch(conversations, (list) => {
  const id = conversationId.value
  if (!id) return
  const found = list.find((item) => item.id === id)
  if (found) {
    activeConversationTitle.value = found.title || ''
  }
})

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
  syncModelPickerWithAgent()
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
  margin: 0 2px;
  padding: 0 4px 0 2px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
  vertical-align: middle;
}

.chat-new__agent:hover {
  background: rgba(15, 23, 42, 0.04);
}

.chat-new__agent-avatar {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.chat-new__agent-arrow {
  font-size: 18px;
  color: var(--box-ink-muted, #8f959e);
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

.composer__file-input {
  display: none;
}

.composer__files {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 4px 6px 0;
}

.composer-file {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  padding: 6px 8px;
  border: 1px solid #e7e9ee;
  border-radius: 10px;
  background: #f7f8fa;
}

.composer-file__thumb {
  width: 36px;
  height: 36px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.composer-file__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--box-ink);
}

.composer-file__remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #8f959e;
  cursor: pointer;
}

.composer-file__remove:hover {
  background: #eceef1;
  color: #1f2329;
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

.chat-active__layout {
  display: flex;
  flex: 1;
  min-height: 0;
  width: 100%;
}

.chat-active__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 16px 0 0;
  width: 100%;
  box-sizing: border-box;
}

.chat-active__header-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.trace-toggle--active {
  color: var(--td-brand-color) !important;
}

.chat-trace-panel {
  flex-shrink: 0;
  width: 320px;
  border-left: 1px solid var(--box-border);
  padding: 16px;
  overflow-y: auto;
  background: #fafbfc;
}

.chat-trace-panel__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-trace-panel__head h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.chat-trace-panel__meta {
  font-size: 12px;
  color: var(--box-muted);
}

.chat-trace-panel__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trace-span {
  padding: 10px 12px;
  border: 1px solid var(--box-border);
  border-radius: 8px;
  background: #fff;
}

.trace-span__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.trace-span__output {
  margin: 0;
  padding: 8px;
  border-radius: 6px;
  background: #f7f8fa;
  font-size: 11px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow-y: auto;
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

.chat-message__body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
}

.chat-message__actions {
  display: flex;
  align-items: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.chat-message:hover .chat-message__actions,
.chat-message:focus-within .chat-message__actions {
  opacity: 1;
}

.chat-message--user .chat-message__actions {
  justify-content: flex-end;
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
  word-break: break-word;
}

.chat-message--user .chat-message__content {
  background: var(--box-hover);
  color: var(--box-ink);
  border-bottom-right-radius: 6px;
  white-space: pre-wrap;
}

.chat-message--assistant .chat-message__content {
  background: transparent;
  color: #1f2329;
  padding-left: 0;
  white-space: normal;
}

.chat-message__images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.chat-message__images:last-child {
  margin-bottom: 0;
}

.chat-message__image {
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  cursor: zoom-in;
}

.chat-message__image :deep(.t-image) {
  width: 168px;
  height: 168px;
  border-radius: 12px;
}

.chat-citations {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

.chat-citations__label {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.chat-citations__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chat-citation-chip {
  border: 1px solid var(--td-component-border);
  background: var(--td-bg-color-container);
  color: var(--td-text-color-primary);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  cursor: pointer;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-citation-chip:hover {
  border-color: var(--td-brand-color);
  color: var(--td-brand-color);
}

.chat-citation-popup {
  max-width: 360px;
}

.chat-citation-popup__title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.chat-citation-popup__body {
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
  white-space: pre-wrap;
  max-height: 160px;
  overflow: auto;
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
