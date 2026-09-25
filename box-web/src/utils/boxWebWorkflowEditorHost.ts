import { listAgents } from '@/api/agent'
import { listKnowledgeBases } from '@/api/knowledge'
import { listMcpServers } from '@/api/mcp'
import { listTools } from '@/api/tool'
import { listWorkflows, updateWorkflowDefinition } from '@/api/workflow'
import type { WorkflowEditorHost } from '@box/ui/workflow/workflowEditorHost'

export const boxWebWorkflowEditorHost: WorkflowEditorHost = {
  async loadKnowledgeBases() {
    const { data } = await listKnowledgeBases()
    return (data.data || []).map((item) => ({ label: item.name, value: item.id }))
  },
  async loadHttpTools() {
    const { data } = await listTools()
    return (data.data || [])
      .filter((item) => item.type === 'HTTP' && item.status === 1)
      .map((item) => ({ label: item.name, value: item.id }))
  },
  async loadMcpServers() {
    const { data } = await listMcpServers()
    return (data.data || []).map((item) => ({
      id: item.id,
      name: item.name,
      status: item.status,
      toolCatalogJson: item.toolCatalogJson,
    }))
  },
  async loadWorkflows() {
    const { data } = await listWorkflows()
    return (data.data || []).map((item) => ({ id: item.id, name: item.name }))
  },
  async loadAgents() {
    const { data } = await listAgents()
    return (data.data || []).map((item) => ({ id: item.id, name: item.name }))
  },
  async saveDefinition(workflowId, definitionJson) {
    await updateWorkflowDefinition(workflowId, definitionJson)
  },
}
