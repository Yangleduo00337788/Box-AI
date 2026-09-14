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
  theme: string
  dismissible: boolean
  status: string
  sortOrder: number
  startsAt?: string
  endsAt?: string
  createdAt?: string
}

export function fetchOpsPlacements() {
  return http.get<Result<OpsPlacementVO[]>>('/ops/placements')
}

export function createOpsPlacement(payload: {
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
    slot: OpsSlot
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
