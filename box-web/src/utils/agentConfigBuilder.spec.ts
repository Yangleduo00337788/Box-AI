import { describe, expect, it } from 'vitest'
import { parseAgentVariablesJson, parseMcpToolCount } from './agentConfigBuilder'

describe('agentConfigBuilder', () => {
  it('parseAgentVariablesJson accepts array JSON', () => {
    expect(parseAgentVariablesJson('[{"name":"x"}]')).toEqual([{ name: 'x' }])
    expect(parseAgentVariablesJson('')).toEqual([])
  })

  it('parseAgentVariablesJson rejects non-array', () => {
    expect(() => parseAgentVariablesJson('{"a":1}')).toThrow(/数组/)
    expect(() => parseAgentVariablesJson('not-json')).toThrow(/数组/)
  })

  it('parseMcpToolCount counts catalog tools', () => {
    expect(parseMcpToolCount('[{"name":"a"},{"name":"b"}]')).toBe(2)
    expect(parseMcpToolCount('')).toBe(0)
    expect(parseMcpToolCount('invalid')).toBe(0)
  })
})
