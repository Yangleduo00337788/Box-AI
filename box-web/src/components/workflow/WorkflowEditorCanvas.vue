<template>
  <WorkflowEditorCanvas
    ref="innerRef"
    :workflow-id="workflowId"
    :platform-models="platformModels"
    :definition-json="definitionJson"
    :editor-host="boxWebWorkflowEditorHost"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import WorkflowEditorCanvas from '@box/ui/components/workflow/WorkflowEditorCanvas.vue'
import type { WorkflowEditorPlatformModel } from '@box/ui/workflow/workflowEditorHost'
import { boxWebWorkflowEditorHost } from '@/utils/boxWebWorkflowEditorHost'

defineProps<{
  workflowId: number
  platformModels: WorkflowEditorPlatformModel[]
  definitionJson?: string
}>()

const innerRef = ref<InstanceType<typeof WorkflowEditorCanvas> | null>(null)

defineExpose({
  loadDefinition: (json?: string) => innerRef.value?.loadDefinition(json),
  getDefinitionJson: () => innerRef.value?.getDefinitionJson() ?? '{}',
  handleUndo: () => innerRef.value?.handleUndo() ?? false,
  handleRedo: () => innerRef.value?.handleRedo() ?? false,
  autoLayout: () => innerRef.value?.autoLayout(),
  get canUndo() {
    return innerRef.value?.canUndo ?? { value: false }
  },
  get canRedo() {
    return innerRef.value?.canRedo ?? { value: false }
  },
  get autoSave() {
    return innerRef.value?.autoSave ?? { status: { value: 'idle' }, cancelAutoSave: () => {} }
  },
})
</script>
