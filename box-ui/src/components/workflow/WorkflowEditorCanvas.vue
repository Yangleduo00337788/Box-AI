<template>
  <div class="editor__layout">
    <aside class="palette">
      <div class="palette__head">
        <h3>节点库</h3>
        <p>单击添加，或拖到画布</p>
      </div>
      <div class="palette__scroll">
        <section v-for="group in paletteGroups" :key="group.key" class="palette__group">
          <h4>{{ group.label }}</h4>
          <button
            v-for="item in group.items"
            :key="item.type"
            type="button"
            class="palette__item"
            :class="{ 'is-disabled': isPaletteDisabled(item.type) }"
            :disabled="isPaletteDisabled(item.type)"
            draggable="true"
            @click="onPaletteClick(item)"
            @dragstart="onPaletteDragStart($event, item)"
          >
            <t-icon :name="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </section>
      </div>
    </aside>

    <div ref="canvasWrapRef" class="canvas-wrap">
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
        :connection-line-options="connectionLineOptions"
        fit-view-on-init
        @drop="onDrop"
        @dragover="onDragOver"
        @node-click="onNodeClick"
        @edge-click="onEdgeClick"
        @node-drag-start="onNodeDragStart"
        @node-drag-stop="onNodeDragStop"
        @pane-click="onPaneClick"
      >
        <Background pattern-color="var(--box-canvas-grid, rgba(0,0,0,0.08))" :gap="24" />
        <Controls />
        <MiniMap pannable zoomable />
      </VueFlow>
    </div>

    <aside v-if="selectedEdge" class="inspector">
      <div class="inspector__head">
        <h3>连线</h3>
        <t-button variant="text" theme="danger" size="small" @click="removeSelectedEdge">删除</t-button>
      </div>
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
      </t-form>
    </aside>

    <aside v-else-if="selectedNode?.data" class="inspector">
      <div class="inspector__head">
        <h3>节点配置</h3>
        <t-button
          v-if="(selectedNode.data as WorkflowNodeData).nodeType !== 'Start'"
          variant="text"
          theme="danger"
          size="small"
          @click="removeSelectedNode"
        >
          删除
        </t-button>
      </div>
      <t-form label-align="top">
        <t-form-item label="显示名称">
          <t-input v-model="(selectedNode.data as WorkflowNodeData).label" />
        </t-form-item>
        <t-form-item label="节点类型">
          <t-input :value="(selectedNode.data as WorkflowNodeData).nodeType" disabled />
        </t-form-item>

        <template v-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Agent'">
          <t-form-item label="智能体">
            <t-select
              v-model="inspector.agentId"
              :options="agentOptions"
              placeholder="选择 Agent"
              clearable
            />
          </t-form-item>
          <t-form-item label="用户消息模板">
            <t-textarea v-model="inspector.agentMessage" :autosize="{ minRows: 2, maxRows: 6 }" />
          </t-form-item>
          <t-form-item label="输出变量名">
            <t-input v-model="inspector.agentOutputVariable" />
          </t-form-item>
        </template>

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

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Knowledge'">
          <t-form-item label="知识库">
            <t-select
              v-model="inspector.knowledgeBaseId"
              :options="knowledgeBaseOptions"
              placeholder="选择知识库"
              clearable
            />
          </t-form-item>
          <t-form-item label="检索问题">
            <t-textarea v-model="inspector.knowledgeQuery" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="{{input.message}}" />
          </t-form-item>
          <t-form-item label="Top K">
            <t-input-number v-model="inspector.knowledgeTopK" :min="1" :max="20" theme="column" />
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

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'SubWorkflow'">
          <t-form-item label="子工作流">
            <t-select
              v-model="inspector.subWorkflowId"
              :options="subWorkflowOptions"
              placeholder="选择工作流"
              clearable
            />
          </t-form-item>
          <t-form-item label="入参 JSON 模板（可选）">
            <t-textarea
              v-model="inspector.subWorkflowInputsJson"
              placeholder='留空则继承当前变量，例如 {"input":{"message":"{{input.message}}"}}'
              :autosize="{ minRows: 3, maxRows: 8 }"
            />
          </t-form-item>
          <t-form-item label="输出变量名">
            <t-input v-model="inspector.subWorkflowOutputVariable" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Webhook'">
          <t-form-item label="URL">
            <t-input v-model="inspector.webhookUrl" />
          </t-form-item>
          <t-form-item label="Payload 模板">
            <t-textarea v-model="inspector.webhookPayload" :autosize="{ minRows: 3, maxRows: 8 }" />
          </t-form-item>
          <t-form-item label="签名密钥（可选）">
            <t-input v-model="inspector.webhookSecret" type="password" />
          </t-form-item>
          <t-form-item label="事件类型">
            <t-input v-model="inspector.webhookEventType" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Tool'">
          <t-form-item label="工具来源">
            <t-select v-model="inspector.toolSourceType" :options="toolSourceOptions" />
          </t-form-item>
          <template v-if="inspector.toolSourceType === 'HTTP'">
            <t-form-item label="HTTP 工具">
              <t-select
                v-model="inspector.toolId"
                :options="toolOptions"
                placeholder="选择工具"
                clearable
              />
            </t-form-item>
            <t-form-item label="请求体模板">
              <t-textarea
                v-model="inspector.toolBodyTemplate"
                :autosize="{ minRows: 2, maxRows: 6 }"
                placeholder="可选，覆盖工具默认 body，支持 {{input.message}}"
              />
            </t-form-item>
          </template>
          <template v-else>
            <t-form-item label="MCP Server">
              <t-select
                v-model="inspector.mcpServerId"
                :options="mcpServerOptions"
                placeholder="选择 MCP Server"
                clearable
              />
            </t-form-item>
            <t-form-item label="MCP 工具">
              <t-select
                v-model="inspector.mcpToolName"
                :options="mcpToolOptions"
                placeholder="选择工具"
                clearable
              />
            </t-form-item>
            <t-form-item label="参数 JSON">
              <t-textarea
                v-model="inspector.toolArgumentsJson"
                :autosize="{ minRows: 2, maxRows: 6 }"
                placeholder='{"query":"{{input.message}}"}'
              />
            </t-form-item>
          </template>
          <t-form-item label="输出变量">
            <t-input v-model="inspector.toolOutputVariable" placeholder="toolResult" />
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

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Switch'">
          <t-form-item label="变量名">
            <t-input v-model="inspector.switchVariable" placeholder="如 input.type" />
          </t-form-item>
          <t-form-item label="运算符">
            <t-select v-model="inspector.switchOperator" :options="conditionOperators" />
          </t-form-item>
          <t-form-item label="默认分支 ID">
            <t-input v-model="inspector.switchDefaultCase" placeholder="default" />
          </t-form-item>
          <div class="switch-cases">
            <div v-for="(caseItem, index) in inspector.switchCases" :key="index" class="switch-case-row">
              <t-input v-model="caseItem.id" placeholder="分支 ID" />
              <t-input v-model="caseItem.value" placeholder="匹配值" />
              <t-button variant="text" theme="danger" @click="removeSwitchCase(index)">删除</t-button>
            </div>
            <t-button variant="dashed" block @click="addSwitchCase">添加分支</t-button>
          </div>
          <t-alert theme="info" message="右侧每个连接点对应一个 case ID，最下方为默认分支" />
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

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Loop'">
          <t-form-item label="图模式">
            <t-switch v-model="inspector.loopGraphBody" />
            <div class="ops-kind-hint">开启后从 body 连线执行子图，next 连线为循环结束后的下一节点</div>
          </t-form-item>
          <t-form-item label="模式">
            <t-select v-model="inspector.loopMode" :options="loopModeOptions" />
          </t-form-item>
          <t-form-item v-if="inspector.loopMode === 'FOREACH'" label="数组变量">
            <t-input v-model="inspector.loopItemsVariable" />
          </t-form-item>
          <t-form-item v-if="inspector.loopMode === 'COUNT'" label="次数">
            <t-input-number v-model="inspector.loopCount" :min="1" :max="100" theme="column" />
          </t-form-item>
          <t-form-item v-if="!inspector.loopGraphBody" label="迭代脚本">
            <t-textarea v-model="inspector.loopCode" :autosize="{ minRows: 4, maxRows: 10 }" />
          </t-form-item>
          <t-form-item label="最大迭代">
            <t-input-number v-model="inspector.loopMaxIterations" :min="1" :max="100" theme="column" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Code'">
          <t-form-item label="函数名">
            <t-input v-model="inspector.codeFunctionName" />
          </t-form-item>
          <t-form-item label="脚本">
            <t-textarea v-model="inspector.codeContent" :autosize="{ minRows: 6, maxRows: 12 }" />
          </t-form-item>
        </template>

        <template v-else-if="(selectedNode.data as WorkflowNodeData).nodeType === 'Parallel'">
          <t-form-item label="图分支">
            <t-switch v-model="inspector.parallelUseGraphBranches" />
            <div class="ops-kind-hint">开启后多条出边并行执行，汇合到同一节点</div>
          </t-form-item>
          <t-form-item v-if="!inspector.parallelUseGraphBranches" label="任务 JSON">
            <t-textarea
              v-model="inspector.parallelTasksJson"
              :autosize="{ minRows: 6, maxRows: 12 }"
              placeholder='[{"type":"TEMPLATE","template":"Hi {{name}}"}]'
            />
          </t-form-item>
        </template>

        <t-form-item v-if="(selectedNode.data as WorkflowNodeData).nodeType !== 'Start'">
          <t-button theme="danger" variant="outline" block @click="removeSelectedNode">删除节点</t-button>
        </t-form-item>
      </t-form>
    </aside>

    <aside v-else class="inspector inspector--empty">
      <t-icon name="edit-1" />
      <p>选中节点或连线后在此配置</p>
      <span>单击左侧节点添加到画布中心，也可拖到指定位置</span>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
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
import WorkflowFlowNode from './WorkflowFlowNode.vue'
import type { ConnectionLineOptions, DefaultEdgeOptions } from '@vue-flow/core'
import {
  createFlowNode,
  defaultWorkflowDefinition,
  definitionToFlow,
  flowToDefinition,
  groupedPaletteItems,
  autoLayoutFlow,
  styledFlowEdge,
  FLOW_EDGE_COLOR,
  type WorkflowNodeData,
} from '../../workflow/workflowFlow'
import type { WorkflowDefinition } from '../../types/workflow'
import type {
  WorkflowEditorHost,
  WorkflowEditorMcpServer,
  WorkflowEditorNamedResource,
  WorkflowEditorPlatformModel,
} from '../../workflow/workflowEditorHost'
import { useWorkflowEditorHistory } from '../../composables/useWorkflowEditorHistory'
import { useWorkflowEditorPersistence } from '../../composables/useWorkflowEditorPersistence'

