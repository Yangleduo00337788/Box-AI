<template>
  <div class="legal-page">
    <div class="legal-page__inner">
      <button type="button" class="legal-page__back" @click="goBack">
        <t-icon name="chevron-left" />
        返回
      </button>
      <t-loading :loading="loading" size="small">
        <article v-if="paragraphs.length" class="legal-doc">
          <h1>{{ title }}</h1>
          <p v-if="updatedAt" class="legal-doc__updated">最后更新：{{ updatedAt }}</p>
          <p v-for="(paragraph, index) in paragraphs" :key="index" class="legal-doc__text">{{ paragraph }}</p>
        </article>
        <t-empty v-else-if="!loading" description="暂无内容" />
      </t-loading>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchSystemContent, type SystemContentVO } from '@/api/system'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const content = ref<SystemContentVO | null>(null)

const isPrivacy = computed(() => String(route.params.doc) === 'privacy')
const title = computed(() => (isPrivacy.value ? '隐私政策' : '用户协议'))
const paragraphs = computed(() => {
  const legal = content.value?.legal
  if (!legal) return []
  return isPrivacy.value ? legal.privacy || [] : legal.terms || []
})
const updatedAt = computed(() => content.value?.legal?.updatedAt || '')

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/login')
}

watch(
  () => route.params.doc,
  (doc) => {
    if (doc !== 'privacy' && doc !== 'terms') {
      router.replace('/legal/terms')
    }
  },
  { immediate: true },
)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await fetchSystemContent()
    content.value = data.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.legal-page {
  min-height: 100vh;
  padding: 32px 24px 64px;
  background: var(--box-bg);
}

.legal-page__inner {
  max-width: 760px;
  margin: 0 auto;
}

.legal-page__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 20px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--box-ink);
  font-size: 14px;
  cursor: pointer;
}

.legal-doc h1 {
  margin: 0 0 8px;
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.legal-doc__updated {
  margin: 0 0 24px;
  font-size: 12px;
  color: var(--box-muted);
}

.legal-doc__text {
  margin: 0 0 14px;
  font: var(--td-font-body-medium);
  color: var(--box-ink);
  line-height: 1.8;
}
</style>
