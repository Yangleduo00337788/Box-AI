<template>
  <div class="editor__layout">
    <aside class="palette">
      <h3>节点库</h3>
      <p class="palette__hint">拖到右侧画布添加</p>
      <div
        v-for="item in PALETTE_ITEMS"
        :key="item.type"
        class="palette__item"
        draggable="true"
        @dragstart="onPaletteDragStart($event, item)"
      >
        <t-icon :name="item.icon" />
        <span>{{ item.label }}</span>
        <small>{{ item.type }}</small>
      </div>
    </aside>

    <div class="canvas-wrap">
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :node-types="nodeTypes"
        :default-edge-options="defaultEdgeOptions"
        :snap-to-grid="true"
        :snap-grid="[16, 16]"
        :nodes-draggable="true"
        :nodes-connectable="true"
        :elements-selectable="true"
        :edges-selectable="true"
        :edges-focusable="true"
        :elevate-edges-on-select="true"
        :select-nodes-on-drag="false"
        :delete-key-code="null"
        fit-view-on-init
        @drop="onDrop"
        @dragover="onDragOver"
        @node-click="onNodeClick"
        @edge-click="onEdgeClick"
        @node-drag-start="onNodeDragStart"
        @node-drag-stop="onNodeDragStop"
        @pane-click="onPaneClick"
      >
        <Background pattern-color="rgba(0,0,0,0.08)" :gap="24" />
        <Controls />
        <MiniMap pannable zoomable />
      </VueFlow>
    </div>

    <aside v-if="selectedEdge" class="inspector">
      <h3>连线配置</h3>
      <t-form label-align="top">
        <t-form-item label="起点">
          <t-input :value="edgeSourceLabel" disabled />
        </t-form-item>
        <t-form-item label="终点">
          <t-input :value="edgeTargetLabel" disabled />
        </t-form-item>
        <t-form-item v-if="selectedEdge.sourceHandle" label="分支">
          <t-tag :theme="selectedEdge.sourceHandle === 'true' ? 'success' : 'warning'" variant="light">
            {{ selectedEdge.sourceHandle === 'true' ? 'True 分支' : 'False 分支' }}
          </t-tag>
        </t-form-item>
        <t-alert theme="info" message="按 Delete 或 Backspace 可删除此连线" />
        <t-form-item>
          <t-button theme="danger" variant="outline" block @click="removeSelectedEdge">删除连线</t-button>
        </t-form-item>
      </t-form>
    </aside>

    <aside v-else-if="selectedNode?.data" class="inspector">
      <h3>节点配置</h3>
      <t-form label-align="top">
        <t-form-item label="显示名称">
          <t-input v-model="(selectedNode.data as WorkflowNodeData).label" />
        </t-form-item>
        <t-form-item label="节点类型">
          <t-input :value="(selectedNode.data as WorkflowNodeData).nodeType" disabled />
        </t-form-item>

        <template v-if="(selectedNode.data as WorkflowNodeData).nodeType === 'LLM'">
          <t-form-item label="平台模型">
            <t-select
              v-model="inspector.llmPlatformModelId"
              :options="platformModelOptions"
              placeholder="选择模型"
              clearable
            />
          </t-form-item>
          <t-form-item label="System Prompt">
            <t-textarea v-model="inspector.llmSystemPrompt" :autosize="{ minRows: 2, maxRows: 4 }" />
          </t-form-item>
          <t-form-item label="User Prompt">
            <t-textarea v-model="inspector.llmUserPrompt" :autosize="{ minRows: 3, maxRows: 6 }" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'HTTP'">
          <t-form-item label="Method">
            <t-select v-model="inspector.httpMethod" :options="httpMethods" />
          </t-form-item>
          <t-form-item label="URL">
            <t-input v-model="inspector.httpUrl" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Condition'">
          <t-form-item label="变量名">
            <t-input v-model="inspector.conditionVariable" placeholder="如 input.message" />
          </t-form-item>
          <t-form-item label="运算符">
            <t-select v-model="inspector.conditionOperator" :options="conditionOperators" />
          </t-form-item>
          <t-form-item label="比较值">
            <t-input v-model="inspector.conditionValue" />
          </t-form-item>
          <t-alert theme="info" message="右侧两个连接点：上方为 true 分支，下方为 false 分支" />
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Delay'">
          <t-form-item label="延迟 (ms)">
            <t-input-number v-model="inspector.delayMs" :min="0" theme="column" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Template'">
          <t-form-item label="模板内容">
            <t-textarea v-model="inspector.templateContent" :autosize="{ minRows: 4, maxRows: 8 }" />
          </t-form-item>
        </template>

        <t-form-item v-if="(selectedNode.data as WorkflowNodeData).nodeType !== 'Start'">
          <t-button theme="danger" variant="outline" block @click="removeSelectedNode">删除节点</t-button>
        </t-form-item>
      </t-form>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import {
  VueFlow,
  addEdge,
  useVueFlow,
  type Connection,
  type Edge,
  type NodeTypesObject,
} from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import WorkflowFlowNode from '@/components/workflow/WorkflowFlowNode.vue'
import {
  createFlowNode,
  defaultWorkflowDefinition,
  definitionToFlow,
  flowToDefinition,
  PALETTE_ITEMS,
  type WorkflowNodeData,
} from '@/utils/workflowFlow'
import type { WorkflowDefinition } from '@/api/workflow'
import type { PlatformModelVO } from '@/api/platform'
import { useWorkflowEditorHistory } from '@/composables/useWorkflowEditorHistory'
import { useWorkflowAutoSave } from '@/composables/useWorkflowAutoSave'

