<template>
  <div class="chat-workspace">
    <div v-if="!conversationId" class="chat-new">
      <div class="chat-stage">
      <chat-brand-hero />

      <chat-composer-stack class="chat-new__composer">
        <box-chat-sender
          v-model="composerText"
          data-testid="chat-composer-input"
          :loading="chatting"
          :can-send="canSendNewChat"
          :placeholder="composerPlaceholder"
          :min-rows="3"
          :show-voice="true"
          :show-model-picker="true"
          :listening="listening"
          :models="platformModels"
          :model-picker-disabled="!modelPickerEnabled"
          :auto-hint="autoModelHint"
          v-model:selected-model-key="selectedModelKey"
          :attachment-items="composerAttachmentItems"
          @send="onComposerSend"
          @stop="stopGeneration"
          @toggle-voice="toggleVoiceInput"
          @remove-attachment="removeComposerAttachment"
          @file-change="onFileSelected"
        />
        <template #agent>
          <chat-agent-rail />
        </template>
      </chat-composer-stack>

      <div v-if="suggestionsLoading" class="suggestions-loading">
        <t-loading size="small" />
      </div>
      <div v-else-if="displaySuggestions.length" class="suggestions">
        <button
          v-for="item in displaySuggestions"
          :key="item.id"
          type="button"
          class="suggestion-card"
          @click="onSuggestionClick(item)"
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
          <p class="chat-active__title-line">
            <span v-if="activeConversationTitle" class="chat-active__title">{{ activeConversationTitle }}</span>
            <span class="chat-active__disclaimer">- 内容由AI生成</span>
          </p>
        </div>
        <div class="chat-active__header-actions">
          <t-tooltip content="导出 Markdown" placement="left" theme="light" :show-arrow="false">
            <t-button variant="text" shape="square" size="small" :disabled="!messages.length" @click="exportConversation">
              <template #icon><t-icon name="download" /></template>
            </t-button>
          </t-tooltip>
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
      </div>

      <box-chat-message-list
        ref="chatListRef"
        class="chat-messages"
        :items="chatListItems"
        :actions-disabled="chatting"
        :show-scroll-button="true"
        :share-mode="shareMode"
        :selected-share-ids="selectedShareMessageIds"
        @regenerate="regenerateReply"
        @share="onShareMessage"
        @feedback="onMessageFeedback"
        @toggle-share-select="onToggleShareSelect"
      />

      <div v-if="shareMode" class="chat-share-bar">
        <span class="chat-share-bar__hint">勾选要分享的消息</span>
        <div class="chat-share-bar__actions">
          <t-button variant="outline" @click="cancelShareMode">取消</t-button>
          <t-button theme="primary" :loading="shareCreating" @click="createShareLink">创建分享链接</t-button>
        </div>
      </div>

      <chat-share-link-dialog v-model:visible="shareLinkDialogVisible" :share-url="createdShareUrl" />
      <chat-message-feedback-dialog
        v-if="conversationId && feedbackMessageId"
        v-model:visible="feedbackDialogVisible"
        :conversation-id="conversationId"
        :message-id="feedbackMessageId"
      />

      <chat-composer-stack v-if="!shareMode" class="chat-active__composer" :show-agent-rail="false">
        <box-chat-sender
          v-model="composerText"
          data-testid="chat-composer-input"
          :loading="chatting"
          :can-send="canSendMessage"
          placeholder="继续对话..."
          :max-rows="6"
          :show-model-picker="true"
          :models="platformModels"
          :model-picker-disabled="!modelPickerEnabled"
          :auto-hint="autoModelHint"
          v-model:selected-model-key="selectedModelKey"
          :attachment-items="composerAttachmentItems"
          @send="onComposerSend"
          @stop="stopGeneration"
          @remove-attachment="removeComposerAttachment"
          @file-change="onFileSelected"
        />
      </chat-composer-stack>
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
import type { TdAttachmentItem } from '@tdesign-vue-next/chat'
import BoxChatMessageList from '@/components/BoxChatMessageList.vue'
import BoxChatSender from '@/components/BoxChatSender.vue'
import ChatMessageFeedbackDialog from '@/components/ChatMessageFeedbackDialog.vue'
import ChatShareLinkDialog from '@/components/ChatShareLinkDialog.vue'
import ChatAgentRail from '@/components/ChatAgentRail.vue'
import ChatBrandHero from '@/components/ChatBrandHero.vue'
import ChatComposerStack from '@/components/ChatComposerStack.vue'
import { buildConversationListItems } from '@/utils/boxChatListItems'
import { extractApiError } from '@/api/apiError'
import { promptToolConfirmation } from '@/composables/useToolConfirmation'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'
import { uploadImageAsset } from '@/api/asset'
import { useAgentSelection } from '@/composables/useAgentSelection'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { useChatSuggestions, type ChatSuggestion } from '@/composables/useChatSuggestions'
import { useCreateAgentDialog } from '@/composables/useCreateAgentDialog'
import { useConversationNav } from '@/composables/useConversationNav'
import { useActiveProject } from '@/composables/useActiveProject'
import { useAuthStore } from '@/stores/auth'
import { getAvatarColor } from '@/utils/format'
import { classifyModelChatKind } from '@/utils/modelCapability'
import {
  createConversation,
  deleteMessage,
  getConversation,
  listMessages,
  regenerateMessageStream,
  sendMessageStream,
  parseMessageCitations,
  createConversationShare,
  submitMessageFeedback,
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
const auth = useAuthStore()
const { refresh: refreshConversations, refreshProjectConversations, conversations } = useConversationNav()
const { activeProjectId } = useActiveProject()

async function refreshConversationNav() {
  await Promise.all([
    refreshConversations({ unassigned: true }),
    refreshProjectConversations(activeProjectId.value),
  ])
}
const { openCreateAgentDialog } = useCreateAgentDialog()
const { agents, selectedAgent, refresh: refreshAgents, selectAgent, selectAgentByConversation } = useAgentSelection()
const { suggestions, loading: suggestionsLoading, refresh: refreshSuggestions } = useChatSuggestions()
const chatting = ref(false)
const composerText = ref('')
const composerAttachments = ref<ComposerAttachment[]>([])
const messages = ref<MessageVO[]>([])
const chatListRef = ref<InstanceType<typeof BoxChatMessageList> | null>(null)
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
const modelChoiceByAgent = new Map<number, string>()

const modelPickerEnabled = computed(() => {
  const agent = selectedAgent.value
  if (!agent) return false
  return agent.modelSource !== 'BYOK'
})

function pickAutoPlatformModel(): PlatformModelVO | undefined {
  const models = platformModels.value
  if (!models.length) return undefined
  const needsVision = composerAttachments.value.some((item) => item.kind === 'image')
  const ranked = needsVision
    ? [...models].sort((left, right) => Number(isVisionModel(right)) - Number(isVisionModel(left)))
    : models
  const preferredId = selectedAgent.value?.platformModelId
  if (!needsVision && preferredId != null) {
    const preferred = ranked.find((item) => item.id === preferredId)
    if (preferred) return preferred
  }
  return ranked[0]
}

function isVisionModel(model: PlatformModelVO) {
  return classifyModelChatKind(model.modelCode, model.modelName, model.description) === 'vision'
}

const autoResolvedModel = computed(() => pickAutoPlatformModel())

const autoModelHint = computed(() => {
  const model = autoResolvedModel.value
  if (!model) {
    return '暂无可用平台模型。请在管理端为已上架模型绑定密钥。'
  }
  const needsVision = composerAttachments.value.some((item) => item.kind === 'image')
  return needsVision
    ? `有图片附件时优先选用多模态模型，当前会使用 ${model.modelName}。`
    : `自动选用当前可用的平台模型，当前会使用 ${model.modelName}。`
})

function resolvePlatformModelId(): number | undefined {
  if (!modelPickerEnabled.value) {
    return undefined
  }
  if (selectedModelKey.value === 'auto') {
    return autoResolvedModel.value?.id
  }
  const id = Number(selectedModelKey.value)
  return Number.isFinite(id) ? id : autoResolvedModel.value?.id
}

function syncModelPickerWithAgent() {
  const agent = selectedAgent.value
  if (!agent || agent.modelSource === 'BYOK') {
    selectedModelKey.value = 'auto'
    return
  }
  selectedModelKey.value = modelChoiceByAgent.get(agent.id) || 'auto'
}

watch(
  () => selectedAgent.value?.id,
  () => {
    syncModelPickerWithAgent()
  },
)

watch(selectedModelKey, (value) => {
  const agentId = selectedAgent.value?.id
  if (agentId != null && modelPickerEnabled.value) {
    modelChoiceByAgent.set(agentId, value)
  }
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

const composerAttachmentItems = computed<TdAttachmentItem[]>(() =>
  composerAttachments.value.map((file) => ({
    key: file.id,
    name: file.name,
    url: file.previewUrl || file.remoteUrl,
    status: file.uploading ? 'progress' : 'success',
    description: file.uploading ? '上传中' : undefined,
  })),
)

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
  suggestions.value
    .filter((item) => !BRAND_SUGGESTION_TITLES.has(item.title.trim().toLowerCase()))
    .slice(0, 3),
)

function onSuggestionClick(item: ChatSuggestion) {
  void startNewChat(item.prompt)
}

const agentOptions = computed<DropdownOption[]>(() =>
  agents.value.map((agent) => ({
    content: agent.name,
    value: agent.id,
    active: agent.id === selectedAgent.value?.id,
  })),
)

const composerPlaceholder = computed(() =>
  selectedAgent.value
    ? `先把这次对话的目标丢进来，材料也可以一起带上…`
    : '先把这次对话的目标丢进来，材料也可以一起带上…',
)

const conversationId = computed(() => {
  const id = route.params.id
  if (!id || Array.isArray(id)) return null
  const num = Number(id)
  return Number.isFinite(num) ? num : null
})

const chatUserName = computed(
  () => auth.user?.nickname || auth.user?.username || auth.user?.email || '我',
)

const shareMode = ref(false)
const selectedShareMessageIds = ref<number[]>([])
const shareCreating = ref(false)
const shareLinkDialogVisible = ref(false)
const createdShareUrl = ref('')
const feedbackDialogVisible = ref(false)
const feedbackMessageId = ref(0)

const chatListItems = computed(() =>
  buildConversationListItems(
    messages.value,
    chatting.value,
    {
      enableRegenerate: true,
      enableDelete: false,
      enableFullActions: true,
    },
    {
      userName: chatUserName.value,
      agentName: selectedAgent.value?.name || 'Box AI',
      userAvatarUrl: auth.user?.avatarUrl,
    },
  ),
)

function onShareMessage(messageIndex: number) {
  if (!conversationId.value) return
  shareMode.value = true
  const ids = messages.value
    .slice(0, messageIndex + 1)
    .map((item) => item.id)
    .filter((id) => id > 0)
  selectedShareMessageIds.value = ids.length ? ids : []
}

function onToggleShareSelect(messageId: number, selected: boolean) {
  const set = new Set(selectedShareMessageIds.value)
  if (selected) {
    set.add(messageId)
  } else {
    set.delete(messageId)
  }
  selectedShareMessageIds.value = [...set].sort((a, b) => a - b)
}

function cancelShareMode() {
  shareMode.value = false
  selectedShareMessageIds.value = []
}

async function createShareLink() {
  const convId = conversationId.value
  if (!convId) return
  const messageIds = selectedShareMessageIds.value.filter((id) => id > 0)
  if (!messageIds.length) {
    MessagePlugin.warning('请至少选择一条消息')
    return
  }
  shareCreating.value = true
  try {
    const { data } = await createConversationShare(convId, {
      title: activeConversationTitle.value || undefined,
      messageIds,
    })
    const path = data.data?.sharePath || `/share/${data.data?.token}`
    createdShareUrl.value = `${window.location.origin}${path}`
    shareLinkDialogVisible.value = true
    shareMode.value = false
    selectedShareMessageIds.value = []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建分享失败'))
  } finally {
    shareCreating.value = false
  }
}

async function onMessageFeedback(messageIndex: number, rating: 'good' | 'bad') {
  const convId = conversationId.value
  const message = messages.value[messageIndex]
  if (!convId || !message?.id) return
  if (rating === 'bad') {
    feedbackMessageId.value = message.id
    feedbackDialogVisible.value = true
    return
  }
  try {
    await submitMessageFeedback(convId, message.id, { rating: 'good' })
    MessagePlugin.success('感谢反馈')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '提交失败'))
  }
}