const props = defineProps<{
  workflowId?: number | null
  platformModels: WorkflowEditorPlatformModel[]
  definitionJson?: string
  editorHost: WorkflowEditorHost
}>()

const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const variables = ref<WorkflowDefinition['variables']>([])
const selectedNode = ref<any>(null)
const selectedEdge = ref<Edge | null>(null)
const draggedPalette = ref<{ type: string; label: string } | null>(null)
const paletteDragging = ref(false)
const canvasWrapRef = ref<HTMLElement | null>(null)
const isDraggingNode = ref(false)

const paletteGroups = groupedPaletteItems()

const { screenToFlowCoordinate, onConnect, getSelectedNodes, getSelectedEdges, fitView } = useVueFlow()

const { recordBeforeChange, resetHistory, undo, redo, canUndo, canRedo } = useWorkflowEditorHistory(nodes, edges)

const autoSave = useWorkflowEditorPersistence(
  () => props.workflowId ?? null,
  () => nodes.value,
  () => edges.value,
  () => variables.value,
  props.editorHost.saveDefinition,
)

const nodeTypes = { workflow: markRaw(WorkflowFlowNode) } as NodeTypesObject
const defaultEdgeOptions: DefaultEdgeOptions = {
  type: 'default',
  animated: true,
  selectable: true,
  focusable: true,
  style: {
    stroke: FLOW_EDGE_COLOR,
    strokeWidth: 2,
  },
}

