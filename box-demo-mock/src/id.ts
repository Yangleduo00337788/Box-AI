const counters: Record<string, number> = {}

export function nextId(key: string, start = 9000): number {
  counters[key] = (counters[key] ?? start) + 1
  return counters[key]
}
