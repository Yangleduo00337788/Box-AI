export interface WorkflowDefinition {
  nodes: Array<{
    id: string
    type: string
    label?: string
    position?: { x: number; y: number }
    config?: Record<string, unknown>
    data?: Record<string, unknown>
  }>
  edges: Array<{
    id?: string
    source: string
    target: string
    sourceHandle?: string
    targetHandle?: string
    label?: string
  }>
  variables: Array<Record<string, unknown>>
}
