import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchActiveOpsPlacements, type AdminOpsSlot, type OpsPlacementVO } from '@/api/ops'

const STORAGE_KEY = 'box.admin.ops.dismissed'
const dismissedIds = ref<number[]>(readDismissed())
let loaded = false
let pending: Promise<void> | null = null
const placements = ref<OpsPlacementVO[]>([])

function readDismissed(): number[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? (JSON.parse(raw) as number[]) : []
    return Array.isArray(parsed) ? parsed.filter((id) => Number.isFinite(id)) : []
  } catch {
    return []
  }
}

function persistDismissed() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(dismissedIds.value))
}

function visible(item: OpsPlacementVO) {
  if (dismissedIds.value.includes(item.id)) {
    return false
  }
  if (item.slot === 'ADMIN_BANNER') {
    return Boolean(item.imageUrl)
  }
  return true
}

export function useAdminOpsPlacements(slot?: AdminOpsSlot) {
  const router = useRouter()
  const loading = ref(false)

  const items = computed(() =>
    placements.value.filter((item) => (!slot || item.slot === slot) && visible(item)),
  )

  async function load() {
    if (loaded) return
    if (pending) {
      await pending
      return
    }
    loading.value = true
    pending = (async () => {
      try {
        const { data } = await fetchActiveOpsPlacements('B', slot)
        placements.value = data.data || []
        loaded = true
      } catch {
        placements.value = []
      } finally {
        loading.value = false
        pending = null
      }
    })()
    await pending
  }

  function dismissLocal(id: number) {
    if (!dismissedIds.value.includes(id)) {
      dismissedIds.value = [...dismissedIds.value, id]
      persistDismissed()
    }
  }

  function openLink(url?: string) {
    if (!url) return
    if (url.startsWith('/')) {
      void router.push(url)
      return
    }
    window.open(url, '_blank', 'noopener,noreferrer')
  }

  onMounted(() => {
    void load()
  })

  return { items, loading, load, dismissLocal, openLink }
}
