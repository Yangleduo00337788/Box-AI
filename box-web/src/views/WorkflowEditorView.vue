<template>
  <div class="editor">
    <header class="editor__header">
      <t-button variant="text" shape="square" @click="router.push('/workflows')">
        <template #icon><t-icon name="chevron-left" /></template>
      </t-button>
      <div>
        <h1>{{ workflow?.name || '工作流编辑器' }}</h1>
        <p>拖拽节点连线 · 点击连线后 Delete 删除 · Ctrl+Z / Ctrl+Y 撤销重做 · 拖拽后自动保存</p>
      </div>
      <t-space>
        <t-button variant="outline" :disabled="!canUndo" @click="onUndo">
          <template #icon><t-icon name="rollback" /></template>
          撤销
        </t-button>
        <t-button variant="outline" :disabled="!canRedo" @click="onRedo">
          <template #icon><t-icon name="rollfront" /></template>
          重做
        </t-button>
        <span v-if="autoSaveStatusLabel" class="autosave-status" :class="`autosave-status--${autoSaveStatus}`">
          {{ autoSaveStatusLabel }}
        </span>
        <t-button variant="outline" :loading="validating" @click="validate">校验</t-button>
        <t-button variant="outline" :loading="running" @click="runDebug">调试</t-button>
        <t-button variant="outline" @click="openPublish">发布</t-button>
        <t-button theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-space>
    </header>

    <t-loading :loading="loading" size="small" class="editor__body">
      <Suspense>
        <WorkflowEditorCanvas
          v-if="workflowId"
          ref="canvasRef"
          :workflow-id="workflowId"
          :platform-models="platformModels"
          :definition-json="workflow?.definitionJson"
        />
        <template #fallback>
          <div class="editor__fallback">加载编辑器…</div>
        </template>
      </Suspense>
    </t-loading>

    <t-drawer v-model:visible="debugVisible" header="调试结果" size="560px" :footer="false">
      <template v-if="debugResult">
        <t-descriptions :column="1" bordered size="small">
          <t-descriptions-item label="状态">{{ debugResult.status }}</t-descriptions-item>
          <t-descriptions-item v-if="debugResult.executionNo" label="执行编号">{{ debugResult.executionNo }}</t-descriptions-item>
          <t-descriptions-item v-if="debugResult.errorMessage" label="错误">{{ debugResult.errorMessage }}</t-descriptions-item>
        </t-descriptions>
        <section v-if="debugResult.nodeTraces?.length" class="debug-traces">
          <h3>节点执行树</h3>
          <div v-for="trace in debugResult.nodeTraces" :key="trace.nodeId" class="debug-trace">
            <div class="debug-trace__head">
              <strong>{{ trace.nodeId }}</strong>
              <t-tag size="small" variant="light">{{ trace.nodeType }}</t-tag>
              <t-tag
                size="small"
                :theme="trace.status === 'SUCCESS' || trace.status === 'SUCCEEDED' ? 'success' : 'danger'"
                variant="light"
              >
                {{ trace.status }}
              </t-tag>
              <span>{{ trace.durationMs }} ms</span>
            </div>
            <pre v-if="trace.output">{{ formatJson(trace.output) }}</pre>
            <p v-if="trace.errorMessage" class="debug-trace__error">{{ trace.errorMessage }}</p>
          </div>
        </section>
        <section v-if="debugResult.outputs" class="debug-output">
          <h3>输出</h3>
          <pre>{{ formatJson(debugResult.outputs) }}</pre>
        </section>
      </template>
    </t-drawer>

    <t-drawer v-model:visible="publishVisible" header="发布与 Webhook" size="560px" :footer="false">
      <t-descriptions v-if="publishInfo" :column="1" bordered size="small">
        <t-descriptions-item label="状态">{{ publishInfo.status }}</t-descriptions-item>
        <t-descriptions-item label="发布版本">
          {{ publishInfo.publishedVersionNo ? `v${publishInfo.publishedVersionNo}` : '—' }}
        </t-descriptions-item>
        <t-descriptions-item label="发布时间">{{ publishInfo.publishedAt || '—' }}</t-descriptions-item>
        <t-descriptions-item v-if="publishInfo.webhookToken" label="Webhook Token">
          <div class="publish-secret-row">
            <code class="publish-secret">{{ publishInfo.webhookToken }}</code>
            <t-button size="small" variant="outline" @click="copyText(publishInfo.webhookToken!)">复制</t-button>
          </div>
        </t-descriptions-item>
        <t-descriptions-item v-if="publishInfo.webhookSecret" label="Webhook Secret">
          <div class="publish-secret-row">
            <code class="publish-secret">{{ publishInfo.webhookSecret }}</code>
            <t-button size="small" variant="outline" @click="copyText(publishInfo.webhookSecret!)">复制</t-button>
          </div>
          <p class="publish-hint publish-hint--inline">用于计算请求头 <code>X-Box-Signature</code>（HMAC-SHA256）。</p>
        </t-descriptions-item>
      </t-descriptions>
      <t-alert
        v-if="publishInfo && publishInfo.status !== 'PUBLISHED'"
        theme="warning"
        message="请先发布当前草稿，Webhook 与 API 执行才会生效。"
        style="margin-top: 12px"
      />
      <section v-if="publishInfo?.webhookUrl" class="publish-section">
        <h3>Webhook 触发</h3>
        <p class="publish-hint">POST 请求体为 JSON，将映射为工作流入参 <code>input</code>。</p>
        <p class="publish-label">完整 URL</p>
        <pre>{{ fullWebhookUrl }}</pre>
        <p class="publish-label">curl 示例</p>
        <pre>{{ webhookExample }}</pre>
      </section>
      <section v-if="publishInfo?.status === 'PUBLISHED'" class="publish-section">
        <h3>API 执行</h3>
        <pre>{{ executeApiExample }}</pre>
      </section>
      <t-space style="margin-top: 16px">
        <t-button theme="primary" :loading="publishing" @click="doPublish">发布当前草稿</t-button>
        <t-button variant="outline" @click="loadPublishStatus">刷新</t-button>
      </t-space>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  getWorkflow,
  getWorkflowPublishStatus,
  publishWorkflow,
  runWorkflowDebug,
  updateWorkflowDefinition,
  validateWorkflow,
  type WorkflowDebugResult,
  type WorkflowPublishVO,
  type WorkflowVO,
} from '@/api/workflow'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'

