<template>
  <div class="settings-page">
    <h1 class="settings-page__title">隐私与协议</h1>
    <p class="settings-page__desc">了解我们如何处理你的数据，以及使用 Box 需遵守的条款。</p>
    <t-loading :loading="loading" size="small">
      <div v-if="content" class="settings-card legal-card">
        <section class="legal-section">
          <h2 class="legal-section__title">隐私政策</h2>
          <div v-if="content.legal.privacyHtml" class="legal-section__html" v-html="content.legal.privacyHtml" />
          <p v-for="paragraph in content.legal.privacy" v-else :key="paragraph" class="legal-section__text">{{ paragraph }}</p>
        </section>
        <section class="legal-section">
          <h2 class="legal-section__title">服务协议</h2>
          <div v-if="content.legal.termsHtml" class="legal-section__html" v-html="content.legal.termsHtml" />
          <p v-for="paragraph in content.legal.terms" v-else :key="paragraph" class="legal-section__text">{{ paragraph }}</p>
        </section>
        <p class="legal-card__updated">最后更新：{{ content.legal.updatedAt }}</p>
      </div>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchSystemContent, type SystemContentVO } from '@/api/system'

const loading = ref(false)
const content = ref<SystemContentVO | null>(null)

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
.legal-section {
  margin-bottom: 24px;
}

.legal-section__title {
  margin: 0 0 10px;
  font: var(--td-font-title-small);
}

.legal-section__html {
  font: var(--td-font-body-medium);
  color: var(--box-muted);
  line-height: 1.7;
}

.legal-section__html :deep(p) {
  margin: 0 0 10px;
}

.legal-section__text {
  margin: 0 0 10px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
  line-height: 1.7;
}

.legal-card__updated {
  margin: 0;
  font-size: 12px;
  color: var(--box-muted);
}
</style>
