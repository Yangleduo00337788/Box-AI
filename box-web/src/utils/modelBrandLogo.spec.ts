import { describe, expect, it } from 'vitest'
import { resolveModelBrandLogo } from './modelBrandLogo'

describe('resolveModelBrandLogo', () => {
  it('matches provider or model family', () => {
    expect(resolveModelBrandLogo('gpt-4o', 'GPT-4o', 'OpenAI')).toBeTruthy()
    expect(resolveModelBrandLogo('qwen2.5-7b', '通义千问', '阿里云')).toBeTruthy()
    expect(resolveModelBrandLogo('deepseek-chat')).toBeTruthy()
    expect(resolveModelBrandLogo('unknown-local-model', '自建', '自建')).toBeUndefined()
  })
})
