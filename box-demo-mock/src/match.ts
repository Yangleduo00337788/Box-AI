export function matchRoute(pattern: string, path: string): Record<string, string> | null {
  const patternParts = pattern.split('/').filter(Boolean)
  const pathParts = path.split('/').filter(Boolean)
  if (patternParts.length !== pathParts.length) {
    return null
  }
  const params: Record<string, string> = {}
  for (let i = 0; i < patternParts.length; i++) {
    const p = patternParts[i]
    const v = pathParts[i]
    if (p.startsWith(':')) {
      params[p.slice(1)] = decodeURIComponent(v)
    } else if (p !== v) {
      return null
    }
  }
  return params
}

export function stripApiPrefix(url: string, prefix: string): string {
  const normalized = url.split('?')[0]
  if (normalized.startsWith(prefix)) {
    return normalized.slice(prefix.length) || '/'
  }
  return normalized
}
