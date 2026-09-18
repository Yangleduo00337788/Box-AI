<template>
  <div v-if="items.length" class="ops-corner" :class="{ 'ops-corner--collapsed': collapsed }">
    <button
      v-if="collapsed"
      type="button"
      class="ops-corner__pill"
      aria-label="展开活动"
      @click="collapsed = false"
    >
      <t-icon name="image" size="18px" />
    </button>
    <div v-else class="ops-corner__card">
      <t-swiper
        v-model:current="slideIndex"
        :autoplay="items.length > 1"
        :interval="6000"
        :height="CARD_HEIGHT"
        :navigation="items.length > 1 ? { showSlideBtn: 'never', type: 'dots' } : { showSlideBtn: 'never' }"
      >
        <t-swiper-item v-for="item in items" :key="item.id">
          <button type="button" class="ops-corner__slide" @click="openLink(item.linkUrl, item.id)">
            <img :src="item.imageUrl" :alt="item.title" />
          </button>
        </t-swiper-item>
      </t-swiper>
      <button
        type="button"
        class="ops-corner__close"
        aria-label="关闭"
        @click.stop="dismissCurrent"
      >
        <t-icon name="close" size="14px" />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useOpsPlacements } from '@/composables/useOpsPlacements'

const CORNER_COLLAPSED_KEY = 'box.ops.cornerCollapsed'
const CARD_HEIGHT = 112

const { items: bannerItems, dismiss, openLink } = useOpsPlacements('CHAT_BANNER')
const { items: adItems } = useOpsPlacements('CHAT_AD')

const collapsed = ref(readCollapsed())
const slideIndex = ref(0)

const items = computed(() =>
  [...bannerItems.value, ...adItems.value]
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id)
    .slice(0, 5),
)

watch(
  () => items.value.map((item) => item.id).join(','),
  () => {
    if (slideIndex.value >= items.value.length) {
      slideIndex.value = 0
    }
  },
)

watch(collapsed, (value) => {
  sessionStorage.setItem(CORNER_COLLAPSED_KEY, value ? '1' : '0')
})

function readCollapsed() {
  try {
    return sessionStorage.getItem(CORNER_COLLAPSED_KEY) === '1'
  } catch {
    return false
  }
}

function dismissCurrent() {
  const item = items.value[slideIndex.value]
  if (item) {
    dismiss(item.id)
  }
}
</script>

<style scoped>
.ops-corner {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 120;
  pointer-events: none;
}

.ops-corner--collapsed {
  bottom: 16px;
}

.ops-corner__card,
.ops-corner__pill {
  pointer-events: auto;
}

.ops-corner__card {
  position: relative;
  width: 200px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid var(--box-border);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
}

.ops-corner__card :deep(.t-swiper) {
  border-radius: 12px;
}

.ops-corner__slide {
  display: block;
  width: 100%;
  height: 112px;
  padding: 0;
  border: none;
  background: var(--box-shell);
  cursor: pointer;
}

.ops-corner__slide img {
  display: block;
  width: 100%;
  height: 112px;
  object-fit: cover;
}

.ops-corner__close {
  position: absolute;
  top: 6px;
  right: 6px;
  z-index: 3;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
  cursor: pointer;
}

.ops-corner__close:hover {
  background: rgba(0, 0, 0, 0.72);
}

.ops-corner__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--box-border);
  border-radius: 999px;
  background: var(--box-surface);
  color: var(--box-ink);
  box-shadow: var(--box-shadow-card);
  cursor: pointer;
}

.ops-corner__pill:hover {
  background: var(--box-hover);
}

@media (max-width: 640px) {
  .ops-corner {
    right: 12px;
    bottom: 12px;
  }

  .ops-corner__card {
    width: 176px;
  }
}
</style>
