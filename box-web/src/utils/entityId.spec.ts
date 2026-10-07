import { describe, expect, it } from 'vitest'
import { sameEntityId } from './entityId'

describe('sameEntityId', () => {
  it('treats numeric and string snowflake ids as equal', () => {
    expect(sameEntityId(5, '5')).toBe(true)
    expect(sameEntityId('1948583928472936448', '1948583928472936448')).toBe(true)
    expect(sameEntityId(1, 2)).toBe(false)
  })
})