const props = defineProps<{
  workflowId: number
  platformModels: PlatformModelVO[]
  definitionJson?: string
}>()

const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const variables = ref<WorkflowDefinition['variables']>([])
const selectedNode = ref<any>(null)
const selectedEdge = ref<Edge | null>(null)
const draggedPalette = ref<{ type: string; label: string } | null>(null)
const isDraggingNode = ref(false)

const { screenToFlowCoordinate, onConnect, getSelectedNodes, getSelectedEdges } = useVueFlow()

const { recordBeforeChange, resetHistory, undo, redo, canUndo, canRedo } = useWorkflowEditorHistory(nodes, edges)

const autoSave = useWorkflowAutoSave(
  () => props.workflowId,
  () => nodes.value,
  () => edges.value,
  () => variables.value,
)

const nodeTypes = { workflow: markRaw(WorkflowFlowNode) } as NodeTypesObject
const defaultEdgeOptions = {
  type: 'smoothstep',
  animated: true,
  selectable: true,
  focusable: true,
}

const platformModelOptions = computed(() =>
  props.platformModels
    .filter((item) => item.status === 1)
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

const httpMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'].map((v) => ({ label: v, value: v }))
const conditionOperators = [
  { label: '等于', value: 'equals' },
  { label: '不等于', value: 'not_equals' },
  { label: '包含', value: 'contains' },
  { label: '为空', value: 'empty' },
  { label: '不为空', value: 'not_empty' },
]

function nodeLabel(id: string) {
  const node = nodes.value.find((item) => item.id === id)
  const data = node?.data as WorkflowNodeData | undefined
  return data?.label || id
}

const edgeSourceLabel = computed(() => (selectedEdge.value ? nodeLabel(selectedEdge.value.source) : ''))
const edgeTargetLabel = computed(() => (selectedEdge.value ? nodeLabel(selectedEdge.value.target) : ''))

function clearSelection() {
  selectedNode.value = null
  selectedEdge.value = null
  nodes.value = nodes.value.map((node) => ({ ...node, selected: false }))
  edges.value = edges.value.map((edge) => ({ ...edge, selected: false }))
}

function selectEdge(edge: Edge) {
  selectedNode.value = null
  selectedEdge.value = edge
  nodes.value = nodes.value.map((node) => ({ ...node, selected: false }))
  edges.value = edges.value.map((item) => ({ ...item, selected: item.id === edge.id }))
}

function selectNode(node: any) {
  selectedEdge.value = null
  selectedNode.value = node
  nodes.value = nodes.value.map((item) => ({ ...item, selected: item.id === node.id }))
  edges.value = edges.value.map((edge) => ({ ...edge, selected: false }))
}

onConnect((connection: Connection) => {
  recordBeforeChange()
  edges.value = addEdge(
    {
      ...connection,
      type: 'smoothstep',
      animated: true,
      selectable: true,
      focusable: true,
    },
    edges.value,
  ) as any[]
  autoSave.scheduleAutoSave()
})

const inspector = reactive({
  llmPlatformModelId: undefined as number | undefined,
  llmSystemPrompt: '',
  llmUserPrompt: '',
  httpMethod: 'GET',
  httpUrl: '',
  conditionVariable: '',
  conditionOperator: 'equals',
  conditionValue: '',
  delayMs: 1000,
  templateContent: '',
})

function configOf(node: any): Record<string, unknown> {
  const data = node?.data as WorkflowNodeData | undefined
  if (!data) return {}
  if (!data.config) data.config = {}
  return data.config
}

function syncInspectorFromNode(node: any) {
  const config = configOf(node)
  inspector.llmPlatformModelId = config.platformModelId as number | undefined
  inspector.llmSystemPrompt = String(config.systemPrompt || '')
  inspector.llmUserPrompt = String(config.userPrompt || config.prompt || '')
  inspector.httpMethod = String(config.method || 'GET')
  inspector.httpUrl = String(config.url || '')
  inspector.conditionVariable = String(config.variable || '')
  inspector.conditionOperator = String(config.operator || 'equals')
  inspector.conditionValue = String(config.value || '')
  inspector.delayMs = Number(config.delayMs ?? 1000)
  inspector.templateContent = String(config.template || '')
}

function syncNodeFromInspector(node: any) {
  const config = configOf(node)
  config.platformModelId = inspector.llmPlatformModelId
  config.systemPrompt = inspector.llmSystemPrompt
  config.userPrompt = inspector.llmUserPrompt
  config.prompt = inspector.llmUserPrompt
  config.method = inspector.httpMethod
  config.url = inspector.httpUrl
  config.variable = inspector.conditionVariable
  config.operator = inspector.conditionOperator
  config.value = inspector.conditionValue
  config.delayMs = inspector.delayMs
  config.template = inspector.templateContent
}

watch(inspector, () => {
  syncNodeFromInspector(selectedNode.value)
  autoSave.scheduleAutoSave()
}, { deep: true })

watch(
  () => props.definitionJson,
  (json) => {
    if (json !== undefined) {
      loadDefinition(json)
    }
  },
  { immediate: true },
)

function loadDefinition(json?: string) {
  clearSelection()
  try {
    const def = JSON.parse(json || '{}') as WorkflowDefinition
    const hasNodes = def.nodes?.length
    const definition = hasNodes ? def : defaultWorkflowDefinition()
    variables.value = definition.variables || []
    const flow = definitionToFlow(definition)
    nodes.value = flow.nodes
    edges.value = flow.edges
  } catch {
    const fallback = defaultWorkflowDefinition()
    variables.value = fallback.variables
    const flow = definitionToFlow(fallback)
    nodes.value = flow.nodes
    edges.value = flow.edges
  }
  resetHistory()
  autoSave.status.value = 'idle'
}

function getDefinitionJson() {
  return JSON.stringify(flowToDefinition(nodes.value, edges.value, variables.value))
}

function onPaletteDragStart(event: DragEvent, item: { type: string; label: string }) {
  draggedPalette.value = item
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('application/vueflow', item.type)
  }
}

