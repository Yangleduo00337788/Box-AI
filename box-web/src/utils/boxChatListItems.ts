import type { KnowledgeCitation } from '@/api/agent'
import { parseMessageCitations, type MessageVO } from '@/api/conversation'
import { parseUserContent, type ChatImagePart } from '@/utils/chatContent'
import {
  toChatUiContent,
  toChatUiRole,
  toChatUiStatus,
  toLocalChatUiContent,
  toLocalChatUiRole,
  toLocalChatUiStatus,
  isLocalLoadingBubble,
  type ChatUiContentBlock,
  type ChatUiRole,
  type ChatUiStatus,
  type LocalChatMessage,
} from '@/utils/chatMessageAdapter'
import { formatChatMessageTime, getAvatarColor } from '@/utils/format'
import {
  parseMessagePlugins,
  parseMessageReasoning,
  stripLegacyPluginSuffix,
  type ChatToolRun,
  type MessagePluginMeta,
} from '@/utils/messageMetadata'
import type { SharedConversationVO } from '@/api/conversation'

export type BoxChatActionBarItem = 'copy' | 'replay' | 'good' | 'bad' | 'share'

export interface BoxChatReplyQuote {
  authorName: string
  excerpt: string
}

export interface BoxChatListItem {
  key: string | number
  messageId?: number
  uiRole: ChatUiRole
  uiContent: ChatUiContentBlock[]
  uiStatus: ChatUiStatus
  animation?: 'gradient'
  plainText: string
  /** 复制操作栏使用的纯文本 */
  copyText: string
  displayName: string
  sentTimeLabel?: string
  sentAt?: string
  avatarUrl?: string
  avatarText: string
  avatarColor?: string
  userImages: ChatImagePart[]
  userText?: string
  citations: KnowledgeCitation[]
  reasoning?: string
  /** 等待首包 / 无正文时展示「思考中」渐变动画 */
  thinkingActive?: boolean
  thinkingLabel?: string
  toolInvokeActive?: boolean
  toolInvokeLabel?: string
  enabledPlugins?: MessagePluginMeta[]
  toolRuns?: ChatToolRun[]
  replyQuote?: BoxChatReplyQuote
  showActions: boolean
  actionBar: BoxChatActionBarItem[]
  showDelete: boolean
}

export interface BoxChatIdentityContext {
  userName: string
  agentName: string
  userAvatarUrl?: string
}

export interface BoxChatListActionsOptions {
  enableRegenerate?: boolean
  enableDelete?: boolean
  /** 主对话：复制 / 重生成 / 赞 / 踩 / 分享 */
  enableFullActions?: boolean
}

function excerptChatText(content: string, maxLen = 120): string {
  const { text } = parseUserContent(content)
  const normalized = (text || content).replace(/\s+/g, ' ').trim()
  if (!normalized) return '…'
  return normalized.length > maxLen ? `${normalized.slice(0, maxLen)}…` : normalized
}

function buildAssistantActionBar(showReplay: boolean, full: boolean): BoxChatActionBarItem[] {
  if (!full) {
    return ['copy']
  }
  const bar: BoxChatActionBarItem[] = ['copy']
  if (showReplay) bar.push('replay')
  bar.push('good', 'bad', 'share')
  return bar
}

function buildUserActionBar(full: boolean): BoxChatActionBarItem[] {
  if (!full) return ['copy']
  return ['copy', 'share']
}

function messageHasBody(isUser: boolean, content: string, userText?: string, imageCount = 0) {
  if (imageCount > 0) return true
  if (isUser) return Boolean(userText?.trim())
  return Boolean(content?.trim())
}

function userIdentity(ctx: BoxChatIdentityContext) {
  const name = ctx.userName || '我'
  return {
    displayName: name,
    avatarText: name.slice(0, 1).toUpperCase(),
    avatarUrl: ctx.userAvatarUrl,
    avatarColor: getAvatarColor(name),
  }
}

function agentIdentity(ctx: BoxChatIdentityContext) {
  const name = ctx.agentName || 'Box AI'
  return {
    displayName: name,
    avatarText: name.slice(0, 1).toUpperCase(),
    avatarColor: getAvatarColor(name),
  }
}

function findConversationReplyQuote(
  messages: MessageVO[],
  index: number,
  userName: string,
): BoxChatReplyQuote | undefined {
  for (let i = index - 1; i >= 0; i--) {
    const prev = messages[i]
    if (prev.role === 'USER') {
      return {
        authorName: userName,
        excerpt: excerptChatText(prev.content || ''),
      }
    }
  }
  return undefined
}

function findLocalReplyQuote(
  messages: LocalChatListMessage[],
  index: number,
  userName: string,
): BoxChatReplyQuote | undefined {
  for (let i = index - 1; i >= 0; i--) {
    const prev = messages[i]
    if (prev.role === 'user') {
      return {
        authorName: userName,
        excerpt: excerptChatText(prev.content || ''),
      }
    }
  }
  return undefined
}

export type ConversationMessageVO = MessageVO & {
  toolRuns?: ChatToolRun[]
}

