<template>
  <div class="settings-page">
    <h1 class="settings-page__title">关于盒子</h1>
    <t-loading :loading="loading" size="small">
      <div v-if="content" class="settings-card about-card">
        <div class="about-card__brand">{{ content.about.productName }}</div>
        <p class="about-card__slogan">{{ content.about.slogan }}</p>
        <dl class="about-card__meta">
          <div><dt>产品名称</dt><dd>{{ content.about.productName }}</dd></div>
          <div><dt>客户端版本</dt><dd>{{ content.about.version }}</dd></div>
          <div><dt>定位</dt><dd>{{ content.about.positioning }}</dd></div>
        </dl>
        <t-space>
          <t-button variant="outline" @click="router.push('/legal/terms')">用户协议</t-button>
          <t-button variant="outline" @click="router.push('/legal/privacy')">隐私政策</t-button>
        </t-space>
      </div>
    </t-loading>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchSystemContent, type SystemContentVO } from '@/api/system'

const router = useRouter()
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
.settings-page__title {
  margin: 0 0 24px;
  font: var(--td-font-title-large);
}

.settings-card {
  max-width: 720px;
  padding: 28px;
  border-radius: 16px;
  background: #f7f8fa;
}

.about-card__brand {
  font-size: 28px;
  font-weight: 700;
  color: var(--box-ink);
}

.about-card__slogan {
  margin: 12px 0 24px;
  max-width: 520px;
  font: var(--td-font-body-medium);
  color: var(--box-muted);
  line-height: 1.7;
}

.about-card__meta {
  margin: 0 0 24px;
}

.about-card__meta div {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
}

.about-card__meta dt {
  width: 96px;
  color: var(--box-muted);
  font-size: 14px;
}

.about-card__meta dd {
  margin: 0;
  color: var(--box-ink);
  font-size: 14px;
}
</style>
