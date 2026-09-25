<template>
  <t-dialog
    v-model:visible="visible"
    header="新建工作空间插件"
    width="800px"
    :confirm-btn="{ content: confirmLabel, loading: saving }"
    @confirm="onConfirm"
    @close="onClose"
  >
    <t-form ref="formRef" :data="form" :rules="rules" label-width="96px">
      <plugin-resource-create-fields
        v-model:category="form.category"
        v-model:title="form.title"
        v-model:description="form.description"
        v-model:manifest="manifestDraft"
        :category-options="categoryOptions"
        :upload-asset="uploadWorkspaceCatalogAsset"
        :workflow-editor-host="boxWebWorkflowEditorHost"
        :workflow-platform-models="workflowPlatformModels"
        title-placeholder="插件在市场中显示的名称"
      />
    </t-form>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps } from 'tdesign-vue-next'
import PluginResourceCreateFields from '@box/ui/components/PluginResourceCreateFields.vue'
import type { WorkflowEditorPlatformModel } from '@box/ui/workflow/workflowEditorHost'
import { createWorkspacePlugin, type PluginCatalogVO } from '@/api/plugin'
import { uploadPluginCatalogAsset } from '@/api/pluginCatalogAsset'
import { listPlatformModels } from '@/api/platform'
import type { PluginCategoryVO } from '@/api/plugin'
import { boxWebWorkflowEditorHost } from '@/utils/boxWebWorkflowEditorHost'
import {
  buildManifestJsonFromDraft,
  emptyManifestDraft,
  validateManifestDraft,
} from '@box/ui/plugin/pluginCatalogManifest'

const visible = defineModel<boolean>('visible', { default: false })
const router = useRouter()

const props = defineProps<{
  categories: PluginCategoryVO[]
  initialCategory?: string
}>()

const emit = defineEmits<{
  created: [plugin: PluginCatalogVO]
}>()

const saving = ref(false)
const formRef = ref<FormInstanceFunctions>()
const manifestDraft = ref(emptyManifestDraft())
const workflowPlatformModels = ref<WorkflowEditorPlatformModel[]>([])

const form = reactive({
  category: '',
  title: '',
  description: '',
})

const rules: FormProps['rules'] = {
  category: [{ required: true, message: '请选择分类' }],
  title: [{ required: true, message: '请输入标题' }],
}

const categoryOptions = computed(() =>
  props.categories.map((item) => ({ label: item.label, value: item.value })),
)

const confirmLabel = computed(() =>
  form.category === 'workflows' ? '创建并打开工作流编辑器' : '创建并安装',
)

async function uploadWorkspaceCatalogAsset(file: File) {
  const { data } = await uploadPluginCatalogAsset(file)
  return data.data
}

async function ensureWorkflowEditorContext() {
  if (workflowPlatformModels.value.length) return
  try {
    const { data } = await listPlatformModels()
    workflowPlatformModels.value = (data.data || []).filter((item) => item.status === 1)
  } catch {
    workflowPlatformModels.value = []
  }
}

watch(
  () => [visible.value, props.initialCategory] as const,
  ([open, initial]) => {
    if (!open) return
    form.category = initial || categoryOptions.value[0]?.value || ''
    form.title = ''
    form.description = ''
    manifestDraft.value = emptyManifestDraft()
    void ensureWorkflowEditorContext()
  },
  { immediate: true },
)

function onClose() {
  formRef.value?.clearValidate()
}

async function onConfirm() {
  const valid = await formRef.value?.validate()
  if (valid !== true) return false
  const manifestError = validateManifestDraft(form.category, manifestDraft.value)
  if (manifestError) {
    MessagePlugin.warning(manifestError)
    return false
  }
  let manifestJson: string
  try {
    manifestJson = buildManifestJsonFromDraft(form.category, manifestDraft.value)
  } catch (e) {
    MessagePlugin.warning(e instanceof Error ? e.message : '资源配置无效')
    return false
  }
  saving.value = true
  try {
    const { data } = await createWorkspacePlugin({
      category: form.category,
      title: form.title,
      description: form.description,
      manifestJson,
    })
    const plugin = data.data
    visible.value = false
    emit('created', plugin)
    if (form.category === 'workflows' && plugin.resourceType === 'workflow' && plugin.resourceId) {
      MessagePlugin.success('已创建；正在打开与工作流列表相同的全屏编辑器')
      await router.push(`/workflows/${plugin.resourceId}/editor`)
    } else {
      MessagePlugin.success('已创建并安装到本工作空间')
    }
  } catch (e) {
    MessagePlugin.error(e instanceof Error ? e.message : '创建失败')
    return false
  } finally {
    saving.value = false
  }
  return true
}
</script>