const connectionLineOptions: ConnectionLineOptions = {
  style: {
    stroke: FLOW_EDGE_COLOR,
    strokeWidth: 2,
  },
}

const knowledgeBaseOptions = ref<Array<{ label: string; value: number }>>([])
const toolOptions = ref<Array<{ label: string; value: number }>>([])

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

const loopModeOptions = [
  { label: 'For Each', value: 'FOREACH' },
  { label: 'Count', value: 'COUNT' },
  { label: 'While', value: 'WHILE' },
]

function addSwitchCase() {
  inspector.switchCases.push({ id: `case${inspector.switchCases.length + 1}`, value: '' })
  if (selectedNode.value) {
    syncNodeFromInspector(selectedNode.value)
  }
}

function removeSwitchCase(index: number) {
  inspector.switchCases.splice(index, 1)
  if (selectedNode.value) {
    syncNodeFromInspector(selectedNode.value)
  }
}

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
    styledFlowEdge({
      ...connection,
    }),
    edges.value,
  ) as any[]
  autoSave.scheduleAutoSave()
})

const inspector = reactive({
  agentId: undefined as number | undefined,
  agentMessage: '{{input.message}}',
  agentOutputVariable: 'agentResult',
  llmPlatformModelId: undefined as number | undefined,
  llmSystemPrompt: '',
  llmUserPrompt: '',
  httpMethod: 'GET',
  httpUrl: '',
  webhookUrl: '',
  webhookPayload: '{{input}}',
  webhookSecret: '',
  webhookEventType: 'workflow.event',
  subWorkflowId: undefined as number | undefined,
  subWorkflowInputsJson: '',
  subWorkflowOutputVariable: 'subWorkflowResult',
  conditionVariable: '',
  conditionOperator: 'equals',
  conditionValue: '',
  switchVariable: '',
  switchOperator: 'equals',
  switchDefaultCase: 'default',
  switchCases: [{ id: 'case1', value: '' }] as Array<{ id: string; value: string }>,
  delayMs: 1000,
  templateContent: '',
  knowledgeBaseId: undefined as number | undefined,
  knowledgeQuery: '{{input.message}}',
  knowledgeTopK: 5,
  toolSourceType: 'HTTP',
  toolId: undefined as number | undefined,
  toolBodyTemplate: '',
  mcpServerId: undefined as number | undefined,
  mcpToolName: '',
  toolArgumentsJson: '{}',
  toolOutputVariable: 'toolResult',
  loopMode: 'FOREACH',
  loopGraphBody: false,
  loopItemsVariable: 'items',
  loopCount: 3,
  loopCode: 'function execute(args) {\n  return args.loopItem;\n}',
  loopMaxIterations: 100,
  codeFunctionName: 'execute',
  codeContent: 'function execute(args) {\n  return args.input;\n}',
  parallelTasksJson: '[{"type":"TEMPLATE","template":"{{input}}"}]',
  parallelUseGraphBranches: false,
})