function applyStreamCitations(index: number, citations: KnowledgeCitation[]) {
  if (index < 0 || !citations.length) return
  messages.value[index].citations = citations
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

function exportConversation() {
  if (!messages.value.length) {
    MessagePlugin.warning('暂无消息可导出')
    return
  }
  const title = activeConversationTitle.value || `conversation-${conversationId.value || 'draft'}`
  const lines = [`# ${title}`, '']
  for (const item of messages.value) {
    const role = item.role === 'ASSISTANT' ? 'Assistant' : item.role === 'USER' ? 'User' : item.role
    lines.push(`## ${role}`, '', item.content || '（空）', '')
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${title.replace(/[\\/:*?"<>|]/g, '_')}.md`
  anchor.click()
  URL.revokeObjectURL(url)
  MessagePlugin.success('对话已导出')
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

async function onDeleteMessage(index: number) {
  const item = messages.value[index]
  if (!item) return
  await removeMessage(item, index)
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
    await refreshConversationNav()
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
  chatListRef.value?.scrollToBottom()
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
      projectId: activeProjectId.value ?? undefined,
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

  const platformModelId = resolvePlatformModelId()
  if (modelPickerEnabled.value && platformModelId == null) {
    MessagePlugin.warning('暂无可用平台模型，请先在管理端绑定密钥，或手动选择模型')
    return
  }

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
  composerText.value = ''
  clearComposerAttachments()
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
      platformModelId,
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
    await refreshConversationNav()
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
    await refreshConversationNav()
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

function onComposerSend(value: string) {
  if (chatting.value) return
  if (conversationId.value) {
    void sendChat(undefined, buildComposerPayload(value))
  } else {
    void startNewChat(value)
  }
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
useReloadOnWorkspaceChange(async () => {
  messages.value = []
  await Promise.all([refreshAgents(), refreshSuggestions(), loadPlatformModels()])
  syncModelPickerWithAgent()
})
</script>

<style scoped>
.chat-workspace {
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: var(--box-surface);
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
  width: min(var(--box-chat-column-width), 100%);
  max-width: var(--box-chat-column-width);
  margin: 0 auto;
  flex-shrink: 0;
}

.chat-stage--active {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  width: min(var(--box-chat-column-width), 100%);
  max-width: var(--box-chat-column-width);
  margin: 0 auto;
  padding: 0 8px;
  box-sizing: border-box;
}

.chat-new__composer,
.chat-active__composer {
  width: min(var(--box-chat-column-width), 100%);
  max-width: var(--box-chat-column-width);
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 4px;
  flex-shrink: 0;
}

.suggestions-loading,
.suggestions {
  margin-top: 16px;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 8px 10px 10px;
  border: 1px solid var(--box-border);
  border-radius: 18px;
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.composer:focus-within {
  border-color: var(--td-component-border);
  box-shadow: 0 0 0 3px var(--box-hover);
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
  color: var(--box-muted);
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
  border: 1px solid var(--box-border);
  border-radius: 10px;
  background: var(--td-bg-color-secondarycontainer);
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
  color: var(--box-muted);
  cursor: pointer;
}

.composer-file__remove:hover {
  background: var(--box-hover);
  color: var(--box-ink);
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
  border: 1px solid var(--box-border);
  background: var(--box-surface);
  color: var(--box-muted);
  font-size: 13px;
  cursor: pointer;
}

.agent-pill--compact {
  margin-top: 0;
}

.agent-pill:hover {
  border-color: var(--td-component-border);
  color: var(--box-ink);
}

.agent-pill__avatar {
  flex-shrink: 0;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.agent-pill__arrow {
  font-size: 14px;
  color: var(--box-muted);
}

.composer__send {
  min-width: 32px;
  width: 32px;
  height: 32px;
  background: var(--td-bg-color-component) !important;
  border-color: var(--td-bg-color-component) !important;
  color: var(--td-text-color-anti) !important;
}

.composer__send :deep(.t-icon) {
  color: var(--td-text-color-anti);
}

.composer__send--ready:not(:disabled) {
  background: var(--box-ink) !important;
  border-color: var(--box-ink) !important;
  color: var(--box-bg) !important;
}

.composer__send--ready:not(:disabled) :deep(.t-icon) {
  color: var(--box-bg);
}

.composer__send:disabled {
  opacity: 1;
  cursor: not-allowed;
  background: var(--td-bg-color-component) !important;
  border-color: var(--td-bg-color-component) !important;
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
  border: 1px solid var(--box-border);
  border-radius: 16px;
  background: var(--box-surface);
  color: var(--box-ink);
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
  color: var(--box-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.suggestion-card :deep(.t-icon) {
  flex-shrink: 0;
  color: var(--box-muted);
  font-size: 16px;
}

.suggestion-card:hover {
  border-color: var(--td-component-border);
  box-shadow: var(--box-shadow-soft);
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

.chat-active__header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
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
  background: var(--box-surface);
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
  background: var(--td-bg-color-secondarycontainer);
  font-size: 11px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow-y: auto;
}

.chat-active__title-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  max-width: 100%;
  min-width: 0;
}

.chat-active__title {
  font-size: 13px;
  color: var(--box-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.chat-active__disclaimer {
  flex-shrink: 0;
  font-size: 12px;
  line-height: 20px;
  color: var(--box-muted);
  opacity: 0.85;
}

.chat-messages {
  padding: 24px 0 12px;
  width: 100%;
  box-sizing: border-box;
}

.chat-share-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--td-bg-color-container);
  border: 1px solid var(--td-component-border);
}

.chat-share-bar__hint {
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.chat-share-bar__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
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
  color: var(--box-ink);
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

.chat-message__content--loading {
  color: var(--box-muted);
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
  background: var(--td-bg-color-component);
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

}
</style>
