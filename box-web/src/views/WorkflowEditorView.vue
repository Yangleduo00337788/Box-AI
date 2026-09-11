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
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  getWorkflow,
  runWorkflowDebug,
  updateWorkflowDefinition,
  validateWorkflow,
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

async function runDebug() {
  running.value = true
  try {
    await save()
    const { data } = await runWorkflowDebug(workflowId.value, { message: 'hello from workflow debug' })
    MessagePlugin.success(`调试结果：${data.data?.status || 'UNKNOWN'}`)
  } finally {
    running.value = false
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
</style>
