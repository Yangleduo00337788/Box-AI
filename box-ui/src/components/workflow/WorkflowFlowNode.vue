<template>
  <div class="wf-node" :class="`wf-node--${data.nodeType.toLowerCase()}`">
    <Handle v-if="data.nodeType !== 'Start'" type="target" :position="Position.Left" />
    <div class="wf-node__head">
      <t-icon :name="iconName" />
      <span class="wf-node__title">{{ data.label }}</span>
    </div>
    <span class="wf-node__type">{{ data.nodeType }}</span>
    <Handle
      v-if="data.nodeType === 'Condition'"
      id="true"
      type="source"
      :position="Position.Right"
      :style="{ top: '35%' }"
    />
    <Handle
      v-if="data.nodeType === 'Condition'"
      id="false"
      type="source"
      :position="Position.Right"
      :style="{ top: '70%' }"
    />
    <template v-else-if="data.nodeType === 'Switch'">
      <Handle
        v-for="(handle, index) in switchHandles"
        :key="handle.id"
        :id="handle.id"
        type="source"
        :position="Position.Right"
        :style="switchHandleStyle(index, switchHandles.length)"
      />
    </template>
    <template v-else-if="data.nodeType === 'Loop' && data.config?.graphBody">
      <Handle id="body" type="source" :position="Position.Right" :style="{ top: '40%' }" />
      <Handle id="next" type="source" :position="Position.Right" :style="{ top: '75%' }" />
    </template>
    <template v-else-if="data.nodeType === 'Parallel' && data.config?.useGraphBranches">
      <Handle
        v-for="(handle, index) in parallelHandles"
        :key="handle"
        :id="handle"
        type="source"
        :position="Position.Right"
        :style="switchHandleStyle(index, parallelHandles.length)"
      />
    </template>
    <Handle
      v-else-if="data.nodeType !== 'Output'"
      type="source"
      :position="Position.Right"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import type { WorkflowNodeData } from '../../workflow/workflowFlow'
import { PALETTE_ITEMS } from '../../workflow/workflowFlow'

interface SwitchCase {
  id: string
  value?: string
}

const props = defineProps<{
  data: WorkflowNodeData
}>()

const iconName = computed(() => {
  const item = PALETTE_ITEMS.find((entry) => entry.type === props.data.nodeType)
  return item?.icon || 'app'
})

const switchHandles = computed(() => {
  if (props.data.nodeType !== 'Switch') return []
  const config = props.data.config || {}
  const cases = Array.isArray(config.cases) ? (config.cases as SwitchCase[]) : []
  const defaultCase = String(config.defaultCase || 'default')
  const handles = cases
    .filter((item) => item?.id)
    .map((item) => ({ id: String(item.id), label: String(item.id) }))
  handles.push({ id: defaultCase, label: `${defaultCase} (默认)` })
  return handles
})

const parallelHandles = computed(() => ['branch-0', 'branch-1', 'branch-2'])

function switchHandleStyle(index: number, total: number) {
  const top = ((index + 1) / (total + 1)) * 100
  return { top: `${top}%` }
}
</script>

<style scoped>
.wf-node {
  min-width: 148px;
  padding: 10px 12px;
  border: 2px solid var(--td-component-border);
  border-radius: 12px;
  background: var(--td-bg-color-container);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.wf-node--start {
  border-color: #2ba471;
}

.wf-node--output {
  border-color: #d54941;
}

.wf-node--llm {
  border-color: var(--td-brand-color);
}

.wf-node--switch {
  border-color: #e37318;
}

.wf-node--loop {
  border-color: #0052d9;
}

.wf-node--parallel {
  border-color: #7c3aed;
}

.wf-node__head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.wf-node__title {
  font-size: 13px;
  font-weight: 600;
}

.wf-node__type {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  color: var(--td-text-color-secondary);
}
</style>
