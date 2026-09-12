import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import { confirmAgentTool } from '@/api/agent'

export interface ToolConfirmPayload {
  confirmationToken: string
  toolKey: string
  toolName?: string
  arguments?: Record<string, unknown>
}

export async function promptToolConfirmation(agentId: number, payload: ToolConfirmPayload): Promise<boolean> {
  const argsText = payload.arguments ? JSON.stringify(payload.arguments, null, 2) : ''
  const body = [
    `工具「${payload.toolName || payload.toolKey}」被标记为危险操作，需要您确认后才会执行。`,
    argsText ? `\n参数：\n${argsText}` : '',
  ].join('')

  return new Promise((resolve) => {
    const dialog = DialogPlugin.confirm({
      header: '确认执行工具',
      body,
      confirmBtn: '确认执行',
      cancelBtn: '取消',
      onConfirm: async () => {
        try {
          const { data } = await confirmAgentTool(agentId, payload.confirmationToken)
          const output = data.data?.output ?? ''
          MessagePlugin.success(output ? `工具已执行：${output}` : '工具已执行')
          dialog.hide()
          resolve(true)
        } catch {
          MessagePlugin.error('工具确认失败')
          resolve(false)
        }
      },
      onClose: () => resolve(false),
      onCancel: () => resolve(false),
    })
  })
}
