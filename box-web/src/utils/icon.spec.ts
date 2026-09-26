import { describe, expect, it } from 'vitest'
import { resolveTIconName } from './icon'

describe('resolveTIconName', () => {
  it('strips filled suffix so TDesign uses stroke icons', () => {
    expect(resolveTIconName('user-filled')).toBe('user')
    expect(resolveTIconName('dashboard')).toBe('dashboard')
    expect(resolveTIconName('')).toBe('')
  })
})
