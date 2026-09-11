<template>
  <div class="settings-page">
    <h1 class="settings-page__title">外观设置</h1>
    <div class="settings-card">
      <t-form label-align="top">
        <t-form-item label="主题模式">
          <t-radio-group v-model="appPreferences.theme">
            <t-radio value="light">浅色</t-radio>
            <t-radio value="dark">深色</t-radio>
            <t-radio value="system">跟随系统</t-radio>
          </t-radio-group>
        </t-form-item>
        <p class="settings-card__hint">深色模式会调整页面背景与文字对比度，侧边栏与内容区同步生效。</p>
      </t-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { appPreferences } from '@/composables/useAppPreferences'
</script>

<style scoped>
.settings-page__title {
  margin: 0 0 24px;
  font: var(--td-font-title-large);
}

.settings-card {
  max-width: 720px;
  padding: 24px;
  border-radius: 16px;
  background: #f7f8fa;
}

.settings-card__hint {
  margin: 0;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}
</style>
