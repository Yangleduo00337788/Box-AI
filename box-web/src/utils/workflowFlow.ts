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
  { type: 'HTTP', label: 'HTTP', icon: 'internet' },
  { type: 'Condition', label: '条件', icon: 'chart-bubble' },
  { type: 'Delay', label: '延迟', icon: 'time' },
  { type: 'Variable', label: '变量', icon: 'data' },
  { type: 'Template', label: '模板', icon: 'file-1' },
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
  if (type === 'Delay') {
    config.delayMs = 1000
  }
  if (type === 'HTTP') {
    config.method = 'GET'
    config.url = 'https://'
  }
  return {
    id,
    type: 'workflow',
    position,
    data: { label, nodeType: type, config },
  }
}
