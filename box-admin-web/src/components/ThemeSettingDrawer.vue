<template>
  <t-drawer
    :visible="visible"
    header="主题配置"
    size="360px"
    :close-on-overlay-click="true"
    @close="emit('update:visible', false)"
    @update:visible="emit('update:visible', $event)"
  >
    <section class="theme-section">
      <h3 class="theme-section__title">导航栏</h3>
      <p class="theme-section__hint">默认浅色侧栏，可切换为深色导航。</p>
      <t-radio-group v-model="appearance.menuTheme" variant="default-filled">
        <t-radio-button value="light">浅色</t-radio-button>
        <t-radio-button value="dark">深色</t-radio-button>
      </t-radio-group>
    </section>

    <section class="theme-section">
      <h3 class="theme-section__title">主题色</h3>
      <p class="theme-section__hint">影响按钮、选中态和品牌标识。</p>
      <div class="theme-swatches">
        <button
          v-for="item in BRAND_PRESETS"
          :key="item.id"
          type="button"
          class="theme-swatch"
          :class="{ 'theme-swatch--active': appearance.brand === item.id }"
          :style="{ background: item.color }"
          :title="item.label"
          :aria-label="item.label"
          @click="appearance.brand = item.id"
        />
      </div>
    </section>

    <template #footer>
      <t-button variant="outline" @click="appearance.reset()">恢复默认</t-button>
    </template>
  </t-drawer>
</template>

<script setup lang="ts">
import { BRAND_PRESETS, useAppearanceStore } from '@/stores/appearance'

defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const appearance = useAppearanceStore()
</script>

<style scoped>
.theme-section + .theme-section {
  margin-top: 28px;
}

.theme-section__title {
  margin: 0 0 4px;
  font: var(--td-font-title-small);
  color: var(--td-text-color-primary);
}

.theme-section__hint {
  margin: 0 0 12px;
  font: var(--td-font-body-small);
  color: var(--td-text-color-placeholder);
}

.theme-swatches {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.theme-swatch {
  width: 28px;
  height: 28px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
}

.theme-swatch--active {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 2px var(--td-brand-color-light);
}
</style>
