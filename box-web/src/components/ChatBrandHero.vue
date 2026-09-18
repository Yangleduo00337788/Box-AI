<template>
  <header class="chat-brand-hero">
    <h1 class="chat-brand-hero__name">{{ productName }}</h1>
    <p class="chat-brand-hero__slogan">{{ slogan }}</p>
  </header>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchSystemContent } from '@/api/system'

const DEFAULT_PRODUCT_NAME = 'Box AI'
const DEFAULT_SLOGAN = '把模型、知识、工具、工作流装进一个 Box，让任何人都能构建自己的 AI Agent。'

const productName = ref(DEFAULT_PRODUCT_NAME)
const slogan = ref(DEFAULT_SLOGAN)

onMounted(async () => {
  try {
    const { data } = await fetchSystemContent()
    const about = data.data?.about
    if (!about) return
    productName.value = about.productName || DEFAULT_PRODUCT_NAME
    slogan.value = about.slogan || DEFAULT_SLOGAN
  } catch {
    // 使用默认品牌文案
  }
})
</script>

<style scoped>
.chat-brand-hero {
  width: 100%;
  margin: 0 0 24px;
  text-align: left;
}

.chat-brand-hero__name {
  margin: 0 0 12px;
  font-size: clamp(28px, 4vw, 36px);
  font-weight: 600;
  line-height: 1.25;
  color: var(--box-ink);
  letter-spacing: -0.02em;
}

.chat-brand-hero__slogan {
  margin: 0;
  max-width: 36em;
  font-size: 15px;
  line-height: 1.65;
  color: var(--box-muted);
}

@media (max-width: 640px) {
  .chat-brand-hero {
    margin-bottom: 20px;
  }

  .chat-brand-hero__slogan {
    font-size: 14px;
  }
}
</style>
