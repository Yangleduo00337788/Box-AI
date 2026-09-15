<template>
  <div v-if="items.length" class="ops-sidebar-ad">
    <t-swiper
      :autoplay="items.length > 1"
      :interval="6000"
      :height="88"
      :navigation="items.length > 1 ? { showSlideBtn: 'never', type: 'dots' } : { showSlideBtn: 'never' }"
    >
      <t-swiper-item v-for="item in items.slice(0, 3)" :key="item.id">
        <button type="button" class="ops-sidebar-ad__slide" @click="openLink(item.linkUrl)">
          <img :src="item.imageUrl" :alt="item.title" />
        </button>
      </t-swiper-item>
    </t-swiper>
  </div>
</template>

<script setup lang="ts">
import { useOpsPlacements } from '@/composables/useOpsPlacements'

const { items, openLink } = useOpsPlacements('CHAT_AD')
</script>

<style scoped>
.ops-sidebar-ad {
  width: 100%;
  overflow: hidden;
  border-radius: 10px;
}

.ops-sidebar-ad :deep(.t-swiper) {
  border-radius: 10px;
}

.ops-sidebar-ad :deep(.t-swiper__navigation-bars),
.ops-sidebar-ad :deep(.t-swiper__pagination) {
  bottom: 4px;
}

.ops-sidebar-ad__slide {
  display: block;
  width: 100%;
  height: 88px;
  padding: 0;
  border: none;
  background: var(--box-shell);
  cursor: pointer;
}

.ops-sidebar-ad__slide img {
  display: block;
  width: 100%;
  height: 88px;
  object-fit: cover;
}
</style>
