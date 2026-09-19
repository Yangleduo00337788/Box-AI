<template>
  <div v-if="citations.length" class="box-chat-citations">
    <span class="box-chat-citations__label">引用来源</span>
    <div class="box-chat-citations__list">
      <t-popup
        v-for="cite in citations"
        :key="cite.index"
        placement="top"
        trigger="hover"
        show-arrow
        destroy-on-close
      >
        <template #content>
          <div class="box-chat-citation-popup">
            <div class="box-chat-citation-popup__title">{{ cite.documentName }}</div>
            <div class="box-chat-citation-popup__body">{{ cite.content }}</div>
          </div>
        </template>
        <button type="button" class="box-chat-citation-chip">
          [{{ cite.index }}] {{ cite.documentName }}
        </button>
      </t-popup>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { KnowledgeCitation } from '@/api/agent'

defineProps<{
  citations: KnowledgeCitation[]
}>()
</script>

<style scoped>
.box-chat-citations {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding-left: 48px;
}

.box-chat-citations__label {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.box-chat-citations__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.box-chat-citation-chip {
  border: 1px solid var(--td-component-border);
  background: var(--td-bg-color-container);
  color: var(--td-text-color-primary);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  cursor: pointer;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.box-chat-citation-chip:hover {
  border-color: var(--td-brand-color);
  color: var(--td-brand-color);
}

.box-chat-citation-popup {
  max-width: 360px;
}

.box-chat-citation-popup__title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.box-chat-citation-popup__body {
  font-size: 12px;
  line-height: 1.5;
  color: var(--td-text-color-secondary);
  white-space: pre-wrap;
  max-height: 160px;
  overflow: auto;
}
</style>
