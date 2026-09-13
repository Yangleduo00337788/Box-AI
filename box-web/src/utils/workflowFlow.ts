import type { Edge, Node } from '@vue-flow/core'
import type { WorkflowDefinition } from '@/api/workflow'

export interface WorkflowNodeData {
  label: string
  nodeType: string
  config: Record<string, unknown>
}

export const PALETTE_ITEMS = [
  { type: 'Start', label: '开始', icon: 'play-circle-stroke' },
  { type: 'LLM', label: 'LLM', icon: 'cpu' },
  { type: 'Agent', label: 'Agent', icon: 'user-talk' },
  { type: 'Knowledge', label: '知识库', icon: 'book' },
  { type: 'HTTP', label: 'HTTP', icon: 'internet' },
  { type: 'Webhook', label: 'Webhook', icon: 'link' },
  { type: 'SubWorkflow', label: '子工作流', icon: 'tree-square-dot' },
  { type: 'Tool', label: '工具', icon: 'tools' },
  { type: 'Condition', label: '条件', icon: 'chart-bubble' },
  { type: 'Switch', label: '分支', icon: 'tree-square-dot-vertical' },
  { type: 'Delay', label: '延迟', icon: 'time' },
  { type: 'Variable', label: '变量', icon: 'data' },
  { type: 'Template', label: '模板', icon: 'file-1' },
  { type: 'Loop', label: '循环', icon: 'refresh' },
  { type: 'Code', label: '代码', icon: 'code' },
  { type: 'Parallel', label: '并行', icon: 'fork' },
  { type: 'Output', label: '输出', icon: 'logout' },
] as const

export function defaultWorkflowDefinition(): WorkflowDefinition {
  return {
    nodes: [
      {
        id: 'start',
        type: 'Start',
        label: '开始',
        position: { x: 80, y: 160 },
        config: {},
      },
      {
        id: 'llm-1',
        type: 'LLM',
        label: 'LLM',
        position: { x: 320, y: 160 },
        config: { prompt: '{{input.message}}' },
      },
      {
        id: 'output-1',
        type: 'Output',
        label: '输出',
        position: { x: 560, y: 160 },
        config: {},
      },
    ],
    edges: [
      { id: 'e-start-llm', source: 'start', target: 'llm-1' },
      { id: 'e-llm-output', source: 'llm-1', target: 'output-1' },
    ],
    variables: [],
  }
}

export function definitionToFlow(definition: WorkflowDefinition): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = (definition.nodes || []).map((node) => ({
    id: node.id,
    type: 'workflow',
    position: node.position || { x: 0, y: 0 },
    data: {
      label: node.label || node.type,
      nodeType: node.type,
      config: node.config || node.data || {},
    },
  }))

  const edges: Edge[] = (definition.edges || []).map((edge, index) => ({
    id: edge.id || `edge-${index}-${edge.source}-${edge.target}`,
    source: edge.source,
    target: edge.target,
    sourceHandle: edge.sourceHandle,
    targetHandle: edge.targetHandle,
    label: edge.label,
    animated: true,
    type: 'smoothstep',
    selectable: true,
    focusable: true,
  }))

  return { nodes, edges }
}

export function flowToDefinition(
  nodes: Node[],
  edges: Edge[],
  variables: WorkflowDefinition['variables'] = [],
): WorkflowDefinition {
  return {
    nodes: nodes.map((node) => {
      const data = (node.data ?? { label: node.id, nodeType: 'Template', config: {} }) as WorkflowNodeData
      return {
        id: node.id,
        type: data.nodeType,
        label: data.label,
        position: { x: node.position.x, y: node.position.y },
        config: data.config || {},
      }
    }),
    edges: edges.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle || undefined,
      targetHandle: edge.targetHandle || undefined,
      label: typeof edge.label === 'string' ? edge.label : undefined,
    })),
    variables,
  }
}

