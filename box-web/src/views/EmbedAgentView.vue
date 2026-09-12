<template>
  <div class="embed-page">
    <header class="embed-header">
      <h1>{{ title }}</h1>
      <p v-if="!apiKey" class="embed-hint">请在 URL 中提供 <code>apiKey</code> 参数</p>
    </header>

    <div ref="messageListRef" class="embed-messages">
      <div v-if="!messages.length" class="embed-empty">开始与智能体对话</div>
      <div
        v-for="(item, index) in messages"
        :key="index"
        class="embed-message"
        :class="`embed-message--${item.role}`"
      >
        {{ item.content }}
      </div>
      <div v-if="loading" class="embed-message embed-message--assistant">正在生成…</div>
    </div>

    <form class="embed-input" @submit.prevent="send">
      <t-input
        v-model="input"
        placeholder="输入消息…"
        :disabled="loading || !apiKey"
        size="large"
      />
      <t-button theme="primary" type="submit" :loading="loading" :disabled="!apiKey">发送</t-button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const agentId = computed(() => Number(route.params.id))
const apiKey = computed(() => String(route.query.apiKey || ''))
const title = computed(() => String(route.query.title || 'Box Agent'))
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const messageListRef = ref<HTMLElement | null>(null)

async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function send() {
  const text = input.value.trim()
  if (!text || !apiKey.value || loading.value) {
    return
  }
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()
  try {
    const response = await fetch(`/api/v1/published/agents/${agentId.value}/chat`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey.value}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message: text, stream: false }),
    })
    const payload = await response.json()
    if (!response.ok || payload.code !== 0) {
      throw new Error(payload.message || '请求失败')
    }
    messages.value.push({ role: 'assistant', content: payload.data?.content || '' })
  } catch (error) {
    MessagePlugin.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

onMounted(() => {
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
  background: var(--td-brand-color, #0052d9);
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
</style>