const toolSourceOptions = [
  { label: 'HTTP 工具', value: 'HTTP' },
  { label: 'MCP 工具', value: 'MCP' },
]

const mcpServers = ref<WorkflowEditorMcpServer[]>([])
const workflows = ref<WorkflowEditorNamedResource[]>([])
const agents = ref<WorkflowEditorNamedResource[]>([])

const agentOptions = computed(() =>
  agents.value.map((item) => ({ label: item.name, value: item.id })),
)

const subWorkflowOptions = computed(() =>
  workflows.value
    .filter((item) => props.workflowId == null || item.id !== props.workflowId)
    .map((item) => ({ label: item.name, value: item.id })),
)

const mcpServerOptions = computed(() =>
  mcpServers.value
    .filter((item) => item.status === 1)
    .map((item) => ({ label: item.name, value: item.id })),
)

const mcpToolOptions = computed(() => {
  const server = mcpServers.value.find((item) => item.id === inspector.mcpServerId)
  if (!server?.toolCatalogJson) return []
  try {
    const items = JSON.parse(server.toolCatalogJson) as Array<{ name?: string; description?: string }>
    if (!Array.isArray(items)) return []
    return items
      .filter((item) => item.name)
      .map((item) => ({
        label: item.description ? `${item.name} — ${item.description}` : item.name!,
        value: item.name!,
      }))
  } catch {
    return []
  }
})

function configOf(node: any): Record<string, unknown> {
  const data = node?.data as WorkflowNodeData | undefined
  if (!data) return {}
  if (!data.config) data.config = {}
  return data.config
}

