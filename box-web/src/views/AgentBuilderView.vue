<template>
  <div class="builder">
    <header class="builder-header">
      <div class="builder-header__left">
        <t-button variant="text" shape="square" @click="router.push('/agents')">
          <template #icon><t-icon name="chevron-left" /></template>
        </t-button>
        <div class="builder-header__meta">
          <h1 class="builder-header__title">{{ agent?.name || '加载中…' }}</h1>
          <p class="builder-header__desc">Agent Builder · 配置与调试</p>
        </div>
        <t-tag v-if="agent" :theme="agent.status === 'PUBLISHED' ? 'success' : 'default'" variant="light" size="small">
          {{ statusLabel(agent.status) }}
        </t-tag>
      </div>
      <t-tag v-if="displayModelName" variant="outline" size="small">{{ displayModelName }}</t-tag>
    </header>

    <t-loading :loading="loading" size="small" class="builder-body">
      <div v-if="agent" class="builder-layout">
        <nav class="builder-nav">
          <button
            v-for="item in navItems"
            :key="item.value"
            type="button"
            class="builder-nav__item"
            :class="{ 'builder-nav__item--active': activeTab === item.value }"
            @click="activeTab = item.value"
          >
            <t-icon :name="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <section class="builder-config">
          <div v-if="activeTab === 'overview'" class="config-panel">
            <h2 class="config-panel__title">概览</h2>
            <p class="config-panel__desc">基础信息与可见性</p>
            <t-form :data="overviewForm" :rules="overviewRules" label-align="top" @submit="saveOverview">
              <t-form-item label="名称" name="name">
                <t-input v-model="overviewForm.name" maxlength="128" />
              </t-form-item>
              <t-form-item label="描述" name="description">
                <t-textarea
                  v-model="overviewForm.description"
                  :autosize="{ minRows: 3, maxRows: 6 }"
                  maxlength="500"
                />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" type="submit" :loading="savingOverview">保存</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'prompt'" class="config-panel">
            <h2 class="config-panel__title">Prompt</h2>
            <p class="config-panel__desc">定义智能体的系统提示词与角色设定</p>
            <t-form label-align="top">
              <t-form-item label="System Prompt">
                <t-textarea
                  v-model="promptForm.systemPrompt"
                  placeholder="例如：你是一位专业的助手，擅长…"
                  :autosize="{ minRows: 12, maxRows: 24 }"
                />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" :loading="savingPrompt" @click="savePrompt">保存 Prompt</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'model'" class="config-panel">
            <h2 class="config-panel__title">模型</h2>
            <p class="config-panel__desc">对话模型与生成参数</p>
            <t-form :data="modelForm" :rules="modelRules" label-align="top" @submit="saveModel">
              <t-form-item label="模型来源" name="modelSource">
                <t-radio-group v-model="modelForm.modelSource">
                  <t-radio value="PLATFORM">平台模型（扣配额）</t-radio>
                  <t-radio v-if="byokEnabled" value="BYOK">自带密钥（BYOK）</t-radio>
                </t-radio-group>
              </t-form-item>
              <t-form-item
                v-if="modelForm.modelSource === 'PLATFORM'"
                label="平台模型"
                name="platformModelId"
              >
                <t-select
                  v-model="modelForm.platformModelId"
                  :options="platformModelOptions"
                  placeholder="请选择平台模型"
                />
              </t-form-item>
              <t-form-item v-else label="自带模型" name="modelId">
                <t-select v-model="modelForm.modelId" :options="byokModelOptions" placeholder="请选择模型" />
              </t-form-item>
              <t-form-item label="Temperature">
                <t-slider v-model="modelForm.temperature" :min="0" :max="2" :step="0.1" />
              </t-form-item>
              <t-form-item label="Top P">
                <t-slider v-model="modelForm.topP" :min="0" :max="1" :step="0.05" />
              </t-form-item>
              <t-form-item label="Max Tokens" name="maxTokens">
                <t-input-number v-model="modelForm.maxTokens" :min="1" :max="128000" theme="column" />
              </t-form-item>
              <t-form-item label="流式输出">
                <t-switch v-model="modelForm.streamEnabled" />
              </t-form-item>
              <t-form-item>
                <t-button theme="primary" type="submit" :loading="savingModel">保存模型配置</t-button>
              </t-form-item>
            </t-form>
          </div>

          <div v-else-if="activeTab === 'knowledge'" class="config-panel">
            <h2 class="config-panel__title">知识库</h2>
            <p class="config-panel__desc">绑定知识库后，对话将自动 RAG 检索</p>
            <t-select
              v-model="selectedKnowledgeId"
              :options="knowledgeOptions"
              placeholder="选择要绑定的知识库"
              clearable
              style="margin-bottom: 12px"
            />
            <t-button theme="primary" :loading="bindingKnowledge" @click="bindKnowledge">绑定知识库</t-button>
            <t-table row-key="id" :data="knowledgeBindings" :columns="knowledgeColumns" size="small" style="margin-top: 16px">
              <template #op="{ row }">
                <t-button variant="text" theme="danger" @click="unbindKnowledge(row.knowledgeBaseId)">解除</t-button>
              </template>
            </t-table>
          </div>

          <div v-else-if="activeTab === 'tools'" class="config-panel">
            <h2 class="config-panel__title">工具</h2>
            <p class="config-panel__desc">绑定 HTTP 工具后，Agent 支持 Tool Calling</p>
            <t-select
              v-model="selectedToolId"
              :options="toolOptions"
              placeholder="选择要绑定的工具"
              clearable
              style="margin-bottom: 12px"
            />
            <t-button theme="primary" :loading="bindingTool" @click="bindTool">绑定工具</t-button>
            <t-table row-key="id" :data="toolBindings" :columns="toolColumns" size="small" style="margin-top: 16px">
              <template #op="{ row }">
                <t-button variant="text" theme="danger" @click="unbindTool(row.toolId)">解除</t-button>
              </template>
            </t-table>
          </div>

          <div v-else-if="activeTab === 'publish'" class="config-panel">
            <h2 class="config-panel__title">发布</h2>
            <p class="config-panel__desc">发布后可被对话与开放 API 调用</p>
            <t-descriptions :column="1" bordered>
              <t-descriptions-item label="状态">{{ statusLabel(agent.status) }}</t-descriptions-item>
              <t-descriptions-item label="发布版本">v{{ publishInfo?.publishedVersionNo || agent.publishedVersion || '—' }}</t-descriptions-item>
              <t-descriptions-item label="发布时间">{{ publishInfo?.publishedAt || '—' }}</t-descriptions-item>
            </t-descriptions>
            <t-space style="margin-top: 16px">
              <t-button theme="primary" :loading="publishing" @click="doPublish">发布</t-button>
              <t-button variant="outline" :loading="publishing" @click="doUnpublish">取消发布</t-button>
            </t-space>
          </div>
        </section>

        <aside class="builder-preview">
          <div class="preview-header">
            <h3>调试预览</h3>
            <t-button variant="text" size="small" @click="clearChat">清空</t-button>
          </div>
          <div ref="chatListRef" class="preview-messages">
            <div v-if="!messages.length" class="preview-empty">
              发送消息测试当前 Prompt 与模型配置
            </div>
            <div
              v-for="(item, index) in messages"
              :key="index"
              class="preview-message"
              :class="`preview-message--${item.role}`"
            >
              <span class="preview-message__role">{{ item.role === 'user' ? '你' : '智能体' }}</span>
              <div
                class="preview-message__content"
                :class="{
                  'preview-message__content--loading':
                    chatting && index === messages.length - 1 && item.role === 'assistant' && !item.content,
                }"
              >
                {{ item.content || (chatting && item.role === 'assistant' ? '思考中…' : '') }}
              </div>
            </div>
          </div>
          <div class="preview-input">
            <t-textarea
              v-model="chatInput"
              placeholder="输入测试消息，Enter 发送"
              :autosize="{ minRows: 2, maxRows: 4 }"
              :disabled="chatting"
              @keydown="onChatKeydown"
            />
            <t-button theme="primary" :loading="chatting" :disabled="chatting || !chatInput.trim()" @click="sendChat">
              发送
            </t-button>
          </div>
        </aside>
      </div>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import {
  bindAgentKnowledge,
  bindAgentTool,
  chatAgent,
  chatAgentStream,
  getAgent,
  getAgentPublishStatus,
  listAgentKnowledge,
  listAgentTools,
  publishAgent,
  unbindAgentKnowledge,
  unbindAgentTool,
  unpublishAgent,
  updateAgent,
  updateAgentModel,
  updateAgentPrompt,
  type AgentKnowledgeBindingVO,
  type AgentPublishVO,
  type AgentToolBindingVO,
  type AgentVO,
} from '@/api/agent'
import { listKnowledgeBases, type KnowledgeBaseVO } from '@/api/knowledge'
import { listTools, type ToolVO } from '@/api/tool'
import { listModels, type ModelVO } from '@/api/model'
import {
  fetchPlatformCapabilities,
  listPlatformModels,
  type PlatformModelVO,
} from '@/api/platform'
import type { ModelSource } from '@/api/agent'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const router = useRouter()
const agentId = computed(() => Number(route.params.id))

