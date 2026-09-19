import type { ComponentResolver } from 'unplugin-vue-components'

const CHAT_EXPORTS = new Set([
  'Attachments',
  'Chat', // alias of ChatList in @tdesign-vue-next/chat 0.7+
  'ChatActionbar',
  'ChatContent',
  'ChatInput',
  'ChatItem',
  'ChatList',
  'ChatLoading',
  'ChatMarkdown',
  'ChatMessage',
  'ChatReasoning',
  'ChatSender',
  'ChatThinking',
  'Chatbot',
])

/** 将 TChat* / TAttachments 解析到 @tdesign-vue-next/chat，避免误从 tdesign-vue-next 导入 */
export function TDesignChatResolver(): ComponentResolver {
  return {
    type: 'component',
    resolve: (name: string) => {
      if (name === 'TAttachments') {
        return { name: 'Attachments', from: '@tdesign-vue-next/chat' }
      }
      if (!name.startsWith('TChat')) {
        return
      }
      const componentName = name.slice(1)
      if (!CHAT_EXPORTS.has(componentName)) {
        return
      }
      return {
        name: componentName,
        from: '@tdesign-vue-next/chat',
      }
    },
  }
}
