<template>
  <header class="chat-brand-hero">
    <div class="chat-brand-hero__copy">
      <p class="chat-brand-hero__eyebrow">{{ eyebrow }}</p>
      <h1 class="chat-brand-hero__headline">{{ headline }}</h1>
      <p v-if="subtitle" class="chat-brand-hero__sub">{{ subtitle }}</p>
    </div>
    <brand-wordmark class="chat-brand-hero__mark" size="lg" align="left" />
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BrandWordmark from '@box/ui/components/BrandWordmark.vue'
import { fetchSystemContent } from '@/api/system'

const DEFAULT_EYEBROW = '盒子 · Box'
const DEFAULT_SLOGAN = '把模型、知识、工具、工作流装进一个 Box，让任何人都能构建自己的 AI Agent。'

const productName = ref('')
const slogan = ref(DEFAULT_SLOGAN)

const eyebrow = computed(() => {
  const name = productName.value.trim()
  if (!name || name === '盒子' || name === 'Box') return DEFAULT_EYEBROW
  if (name.includes('·')) return name
  return `盒子 · ${name}`
})

const headline = computed(() => splitSlogan(slogan.value).headline)
const subtitle = computed(() => splitSlogan(slogan.value).subtitle)

function splitSlogan(raw: string) {
  const text = raw.trim() || DEFAULT_SLOGAN
  const match = text.match(/^(.+?)[，,](.+)$/s)
  if (match) {
    return {
      headline: match[1].trim(),
      subtitle: match[2].replace(/^[。.\s]+/, '').trim(),
    }
  }
  return { headline: text, subtitle: '' }
}

onMounted(async () => {
  try {
    const { data } = await fetchSystemContent()
    const about = data.data?.about
    if (!about) return
    productName.value = about.productName || ''
    slogan.value = about.slogan || DEFAULT_SLOGAN
  } catch {
    // 使用默认品牌文案
  }
})
</script>

<style scoped>
.chat-brand-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px 28px;
  width: 100%;
  margin: 0 0 28px;
  text-align: left;
}

.chat-brand-hero__copy {
  flex: 1;
  min-width: 0;
}

.chat-brand-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.18em;
  color: var(--box-ink);
}

.chat-brand-hero__headline {
  margin: 0 0 8px;
  font-size: clamp(18px, 2.1vw, 24px);
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: var(--box-ink);
}

.chat-brand-hero__sub {
  margin: 0;
  font-size: 15px;
  font-weight: 400;
  line-height: 1.7;
  color: var(--box-muted);
}

.chat-brand-hero__mark {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .chat-brand-hero {
    margin-bottom: 20px;
    gap: 12px 16px;
  }

  .chat-brand-hero__eyebrow {
    font-size: 12px;
    letter-spacing: 0.14em;
  }

  .chat-brand-hero__headline {
    font-size: 17px;
    margin-bottom: 6px;
  }

  .chat-brand-hero__sub {
    font-size: 14px;
  }

  .chat-brand-hero__mark :deep(.brand-wordmark--lg .brand-wordmark__img) {
    height: 48px;
  }
}
</style>