const loading = ref(true)
const agent = ref<AgentVO | null>(null)
const models = ref<ModelVO[]>([])
const platformModels = ref<PlatformModelVO[]>([])
const byokEnabled = ref(false)
const activeTab = ref('overview')

const savingOverview = ref(false)
const savingPrompt = ref(false)
const savingModel = ref(false)
const chatting = ref(false)
const chatInput = ref('')
const messages = ref<ChatMessage[]>([])
const chatListRef = ref<HTMLElement | null>(null)

const navItems = [
  { value: 'overview', label: '概览', icon: 'home' },
  { value: 'prompt', label: 'Prompt', icon: 'edit' },
  { value: 'model', label: '模型', icon: 'cpu' },
  { value: 'knowledge', label: '知识库', icon: 'book' },
  { value: 'tools', label: '工具', icon: 'tools' },
  { value: 'publish', label: '发布', icon: 'upload' },
]

const knowledgeBases = ref<KnowledgeBaseVO[]>([])
const tools = ref<ToolVO[]>([])
const knowledgeBindings = ref<AgentKnowledgeBindingVO[]>([])
const toolBindings = ref<AgentToolBindingVO[]>([])
const publishInfo = ref<AgentPublishVO | null>(null)
const selectedKnowledgeId = ref<number | undefined>()
const selectedToolId = ref<number | undefined>()
const bindingKnowledge = ref(false)
const bindingTool = ref(false)
const publishing = ref(false)

