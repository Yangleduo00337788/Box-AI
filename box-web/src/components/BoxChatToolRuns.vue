<template>
  <div v-if="runs.length" class="box-chat-tool-runs">
    <button
      v-for="(run, index) in runs"
      :key="`${run.toolKey}-${index}`"
      type="button"
      class="box-chat-tool-runs__chip"
      :class="`box-chat-tool-runs__chip--${run.status}`"
      :disabled="run.status === 'running'"
      @click="emit('open', run)"
    >
      <t-loading v-if="run.status === 'running'" size="small" />
      <t-icon v-else-if="run.status === 'success'" name="check-circle" size="14px" />
      <t-icon v-else name="error-circle" size="14px" />
      <span>{{ run.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import type { ChatToolRun } from '@/utils/messageMetadata'

defineProps<{
  runs: ChatToolRun[]
}>()

const emit = defineEmits<{
  open: [run: ChatToolRun]
}>()
</script>

<style scoped>
.box-chat-tool-runs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.box-chat-tool-runs__chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--td-component-border);
  background: var(--td-bg-color-container);
  font-size: 12px;
  line-height: 20px;
  cursor: pointer;
  color: var(--td-text-color-secondary);
}

.box-chat-tool-runs__chip--running {
  cursor: default;
  border-color: var(--td-brand-color-light);
}

.box-chat-tool-runs__chip--success {
  border-color: var(--td-success-color-3);
  color: var(--td-success-color);
}

.box-chat-tool-runs__chip--failed {
  border-color: var(--td-error-color-3);
  color: var(--td-error-color);
}
</style>
