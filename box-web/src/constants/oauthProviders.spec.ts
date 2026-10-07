import { describe, expect, it } from 'vitest'
import { mergeOAuthProviders } from './oauthProviders'

describe('mergeOAuthProviders', () => {
  it('keeps GitHub and Google for personal portal', () => {
    const merged = mergeOAuthProviders(
      [{ provider: 'github', displayName: 'GitHub', enabled: true, authorizePath: '/api/v1/auth/oauth/github/authorize' }],
      'personal',
    )
    expect(merged.map((item) => item.provider)).toEqual(['github', 'google'])
    expect(merged.find((item) => item.provider === 'github')?.enabled).toBe(true)
    expect(merged.find((item) => item.provider === 'google')?.enabled).toBe(false)
  })

  it('hides third-party login on enterprise portal', () => {
    const merged = mergeOAuthProviders(
      [{ provider: 'github', displayName: 'GitHub', enabled: true, authorizePath: '/api/v1/auth/oauth/github/authorize' }],
      'enterprise',
    )
    expect(merged).toEqual([])
  })
})
