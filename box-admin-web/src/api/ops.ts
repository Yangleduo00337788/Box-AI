import http, { type Result } from './http'

export type OpsAudience = 'C' | 'B'
export type OpsSlot = 'CHAT_HOME' | 'GLOBAL_ALERT' | 'CHAT_BANNER' | 'CHAT_AD'
export type AdminOpsSlot = 'ADMIN_HEADER' | 'ADMIN_BANNER'
export type OpsSlotAny = OpsSlot | AdminOpsSlot
export type OpsKind = 'ANNOUNCEMENT' | 'PROMO' | 'BANNER' | 'AD'

export interface OpsPlacementVO {
  id: number
  audience: OpsAudience
  slot: OpsSlotAny
  kind: OpsKind
  title: string
  body?: string
  linkUrl?: string
  linkLabel?: string
  iconName?: string
  iconUrl?: string
  iconSvg?: string
  imageUrl?: string
  theme: string
  dismissible: boolean
  status: string
  sortOrder: number
  startsAt?: string
  endsAt?: string
  createdAt?: string
  impressions?: number
  clicks?: number
}

export function fetchOpsPlacements() {
  return http.get<Result<OpsPlacementVO[]>>('/ops/placements')
}

export function fetchActiveOpsPlacements(audience: OpsAudience = 'B', slot?: AdminOpsSlot | OpsSlot) {
  const params: Record<string, string> = { audience }
  if (slot) params.slot = slot
  return http.get<Result<OpsPlacementVO[]>>('/ops/placements/active', { params })
}

export function createOpsPlacement(payload: {
  audience?: OpsAudience
  slot: OpsSlotAny
  kind: OpsKind
  title: string
  body?: string
  linkUrl?: string
  linkLabel?: string
  iconName?: string
  iconUrl?: string
  iconSvg?: string
  imageUrl?: string
  theme?: string
  dismissible?: boolean
  sortOrder?: number
  startsAt?: string
  endsAt?: string
}) {
  return http.post<Result<OpsPlacementVO>>('/ops/placements', payload)
}

export function updateOpsPlacement(
  id: number,
  payload: Partial<{
    audience: OpsAudience
    slot: OpsSlotAny
    kind: OpsKind
    title: string
    body: string
    linkUrl: string
    linkLabel: string
    iconName: string
    iconUrl: string
    iconSvg: string
    imageUrl: string
    theme: string
    dismissible: boolean
    status: string
    sortOrder: number
    startsAt: string
    endsAt: string
  }>,
) {
  return http.put<Result<OpsPlacementVO>>(`/ops/placements/${id}`, payload)
}

export function deleteOpsPlacement(id: number) {
  return http.delete<Result<void>>(`/ops/placements/${id}`)
}