function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function onDrop(event: DragEvent) {
  event.preventDefault()
  const type = event.dataTransfer?.getData('application/vueflow') || draggedPalette.value?.type
  const label = draggedPalette.value?.label || type
  if (!type || !label) return

  const position = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })

  if (type === 'Start' && nodes.value.some((node) => (node.data as WorkflowNodeData | undefined)?.nodeType === 'Start')) {
    MessagePlugin.warning('工作流只能有一个开始节点')
    draggedPalette.value = null
    return
  }

  recordBeforeChange()
  nodes.value.push(createFlowNode(type, label, position))
  draggedPalette.value = null
  autoSave.scheduleAutoSave()
}

function onNodeClick(event: any) {
  selectNode(event.node)
  syncInspectorFromNode(selectedNode.value)
}

function onEdgeClick(event: any) {
  selectEdge(event.edge)
}

function onPaneClick() {
  clearSelection()
}

function onNodeDragStart() {
  if (!isDraggingNode.value) {
    recordBeforeChange()
    isDraggingNode.value = true
  }
}

function onNodeDragStop() {
  isDraggingNode.value = false
  autoSave.scheduleAutoSave()
}

function removeNodeById(id: string) {
  const node = nodes.value.find((item) => item.id === id)
  const nodeType = (node?.data as WorkflowNodeData | undefined)?.nodeType
  if (nodeType === 'Start') {
    MessagePlugin.warning('不能删除开始节点')
    return false
  }
  recordBeforeChange()
  nodes.value = nodes.value.filter((item) => item.id !== id)
  edges.value = edges.value.filter((edge) => edge.source !== id && edge.target !== id)
  if (selectedNode.value?.id === id) {
    selectedNode.value = null
  }
  if (selectedEdge.value && (selectedEdge.value.source === id || selectedEdge.value.target === id)) {
    selectedEdge.value = null
  }
  autoSave.scheduleAutoSave()
  return true
}