const knowledgeOptions = computed(() =>
  knowledgeBases.value.map((item) => ({ label: item.name, value: item.id })),
)
const toolOptions = computed(() => tools.value.map((item) => ({ label: `${item.name} (${item.toolKey})`, value: item.id })))
const knowledgeColumns = [
  { colKey: 'knowledgeBaseId', title: '知识库 ID' },
  { colKey: 'topK', title: 'Top K', width: 80 },
  { colKey: 'op', title: '操作', width: 100 },
]
const toolColumns = [
  { colKey: 'toolId', title: '工具 ID' },
  { colKey: 'enabled', title: '启用', width: 80 },
  { colKey: 'op', title: '操作', width: 100 },
]

const overviewForm = reactive({ name: '', description: '' })
const promptForm = reactive({ systemPrompt: '' })
const modelForm = reactive({
  modelSource: 'PLATFORM' as ModelSource,
  platformModelId: undefined as number | undefined,
  modelId: undefined as number | undefined,
  temperature: 0.7,
  topP: 1,
  maxTokens: 4096,
  streamEnabled: true,
})

const overviewRules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入名称' }],
}

const modelRules: FormProps['rules'] = {
  modelSource: [{ required: true, message: '请选择模型来源' }],
  maxTokens: [{ required: true, message: '请输入 Max Tokens' }],
}

