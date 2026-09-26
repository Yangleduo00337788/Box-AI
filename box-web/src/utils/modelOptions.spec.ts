import { describe, expect, it } from 'vitest'
import { groupedPlatformModelOptions } from './modelOptions'
import type { PlatformModelVO } from '@/api/platform'

function model(partial: Partial<PlatformModelVO> & Pick<PlatformModelVO, 'id' | 'modelName' | 'status'>): PlatformModelVO {
  return {
    providerId: 1,
    providerName: 'OpenAI',
    modelCode: 'm',
    ...partial,
  }
}

describe('groupedPlatformModelOptions', () => {
  it('groups listed models and drops disabled ones', () => {
    const options = groupedPlatformModelOptions([
      model({ id: 1, modelName: 'GPT-4o', status: 1, providerName: 'OpenAI' }),
      model({ id: 2, modelName: 'Qwen', status: 1, providerName: '阿里云' }),
      model({ id: 3, modelName: 'Hidden', status: 0, providerName: 'OpenAI' }),
      model({ id: 4, modelName: 'Local', status: 1, providerName: '' }),
    ])
    expect(options).toEqual([
      { group: 'OpenAI', children: [{ label: 'GPT-4o', value: 1 }] },
      { group: '阿里云', children: [{ label: 'Qwen', value: 2 }] },
      { group: '其他服务商', children: [{ label: 'Local', value: 4 }] },
    ])
  })
})