export function createFlowNode(type: string, label: string, position: { x: number; y: number }): Node {
  const id = `${type.toLowerCase()}-${Date.now()}`
  const config: Record<string, unknown> = {}
  if (type === 'LLM') {
    config.prompt = '{{input.message}}'
  }
  if (type === 'Agent') {
    config.message = '{{input.message}}'
    config.outputVariable = 'agentResult'
  }
  if (type === 'Delay') {
    config.delayMs = 1000
  }
  if (type === 'HTTP') {
    config.method = 'GET'
    config.url = 'https://'
  }
  if (type === 'Webhook') {
    config.url = 'https://'
    config.payload = '{{input}}'
    config.eventType = 'workflow.event'
  }
  if (type === 'SubWorkflow') {
    config.outputVariable = 'subWorkflowResult'
  }
  if (type === 'Loop') {
    config.mode = 'FOREACH'
    config.itemsVariable = 'items'
    config.itemVariable = 'loopItem'
    config.indexVariable = 'loopIndex'
    config.outputVariable = 'loopResults'
    config.maxIterations = 100
  }
  if (type === 'Code') {
    config.functionName = 'execute'
    config.outputVariable = 'codeResult'
    config.code = 'function execute(args) {\n  return args.input;\n}'
  }
  if (type === 'Parallel') {
    config.outputVariable = 'parallelResults'
    config.tasks = [
      { type: 'TEMPLATE', template: '{{input}}' },
    ]
  }
  if (type === 'Switch') {
    config.variable = 'input.type'
    config.operator = 'equals'
    config.defaultCase = 'default'
    config.cases = [
      { id: 'case1', value: '' },
      { id: 'case2', value: '' },
    ]
  }
  return {
    id,
    type: 'workflow',
    position,
    data: { label, nodeType: type, config },
  }
}

export function autoLayoutFlow<T extends { id: string; position: { x: number; y: number } }>(
  nodes: T[],
  edges: Array<{ source: string; target: string }>,
  options?: { columnWidth?: number; rowHeight?: number; originX?: number; originY?: number },
): T[] {
  if (!nodes.length) {
    return nodes
  }
  const columnWidth = options?.columnWidth ?? 280
  const rowHeight = options?.rowHeight ?? 140
  const originX = options?.originX ?? 80
  const originY = options?.originY ?? 80
  const incoming = new Map<string, number>()
  const outgoing = new Map<string, string[]>()
  for (const node of nodes) {
    incoming.set(node.id, 0)
    outgoing.set(node.id, [])
  }
  for (const edge of edges) {
    if (!incoming.has(edge.target) || !outgoing.has(edge.source)) {
      continue
    }
    incoming.set(edge.target, (incoming.get(edge.target) || 0) + 1)
    outgoing.get(edge.source)!.push(edge.target)
  }
  const layer = new Map<string, number>()
  const queue = nodes.filter((node) => (incoming.get(node.id) || 0) === 0).map((node) => node.id)
  for (const id of queue) {
    layer.set(id, 0)
  }
  while (queue.length) {
    const id = queue.shift()!
    const current = layer.get(id) || 0
    for (const next of outgoing.get(id) || []) {
      layer.set(next, Math.max(layer.get(next) || 0, current + 1))
      const remain = (incoming.get(next) || 1) - 1
      incoming.set(next, remain)
      if (remain === 0) {
        queue.push(next)
      }
    }
  }
  const columns = new Map<number, string[]>()
  for (const node of nodes) {
    const index = layer.get(node.id) ?? 0
    const column = columns.get(index) || []
    column.push(node.id)
    columns.set(index, column)
  }
  const positionById = new Map<string, { x: number; y: number }>()
  for (const [index, ids] of columns.entries()) {
    ids.forEach((id, row) => {
      positionById.set(id, {
        x: originX + index * columnWidth,
        y: originY + row * rowHeight,
      })
    })
  }
  return nodes.map((node) => {
    const position = positionById.get(node.id) || node.position
    return { ...node, position }
  })
}

