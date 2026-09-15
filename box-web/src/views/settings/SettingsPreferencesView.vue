<template>
  <div class="settings-page">
    <h1 class="settings-page__title">偏好设置</h1>
    <p class="settings-page__desc">外观与对话操作会保存在当前账号，换设备登录后仍会同步。</p>

    <section class="settings-card">
      <div class="settings-row">
        <div class="settings-row__copy">
          <h2 class="settings-row__title">主题模式</h2>
          <p class="settings-row__hint">深色模式会同步侧边栏、设置页与内容区对比度。</p>
        </div>
        <t-radio-group v-model="appPreferences.theme" variant="default-filled">
          <t-radio-button value="light">浅色</t-radio-button>
          <t-radio-button value="dark">深色</t-radio-button>
          <t-radio-button value="system">跟随系统</t-radio-button>
        </t-radio-group>
      </div>

      <div class="settings-row">
        <div class="settings-row__copy">
          <h2 class="settings-row__title">Enter 发送消息</h2>
          <p class="settings-row__hint">
            {{ appPreferences.sendWithEnter ? '按 Enter 发送，Shift + Enter 换行。' : '按 Ctrl + Enter 发送，Enter 换行。' }}
          </p>
        </div>
        <t-switch v-model="appPreferences.sendWithEnter" />
      </div>
    </section>

    <section class="settings-card">
      <h2 class="settings-card__heading">快捷键</h2>
      <ul class="shortcut-list">
        <li>
          <span>打开全局搜索</span>
          <span class="shortcut-keys"><kbd>Ctrl</kbd> + <kbd>K</kbd></span>
        </li>
        <li>
          <span>关闭弹窗</span>
          <span class="shortcut-keys"><kbd>Esc</kbd></span>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { appPreferences } from '@/composables/useAppPreferences'
</script>

<style scoped>
.settings-card__heading {
  margin: 0 0 12px;
  font: var(--td-font-title-small);
}

.shortcut-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.shortcut-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  color: var(--box-ink);
  font-size: 14px;
}

.shortcut-list li + li {
  border-top: 1px solid var(--box-border);
}

.shortcut-keys {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--box-muted);
}

kbd {
  display: inline-block;
  padding: 1px 6px;
  border: 1px solid var(--box-border);
  border-radius: 4px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
}
</style>
