import { describe, expect, it } from 'vitest'
import { classifyModelChatKind, isOcrOrVisionModel } from './modelCapability'

describe('modelCapability', () => {
  it('classifies embedding ocr vision and text', () => {
    expect(classifyModelChatKind('text-embedding-3-small')).toBe('embedding')
    expect(classifyModelChatKind('paddleocr')).toBe('ocr')
    expect(classifyModelChatKind('qwen2.5-vl')).toBe('vision')
    expect(classifyModelChatKind('qwen2.5-7b-instruct')).toBe('text')
  })

  it('isOcrOrVisionModel is true for ocr and vision only', () => {
    expect(isOcrOrVisionModel('paddleocr')).toBe(true)
    expect(isOcrOrVisionModel('qwen2-vl')).toBe(true)
    expect(isOcrOrVisionModel('qwen2.5-7b-instruct')).toBe(false)
  })
})
