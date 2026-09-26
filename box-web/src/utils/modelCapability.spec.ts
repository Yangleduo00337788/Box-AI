import { describe, expect, it } from 'vitest'
import { classifyModelChatKind, isMultimodalCapability, modelCapabilityLabel } from './modelCapability'

describe('modelCapability', () => {
  it('classifies common model families', () => {
    expect(classifyModelChatKind('text-embedding-3-small')).toBe('embedding')
    expect(classifyModelChatKind('qwen-reranker')).toBe('rerank')
    expect(classifyModelChatKind('paddleocr')).toBe('ocr')
    expect(classifyModelChatKind('whisper-1')).toBe('audio')
    expect(classifyModelChatKind('kling-v1')).toBe('video')
    expect(classifyModelChatKind('flux-schnell')).toBe('image')
    expect(classifyModelChatKind('qwen2.5-vl')).toBe('vision')
    expect(classifyModelChatKind('qwen2.5-7b-instruct')).toBe('text')
    expect(classifyModelChatKind()).toBe('text')
  })

  it('labels multimodal kinds', () => {
    expect(modelCapabilityLabel('ocr')).toBe('OCR')
    expect(isMultimodalCapability('text')).toBe(false)
    expect(isMultimodalCapability('vision')).toBe(true)
  })
})
