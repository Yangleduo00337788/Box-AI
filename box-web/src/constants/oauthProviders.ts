import type { OAuthProviderVO } from '@/api/auth'

export const OAUTH_PROVIDER_ORDER = ['github', 'google'] as const

export const OAUTH_PROVIDER_LABELS: Record<string, string> = {
  github: 'GitHub',
  google: 'Google',
}

export function mergeOAuthProviders(remote: OAuthProviderVO[] | undefined): OAuthProviderVO[] {
  const remoteMap = new Map((remote || []).map((item) => [item.provider, item]))
  return OAUTH_PROVIDER_ORDER.map((provider) => {
    const found = remoteMap.get(provider)
    return {
      provider,
      displayName: found?.displayName || OAUTH_PROVIDER_LABELS[provider] || provider,
      enabled: Boolean(found?.enabled),
      authorizePath: found?.authorizePath || `/api/v1/auth/oauth/${provider}/authorize`,
    }
  })
}