export function buildConversationListItems(
  messages: ConversationMessageVO[],
  chatting: boolean,
  options: BoxChatListActionsOptions = {},
  identity: BoxChatIdentityContext,
): BoxChatListItem[] {
  const {
    enableRegenerate = true,
    enableDelete = false,
    enableFullActions = true,
  } = options
  const user = userIdentity(identity)
  const agent = agentIdentity(identity)

  return messages.map((item, index) => {
    const toolRuns = item.toolRuns || []
    const runningTool = toolRuns.find((run) => run.status === 'running')
    const hasAssistantText = Boolean(item.content?.trim())
    const thinkingActive =
      chatting &&
      index === messages.length - 1 &&
      item.role === 'ASSISTANT' &&
      !hasAssistantText &&
      !runningTool
    const toolInvokeActive =
      chatting && index === messages.length - 1 && item.role === 'ASSISTANT' && Boolean(runningTool)
    const streaming =
      chatting &&
      index === messages.length - 1 &&
      item.role === 'ASSISTANT' &&
      Boolean(item.content?.trim())
    const citations = item.citations?.length ? item.citations : parseMessageCitations(item.metadataJson)
    const reasoning = parseMessageReasoning(item.metadataJson)
    const isUser = item.role === 'USER'
    const rawContent = isUser ? stripLegacyPluginSuffix(item.content || '') : item.content || ''
    const parts = isUser ? parseUserContent(rawContent) : null
    const enabledPlugins = isUser ? parseMessagePlugins(item.metadataJson) : []
    const showReplay =
      enableRegenerate &&
      item.role === 'ASSISTANT' &&
      index === messages.length - 1 &&
      !chatting
    const id = isUser ? user : agent
    const userImages = isUser ? parseUserContent(item.content || '').images : []
    const hasBody =
      messageHasBody(isUser, rawContent, parts?.text, userImages.length) ||
      enabledPlugins.length > 0 ||
      (toolRuns.length > 0 && !isUser)
    const sentAt = item.createdAt || undefined
    const sentTimeLabel = formatChatMessageTime(sentAt) || undefined
    const copyText = isUser ? (parts?.text || rawContent || '') : item.content || ''

    return {
      key: item.id ?? index,
      messageId: item.id > 0 ? item.id : undefined,
      uiRole: toChatUiRole(item.role),
      uiContent: toChatUiContent(item, index, messages, chatting),
      uiStatus: thinkingActive
        ? 'complete'
        : streaming
          ? 'streaming'
          : toChatUiStatus(item, index, messages, chatting),
      animation: streaming ? 'gradient' : undefined,
      plainText: item.content || '',
      copyText,
      displayName: id.displayName,
      sentTimeLabel,
      sentAt,
      avatarText: id.avatarText,
      avatarUrl: isUser ? user.avatarUrl : undefined,
      avatarColor: id.avatarColor,
      userImages,
      userText: parts?.text,
      citations,
      reasoning,
      thinkingActive,
      thinkingLabel: '正在思考中',
      toolInvokeActive,
      toolInvokeLabel: runningTool ? `正在调用 ${runningTool.label}` : undefined,
      enabledPlugins,
      toolRuns: !isUser ? toolRuns : undefined,
      replyQuote: isUser ? undefined : findConversationReplyQuote(messages, index, user.displayName),
      showActions: !thinkingActive && hasBody,
      actionBar: isUser
        ? buildUserActionBar(enableFullActions)
        : buildAssistantActionBar(showReplay, enableFullActions),
      showDelete: enableDelete,
    }
  })
}

export interface LocalChatListMessage extends LocalChatMessage {
  citations?: KnowledgeCitation[]
  createdAt?: string
}

export function buildLocalListItems(
  messages: LocalChatListMessage[],
  chatting: boolean,
  options: BoxChatListActionsOptions = {},
  identity: BoxChatIdentityContext,
): BoxChatListItem[] {
  const { enableRegenerate = false, enableDelete = false, enableFullActions = false } = options
  const user = userIdentity(identity)
  const agent = agentIdentity(identity)

  return messages.map((item, index) => {
    const thinkingActive = isLocalLoadingBubble(item, index, messages, chatting)
    const streaming =
      chatting &&
      index === messages.length - 1 &&
      item.role === 'assistant' &&
      Boolean(item.content?.trim())
    const isUser = item.role === 'user'
    const id = isUser ? user : agent
    const hasBody = messageHasBody(isUser, item.content || '', isUser ? item.content : undefined, 0)
    const sentAt = item.createdAt || undefined
    const sentTimeLabel = formatChatMessageTime(sentAt) || undefined
    const copyText = item.content || ''

    return {
      key: index,
      uiRole: toLocalChatUiRole(item.role),
      uiContent: toLocalChatUiContent(item, index, messages, chatting),
      uiStatus: thinkingActive
        ? 'complete'
        : streaming
          ? 'streaming'
          : toLocalChatUiStatus(item, index, messages, chatting),
      animation: streaming ? 'gradient' : undefined,
      plainText: item.content || '',
      copyText,
      displayName: id.displayName,
      sentTimeLabel,
      sentAt,
      avatarText: id.avatarText,
      avatarUrl: isUser ? user.avatarUrl : undefined,
      avatarColor: id.avatarColor,
      userImages: [],
      userText: isUser ? item.content : undefined,
      citations: item.citations || [],
      reasoning: undefined,
      thinkingActive,
      replyQuote: isUser ? undefined : findLocalReplyQuote(messages, index, user.displayName),
      showActions: !thinkingActive && hasBody,
      actionBar: isUser
        ? buildUserActionBar(enableFullActions)
        : buildAssistantActionBar(
            enableRegenerate && item.role === 'assistant' && index === messages.length - 1 && !chatting,
            enableFullActions,
          ),
      showDelete: enableDelete,
    }
  })
}

export function buildSharedListItems(data: SharedConversationVO): BoxChatListItem[] {
  const localMessages: LocalChatListMessage[] = data.messages.map((message) => ({
    role: message.role === 'USER' ? 'user' : 'assistant',
    content: message.content,
  }))
  return buildLocalListItems(
    localMessages,
    false,
    { enableRegenerate: false, enableDelete: false, enableFullActions: false },
    { userName: data.userName, agentName: data.agentName },
  )
}
