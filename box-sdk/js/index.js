/**
 * Minimal Box published Agent client.
 * @param {object} options
 * @param {string} options.baseUrl  e.g. https://api.example.com
 * @param {string} options.apiKey
 */
export class BoxClient {
  constructor({ baseUrl, apiKey }) {
    if (!baseUrl) {
      throw new Error('baseUrl is required')
    }
    if (!apiKey) {
      throw new Error('apiKey is required')
    }
    this.baseUrl = String(baseUrl).replace(/\/$/, '')
    this.apiKey = apiKey
  }

  async chat(agentId, message, options = {}) {
    const {
      stream = false,
      onDelta,
      onCitations,
      onToolStart,
      onToolDelta,
      onToolEnd,
      onToolConfirm,
      history = [],
      toolConfirmationToken,
    } = options
    const response = await fetch(`${this.baseUrl}/api/v1/published/agents/${agentId}/chat`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json',
        Accept: stream ? 'text/event-stream, application/json' : 'application/json',
      },
      body: JSON.stringify({
        message,
        stream,
        history: this.#normalizeHistory(history),
        toolConfirmationToken,
      }),
    })
    if (!stream) {
      const payload = await response.json()
      if (!response.ok || payload.code !== 0) {
        throw new Error(payload.message || `HTTP ${response.status}`)
      }
      return {
        content: payload.data?.content || '',
        citations: payload.data?.citations || [],
        executionId: payload.data?.executionId,
      }
    }
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Streaming is not supported in this environment')
    }
    const decoder = new TextDecoder()
    let buffer = ''
    let content = ''
    while (true) {
      const { done, value } = await reader.read()
      if (value) {
        buffer += decoder.decode(value, { stream: true })
      }
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        const rawEvent = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        for (const line of rawEvent.split('\n')) {
          if (!line.startsWith('data:')) continue
          const payload = line.slice(5).trim()
          if (!payload) continue
          let event
          try {
            event = JSON.parse(payload)
          } catch {
            continue
          }
          if (event.type === 'delta' && event.content) {
            content += event.content
            onDelta?.(event.content)
          }
          if (event.type === 'citations' && event.content) {
            try {
              onCitations?.(JSON.parse(event.content))
            } catch {
              // ignore malformed citations
            }
          }
          if (event.type === 'tool.start') {
            onToolStart?.(this.#parseToolPayload(event.content))
          }
          if (event.type === 'tool.delta') {
            onToolDelta?.(this.#parseToolPayload(event.content))
          }
          if (event.type === 'tool.end') {
            onToolEnd?.(this.#parseToolPayload(event.content))
          }
          if (event.type === 'tool.confirm') {
            onToolConfirm?.(this.#parseToolConfirmPayload(event.content))
          }
          if (event.type === 'error') {
            throw new Error(event.message || 'stream error')
          }
          if (event.type === 'done') {
            return content
          }
        }
        boundary = buffer.indexOf('\n\n')
      }
      if (done) {
        return content
      }
    }
  }

  async embedConfig(agentId) {
    const response = await fetch(`${this.baseUrl}/api/v1/published/agents/${agentId}/embed-config`)
    const payload = await response.json()
    if (!response.ok || payload.code !== 0) {
      throw new Error(payload.message || `HTTP ${response.status}`)
    }
    return payload.data
  }

  #normalizeHistory(history) {
    if (!Array.isArray(history)) return []
    return history
      .filter((item) => item?.content?.trim())
      .map((item) => ({
        role: item.role,
        content: String(item.content).trim(),
      }))
  }

  #parseToolPayload(raw) {
    if (!raw) return null
    try {
      const parsed = JSON.parse(raw)
      return parsed?.toolKey ? parsed : null
    } catch {
      return null
    }
  }

  #parseToolConfirmPayload(raw) {
    if (!raw) return null
    try {
      const parsed = JSON.parse(raw)
      return parsed?.confirmationToken && parsed?.toolKey ? parsed : null
    } catch {
      return null
    }
  }
}