function syncInspectorFromNode(node: any) {
  const config = configOf(node)
  inspector.agentId = config.agentId as number | undefined
  inspector.agentMessage = String(config.message || '{{input.message}}')
  inspector.agentOutputVariable = String(config.outputVariable || 'agentResult')
  inspector.llmPlatformModelId = config.platformModelId as number | undefined
  inspector.llmSystemPrompt = String(config.systemPrompt || '')
  inspector.llmUserPrompt = String(config.userPrompt || config.prompt || '')
  inspector.httpMethod = String(config.method || 'GET')
  inspector.httpUrl = String(config.url || '')
  inspector.webhookUrl = String(config.url || '')
  inspector.webhookPayload = String(config.payload || '{{input}}')
  inspector.webhookSecret = String(config.secret || '')
  inspector.webhookEventType = String(config.eventType || 'workflow.event')
  inspector.subWorkflowId = config.workflowId as number | undefined
  inspector.subWorkflowInputsJson = String(config.inputsJson || '')
  inspector.subWorkflowOutputVariable = String(config.outputVariable || 'subWorkflowResult')
  inspector.conditionVariable = String(config.variable || '')
  inspector.conditionOperator = String(config.operator || 'equals')
  inspector.conditionValue = String(config.value || '')
  inspector.switchVariable = String(config.variable || '')
  inspector.switchOperator = String(config.operator || 'equals')
  inspector.switchDefaultCase = String(config.defaultCase || 'default')
  inspector.switchCases = Array.isArray(config.cases)
    ? (config.cases as Array<{ id?: string; value?: string }>).map((item) => ({
        id: String(item.id || ''),
        value: String(item.value || ''),
      }))
    : [{ id: 'case1', value: '' }]
  inspector.delayMs = Number(config.delayMs ?? 1000)
  inspector.templateContent = String(config.template || '')
  inspector.knowledgeBaseId = config.knowledgeBaseId as number | undefined
  inspector.knowledgeQuery = String(config.query || config.prompt || '{{input.message}}')
  inspector.knowledgeTopK = Number(config.topK ?? 5)
  inspector.toolSourceType = String(config.sourceType || 'HTTP')
  inspector.toolId = config.toolId as number | undefined
  inspector.toolBodyTemplate = String(config.bodyTemplate || '')
  inspector.mcpServerId = config.mcpServerId as number | undefined
  inspector.mcpToolName = String(config.mcpToolName || '')
  inspector.toolArgumentsJson = String(config.argumentsJson || '{}')
  inspector.toolOutputVariable = String(config.outputVariable || 'toolResult')
  inspector.loopMode = String(config.mode || 'FOREACH')
  inspector.loopGraphBody = Boolean(config.graphBody)
  inspector.loopItemsVariable = String(config.itemsVariable || 'items')
  inspector.loopCount = Number(config.count ?? 3)
  inspector.loopCode = String(config.code || 'function execute(args) {\n  return args.loopItem;\n}')
  inspector.loopMaxIterations = Number(config.maxIterations ?? 100)
  inspector.codeFunctionName = String(config.functionName || 'execute')
  inspector.codeContent = String(config.code || 'function execute(args) {\n  return args.input;\n}')
  inspector.parallelTasksJson = config.tasks
    ? JSON.stringify(config.tasks, null, 2)
    : String(config.tasksJson || '[{"type":"TEMPLATE","template":"{{input}}"}]')
  inspector.parallelUseGraphBranches = Boolean(config.useGraphBranches)
}

