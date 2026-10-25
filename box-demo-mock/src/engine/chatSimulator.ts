const FAQ = `企业版能力（演示模拟）：
- 多工作空间与 RBAC
- 操作审计与 API Key 治理
- 套餐额度与全链路 Trace
- 知识库 RAG 与工具 / MCP 调用`

export function buildAssistantReply(userText: string): string {
  const q = userText.trim().toLowerCase()
  if (!q) {
    return '你好，我是 Box 演示环境的客服助手。可以直接问我企业版、知识库、工作流或套餐相关问题。'
  }
  if (q.includes('价格') || q.includes('套餐') || q.includes('计费')) {
    return '当前租户为「团队版」演示：月 Token 约 500 万，席位 20。个人版/企业版可在「设置 → 套餐与额度」查看模拟数据。'
  }
  if (q.includes('工作流') || q.includes('workflow')) {
    return '你可以在「插件市场 → 我的工作流」打开「客服分流」示例：含知识库检索、LLM 与 Switch 分支。支持校验、调试与发布（演示态会立即成功）。'
  }
  if (q.includes('知识') || q.includes('rag') || q.includes('文档')) {
    return '演示知识库「产品 FAQ」含 PDF/MD/CSV 样例。上传后会走 PROCESSING → READY；Test Answer 与对话引用均为模拟，但界面流程与线上一致。'
  }
  if (q.includes('工具') || q.includes('mcp') || q.includes('订单')) {
    return '已绑定 HTTP 工具「查询订单」与 MCP「CRM」。发送消息时可能触发 tool.start / tool.end 事件（演示模拟），可在气泡下方看到工具运行条。'
  }
  return `${FAQ}\n\n—— 针对你的问题：「${userText.slice(0, 200)}」`
}

export function demoToolEvents() {
  return [
    { type: 'tool.start' as const, content: JSON.stringify({ toolKey: 'http_order', toolName: '查询订单', status: 'running' }) },
    { type: 'tool.end' as const, content: JSON.stringify({ toolKey: 'http_order', toolName: '查询订单', output: '{"orderId":"DEMO-10086","status":"DELIVERED"}', status: 'success' }) },
  ]
}
