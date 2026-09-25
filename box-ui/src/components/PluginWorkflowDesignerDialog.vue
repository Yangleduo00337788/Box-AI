<template>
  <t-dialog
    v-model:visible="visible"
    header="工作流编排"
    width="96vw"
    top="2vh"
    :footer="false"
    class="plugin-wf-dialog"
  >
    <div class="plugin-wf-dialog__toolbar">
      <span class="plugin-wf-dialog__hint">与 C 端工作流编辑器相同：节点库 · 右侧配置 · Delete 删除 · Ctrl+Z 撤销</span>
      <t-space>
        <t-tooltip content="撤销 Ctrl+Z">
          <t-button variant="outline" size="small" :disabled="!canUndo" @click="onUndo">撤销</t-button>
        </t-tooltip>
        <t-tooltip content="重做 Ctrl+Y">
          <t-button variant="outline" size="small" :disabled="!canRedo" @click="onRedo">重做</t-button>
        </t-tooltip>
        <t-button variant="outline" size="small" @click="onAutoLayout">自动布局</t-button>
        <t-button variant="outline" @click="visible = false">取消</t-button>
        <t-button theme="primary" @click="apply">应用到插件</t-button>
      </t-space>
    </div>
    <div class="plugin-wf-dialog__body">
      <WorkflowEditorCanvas
        v-if="visible"
        ref="canvasRef"
        :workflow-id="null"
        :platform-models="platformModels"
        :definition-json="definitionJson"
        :editor-host="editorHost"
      />
    </div>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import WorkflowEditorCanvas from './workflow/WorkflowEditorCanvas.vue'
import type { WorkflowEditorHost, WorkflowEditorPlatformModel } from '../workflow/workflowEditorHost'
import { emptyWorkflowEditorHost } from '../workflow/workflowEditorHost'

const visible = defineModel<boolean>('visible', { default: false })
const definitionJson = defineModel<string>('definitionJson', { default: '' })

withDefaults(
  defineProps<{
    platformModels?: WorkflowEditorPlatformModel[]
    editorHost?: WorkflowEditorHost
  }>(),
  {
    platformModels: () => [],
    editorHost: () => emptyWorkflowEditorHost,
  },
)

const canvasRef = ref<{
  loadDefinition: (json?: string) => void
  getDefinitionJson: () => string
  handleUndo: () => boolean
  handleRedo: () => boolean
  autoLayout: () => void
  canUndo: { value: boolean }
  canRedo: { value: boolean }
} | null>(null)

const canUndo = computed(() => canvasRef.value?.canUndo.value ?? false)
const canRedo = computed(() => canvasRef.value?.canRedo.value ?? false)

watch(visible, async (open) => {
  if (!open) return
  await nextTick()
  canvasRef.value?.loadDefinition(definitionJson.value)
})

function onUndo() {
  if (canvasRef.value?.handleUndo()) {
    MessagePlugin.info('已撤销')
  }
}

function onRedo() {
  if (canvasRef.value?.handleRedo()) {
    MessagePlugin.info('已重做')
  }
}

function onAutoLayout() {
  canvasRef.value?.autoLayout()
}

function apply() {
  definitionJson.value = canvasRef.value?.getDefinitionJson() ?? definitionJson.value
  visible.value = false
  MessagePlugin.success('已写入插件工作流定义')
  return true
}
</script>

<style scoped>
.plugin-wf-dialog__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.plugin-wf-dialog__hint {
  font: var(--td-font-body-small);
  color: var(--td-text-color-secondary);
}
.plugin-wf-dialog__body {
  height: 72vh;
  min-height: 480px;
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
  overflow: hidden;
}
:deep(.plugin-wf-dialog .t-dialog__body) {
  padding-top: 8px;
}
</style>