const WorkflowEditorCanvas = defineAsyncComponent(
  () => import('@/components/workflow/WorkflowEditorCanvas.vue'),
)

const route = useRoute()
const router = useRouter()
const workflowId = computed(() => Number(route.params.id))

const loading = ref(false)
const saving = ref(false)
const validating = ref(false)
const running = ref(false)
const debugVisible = ref(false)
const publishVisible = ref(false)
const publishing = ref(false)
const publishInfo = ref<WorkflowPublishVO | null>(null)
const debugResult = ref<WorkflowDebugResult | null>(null)
const workflow = ref<WorkflowVO | null>(null)
const platformModels = ref<PlatformModelVO[]>([])
const canvasRef = ref<{
  loadDefinition: (json?: string) => void
  getDefinitionJson: () => string
  handleUndo: () => boolean
  handleRedo: () => boolean
  canUndo: { value: boolean }
  canRedo: { value: boolean }
  autoSave: { status: { value: string }; cancelAutoSave: () => void }
} | null>(null)

const canUndo = computed(() => canvasRef.value?.canUndo.value ?? false)
const canRedo = computed(() => canvasRef.value?.canRedo.value ?? false)
const autoSaveStatus = computed(() => canvasRef.value?.autoSave.status.value ?? 'idle')

const autoSaveStatusLabel = computed(() => {
  switch (autoSaveStatus.value) {
    case 'pending':
      return '待保存…'
    case 'saving':
      return '保存中…'
    case 'saved':
      return '已自动保存'
    case 'error':
      return '自动保存失败'
    default:
      return ''
  }
})

const fullWebhookUrl = computed(() => {
  if (!publishInfo.value?.webhookUrl) return ''
  const path = publishInfo.value.webhookUrl
  return path.startsWith('http') ? path : `${window.location.origin}${path}`
})

const webhookExample = computed(() => {
  const url = fullWebhookUrl.value
  const secret = publishInfo.value?.webhookSecret
  if (!url) return ''
  if (!secret) {
    return `curl -X POST \\
  -H "Content-Type: application/json" \\
  -H "X-Box-Signature: sha256=<hmac-sha256(secret, body)>" \\
  -d '{"message":"hello"}' \\
  ${url}`
  }
  return `PAYLOAD='{"message":"hello"}'
SIG=sha256=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac '${secret}' | awk '{print $2}')
curl -X POST '${url}' \\
  -H "Content-Type: application/json" \\
  -H "X-Box-Signature: $SIG" \\
  -d "$PAYLOAD"`
})

const executeApiExample = computed(
  () => `curl -X POST \\
  -H "Authorization: Bearer <token>" \\
  -H "X-Workspace-Id: <workspaceId>" \\
  -H "Content-Type: application/json" \\
  -d '{"inputs":{"input":{"message":"hello"}}}' \\
  /api/v1/runtime/workflows/${workflowId.value}/execute`,
)

