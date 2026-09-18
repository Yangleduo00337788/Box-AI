import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchOpsPlacements, trackOpsPlacement, type OpsPlacementVO, type OpsSlot } from '@/api/ops'

const STORAGE_KEY = 'box.ops.dismissed'
const TRACKED_IMPRESSIONS_KEY = 'box.ops.trackedImpressions'
const placements = ref<OpsPlacementVO[]>([])
const dismissedIds = ref<number[]>(readDismissed())
let loaded = false
let pending: Promise<void> | null = null

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

function readTrackedImpressions(): Set<number> {
  try {
    const raw = sessionStorage.getItem(TRACKED_IMPRESSIONS_KEY)
    const parsed = raw ? (JSON.parse(raw) as number[]) : []
    return new Set(parsed.filter((id) => Number.isFinite(id)))
  } catch {
    return new Set()
  }
}

const trackedImpressions = readTrackedImpressions()

function markImpression(id: number) {
  if (trackedImpressions.has(id)) return
  trackedImpressions.add(id)
  sessionStorage.setItem(TRACKED_IMPRESSIONS_KEY, JSON.stringify([...trackedImpressions]))
  void trackOpsPlacement(id, 'impression').catch(() => undefined)
}

function visible(item: OpsPlacementVO) {
  if (dismissedIds.value.includes(item.id)) {
    return false
  }
  if (item.slot === 'CHAT_BANNER' || item.slot === 'CHAT_AD') {
    return Boolean(item.imageUrl)
  }
  return true
}

export function useOpsPlacements(slot?: OpsSlot) {
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
        const { data } = await fetchOpsPlacements()
        placements.value = data.data || []
        loaded = true
        for (const item of placements.value) {
          if ((!slot || item.slot === slot) && visible(item)) {
            markImpression(item.id)
          }
        }
      } catch {
        placements.value = []
      } finally {
        loading.value = false
        pending = null
      }
    })()
    await pending
  }

  function dismiss(id: number) {
    if (!dismissedIds.value.includes(id)) {
      dismissedIds.value = [...dismissedIds.value, id]
      persistDismissed()
    }
  }

  function openLink(url?: string, placementId?: number) {
    if (placementId) {
      void trackOpsPlacement(placementId, 'click').catch(() => undefined)
    }
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

  return { items, loading, load, dismiss, openLink }
}
