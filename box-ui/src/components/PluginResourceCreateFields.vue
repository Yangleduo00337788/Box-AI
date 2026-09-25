<template>
  <div class="plugin-resource-create-fields">
    <t-form-item v-if="showPluginCode" label="编码" name="pluginCode">
      <t-input v-model="pluginCode" placeholder="如 official-httpbin-get" />
    </t-form-item>
    <t-form-item label="分类" name="category">
      <t-select v-model="category" :options="categoryOptions" placeholder="选择分类" />
    </t-form-item>
    <t-form-item label="标题" name="title">
      <t-input v-model="title" :placeholder="titlePlaceholder" />
    </t-form-item>
    <t-form-item label="描述" name="description">
      <t-textarea v-model="description" :autosize="{ minRows: 2, maxRows: 4 }" />
    </t-form-item>
    <t-form-item v-if="showSortOrder" label="排序" name="sortOrder">
      <t-input-number v-model="sortOrder" :min="0" theme="column" />
    </t-form-item>
    <plugin-manifest-fields
      v-if="category"
      v-model:manifest="manifest"
      :category="category"
      :upload-asset="uploadAsset"
      :workflow-editor-host="workflowEditorHost"
      :workflow-platform-models="workflowPlatformModels"
    />
  </div>
</template>

<script setup lang="ts">
import type { PluginManifestDraft } from '../plugin/pluginCatalogManifest'
import type { WorkflowEditorHost, WorkflowEditorPlatformModel } from '../workflow/workflowEditorHost'
import { emptyWorkflowEditorHost } from '../workflow/workflowEditorHost'
import PluginManifestFields, { type PluginCatalogAssetUploadResult } from './PluginManifestFields.vue'

withDefaults(
  defineProps<{
    categoryOptions: { label: string; value: string }[]
    uploadAsset: (file: File) => Promise<PluginCatalogAssetUploadResult>
    showPluginCode?: boolean
    showSortOrder?: boolean
    titlePlaceholder?: string
    workflowEditorHost?: WorkflowEditorHost
    workflowPlatformModels?: WorkflowEditorPlatformModel[]
  }>(),
  {
    workflowEditorHost: () => emptyWorkflowEditorHost,
    workflowPlatformModels: () => [],
  },
)

const category = defineModel<string>('category', { required: true })
const title = defineModel<string>('title', { required: true })
const description = defineModel<string>('description', { required: true })
const manifest = defineModel<PluginManifestDraft>('manifest', { required: true })
const pluginCode = defineModel<string>('pluginCode', { default: '' })
const sortOrder = defineModel<number>('sortOrder', { default: 0 })
</script>
