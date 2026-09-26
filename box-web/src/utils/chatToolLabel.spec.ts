import { describe, expect, it } from 'vitest'
import { resolveToolInvokeLabel } from './chatToolLabel'

describe('resolveToolInvokeLabel', () => {
  it('prefers matching plugin title and category', () => {
    expect(
      resolveToolInvokeLabel('plugin-12-search', [
        { id: 12, title: 'Search', category: 'tools' },
      ]),
    ).toBe('工具 · Search')
  })

  it('formats mcp and workflow keys', () => {
    expect(resolveToolInvokeLabel('mcp_list_files')).toBe('MCP · list files')
    expect(resolveToolInvokeLabel('workflow_refund')).toBe('工作流 · refund')
    expect(resolveToolInvokeLabel('echo')).toBe('工具 · echo')
  })
})
