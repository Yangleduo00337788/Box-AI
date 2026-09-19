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
import { getAvatarColor } from '@/utils/format'
import { parseMessageReasoning } from '@/utils/messageMetadata'
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
  displayName: string
  avatarUrl?: string
  avatarText: string
  avatarColor?: string
  userImages: ChatImagePart[]
  userText?: string
  citations: KnowledgeCitation[]
  reasoning?: string
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

function buildActionBar(showReplay: boolean, full: boolean): BoxChatActionBarItem[] {
  if (!full) {
    return ['copy']
  }
  const bar: BoxChatActionBarItem[] = ['copy']
  if (showReplay) bar.push('replay')
  bar.push('good', 'bad', 'share')
  return bar
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

export function buildConversationListItems(
  messages: MessageVO[],
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
    const loading =
      chatting && index === messages.length - 1 && item.role === 'ASSISTANT' && !item.content
    const citations = item.citations?.length ? item.citations : parseMessageCitations(item.metadataJson)
    const reasoning = parseMessageReasoning(item.metadataJson)
    const isUser = item.role === 'USER'
    const parts = isUser ? parseUserContent(item.content || '') : null
    const showReplay =
      enableRegenerate &&
      item.role === 'ASSISTANT' &&
      index === messages.length - 1 &&
      !chatting
    const id = isUser ? user : agent

    return {
      key: item.id ?? index,
      messageId: item.id > 0 ? item.id : undefined,
      uiRole: toChatUiRole(item.role),
      uiContent: toChatUiContent(item, index, messages, chatting),
      uiStatus: toChatUiStatus(item, index, messages, chatting),
      animation: loading ? 'gradient' : undefined,
      plainText: item.content || '',
      displayName: id.displayName,
      avatarText: id.avatarText,
      avatarUrl: id.avatarUrl,
      avatarColor: id.avatarColor,
      userImages: isUser ? parseUserContent(item.content || '').images : [],
      userText: parts?.text,
      citations,
      reasoning,
      replyQuote: isUser ? undefined : findConversationReplyQuote(messages, index, user.displayName),
      showActions: !isUser && !loading && Boolean(item.content?.trim()),
      actionBar: buildActionBar(showReplay, enableFullActions),
      showDelete: enableDelete,
    }
  })
}

export interface LocalChatListMessage extends LocalChatMessage {
  citations?: KnowledgeCitation[]
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
    const loading = isLocalLoadingBubble(item, index, messages, chatting)
    const isUser = item.role === 'user'
    const id = isUser ? user : agent

    return {
      key: index,
      uiRole: toLocalChatUiRole(item.role),
      uiContent: toLocalChatUiContent(item, index, messages, chatting),
      uiStatus: toLocalChatUiStatus(item, index, messages, chatting),
      animation: loading ? 'gradient' : undefined,
      plainText: item.content || '',
      displayName: id.displayName,
      avatarText: id.avatarText,
      avatarUrl: isUser ? id.avatarUrl : undefined,
      avatarColor: id.avatarColor,
      userImages: [],
      userText: isUser ? item.content : undefined,
      citations: item.citations || [],
      reasoning: undefined,
      replyQuote: isUser ? undefined : findLocalReplyQuote(messages, index, user.displayName),
      showActions:
        !isUser && !loading && Boolean(item.content?.trim()),
      actionBar: buildActionBar(
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
