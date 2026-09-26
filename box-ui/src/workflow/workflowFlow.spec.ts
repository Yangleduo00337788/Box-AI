import { describe, expect, it } from 'vitest'
import { branchLabelFromHandle } from './workflowFlow'

describe('branchLabelFromHandle', () => {
  it('prefers an explicit label', () => {
    expect(branchLabelFromHandle('true', '  自定义  ')).toBe('自定义')
  })

  it('maps known handles to display labels', () => {
    expect(branchLabelFromHandle('true')).toBe('True')
    expect(branchLabelFromHandle('FALSE')).toBe('False')
    expect(branchLabelFromHandle('body')).toBe('循环体')
    expect(branchLabelFromHandle('next')).toBe('完成后')
    expect(branchLabelFromHandle('default')).toBe('默认')
    expect(branchLabelFromHandle('branch-0')).toBe('分支 1')
    expect(branchLabelFromHandle('other')).toBe('other')
    expect(branchLabelFromHandle(null)).toBeUndefined()
  })
})
