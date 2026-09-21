/** Agent 配置保存：变量 JSON 校验（AgentBuilder saveConfig 使用） */
export function parseAgentVariablesJson(raw: string): unknown[] {
  let variables: unknown
  try {
    variables = JSON.parse(raw || '[]')
  } catch {
    throw new Error('变量 JSON 格式无效，请使用数组格式')
  }
  if (!Array.isArray(variables)) {
    throw new Error('变量 JSON 格式无效，请使用数组格式')
  }
  return variables
}

export function parseMcpToolCount(catalog?: string) {
  if (!catalog) return 0
  try {
    const items = JSON.parse(catalog)
    return Array.isArray(items) ? items.length : 0
  } catch {
    return 0
  }
}