function syncNodeFromInspector(node: any) {
  const config = configOf(node)
  const nodeType = (node?.data as WorkflowNodeData | undefined)?.nodeType
  if (nodeType === 'Agent') {
    config.agentId = inspector.agentId
    config.message = inspector.agentMessage
    config.outputVariable = inspector.agentOutputVariable || 'agentResult'
  }
  config.platformModelId = inspector.llmPlatformModelId
  config.systemPrompt = inspector.llmSystemPrompt
  config.userPrompt = inspector.llmUserPrompt
  config.prompt = inspector.llmUserPrompt
  config.method = inspector.httpMethod
  if (nodeType === 'Webhook') {
    config.url = inspector.webhookUrl
    config.payload = inspector.webhookPayload
    config.secret = inspector.webhookSecret || undefined
    config.eventType = inspector.webhookEventType
  } else if (nodeType === 'SubWorkflow') {
    config.workflowId = inspector.subWorkflowId
    config.inputsJson = inspector.subWorkflowInputsJson || undefined
    config.outputVariable = inspector.subWorkflowOutputVariable || 'subWorkflowResult'
  } else {
    config.url = inspector.httpUrl
  }
  if (nodeType === 'Condition') {
    config.variable = inspector.conditionVariable
    config.operator = inspector.conditionOperator
    config.value = inspector.conditionValue
  }
  if (nodeType === 'Switch') {
    config.variable = inspector.switchVariable
    config.operator = inspector.switchOperator
    config.defaultCase = inspector.switchDefaultCase || 'default'
    config.cases = inspector.switchCases
      .filter((item) => item.id.trim())
      .map((item) => ({ id: item.id.trim(), value: item.value }))
  }
  config.delayMs = inspector.delayMs
  config.template = inspector.templateContent
  config.knowledgeBaseId = inspector.knowledgeBaseId
  config.query = inspector.knowledgeQuery
  config.topK = inspector.knowledgeTopK
  config.sourceType = inspector.toolSourceType
  config.toolId = inspector.toolId
  config.bodyTemplate = inspector.toolBodyTemplate || undefined
  config.mcpServerId = inspector.mcpServerId
  config.mcpToolName = inspector.mcpToolName || undefined
  config.argumentsJson = inspector.toolArgumentsJson || '{}'
  config.outputVariable = inspector.toolOutputVariable || 'toolResult'
  if (nodeType === 'Loop') {
    config.mode = inspector.loopMode
    config.graphBody = inspector.loopGraphBody
    config.itemsVariable = inspector.loopItemsVariable
    config.count = inspector.loopCount
    config.code = inspector.loopCode
    config.maxIterations = inspector.loopMaxIterations
    config.outputVariable = 'loopResults'
  }
  if (nodeType === 'Code') {
    config.functionName = inspector.codeFunctionName || 'execute'
    config.code = inspector.codeContent
    config.outputVariable = 'codeResult'
  }
  if (nodeType === 'Parallel') {
    config.useGraphBranches = inspector.parallelUseGraphBranches
    if (!inspector.parallelUseGraphBranches) {
      try {
        config.tasks = JSON.parse(inspector.parallelTasksJson || '[]')
      } catch {
        config.tasks = []
      }
    } else {
      delete config.tasks
    }
    config.outputVariable = 'parallelResults'
  }
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

function hasStartNode() {
  return nodes.value.some((node) => (node.data as WorkflowNodeData | undefined)?.nodeType === 'Start')
}

function isPaletteDisabled(type: string) {
  return type === 'Start' && hasStartNode()
}

function addNodeAt(type: string, label: string, position: { x: number; y: number }) {
  if (type === 'Start' && hasStartNode()) {
    MessagePlugin.warning('工作流只能有一个开始节点')
    return
  }
  recordBeforeChange()
  const node = createFlowNode(type, label, position)
  nodes.value.push(node)
  selectNode(node)
  syncInspectorFromNode(node)
  autoSave.scheduleAutoSave()
}

function addNodeAtCanvasCenter(item: { type: string; label: string }) {
  const wrap = canvasWrapRef.value
  const rect = wrap?.getBoundingClientRect()
  const offset = (nodes.value.length % 6) * 20
  const position = screenToFlowCoordinate({
    x: (rect?.left ?? 0) + (rect?.width ?? 480) / 2 + offset,
    y: (rect?.top ?? 0) + (rect?.height ?? 320) / 2 + offset,
  })
  addNodeAt(item.type, item.label, position)
}

function onPaletteClick(item: { type: string; label: string }) {
  if (paletteDragging.value) {
    paletteDragging.value = false
    return
  }
  if (isPaletteDisabled(item.type)) return
  addNodeAtCanvasCenter(item)
}

function onPaletteDragStart(event: DragEvent, item: { type: string; label: string }) {
  if (isPaletteDisabled(item.type)) {
    event.preventDefault()
    return
  }
  paletteDragging.value = true
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
  paletteDragging.value = false
  draggedPalette.value = null
  if (!type || !label) return
  addNodeAt(type, label, screenToFlowCoordinate({ x: event.clientX, y: event.clientY }))
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

function autoLayout() {
  recordBeforeChange()
  nodes.value = autoLayoutFlow(nodes.value, edges.value)
  clearSelection()
  autoSave.scheduleAutoSave()
  void nextTick(() => fitView({ padding: 0.2 }))
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

onMounted(async () => {
  window.addEventListener('keydown', onWindowKeyDown)
  const host = props.editorHost
  try {
    knowledgeBaseOptions.value = await host.loadKnowledgeBases()
  } catch {
    knowledgeBaseOptions.value = []
  }
  try {
    toolOptions.value = await host.loadHttpTools()
  } catch {
    toolOptions.value = []
  }
  try {
    mcpServers.value = await host.loadMcpServers()
  } catch {
    mcpServers.value = []
  }
  try {
    workflows.value = await host.loadWorkflows()
  } catch {
    workflows.value = []
  }
  try {
    agents.value = await host.loadAgents()
  } catch {
    agents.value = []
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onWindowKeyDown)
})

defineExpose({
  loadDefinition,
  getDefinitionJson,
  handleUndo,
  handleRedo,
  autoLayout,
  canUndo,
  canRedo,
  autoSave,
})
</script>

<style scoped>
.editor__layout {
  display: grid;
  grid-template-columns: 196px minmax(0, 1fr) 320px;
  height: 100%;
  min-height: 0;
  background: var(--box-surface);
}

.palette,
.inspector {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--box-shell);
  border-right: 1px solid var(--box-border);
}

.inspector {
  border-right: 0;
  border-left: 1px solid var(--box-border);
  padding: 14px 16px;
  overflow: auto;
}

.palette__head,
.inspector__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-shrink: 0;
  padding: 14px 12px 8px;
}

.inspector__head {
  padding: 0 0 12px;
}

.palette__head h3,
.inspector h3 {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.palette__head p {
  margin: 4px 0 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.palette__head {
  flex-direction: column;
  align-items: flex-start;
}

.palette__scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 10px 12px;
}

.palette__group + .palette__group {
  margin-top: 12px;
}

.palette__group h4 {
  margin: 0 0 6px;
  padding: 0 4px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  letter-spacing: 0.04em;
}

.palette__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin: 0 0 4px;
  padding: 7px 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--box-ink);
  font: var(--td-font-body-medium);
  text-align: left;
  cursor: grab;
}

