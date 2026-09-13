<template>
  <div class="embed-page" :style="pageStyle">
    <header class="embed-header">
      <div class="embed-header__brand">
        <img v-if="embedConfig.logoUrl" :src="embedConfig.logoUrl" alt="" class="embed-logo" />
        <h1>{{ displayTitle }}</h1>
      </div>
      <p v-if="!apiKey" class="embed-hint">请在 URL 中提供 <code>apiKey</code> 参数</p>
    </header>

    <div ref="messageListRef" class="embed-messages">
      <div v-if="!messages.length" class="embed-empty">
        <p v-if="embedConfig.welcomeMessage" class="embed-welcome">{{ embedConfig.welcomeMessage }}</p>
        <p v-else>开始与智能体对话</p>
        <div v-if="embedConfig.suggestedQuestions?.length" class="embed-suggestions">
          <button
            v-for="question in embedConfig.suggestedQuestions"
            :key="question"
            type="button"
            class="embed-suggestion"
            :disabled="loading || !apiKey || !agentId"
            @click="askSuggestion(question)"
          >
            {{ question }}
          </button>
        </div>
      </div>
      <div
        v-for="(item, index) in messages"
        :key="index"
        class="embed-message"
        :class="`embed-message--${item.role}`"
      >
        {{ item.content }}
      </div>
    </div>

    <form class="embed-input" @submit.prevent="send">
      <t-input
        v-model="input"
        placeholder="输入消息…"
        :disabled="loading || !apiKey || !agentId"
        size="large"
      />
      <t-button theme="primary" type="submit" :loading="loading" :disabled="!apiKey || !agentId">发送</t-button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  chatPublishedAgentStream,
  getPublishedAgentEmbedConfig,
  type AgentEmbedConfigVO,
} from '@/api/agent'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const agentId = ref<number>(Number(route.params.id) || 0)
const apiKey = computed(() => String(route.query.apiKey || ''))
const queryTitle = computed(() => String(route.query.title || ''))
const embedConfig = ref<AgentEmbedConfigVO>({
  themeColor: '#0052d9',
  suggestedQuestions: [],
})
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

const displayTitle = computed(
  () => queryTitle.value || embedConfig.value.agentName || 'Box Agent',
)
const pageStyle = computed(() => ({
  '--embed-theme-color': embedConfig.value.themeColor || '#0052d9',
}))

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function applyEmbed(data?: AgentEmbedConfigVO | null) {
  if (!data) {
    return
  }
  embedConfig.value = {
    themeColor: data.themeColor || '#0052d9',
    logoUrl: data.logoUrl,
    welcomeMessage: data.welcomeMessage,
    suggestedQuestions: data.suggestedQuestions || [],
    agentName: data.agentName,
  }
}

async function sendMessage(text: string) {
  if (!text || !apiKey.value || !agentId.value || loading.value) {
    return
  }
  messages.value.push({ role: 'user', content: text })
  const assistant: ChatMessage = { role: 'assistant', content: '' }
  messages.value.push(assistant)
  loading.value = true
  await scrollToBottom()
  try {
    await chatPublishedAgentStream(agentId.value, apiKey.value, text, async (chunk) => {
      assistant.content += chunk
      await scrollToBottom()
    })
  } catch (error) {
    if (!assistant.content) {
      messages.value.pop()
    }
    MessagePlugin.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

async function send() {
  const text = input.value.trim()
  if (!text) {
    return
  }
  input.value = ''
  await sendMessage(text)
}

async function askSuggestion(question: string) {
  await sendMessage(question)
}

async function loadEmbedConfig() {
  if (!agentId.value) {
    try {
      const response = await fetch(`/api/v1/published/embed/resolve?host=${encodeURIComponent(window.location.hostname)}`)
      const payload = await response.json()
      if (payload?.code === 0 && payload.data?.agentId) {
        agentId.value = Number(payload.data.agentId)
        applyEmbed(payload.data.embed)
        return
      }
      MessagePlugin.warning('无法根据当前域名解析嵌入对话')
      return
    } catch {
      MessagePlugin.warning('无法根据当前域名解析嵌入对话')
      return
    }
  }
  try {
    const { data } = await getPublishedAgentEmbedConfig(agentId.value)
    applyEmbed(data.data)
  } catch {
    // keep defaults when agent is not published or config unavailable
  }
}

onMounted(async () => {
  await loadEmbedConfig()
  if (!apiKey.value) {
    MessagePlugin.warning('缺少 apiKey 参数，无法调用开放 API')
  }
})
</script>

<style scoped>
.embed-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--box-surface, #fff);
}

.embed-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--box-border, #e7e7e7);
}

.embed-header__brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.embed-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  object-fit: cover;
}

.embed-header h1 {
  margin: 0;
  font: var(--td-font-title-medium);
}

.embed-hint {
  margin: 8px 0 0;
  color: var(--box-muted, #666);
  font-size: 13px;
}

.embed-messages {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}

.embed-empty {
  color: var(--box-muted, #666);
  text-align: center;
  padding: 48px 0;
}

.embed-welcome {
  margin: 0 0 16px;
  font-size: 15px;
  color: var(--box-ink, #222);
}

.embed-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 640px;
  margin: 0 auto;
}

.embed-suggestion {
  border: 1px solid color-mix(in srgb, var(--embed-theme-color, #0052d9) 35%, #fff);
  background: color-mix(in srgb, var(--embed-theme-color, #0052d9) 8%, #fff);
  color: var(--box-ink, #222);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
}

.embed-suggestion:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.embed-message {
  max-width: 80%;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.embed-message--user {
  margin-left: auto;
  background: var(--embed-theme-color, var(--td-brand-color, #0052d9));
  color: #fff;
}

.embed-message--assistant {
  background: #f3f4f6;
  color: var(--box-ink, #222);
}

.embed-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--box-border, #e7e7e7);
}

.embed-input :deep(.t-button--theme-primary) {
  background-color: var(--embed-theme-color, var(--td-brand-color, #0052d9));
  border-color: var(--embed-theme-color, var(--td-brand-color, #0052d9));
}
</style>
