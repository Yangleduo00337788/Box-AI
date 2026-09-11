import http, { type Result } from './http'

export interface SidebarPinsVO {
  pinnedAgentIds: number[]
  pinnedConversationIds: number[]
}

export function fetchSidebarPins() {
  return http.get<Result<SidebarPinsVO>>('/sidebar/pins')
}

export function replaceSidebarPins(payload: SidebarPinsVO) {
  return http.put<Result<SidebarPinsVO>>('/sidebar/pins', payload)
}

export interface SidebarSelectionVO {
  selectedAgentId: number | null
}

export function fetchSidebarSelection() {
  return http.get<Result<SidebarSelectionVO>>('/sidebar/selection')
}

export function updateSidebarSelection(selectedAgentId: number | null) {
  return http.put<Result<SidebarSelectionVO>>('/sidebar/selection', { selectedAgentId })
}
