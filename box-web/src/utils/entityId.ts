export type EntityId = number | string

export function entityIdKey(id: EntityId | null | undefined): string {
  return id == null ? '' : String(id)
}

export function sameEntityId(left: EntityId | null | undefined, right: EntityId | null | undefined): boolean {
  if (left == null || right == null) {
    return false
  }
  return entityIdKey(left) === entityIdKey(right)
}
