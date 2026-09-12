import { storeToRefs } from 'pinia'
import { usePermissionStore } from '@/stores/permission'
import type { PermissionCode } from '@/constants/permissions'

export function usePermission() {
  const store = usePermissionStore()
  const { codes, loaded } = storeToRefs(store)

  return {
    codes,
    loaded,
    can: (code: PermissionCode | string) => store.can(code),
    canAny: (items: Array<PermissionCode | string>) => store.canAny(items),
    reload: () => store.load(true),
  }
}
