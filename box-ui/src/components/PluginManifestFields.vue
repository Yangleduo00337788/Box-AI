<template>
  <div class="plugin-manifest-fields">
    <template v-if="category === 'knowledge'">
      <p class="form-hint">上传完整文档包；安装到工作空间后会创建知识库并导入下列文件。</p>
      <t-upload :auto-upload="true" :request-method="uploadBundleFile" multiple theme="file-flow" />
      <ul v-if="manifest.bundleDocuments.length" class="bundle-list">
        <li v-for="(doc, index) in manifest.bundleDocuments" :key="doc.storageKey">
          <span>{{ doc.fileName }}</span>
          <t-link theme="danger" @click="removeBundle(index)">移除</t-link>
        </li>
      </ul>
    </template>

    <template v-else-if="category === 'skills'">
      <t-form-item label="SKILL.md">
        <t-space>
          <t-upload :auto-upload="true" :request-method="uploadSkillMd" accept=".md" theme="file">
            <t-button variant="outline" size="small">上传 Markdown</t-button>
          </t-upload>
          <t-upload :auto-upload="true" :request-method="uploadSkillZip" accept=".zip" theme="file">
            <t-button variant="outline" size="small">上传压缩包</t-button>
          </t-upload>
        </t-space>
        <p v-if="manifest.skillMdFileName" class="form-hint">已上传：{{ manifest.skillMdFileName }}</p>
        <p v-if="manifest.skillPackageFileName" class="form-hint">已上传包：{{ manifest.skillPackageFileName }}</p>
      </t-form-item>
      <t-form-item label="技能说明">
        <t-textarea
          v-model="manifest.instructions"
          :autosize="{ minRows: 4, maxRows: 12 }"
          placeholder="可选：直接粘贴 Skill 正文（文件优先）"
        />
      </t-form-item>
    </template>

    <template v-else-if="category === 'tools'">
      <t-space style="margin-bottom: 8px">
        <t-button variant="outline" size="small" @click="openOpenApiWizard">OpenAPI 导入向导</t-button>
      </t-space>
      <t-form-item label="请求方法">
        <t-select v-model="manifest.method" :options="methodOptions" />
      </t-form-item>
      <t-form-item label="请求地址">
        <t-input v-model="manifest.url" placeholder="https://api.example.com/..." />
      </t-form-item>
      <t-form-item label="超时(ms)">
        <t-input-number v-model="manifest.timeoutMs" :min="1000" :step="1000" theme="column" />
      </t-form-item>
      <t-form-item label="Headers (JSON)">
        <t-textarea v-model="manifest.headersJson" :autosize="{ minRows: 2, maxRows: 6 }" />
      </t-form-item>
      <t-form-item label="Query (JSON)">
        <t-textarea v-model="manifest.queryParamsJson" :autosize="{ minRows: 2, maxRows: 4 }" />
      </t-form-item>
      <t-form-item label="Body 类型">
        <t-select v-model="manifest.bodyType" :options="bodyTypeOptions" />
      </t-form-item>
      <t-form-item v-if="manifest.bodyType !== 'NONE'" label="Body 模板">
        <t-textarea v-model="manifest.bodyTemplate" :autosize="{ minRows: 2, maxRows: 8 }" />
      </t-form-item>
    </template>

    <template v-else-if="category === 'mcp'">
      <t-radio-group v-model="manifest.mcpInputMode" variant="default-filled" style="margin-bottom: 12px">
        <t-radio-button value="form">表单</t-radio-button>
        <t-radio-button value="json">JSON（mcpServers）</t-radio-button>
      </t-radio-group>
      <template v-if="manifest.mcpInputMode === 'form'">
        <t-form-item label="传输方式">
          <t-select v-model="manifest.transportType" :options="transportOptions" />
        </t-form-item>
        <t-form-item label="服务地址">
          <t-input v-model="manifest.endpointUrl" />
        </t-form-item>
        <t-form-item label="鉴权">
          <t-select v-model="manifest.authType" :options="authOptions" />
        </t-form-item>
        <t-form-item label="工具列表 JSON">
          <t-textarea v-model="manifest.toolCatalogJson" :autosize="{ minRows: 3, maxRows: 8 }" />
        </t-form-item>
      </template>
      <template v-else>
        <t-textarea v-model="manifest.mcpJson" :autosize="{ minRows: 10, maxRows: 20 }" />
      </template>
    </template>

    <template v-else-if="category === 'workflows'">
      <p class="form-hint">
        与 C 端「工作流编辑器」同一套画布（节点库、右侧配置、撤销/自动布局）。编排结果写入 manifest，用户安装插件时会得到同名工作流。
      </p>
      <t-space style="margin-bottom: 8px">
        <t-button variant="outline" size="small" @click="workflowDesignerVisible = true">打开画布编排</t-button>
        <t-upload :auto-upload="true" :request-method="importWorkflowFile" accept=".json" theme="file">
          <t-button variant="outline" size="small">导入 JSON 文件</t-button>
        </t-upload>
      </t-space>
      <t-textarea v-model="manifest.definitionJson" :autosize="{ minRows: 8, maxRows: 16 }" />
      <p class="form-hint">{{ workflowSummary }}</p>
    </template>

    <t-dialog
      v-model:visible="openApiVisible"
      header="OpenAPI 导入向导"
      width="760px"
      :confirm-btn="{ content: '应用到工具', disabled: !openApiSelected }"
      @confirm="applyOpenApi"
    >
      <t-space direction="vertical" style="width: 100%">
        <t-upload :auto-upload="true" :request-method="loadOpenApiFile" accept=".json,.yaml,.yml" theme="file">
          <t-button variant="outline" size="small">上传 openapi.json / .yaml</t-button>
        </t-upload>
        <t-input v-model="openApiServerOverride" placeholder="可选：覆盖 servers.url，如 https://api.example.com" />
        <t-textarea
          v-model="openApiRaw"
          :autosize="{ minRows: 8, maxRows: 14 }"
          placeholder="或粘贴 OpenAPI 3 JSON / YAML"
        />
        <t-button variant="outline" size="small" @click="refreshOpenApiOperations">解析接口列表</t-button>
        <t-select
          v-if="openApiOperations.length"
          v-model="openApiSelected"
          :options="openApiOperations.map((o) => ({ label: o.summary, value: o.id }))"
          placeholder="选择要导入的 operation"
        />
      </t-space>
    </t-dialog>

    <plugin-workflow-designer-dialog
      v-model:visible="workflowDesignerVisible"
      v-model:definition-json="manifest.definitionJson"
      :editor-host="workflowEditorHost"
      :platform-models="workflowPlatformModels"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PluginManifestDraft } from '../plugin/pluginCatalogManifest'
