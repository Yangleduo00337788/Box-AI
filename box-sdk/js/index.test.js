import { test } from 'node:test'
import assert from 'node:assert/strict'
import { BoxClient } from './index.js'

test('constructor requires baseUrl and apiKey and strips trailing slash', () => {
  assert.throws(() => new BoxClient({ baseUrl: '', apiKey: 'k' }), /baseUrl is required/)
  assert.throws(() => new BoxClient({ baseUrl: 'https://api.example.com', apiKey: '' }), /apiKey is required/)
  const client = new BoxClient({ baseUrl: 'https://api.example.com/', apiKey: 'secret' })
  assert.equal(client.baseUrl, 'https://api.example.com')
})

test('chat returns json payload and drops empty history', async () => {
  const client = new BoxClient({ baseUrl: 'https://api.example.com', apiKey: 'secret' })
  const originalFetch = globalThis.fetch
  globalThis.fetch = async (url, init) => {
    assert.equal(url, 'https://api.example.com/api/v1/published/agents/9/chat')
    assert.equal(init.headers.Authorization, 'Bearer secret')
    const body = JSON.parse(init.body)
    assert.equal(body.message, 'hi')
    assert.deepEqual(body.history, [{ role: 'user', content: 'keep' }])
    return {
      ok: true,
      json: async () => ({
        code: 0,
        data: { content: 'hello', citations: [{ id: 1 }], executionId: 'ex-1' },
      }),
    }
  }
  try {
    const result = await client.chat(9, 'hi', {
      history: [{ role: 'user', content: 'keep' }, { role: 'assistant', content: '  ' }],
    })
    assert.equal(result.content, 'hello')
    assert.equal(result.executionId, 'ex-1')
    assert.equal(result.citations.length, 1)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('chat throws business error from payload', async () => {
  const client = new BoxClient({ baseUrl: 'https://api.example.com', apiKey: 'secret' })
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => ({
    ok: true,
    status: 200,
    json: async () => ({ code: 1, message: 'quota exceeded' }),
  })
  try {
    await assert.rejects(() => client.chat(1, 'hi'), /quota exceeded/)
  } finally {
    globalThis.fetch = originalFetch
  }
})
