<template>
  <div class="settings-page">
    <h1 class="settings-page__title">通用设置</h1>
    <div class="settings-card">
      <t-form label-align="top">
        <t-form-item label="Enter 发送消息">
          <t-switch v-model="appPreferences.sendWithEnter" />
        </t-form-item>
        <p class="settings-card__hint">
          {{ appPreferences.sendWithEnter ? '按 Enter 发送，Shift+Enter 换行。' : '按 Ctrl+Enter 发送，Enter 换行。' }}
        </p>
      </t-form>

      <section class="shortcut-section">
        <h2 class="shortcut-section__title">快捷键</h2>
        <ul class="shortcut-list">
          <li><kbd>Ctrl</kbd> + <kbd>K</kbd> 打开全局搜索</li>
          <li><kbd>Esc</kbd> 关闭弹窗</li>
        </ul>
      </section>
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

.shortcut-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--box-border);
}

.shortcut-section__title {
  margin: 0 0 10px;
  font: var(--td-font-title-small);
}

.shortcut-list {
  margin: 0;
  padding-left: 18px;
  color: var(--box-muted);
  line-height: 1.9;
}

kbd {
  display: inline-block;
  padding: 1px 6px;
  border: 1px solid var(--box-border);
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
}
</style>
