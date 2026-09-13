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

  async chat(agentId, message, { stream = false, onDelta } = {}) {
    const response = await fetch(`${this.baseUrl}/api/v1/published/agents/${agentId}/chat`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json',
        Accept: stream ? 'text/event-stream, application/json' : 'application/json',
      },
      body: JSON.stringify({ message, stream }),
    })
    if (!stream) {
      const payload = await response.json()
      if (!response.ok || payload.code !== 0) {
        throw new Error(payload.message || `HTTP ${response.status}`)
      }
      return payload.data?.content || ''
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
          const event = JSON.parse(line.slice(5).trim())
          if (event.type === 'delta' && event.content) {
            content += event.content
            onDelta?.(event.content)
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
}
