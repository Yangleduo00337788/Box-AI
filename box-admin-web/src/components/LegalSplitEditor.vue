<template>
  <div class="legal-split">
    <div class="legal-split__pane">
      <div class="legal-split__label">源码</div>
      <t-textarea
        :value="modelValue"
        class="legal-split__source"
        :autosize="{ minRows: 14, maxRows: 22 }"
        :placeholder="placeholder"
        @change="onChange"
      />
    </div>
    <div class="legal-split__pane">
      <div class="legal-split__label">预览</div>
      <div class="legal-split__preview" v-html="previewHtml" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { legalSourceToHtml } from '@/utils/legalHtml'

const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const previewHtml = computed(() => legalSourceToHtml(props.modelValue))

function onChange(value: string) {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.legal-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.legal-split__label {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--box-muted, #8f959e);
}

.legal-split__source :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.legal-split__preview {
  min-height: 280px;
  max-height: 420px;
  overflow: auto;
  padding: 16px 18px;
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 8px;
  background: #fff;
  color: var(--box-ink, #1f2329);
  line-height: 1.8;
}

.legal-split__preview :deep(h1),
.legal-split__preview :deep(h2),
.legal-split__preview :deep(h3) {
  margin: 0 0 12px;
}

.legal-split__preview :deep(p) {
  margin: 0 0 12px;
}

.legal-split__preview :deep(a) {
  color: var(--td-brand-color);
}

@media (max-width: 960px) {
  .legal-split {
    grid-template-columns: 1fr;
  }
}
</style>
