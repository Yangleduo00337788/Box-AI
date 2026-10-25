<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { screenshotImage } from '@/utils/assets'

const agent = ref('客服助手 · 已发布')
const input = ref('')
const streaming = ref(false)

type Msg = { role: 'user' | 'assistant'; text: string; citations?: string[] }
const messages = ref<Msg[]>([
  {
    role: 'assistant',
    text: '你好，我是 Box 演示助手。本对话为前端模拟，展示流式回复与知识库引用样式。',
  },
])

const demoReply =
  '根据《产品 FAQ》与工单知识库：企业版支持多工作空间 RBAC、审计日志与自定义模型池。如需试用完整后端，请按仓库 README 私有化部署。'

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  streaming.value = true
  const assistant: Msg = { role: 'assistant', text: '', citations: ['产品 FAQ.pdf · 第 2 页', '工单知识库 · 条目 #128'] }
  messages.value.push(assistant)
  const idx = messages.value.length - 1
  for (let i = 0; i < demoReply.length; i++) {
    await new Promise((r) => setTimeout(r, 18))
    messages.value[idx] = { ...assistant, text: demoReply.slice(0, i + 1) }
  }
  streaming.value = false
}

const workspaceShot = screenshotImage('chat-workspace.png')

onMounted(() => {
  document.title = '对话工作台 · Box AI 演示'
})
</script>

<template>
  <div class="demo-page chat-demo">
    <header class="demo-page__header">
      <h1>对话工作台</h1>
      <p>模拟 SSE 流式输出、已发布 Agent 与 citations 引用侧栏。</p>
    </header>

    <t-row :gutter="16">
      <t-col :xs="12" :lg="7">
        <t-card title="演示对话" class="chat-panel">
          <div class="chat-toolbar">
            <t-select v-model="agent" :options="[{ label: agent, value: agent }]" style="width: 220px" />
            <t-tag theme="success" variant="light" size="small">Published</t-tag>
          </div>
          <div class="chat-messages">
            <div v-for="(m, i) in messages" :key="i" :class="['chat-bubble', m.role]">
              <div class="chat-bubble__text">{{ m.text }}</div>
              <div v-if="m.citations?.length" class="chat-citations">
                <span class="label">引用</span>
                <t-tag v-for="c in m.citations" :key="c" size="small" variant="outline">{{ c }}</t-tag>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <t-textarea v-model="input" placeholder="输入问题，体验模拟流式回复…" :autosize="{ minRows: 2, maxRows: 4 }" />
            <t-button theme="primary" :loading="streaming" @click="send">发送</t-button>
          </div>
        </t-card>
      </t-col>
      <t-col :xs="12" :lg="5">
        <p class="shot-label">产品真实界面参考</p>
        <div class="demo-shot">
          <img :src="workspaceShot" alt="对话工作台" loading="lazy" />
        </div>
      </t-col>
    </t-row>
  </div>
</template>

<style scoped>
.chat-panel {
  min-height: 420px;
  display: flex;
  flex-direction: column;
}

.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-messages {
  flex: 1;
  min-height: 200px;
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.chat-bubble {
  max-width: 92%;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.chat-bubble.user {
  align-self: flex-end;
  background: var(--td-brand-color-10);
  color: #fff;
}

.chat-bubble.assistant {
  align-self: flex-start;
  background: var(--td-bg-color-secondarycontainer);
}

.chat-citations {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.chat-citations .label {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.chat-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.shot-label {
  font-size: 13px;
  color: var(--td-text-color-secondary);
  margin: 0 0 8px;
}
</style>
