export function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

export function nowIso(): string {
  return new Date().toISOString()
}

export function parseQuery(url: string): Record<string, string> {
  const q = url.includes('?') ? url.split('?')[1] : ''
  const out: Record<string, string> = {}
  for (const part of q.split('&')) {
    if (!part) continue
    const [k, v] = part.split('=')
    out[decodeURIComponent(k)] = decodeURIComponent(v || '')
  }
  return out
}
