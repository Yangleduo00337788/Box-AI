const AVATAR_COLORS = ['#f9a8d4', '#93c5fd', '#86efac', '#fcd34d', '#c4b5fd', '#fda4af']

export function formatRelativeTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''

  const diffMs = Date.now() - date.getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时`

  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天`

  const months = Math.floor(days / 30)
  return `${months} 个月`
}

export function getAvatarColor(seed: string) {
  let hash = 0
  for (let i = 0; i < seed.length; i += 1) {
    hash = seed.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length]
}
