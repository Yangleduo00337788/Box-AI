import type { OAuthProviderVO } from '@/api/auth'
import type { PortalType } from '@/constants/portal'

export const PERSONAL_OAUTH_PROVIDERS = ['github', 'google'] as const
export const OAUTH_PROVIDER_ORDER = [...PERSONAL_OAUTH_PROVIDERS] as const

export const OAUTH_PROVIDER_LABELS: Record<string, string> = {
  github: 'GitHub',
  google: 'Google',
}

export function mergeOAuthProviders(
  remote: OAuthProviderVO[] | undefined,
  portal?: PortalType,
): OAuthProviderVO[] {
  if (portal === 'enterprise') {
    return []
  }
  const remoteMap = new Map((remote || []).map((item) => [item.provider, item]))
  return PERSONAL_OAUTH_PROVIDERS.map((provider) => {
    const found = remoteMap.get(provider)
    return {
      provider,
      displayName: found?.displayName || OAUTH_PROVIDER_LABELS[provider] || provider,
      enabled: Boolean(found?.enabled),
      authorizePath: found?.authorizePath || `/api/v1/auth/oauth/${provider}/authorize`,
    }
  })
}