const platformModelOptions = computed(() =>
  platformModels.value
    .filter((item) => item.status === 1)
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

const byokModelOptions = computed(() =>
  models.value
    .filter((item) => item.status === 1 && item.modelType === 'CHAT')
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

const displayModelName = computed(() => {
  if (!agent.value) return ''
  if (agent.value.modelSource === 'BYOK') {
    return agent.value.modelName ? `${agent.value.modelName}（BYOK）` : '自带密钥'
  }
  return agent.value.platformModelName || agent.value.modelName || ''
})

function statusLabel(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'ARCHIVED') return '已归档'
  return '草稿'
}

function applyAgent(data: AgentVO) {
  agent.value = data
  overviewForm.name = data.name
  overviewForm.description = data.description || ''
  promptForm.systemPrompt = data.systemPrompt || ''
  modelForm.modelSource = data.modelSource === 'BYOK' ? 'BYOK' : 'PLATFORM'
  modelForm.platformModelId = data.platformModelId
  modelForm.modelId = data.modelId
  modelForm.temperature = data.temperature ?? 0.7
  modelForm.topP = data.topP ?? 1
  modelForm.maxTokens = data.maxTokens ?? 4096
  modelForm.streamEnabled = data.streamEnabled ?? true
}

async function loadAgent() {
  loading.value = true
  try {
    const [
      { data: agentRes },
      { data: modelRes },
      { data: platformRes },
      { data: capRes },
      { data: kbRes },
      { data: toolRes },
      { data: bindKbRes },
      { data: bindToolRes },
      { data: publishRes },
    ] = await Promise.all([
      getAgent(agentId.value),
      listModels(),
      listPlatformModels(),
      fetchPlatformCapabilities(),
      listKnowledgeBases(),
      listTools(),
      listAgentKnowledge(agentId.value),
      listAgentTools(agentId.value),
      getAgentPublishStatus(agentId.value),
    ])
    models.value = modelRes.data || []
    platformModels.value = platformRes.data || []
    byokEnabled.value = capRes.data?.byokEnabled === true
    knowledgeBases.value = kbRes.data || []
    tools.value = toolRes.data || []
    knowledgeBindings.value = bindKbRes.data || []
    toolBindings.value = bindToolRes.data || []
    publishInfo.value = publishRes.data || null
    if (agentRes.data) {
      applyAgent(agentRes.data)
    }
  } finally {
    loading.value = false
  }
}

async function bindKnowledge() {
  if (!selectedKnowledgeId.value) return
  bindingKnowledge.value = true
  try {
    await bindAgentKnowledge(agentId.value, { knowledgeBaseId: selectedKnowledgeId.value, topK: 5 })
    const { data } = await listAgentKnowledge(agentId.value)
    knowledgeBindings.value = data.data || []
    MessagePlugin.success('知识库已绑定')
  } finally {
    bindingKnowledge.value = false
  }
}

async function unbindKnowledge(knowledgeBaseId: number) {
  await unbindAgentKnowledge(agentId.value, knowledgeBaseId)
  knowledgeBindings.value = knowledgeBindings.value.filter((item) => item.knowledgeBaseId !== knowledgeBaseId)
}

async function bindTool() {
  if (!selectedToolId.value) return
  bindingTool.value = true
  try {
    await bindAgentTool(agentId.value, { toolId: selectedToolId.value, enabled: true })
    const { data } = await listAgentTools(agentId.value)
    toolBindings.value = data.data || []
    MessagePlugin.success('工具已绑定')
  } finally {
    bindingTool.value = false
  }
}

async function unbindTool(toolId: number) {
  await unbindAgentTool(agentId.value, toolId)
  toolBindings.value = toolBindings.value.filter((item) => item.toolId !== toolId)
}

async function doPublish() {
  publishing.value = true
  try {
    const { data } = await publishAgent(agentId.value)
    publishInfo.value = data.data || null
    if (agent.value && data.data) {
      agent.value.status = data.data.status
    }
    MessagePlugin.success('发布成功')
  } finally {
    publishing.value = false
  }
}

async function doUnpublish() {
  publishing.value = true
  try {
    const { data } = await unpublishAgent(agentId.value)
    publishInfo.value = data.data || null
    if (agent.value && data.data) {
      agent.value.status = data.data.status
    }
    MessagePlugin.success('已取消发布')
  } finally {
    publishing.value = false
  }
}

const saveOverview: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true || !agent.value) return
  savingOverview.value = true
  try {
    const { data } = await updateAgent(agentId.value, {
      name: overviewForm.name.trim(),
      description: overviewForm.description.trim() || undefined,
      modelId: agent.value.modelId,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('基础信息已保存')
  } finally {
    savingOverview.value = false
  }
}

async function savePrompt() {
  if (!agent.value) return
  savingPrompt.value = true
  try {
    const { data } = await updateAgentPrompt(agentId.value, {
      systemPrompt: promptForm.systemPrompt.trim() || undefined,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('Prompt 已保存')
  } finally {
    savingPrompt.value = false
  }
}

const saveModel: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  if (modelForm.modelSource === 'PLATFORM' && !modelForm.platformModelId) return
  if (modelForm.modelSource === 'BYOK' && !modelForm.modelId) return
  savingModel.value = true
  try {
    const { data } = await updateAgentModel(agentId.value, {
      modelSource: modelForm.modelSource,
      platformModelId: modelForm.modelSource === 'PLATFORM' ? modelForm.platformModelId : undefined,
      modelId: modelForm.modelSource === 'BYOK' ? modelForm.modelId : undefined,
      temperature: modelForm.temperature,
      topP: modelForm.topP,
      maxTokens: modelForm.maxTokens,
      streamEnabled: modelForm.streamEnabled,
    })
    if (data.data) applyAgent(data.data)
    MessagePlugin.success('模型配置已保存')
  } finally {
    savingModel.value = false
  }
}

function clearChat() {
  messages.value = []
}

async function scrollChatToBottom() {
  await nextTick()
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

async function sendChat() {
  const text = chatInput.value.trim()
  if (!text || chatting.value) return
  messages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  await scrollChatToBottom()
  chatting.value = true
  const useStream = modelForm.streamEnabled !== false
  let assistantIndex = -1
  try {
    if (useStream) {
      assistantIndex = messages.value.length
      messages.value.push({ role: 'assistant', content: '' })
      await scrollChatToBottom()
      await chatAgentStream(agentId.value, text, (delta) => {
        messages.value[assistantIndex].content += delta
        scrollChatToBottom()
      })
      if (!messages.value[assistantIndex].content) {
        messages.value[assistantIndex].content = '（无回复）'
      }
    } else {
      const { data } = await chatAgent(agentId.value, text)
      messages.value.push({ role: 'assistant', content: data.data?.content || '（无回复）' })
      await scrollChatToBottom()
    }
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '对话失败'))
    if (useStream && assistantIndex >= 0 && !messages.value[assistantIndex]?.content) {
      messages.value.splice(assistantIndex, 1)
    }
  } finally {
    chatting.value = false
  }
}

function onChatKeydown(_value: string, context: { e: KeyboardEvent }) {
  if (context.e.key === 'Enter' && !context.e.shiftKey && !chatting.value) {
    context.e.preventDefault()
    sendChat()
  }
}

watch(
  () => route.params.id,
  () => {
    if (route.name === 'agent-builder') {
      loadAgent()
    }
  },
)

onMounted(loadAgent)
</script>

<style scoped>
.builder {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 16px 20px;
  box-sizing: border-box;
  background: #fff;
}

.builder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--box-border);
  flex-shrink: 0;
}