function onUndo() {
  if (canvasRef.value?.handleUndo()) {
    MessagePlugin.info('已撤销')
  }
}

function onRedo() {
  if (canvasRef.value?.handleRedo()) {
    MessagePlugin.info('已重做')
  }
}

async function load() {
  loading.value = true
  try {
    const [{ data: workflowRes }, { data: modelRes }] = await Promise.all([
      getWorkflow(workflowId.value),
      listPlatformModels(),
    ])
    workflow.value = workflowRes.data || null
    platformModels.value = modelRes.data || []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canvasRef.value) return
  saving.value = true
  try {
    canvasRef.value.autoSave.cancelAutoSave()
    await updateWorkflowDefinition(workflowId.value, canvasRef.value.getDefinitionJson())
    canvasRef.value.autoSave.status.value = 'saved'
    MessagePlugin.success('工作流已保存')
  } finally {
    saving.value = false
  }
}

async function validate() {
  validating.value = true
  try {
    await save()
    const { data } = await validateWorkflow(workflowId.value)
    if (data.data?.valid) {
      MessagePlugin.success('校验通过')
    } else {
      MessagePlugin.warning((data.data?.errors || ['校验失败']).join('；'))
    }
  } finally {
    validating.value = false
  }
}

function formatJson(value: unknown) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

async function runDebug() {
  running.value = true
  try {
    await save()
    const { data } = await runWorkflowDebug(workflowId.value, {
      input: { message: 'hello from workflow debug' },
    })
    debugResult.value = data.data || null
    debugVisible.value = true
    if (data.data?.status === 'SUCCEEDED' || data.data?.status === 'SUCCESS') {
      MessagePlugin.success('调试完成')
    } else {
      MessagePlugin.warning(data.data?.errorMessage || `调试结果：${data.data?.status || 'UNKNOWN'}`)
    }
  } finally {
    running.value = false
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    MessagePlugin.success('已复制')
  } catch {
    MessagePlugin.error('复制失败')
  }
}

async function loadPublishStatus() {
  const { data } = await getWorkflowPublishStatus(workflowId.value)
  publishInfo.value = data.data
}

async function openPublish() {
  publishVisible.value = true
  await loadPublishStatus()
}

async function doPublish() {
  publishing.value = true
  try {
    await save()
    const { data: validateRes } = await validateWorkflow(workflowId.value)
    if (!validateRes.data?.valid) {
      MessagePlugin.warning((validateRes.data?.errors || ['校验失败']).join('；'))
      return
    }
    const { data } = await publishWorkflow(workflowId.value)
    publishInfo.value = data.data
    if (workflow.value && data.data) {
      workflow.value = { ...workflow.value, status: data.data.status }
    }
    MessagePlugin.success('工作流已发布')
  } finally {
    publishing.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    load()
  },
  { immediate: true },
)
</script>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 96px);
}

.editor__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--td-component-border);
}

.editor__header h1 {
  margin: 0;
  font-size: 18px;
}

.editor__header p {
  margin: 4px 0 0;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.editor__header :deep(.t-space) {
  margin-left: auto;
}

.editor__body {
  flex: 1;
  min-height: 0;
}

.editor__fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 560px;
  color: var(--td-text-color-secondary);
}

.autosave-status {
  font-size: 12px;
  color: var(--td-text-color-secondary);
  min-width: 72px;
}

.autosave-status--pending,
.autosave-status--saving {
  color: var(--td-warning-color);
}

.autosave-status--saved {
  color: var(--td-success-color);
}

.autosave-status--error {
  color: var(--td-error-color);
}

.debug-traces,
.debug-output {
  margin-top: 20px;
}

.debug-traces h3,
.debug-output h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.debug-trace {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
}

.debug-trace__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.debug-trace pre,
.debug-output pre {
  margin: 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.debug-trace__error {
  margin: 0;
  color: var(--td-error-color);
  font-size: 12px;
}

.publish-section {
  margin-top: 20px;
}

.publish-section h3 {
  margin: 0 0 8px;
  font-size: 15px;
}

.publish-hint {
  margin: 0 0 8px;
  color: var(--td-text-color-secondary);
  font-size: 13px;
}

.publish-hint--inline {
  margin: 8px 0 0;
}

.publish-label {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 500;
}

.publish-secret-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.publish-secret {
  flex: 1;
  min-width: 0;
  padding: 4px 8px;
  border-radius: 4px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
  word-break: break-all;
}

.publish-section pre {
  margin: 0 0 12px;
  padding: 10px;
  border-radius: 6px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
