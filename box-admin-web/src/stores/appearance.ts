import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

const STORAGE_KEY = 'box.admin.appearance'

export type MenuTheme = 'light' | 'dark'
export type BrandId = 'ink' | 'blue' | 'cyan' | 'green' | 'orange'

export type BrandPreset = {
  id: BrandId
  label: string
  color: string
  hover: string
  light: string
  candyFrom: string
  candyMid: string
  candyTo: string
  candyActive: string
  chartColors: string[]
}

export const BRAND_PRESETS: BrandPreset[] = [
  {
    id: 'ink',
    label: '默认墨色',
    color: '#181818',
    hover: '#2c2c2c',
    light: '#f3f3f3',
    candyFrom: '#fffbf8',
    candyMid: '#fefcfb',
    candyTo: '#fbfdfd',
    candyActive: '#fdecda',
    chartColors: ['#8b7cf6', '#5b8def', '#3ecfcf', '#7ed3a8', '#f7b267', '#f78da7'],
  },
  {
    id: 'blue',
    label: '品牌蓝',
    color: '#0052d9',
    hover: '#266fe8',
    light: '#f2f3ff',
    candyFrom: '#fffbf8',
    candyMid: '#fefcfb',
    candyTo: '#fbfdfd',
    candyActive: '#fdecda',
    chartColors: ['#0052d9', '#4c7dff', '#7aa2ff', '#59d0d0', '#8b7cf6', '#f78da7'],
  },
  {
    id: 'cyan',
    label: '青色',
    color: '#0594fa',
    hover: '#29a4fb',
    light: '#f0f9ff',
    candyFrom: '#fffbf8',
    candyMid: '#fefcfb',
    candyTo: '#fbfdfd',
    candyActive: '#fdecda',
    chartColors: ['#0594fa', '#3ecfcf', '#7ed3a8', '#5b8def', '#8b7cf6', '#f7b267'],
  },
  {
    id: 'green',
    label: '绿色',
    color: '#00a870',
    hover: '#07c383',
    light: '#e8f8f2',
    candyFrom: '#fffbf8',
    candyMid: '#fefcfb',
    candyTo: '#fbfdfd',
    candyActive: '#fdecda',
    chartColors: ['#00a870', '#3ecfcf', '#7ed3a8', '#5b8def', '#f7b267', '#8b7cf6'],
  },
  {
    id: 'orange',
    label: '橙色',
    color: '#ed7b2f',
    hover: '#f2995f',
    light: '#fef3e6',
    candyFrom: '#fffbf8',
    candyMid: '#fefcfb',
    candyTo: '#fbfdfd',
    candyActive: '#fdecda',
    chartColors: ['#ed7b2f', '#f7b267', '#f78da7', '#8b7cf6', '#5b8def', '#3ecfcf'],
  },
]

type AppearanceState = {
  menuTheme: MenuTheme
  brand: BrandId
}

const defaults: AppearanceState = {
  menuTheme: 'light',
  brand: 'ink',
}

function readStored(): AppearanceState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...defaults }
    const parsed = JSON.parse(raw) as Partial<AppearanceState>
    return {
      menuTheme: parsed.menuTheme === 'dark' ? 'dark' : 'light',
      brand: BRAND_PRESETS.some((item) => item.id === parsed.brand) ? (parsed.brand as BrandId) : 'ink',
    }
  } catch {
    return { ...defaults }
  }
}

export const useAppearanceStore = defineStore('appearance', () => {
  const stored = readStored()
  const menuTheme = ref<MenuTheme>(stored.menuTheme)
  const brand = ref<BrandId>(stored.brand)

  const brandPreset = computed(
    () => BRAND_PRESETS.find((item) => item.id === brand.value) || BRAND_PRESETS[0],
  )

  const brandStyle = computed(() => ({
    '--td-brand-color': brandPreset.value.color,
    '--td-brand-color-hover': brandPreset.value.hover,
    '--td-brand-color-active': brandPreset.value.color,
    '--td-brand-color-light': brandPreset.value.light,
    '--td-brand-color-focus': brandPreset.value.light,
    '--admin-candy-from': brandPreset.value.candyFrom,
    '--admin-candy-mid': brandPreset.value.candyMid,
    '--admin-candy-to': brandPreset.value.candyTo,
    '--admin-candy-gradient': `linear-gradient(180deg, ${brandPreset.value.candyFrom} 0%, ${brandPreset.value.candyMid} 48%, ${brandPreset.value.candyTo} 100%)`,
    '--admin-candy-active-bg': brandPreset.value.candyActive,
  }))

  watch([menuTheme, brand], () => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ menuTheme: menuTheme.value, brand: brand.value }),
    )
  })

  function reset() {
    menuTheme.value = defaults.menuTheme
    brand.value = defaults.brand
  }

  return { menuTheme, brand, brandPreset, brandStyle, reset }
})