import { workflowNodeSummary } from '../plugin/pluginCatalogManifest'
import PluginWorkflowDesignerDialog from './PluginWorkflowDesignerDialog.vue'
import {
  buildToolFromOpenApiOperation,
  listOpenApiOperations,
  parseOpenApiDocument,
  type OpenApiOperationOption,
} from '../plugin/openapiToolImport'
import type { WorkflowEditorHost, WorkflowEditorPlatformModel } from '../workflow/workflowEditorHost'
import { emptyWorkflowEditorHost } from '../workflow/workflowEditorHost'

export interface PluginCatalogAssetUploadResult {
  storageKey: string
  fileName: string
  size: number
}

const manifest = defineModel<PluginManifestDraft>('manifest', { required: true })

const props = withDefaults(
  defineProps<{
    category: string
    uploadAsset: (file: File) => Promise<PluginCatalogAssetUploadResult>
    workflowEditorHost?: WorkflowEditorHost
    workflowPlatformModels?: WorkflowEditorPlatformModel[]
  }>(),
  {
    workflowEditorHost: () => emptyWorkflowEditorHost,
    workflowPlatformModels: () => [],
  },
)

const workflowEditorHost = computed(() => props.workflowEditorHost)
const workflowPlatformModels = computed(() => props.workflowPlatformModels)

const openApiVisible = ref(false)
const openApiRaw = ref('')
const openApiServerOverride = ref('')
const openApiSelected = ref('')
const openApiOperations = ref<OpenApiOperationOption[]>([])
const openApiDoc = ref<unknown>(null)
const workflowDesignerVisible = ref(false)

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
]
const transportOptions = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'SSE', value: 'SSE' },
]
const authOptions = [
  { label: '无', value: 'NONE' },
  { label: 'Bearer', value: 'BEARER' },
]
const bodyTypeOptions = [
  { label: '无', value: 'NONE' },
  { label: 'JSON', value: 'JSON' },
  { label: 'Form', value: 'FORM' },
  { label: 'Raw', value: 'RAW' },
]