.builder-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.builder-header__meta {
  min-width: 0;
}

.builder-header__title {
  margin: 0;
  font: var(--td-font-title-medium);
  color: var(--box-ink);
}

.builder-header__desc {
  margin: 2px 0 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.builder-body {
  flex: 1;
  min-height: 0;
}

.builder-layout {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) 360px;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.builder-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
}

.builder-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
  font: var(--td-font-body-medium);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.builder-nav__item:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.builder-nav__item--active {
  background: var(--box-active);
  color: var(--box-ink);
  font-weight: 500;
}

.builder-config,
.builder-preview {
  min-height: 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
}

.builder-config {
  overflow-y: auto;
  padding: 24px;
}

.config-panel__title {
  margin: 0 0 4px;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.config-panel__desc {
  margin: 0 0 20px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.builder-preview {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--box-border);
  flex-shrink: 0;
}

.preview-header h3 {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.preview-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-empty {
  margin: auto;
  max-width: 220px;
  text-align: center;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.preview-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 92%;
}

.preview-message--user {
  align-self: flex-end;
  align-items: flex-end;
}

.preview-message--assistant {
  align-self: flex-start;
}

.preview-message__role {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.preview-message__content {
  padding: 10px 12px;
  border-radius: 12px;
  font: var(--td-font-body-medium);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-message--user .preview-message__content {
  background: var(--td-gray-color-2);
  color: var(--box-ink);
}

.preview-message--assistant .preview-message__content {
  background: var(--td-gray-color-1);
  color: var(--box-ink);
}

.preview-message__content--loading {
  color: var(--box-muted);
}

.preview-input {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 16px 16px;
  border-top: 1px solid var(--box-border);
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .builder-layout {
    grid-template-columns: 180px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) 320px;
  }

  .builder-preview {
    grid-column: 1 / -1;
  }
}
</style>
