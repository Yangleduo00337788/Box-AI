import http, { type Result } from './http'

export type OpsSlot = 'CHAT_HOME' | 'GLOBAL_ALERT' | 'CHAT_BANNER' | 'CHAT_AD'
export type OpsKind = 'ANNOUNCEMENT' | 'PROMO' | 'BANNER' | 'AD'

export interface OpsPlacementVO {
  id: number
  slot: OpsSlot
  kind: OpsKind
  title: string
  body?: string
  linkUrl?: string
  linkLabel?: string
  iconName?: string
  iconUrl?: string
  iconSvg?: string
  imageUrl?: string
  theme: 'info' | 'success' | 'warning' | 'error'
  dismissible: boolean
  status: string
  sortOrder: number
  startsAt?: string
  endsAt?: string
  createdAt?: string
}

export function fetchOpsPlacements(slot?: OpsSlot) {
  return http.get<Result<OpsPlacementVO[]>>('/ops/placements', { params: slot ? { slot } : undefined })
}