const workflowSummary = computed(() => workflowNodeSummary(manifest.value.definitionJson))

function openOpenApiWizard() {
  openApiVisible.value = true
  if (openApiRaw.value.trim()) {
    refreshOpenApiOperations()
  }
}

function refreshOpenApiOperations() {
  try {
    openApiDoc.value = parseOpenApiDocument(openApiRaw.value)
    openApiOperations.value = listOpenApiOperations(openApiDoc.value)
    openApiSelected.value = openApiOperations.value[0]?.id || ''
  } catch (e) {
    MessagePlugin.warning(e instanceof Error ? e.message : 'OpenAPI 解析失败')
  }
}

async function loadOpenApiFile(file: { raw?: File } | File) {
  const raw = resolveUploadFile(file)
  if (!raw) return { status: 'fail' as const, error: '无效文件' }
  openApiRaw.value = await raw.text()
  refreshOpenApiOperations()
  MessagePlugin.success('已加载 OpenAPI 文件')
  return { status: 'success' as const, response: {} }
}

function applyOpenApi() {
  try {
    if (!openApiDoc.value) refreshOpenApiOperations()
    if (!openApiSelected.value) {
      MessagePlugin.warning('请选择接口')
      return false
    }
    const tool = buildToolFromOpenApiOperation(
      openApiDoc.value,
      openApiSelected.value,
      openApiServerOverride.value,
    )
    manifest.value.method = tool.method
    manifest.value.url = tool.url
    manifest.value.headersJson = tool.headersJson
    manifest.value.queryParamsJson = tool.queryParamsJson
    manifest.value.bodyType = tool.bodyType
    manifest.value.bodyTemplate = tool.bodyTemplate
    openApiVisible.value = false
    MessagePlugin.success('已从 OpenAPI 填充工具配置')
  } catch (e) {
    MessagePlugin.warning(e instanceof Error ? e.message : 'OpenAPI 导入失败')
    return false
  }
  return true
}

function resolveUploadFile(file: { raw?: File } | File): File | null {
  if (file instanceof File) return file
  return file.raw ?? null
}

async function uploadBundleFile(file: { raw?: File } | File) {
  const raw = resolveUploadFile(file)
  if (!raw) return { status: 'fail' as const, error: '无效文件' }
  return wrapUpload(raw, (asset) => {
    manifest.value.bundleDocuments.push({
      storageKey: asset.storageKey,
      fileName: asset.fileName,
      size: asset.size,
    })
  })
}

async function uploadSkillMd(file: { raw?: File } | File) {
  const raw = resolveUploadFile(file)
  if (!raw) return { status: 'fail' as const, error: '无效文件' }
  return wrapUpload(raw, (asset) => {
    manifest.value.skillMdStorageKey = asset.storageKey
    manifest.value.skillMdFileName = asset.fileName
  })
}

async function uploadSkillZip(file: { raw?: File } | File) {
  const raw = resolveUploadFile(file)
  if (!raw) return { status: 'fail' as const, error: '无效文件' }
  return wrapUpload(raw, (asset) => {
    manifest.value.skillPackageStorageKey = asset.storageKey
    manifest.value.skillPackageFileName = asset.fileName
  })
}

async function importWorkflowFile(file: { raw?: File } | File) {
  const raw = resolveUploadFile(file)
  if (!raw) return { status: 'fail' as const, error: '无效文件' }
  manifest.value.definitionJson = await raw.text()
  MessagePlugin.success('已导入工作流 JSON')
  return { status: 'success' as const, response: {} }
}

function removeBundle(index: number) {
  manifest.value.bundleDocuments.splice(index, 1)
}

async function wrapUpload(file: File, onSuccess: (asset: PluginCatalogAssetUploadResult) => void) {
  try {
    const asset = await props.uploadAsset(file)
    onSuccess(asset)
    MessagePlugin.success(`${file.name} 已上传`)
    return { status: 'success' as const, response: asset }
  } catch (e) {
    const msg = e instanceof Error ? e.message : '上传失败'
    MessagePlugin.error(msg)
    return { status: 'fail' as const, error: msg }
  }
}
</script>

<style scoped>
.form-hint {
  margin: 0 0 12px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  line-height: 1.5;
}
.bundle-list {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}
.bundle-list li {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid var(--td-component-border);
  font-size: 13px;
}
</style>