.palette__item :deep(.t-icon) {
  color: var(--box-muted);
}

.palette__item:hover:not(:disabled) {
  background: var(--box-hover);
}

.palette__item:disabled,
.palette__item.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.canvas-wrap {
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.canvas-wrap :deep(.vue-flow) {
  width: 100%;
  height: 100%;
  background: var(--td-bg-color-page, #f5f5f5);
}

.canvas-wrap :deep(.vue-flow__edge-path),
.canvas-wrap :deep(.vue-flow__connection-path) {
  stroke: #c5c7c9;
  stroke-width: 2;
  fill: none;
}

.canvas-wrap :deep(.vue-flow__edge.animated .vue-flow__edge-path) {
  stroke-dasharray: 36 10 !important;
  animation: wf-edge-flow 1.6s linear infinite !important;
}

.canvas-wrap :deep(.vue-flow__edge.selected .vue-flow__edge-path),
.canvas-wrap :deep(.vue-flow__edge.selectable:focus .vue-flow__edge-path),
.canvas-wrap :deep(.vue-flow__edge.selectable:focus-visible .vue-flow__edge-path) {
  stroke: var(--td-brand-color, #0052d9);
  stroke-width: 2.5;
}

.canvas-wrap :deep(.vue-flow__edge-text) {
  font-size: 11px;
  font-weight: 600;
  fill: #8a8f96;
}

@keyframes wf-edge-flow {
  to {
    stroke-dashoffset: -46;
  }
}

.canvas-wrap :deep(.vue-flow__minimap) {
  overflow: hidden;
  border: 1px solid var(--box-border);
  border-radius: 10px;
  box-shadow: var(--box-shadow-card);
}

.inspector--empty {
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
  color: var(--box-muted);
  padding: 24px 20px;
}

.inspector--empty :deep(.t-icon) {
  font-size: 28px;
}

.inspector--empty p {
  margin: 0;
  font: var(--td-font-body-medium);
  color: var(--box-ink);
}

.inspector--empty span {
  max-width: 220px;
  font: var(--td-font-body-small);
  line-height: 1.5;
}

@media (max-width: 1080px) {
  .editor__layout {
    grid-template-columns: 168px minmax(0, 1fr) 280px;
  }
}

.switch-cases {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.switch-case-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
  align-items: center;
}
</style>
