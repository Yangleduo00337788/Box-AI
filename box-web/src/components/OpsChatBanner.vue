<template>
  <div v-if="items.length" class="ops-chat-banner">
    <t-swiper
      :autoplay="items.length > 1"
      :interval="5000"
      :height="140"
      :navigation="{ showSlideBtn: items.length > 1 ? 'hover' : 'never', type: 'dots' }"
    >
      <t-swiper-item v-for="item in items" :key="item.id">
        <button type="button" class="ops-chat-banner__slide" @click="openLink(item.linkUrl)">
          <img :src="item.imageUrl" :alt="item.title" />
        </button>
      </t-swiper-item>
    </t-swiper>
  </div>
</template>

<script setup lang="ts">
import { useOpsPlacements } from '@/composables/useOpsPlacements'

const { items, openLink } = useOpsPlacements('CHAT_BANNER')
</script>

<style scoped>
.ops-chat-banner {
  width: 100%;
  margin: 0 0 16px;
  overflow: hidden;
  border-radius: 16px;
}

.ops-chat-banner :deep(.t-swiper) {
  border-radius: 16px;
}

.ops-chat-banner__slide {
  display: block;
  width: 100%;
  height: 140px;
  padding: 0;
  border: none;
  background: #f5f6f8;
  cursor: pointer;
}

.ops-chat-banner__slide img {
  display: block;
  width: 100%;
  height: 140px;
  object-fit: cover;
}
</style>