function removeSelectedEdge() {
  if (!selectedEdge.value) return
  recordBeforeChange()
  const edgeId = selectedEdge.value.id
  edges.value = edges.value.filter((edge) => edge.id !== edgeId)
  selectedEdge.value = null
  autoSave.scheduleAutoSave()
}

function deleteSelectedElements() {
  const selectedNodes = getSelectedNodes.value
  let selectedEdgesList: any[] = getSelectedEdges.value as any[]
  if (!selectedEdgesList.length && selectedEdge.value) {
    selectedEdgesList = [selectedEdge.value]
  }

  if (!selectedNodes.length && !selectedEdgesList.length) {
    if (selectedNode.value?.id) {
      removeNodeById(selectedNode.value.id)
    }
    return
  }

  if (selectedNodes.some((node) => (node.data as WorkflowNodeData | undefined)?.nodeType === 'Start')) {
    MessagePlugin.warning('不能删除开始节点')
    return
  }

  recordBeforeChange()
  const nodeIds = new Set(selectedNodes.map((node) => node.id))
  const edgeIds = new Set(selectedEdgesList.map((edge) => edge.id))

  nodes.value = nodes.value.filter((node) => !nodeIds.has(node.id))
  edges.value = edges.value.filter(
    (edge) =>
      !edgeIds.has(edge.id) && !nodeIds.has(edge.source) && !nodeIds.has(edge.target),
  )
  clearSelection()
  autoSave.scheduleAutoSave()
}

function removeSelectedNode() {
  if (!selectedNode.value?.id) return
  removeNodeById(selectedNode.value.id)
}

function handleUndo() {
  if (!undo()) return false
  clearSelection()
  autoSave.scheduleAutoSave()
  return true
}

function handleRedo() {
  if (!redo()) return false
  clearSelection()
  autoSave.scheduleAutoSave()
  return true
}

function isEditableTarget(target: EventTarget | null) {
  if (!(target instanceof HTMLElement)) return false
  const tag = target.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || target.isContentEditable
}

function onWindowKeyDown(event: KeyboardEvent) {
  if (isEditableTarget(event.target)) return

  if (event.key === 'Delete' || event.key === 'Backspace') {
    event.preventDefault()
    deleteSelectedElements()
    return
  }

  if (event.ctrlKey && !event.shiftKey && event.key.toLowerCase() === 'z') {
    event.preventDefault()
    if (handleUndo()) {
      MessagePlugin.info('已撤销')
    }
    return
  }

  if (event.ctrlKey && (event.key.toLowerCase() === 'y' || (event.shiftKey && event.key.toLowerCase() === 'z'))) {
    event.preventDefault()
    if (handleRedo()) {
      MessagePlugin.info('已重做')
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', onWindowKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onWindowKeyDown)
})

defineExpose({
  loadDefinition,
  getDefinitionJson,
  handleUndo,
  handleRedo,
  canUndo,
  canRedo,
  autoSave,
})
</script>

<style scoped>
.editor__layout {
  display: grid;
  grid-template-columns: 200px 1fr auto;
  gap: 16px;
  height: 100%;
  min-height: 560px;
}

.palette,
.inspector {
  border: 1px solid var(--td-component-border);
  border-radius: 12px;
  padding: 12px;
  background: var(--td-bg-color-container);
  overflow: auto;
}

.palette h3,
.inspector h3 {
  margin: 0 0 8px;
  font-size: 14px;
}

.palette__hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.palette__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 8px;
  align-items: center;
  margin-bottom: 8px;
  padding: 10px;
  border: 1px dashed var(--td-component-border);
  border-radius: 10px;
  cursor: grab;
  background: var(--td-bg-color-container-hover, #fafafa);
}

.palette__item small {
  grid-column: 2;
  font-size: 11px;
  color: var(--td-text-color-placeholder);
}

.canvas-wrap {
  position: relative;
  border: 1px solid var(--td-component-border);
  border-radius: 12px;
  overflow: hidden;
  min-height: 560px;
}

.canvas-wrap :deep(.vue-flow) {
  width: 100%;
  height: 100%;
  min-height: 560px;
  background: var(--td-bg-color-page, #f5f5f5);
}

.canvas-wrap :deep(.vue-flow__edge.selected .vue-flow__edge-path),
.canvas-wrap :deep(.vue-flow__edge.selectable:focus .vue-flow__edge-path),
.canvas-wrap :deep(.vue-flow__edge.selectable:focus-visible .vue-flow__edge-path) {
  stroke: var(--td-brand-color, #0052d9);
  stroke-width: 2.5;
}

.inspector {
  width: 300px;
}
</style>
