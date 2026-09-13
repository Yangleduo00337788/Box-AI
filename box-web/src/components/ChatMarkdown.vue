<template>
  <div class="chat-md" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  content: string
}>()

const html = computed(() => renderMarkdown(props.content))
</script>

<style scoped>
.chat-md {
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
}

.chat-md :deep(> *:first-child) {
  margin-top: 0;
}

.chat-md :deep(> *:last-child) {
  margin-bottom: 0;
}

.chat-md :deep(p) {
  margin: 0 0 0.7em;
}

.chat-md :deep(h1),
.chat-md :deep(h2),
.chat-md :deep(h3),
.chat-md :deep(h4) {
  margin: 1em 0 0.45em;
  font-weight: 600;
  line-height: 1.35;
}

.chat-md :deep(h1) {
  font-size: 1.25em;
}

.chat-md :deep(h2) {
  font-size: 1.12em;
}

.chat-md :deep(h3),
.chat-md :deep(h4) {
  font-size: 1em;
}

.chat-md :deep(ul),
.chat-md :deep(ol) {
  margin: 0 0 0.7em;
  padding-left: 1.4em;
}

.chat-md :deep(li) {
  margin: 0.15em 0;
}

.chat-md :deep(blockquote) {
  margin: 0 0 0.7em;
  padding: 0 0 0 12px;
  border-left: 3px solid #e5e6eb;
  color: #646a73;
}

.chat-md :deep(a) {
  color: var(--td-brand-color);
}

.chat-md :deep(code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: #f2f3f5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.88em;
}

.chat-md :deep(pre) {
  margin: 0 0 0.7em;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f5f6f7;
  overflow-x: auto;
}

.chat-md :deep(pre code) {
  padding: 0;
  background: transparent;
  font-size: 13px;
}

.chat-md :deep(table) {
  width: 100%;
  margin: 0 0 0.7em;
  border-collapse: collapse;
  font-size: 14px;
}

.chat-md :deep(th),
.chat-md :deep(td) {
  padding: 6px 10px;
  border: 1px solid #e5e6eb;
  text-align: left;
}

.chat-md :deep(img) {
  max-width: 100%;
  border-radius: 10px;
}

.chat-md :deep(hr) {
  margin: 12px 0;
  border: none;
  border-top: 1px solid #e5e6eb;
}
</style>
