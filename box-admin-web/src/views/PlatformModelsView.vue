<template>
  <div class="platform-models-page">
    <page-header title="平台模型池" desc="接入上游模型服务商，配置平台密钥，并从上游拉取模型供 C 端选用。" />

    <t-card :bordered="true" class="models-card">
      <t-tabs v-model="tab">
        <t-tab-panel value="providers" label="服务商" />
        <t-tab-panel value="models" label="模型" />
        <t-tab-panel value="keys" label="平台密钥" />
      </t-tabs>

      <div class="tab-body">
        <div v-if="tab === 'providers'" class="toolbar">
          <t-button theme="primary" @click="openProvider()">新建服务商</t-button>
        </div>
        <t-table
          v-if="tab === 'providers'"
          row-key="id"
          :data="providers"
          :columns="providerColumns"
          :loading="loading"
          bordered
          stripe
        />

        <div v-if="tab === 'models'" class="toolbar">
          <t-space>
            <t-select
              v-model="modelProviderId"
              :options="providerFilterOptions"
              placeholder="按服务商筛选"
              clearable
              filterable
              style="width: 240px"
              @change="loadModels"
            />
            <t-button
              theme="primary"
              :disabled="!modelProviderId"
              :loading="upstreamLoading"
              @click="openImport"
            >
              从上游获取
            </t-button>
            <t-button
              variant="outline"
              :disabled="!modelProviderId"
              :loading="saving"
              @click="syncLimits"
            >
              从上游同步参数
            </t-button>
            <t-button variant="outline" @click="openModel()">手动新建</t-button>
          </t-space>
        </div>
        <t-table
          v-if="tab === 'models'"
          row-key="id"
          :data="models"
          :columns="modelColumns"
          :loading="loading"
          bordered
          stripe
        />

        <div v-if="tab === 'keys'" class="toolbar">
          <t-space>
            <t-select
              v-model="credentialProviderId"
              :options="providerOptions"
              placeholder="选择服务商"
              filterable
              style="width: 240px"
              @change="loadCredentials"
            />
            <t-button theme="primary" :disabled="!credentialProviderId" @click="openKey">新增密钥</t-button>
          </t-space>
        </div>
        <t-table
          v-if="tab === 'keys'"
          row-key="id"
          :data="credentials"
          :columns="keyColumns"
          :loading="credentialLoading"
          bordered
          stripe
        />
      </div>
    </t-card>

    <t-dialog
      v-model:visible="providerVisible"
      :header="editingProviderId ? '编辑服务商' : '新建服务商'"
      width="520px"
      :confirm-btn="{ loading: saving }"
      @confirm="submitProvider"
    >
      <t-form ref="providerFormRef" :data="providerForm" :rules="providerRules" label-width="100px">
        <t-form-item label="编码" name="providerCode">
          <t-input v-model="providerForm.providerCode" :disabled="!!editingProviderId" placeholder="如 openai、deepseek" />
        </t-form-item>
        <t-form-item label="名称" name="providerName">
          <t-input v-model="providerForm.providerName" placeholder="如 OpenAI" />
        </t-form-item>
        <t-form-item label="接口地址" name="baseUrl">
          <t-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com/v1" />
        </t-form-item>
        <t-form-item v-if="editingProviderId" label="状态" name="status">
          <t-radio-group v-model="providerForm.status">
            <t-radio :value="1">启用</t-radio>
            <t-radio :value="0">停用</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="modelVisible"
      :header="editingModelId ? '编辑模型' : '手动新建模型'"
      width="520px"
      :confirm-btn="{ loading: saving }"
      @confirm="submitModel"
    >
      <t-form ref="modelFormRef" :data="modelForm" :rules="modelRules" label-width="100px">
        <t-form-item label="服务商" name="providerId">
          <t-select v-model="modelForm.providerId" :options="providerOptions" :disabled="!!editingModelId" placeholder="请选择" />
        </t-form-item>
        <t-form-item v-if="!editingModelId" label="模型编码" name="modelCode">
          <t-input v-model="modelForm.modelCode" placeholder="如 gpt-4o-mini" />
        </t-form-item>
        <t-form-item label="显示名称" name="modelName">
          <t-input v-model="modelForm.modelName" placeholder="如 GPT-4o Mini" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="modelForm.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="上下文窗口" name="contextWindow">
          <t-input-number v-model="modelForm.contextWindow" :min="0" theme="column" placeholder="暂无则留空" />
        </t-form-item>
        <t-form-item label="最大输出" name="maxOutputTokens">
          <t-input-number v-model="modelForm.maxOutputTokens" :min="0" theme="column" placeholder="暂无则留空" />
        </t-form-item>
        <t-form-item v-if="editingModelId" label="状态" name="status">
          <t-radio-group v-model="modelForm.status">
            <t-radio :value="1">启用</t-radio>
            <t-radio :value="0">停用</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="importVisible"
      header="从上游获取模型"
      width="640px"
      placement="center"
      :confirm-on-enter="false"
      :confirm-btn="{ content: `导入选中（${selectedUpstream.length}）`, loading: saving, disabled: !selectedUpstream.length }"
      @confirm="submitImport"
    >
      <p class="hint">
        将调用该服务商 OpenAI 兼容的 <code>/models</code> 接口。上下文和最大输出仅在上游返回真实数值时写入，未返回则显示「暂无」。
      </p>
      <t-input
        v-model="upstreamKeyword"
        clearable
        placeholder="搜索模型编码"
        class="import-search"
      >
        <template #prefixIcon>
          <SearchIcon />
        </template>
      </t-input>
      <div class="import-toolbar">
        <t-checkbox
          :checked="filteredAllChecked"
          :indeterminate="filteredIndeterminate"
          :disabled="!importableFiltered.length"
          @change="toggleFilteredAll"
        >
          全选未导入
        </t-checkbox>
        <t-button
          variant="text"
          theme="primary"
          size="small"
          :disabled="!selectedUpstream.length"
          @click="selectedUpstream = []"
        >
          清空选择
        </t-button>
        <span class="import-count">
          已选 {{ selectedUpstream.length }} / 可导入 {{ importableModels.length }}，共 {{ upstreamModels.length }}
        </span>
      </div>
      <t-loading :loading="upstreamLoading" size="small">
        <div class="import-list">
          <t-empty v-if="!upstreamLoading && !filteredUpstream.length" description="没有匹配的模型" />
          <t-checkbox-group v-else v-model="selectedUpstream" lazy-load>
            <t-checkbox
              v-for="item in filteredUpstream"
              :key="item.modelCode"
              :value="item.modelCode"
              :disabled="item.imported"
              :title="item.modelCode"
            >
              <span class="import-code">{{ item.modelName || item.modelCode }}</span>
              <span class="import-limits">
                {{ formatTokens(item.contextWindow) }} / {{ formatTokens(item.maxOutputTokens) }}
              </span>
              <span v-if="item.imported" class="import-imported">已导入</span>
            </t-checkbox>
          </t-checkbox-group>
        </div>
      </t-loading>
    </t-dialog>

    <t-dialog v-model:visible="keyVisible" header="新增平台密钥" width="520px" :confirm-btn="{ loading: saving }" @confirm="submitKey">
      <t-form ref="keyFormRef" :data="keyForm" :rules="keyRules" label-width="100px">
        <t-form-item label="服务商" name="providerId">
          <t-select v-model="keyForm.providerId" :options="providerOptions" placeholder="请选择" />
        </t-form-item>
        <t-form-item label="密钥名称" name="credentialName">
          <t-input v-model="keyForm.credentialName" placeholder="如 生产环境主密钥" />
        </t-form-item>
        <t-form-item label="API Key" name="apiKey">
          <t-input v-model="keyForm.apiKey" type="password" placeholder="密钥将加密存储" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { SearchIcon } from 'tdesign-icons-vue-next'
import { DialogPlugin, MessagePlugin, Space } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createPlatformCredential,
  createPlatformModel,
  createPlatformProvider,
  deletePlatformCredential,
  deletePlatformModel,
  deletePlatformProvider,
  fetchPlatformCredentials,
  fetchPlatformModels,
  fetchPlatformProviders,
  fetchUpstreamModels,
  importUpstreamModels,
  syncMissingModelLimits,
  updatePlatformModel,
  updatePlatformProvider,
  type PlatformCredentialVO,
  type PlatformModelVO,
  type PlatformProviderVO,
  type UpstreamModelVO,
} from '@/api/platform'

const tab = ref('providers')
const loading = ref(false)
const credentialLoading = ref(false)
const upstreamLoading = ref(false)
const saving = ref(false)
const providers = ref<PlatformProviderVO[]>([])
const models = ref<PlatformModelVO[]>([])
const credentials = ref<PlatformCredentialVO[]>([])
const upstreamModels = ref<UpstreamModelVO[]>([])
const selectedUpstream = ref<string[]>([])
const upstreamKeyword = ref('')
const credentialProviderId = ref<number | undefined>()
const modelProviderId = ref<number | undefined>()

const providerVisible = ref(false)
const modelVisible = ref(false)
const importVisible = ref(false)
const keyVisible = ref(false)
const editingProviderId = ref<number | null>(null)
const editingModelId = ref<number | null>(null)
const providerFormRef = ref<FormInstanceFunctions>()
const modelFormRef = ref<FormInstanceFunctions>()
const keyFormRef = ref<FormInstanceFunctions>()

const providerForm = reactive({
  providerCode: '',
  providerName: '',
  providerType: 'OPENAI_COMPATIBLE',
  baseUrl: '',
  status: 1,
})
const modelForm = reactive({
  providerId: undefined as number | undefined,
  modelCode: '',
  modelName: '',
  description: '',
  contextWindow: undefined as number | undefined,
  maxOutputTokens: undefined as number | undefined,
  status: 1,
})
const keyForm = reactive({
  providerId: undefined as number | undefined,
  credentialName: '',
  apiKey: '',
})

const providerRules: FormProps['rules'] = {
  providerCode: [{ required: true, message: '请输入编码' }],
  providerName: [{ required: true, message: '请输入名称' }],
}
const modelRules: FormProps['rules'] = {
  providerId: [{ required: true, message: '请选择服务商' }],
  modelCode: [{ required: true, message: '请输入模型编码' }],
  modelName: [{ required: true, message: '请输入显示名称' }],
}
const keyRules: FormProps['rules'] = {
  providerId: [{ required: true, message: '请选择服务商' }],
  credentialName: [{ required: true, message: '请输入密钥名称' }],
  apiKey: [{ required: true, message: '请输入 API Key' }],
}

const providerOptions = computed(() =>
  providers.value.map((item) => ({ label: item.providerName, value: item.id })),
)

const providerFilterOptions = computed(() => providerOptions.value)

const importableModels = computed(() => upstreamModels.value.filter((item) => !item.imported))

const filteredUpstream = computed(() => {
  const keyword = upstreamKeyword.value.trim().toLowerCase()
  if (!keyword) return upstreamModels.value
  return upstreamModels.value.filter((item) => {
    const code = (item.modelCode || '').toLowerCase()
    const name = (item.modelName || '').toLowerCase()
    return code.includes(keyword) || name.includes(keyword)
  })
})

const importableFiltered = computed(() => filteredUpstream.value.filter((item) => !item.imported))

const filteredAllChecked = computed(
  () =>
    importableFiltered.value.length > 0 &&
    importableFiltered.value.every((item) => selectedUpstream.value.includes(item.modelCode)),
)

const filteredIndeterminate = computed(() => {
  const selected = importableFiltered.value.filter((item) => selectedUpstream.value.includes(item.modelCode)).length
  return selected > 0 && selected < importableFiltered.value.length
})

function toggleFilteredAll(checked: boolean) {
  const codes = new Set(selectedUpstream.value)
  for (const item of importableFiltered.value) {
    if (checked) codes.add(item.modelCode)
    else codes.delete(item.modelCode)
  }
  selectedUpstream.value = [...codes]
}

const providerColumns: PrimaryTableCol<PlatformProviderVO>[] = [
  { colKey: 'providerName', title: '名称', width: 140 },
  { colKey: 'providerCode', title: '编码', width: 120 },
  { colKey: 'providerType', title: '类型', width: 160 },
  { colKey: 'baseUrl', title: '接口地址', ellipsis: true },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) => (row.status === 1 ? '启用' : '停用'),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    cell: (_, { row }) =>
      h(Space, { size: 'small' }, () => [
        h('a', { href: 'javascript:void(0)', onClick: () => openProvider(row) }, '编辑'),
        h('a', { href: 'javascript:void(0)', onClick: () => confirmDeleteProvider(row) }, '删除'),
      ]),
  },
]

function formatTokens(value?: number | null) {
  if (value == null || value <= 0) return '暂无'
  if (value >= 1_000_000 && value % 1_000_000 === 0) return `${value / 1_000_000}M`
  if (value >= 1000 && value % 1000 === 0) return `${value / 1000}K`
  if (value >= 1000) return `${Math.round(value / 100) / 10}K`
  return String(value)
}

const modelColumns: PrimaryTableCol<PlatformModelVO>[] = [
  { colKey: 'modelName', title: '名称', width: 160 },
  { colKey: 'modelCode', title: '编码', width: 160 },
  { colKey: 'providerName', title: '服务商', width: 120 },
  {
    colKey: 'contextWindow',
    title: '上下文',
    width: 100,
    cell: (_, { row }) => formatTokens(row.contextWindow),
  },
  {
    colKey: 'maxOutputTokens',
    title: '最大输出',
    width: 100,
    cell: (_, { row }) => formatTokens(row.maxOutputTokens),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) => (row.status === 1 ? '启用' : '停用'),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    cell: (_, { row }) =>
      h(Space, { size: 'small' }, () => [
        h('a', { href: 'javascript:void(0)', onClick: () => openModel(row) }, '编辑'),
        h('a', { href: 'javascript:void(0)', onClick: () => confirmDeleteModel(row) }, '删除'),
      ]),
  },
]

const keyColumns: PrimaryTableCol<PlatformCredentialVO>[] = [
  { colKey: 'credentialName', title: '名称', width: 160 },
  { colKey: 'apiKeyMasked', title: '密钥', ellipsis: true },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) => (row.status === 1 ? '启用' : '停用'),
  },
  { colKey: 'lastUsedAt', title: '最近使用', width: 180 },
  {
    colKey: 'actions',
    title: '操作',
    width: 80,
    cell: (_, { row }) =>
      h('a', { href: 'javascript:void(0)', onClick: () => confirmDeleteKey(row) }, '删除'),
  },
]

async function loadProviders() {
  const { data } = await fetchPlatformProviders()
  providers.value = data.data || []
  if (!credentialProviderId.value && providers.value.length) {
    credentialProviderId.value = providers.value[0].id
  }
}

async function loadModels() {
  loading.value = true
  try {
    const { data } = await fetchPlatformModels(modelProviderId.value)
    models.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function loadBase() {
  loading.value = true
  try {
    await loadProviders()
    const { data } = await fetchPlatformModels(modelProviderId.value)
    models.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function loadCredentials() {
  if (!credentialProviderId.value) {
    credentials.value = []
    return
  }
  credentialLoading.value = true
  try {
    const { data } = await fetchPlatformCredentials(credentialProviderId.value)
    credentials.value = data.data || []
  } finally {
    credentialLoading.value = false
  }
}

function openProvider(row?: PlatformProviderVO) {
  editingProviderId.value = row?.id ?? null
  providerForm.providerCode = row?.providerCode || ''
  providerForm.providerName = row?.providerName || ''
  providerForm.providerType = row?.providerType || 'OPENAI_COMPATIBLE'
  providerForm.baseUrl = row?.baseUrl || ''
  providerForm.status = row?.status ?? 1
  providerVisible.value = true
}

function openModel(row?: PlatformModelVO) {
  editingModelId.value = row?.id ?? null
  modelForm.providerId = row?.providerId || modelProviderId.value || providers.value[0]?.id
  modelForm.modelCode = row?.modelCode || ''
  modelForm.modelName = row?.modelName || ''
  modelForm.description = row?.description || ''
  modelForm.contextWindow = row?.contextWindow || undefined
  modelForm.maxOutputTokens = row?.maxOutputTokens || undefined
  modelForm.status = row?.status ?? 1
  modelVisible.value = true
}

async function openImport() {
  if (!modelProviderId.value) {
    MessagePlugin.warning('请先选择服务商')
    return
  }
  selectedUpstream.value = []
  upstreamKeyword.value = ''
  upstreamLoading.value = true
  importVisible.value = true
  try {
    const { data } = await fetchUpstreamModels(modelProviderId.value)
    upstreamModels.value = data.data || []
  } catch {
    importVisible.value = false
  } finally {
    upstreamLoading.value = false
  }
}

function openKey() {
  keyForm.providerId = credentialProviderId.value || providers.value[0]?.id
  keyForm.credentialName = ''
  keyForm.apiKey = ''
  keyVisible.value = true
}

function optionalTokenLimit(value?: number) {
  return value != null && value > 0 ? value : undefined
}

async function submitProvider() {
  const valid = await providerFormRef.value?.validate()
  if (valid !== true) return false
  saving.value = true
  try {
    if (editingProviderId.value) {
      await updatePlatformProvider(editingProviderId.value, {
        providerName: providerForm.providerName.trim(),
        providerType: providerForm.providerType,
        baseUrl: providerForm.baseUrl.trim() || undefined,
        status: providerForm.status,
      })
      MessagePlugin.success('服务商已更新')
    } else {
      await createPlatformProvider({
        providerCode: providerForm.providerCode.trim(),
        providerName: providerForm.providerName.trim(),
        providerType: providerForm.providerType,
        baseUrl: providerForm.baseUrl.trim() || undefined,
      })
      MessagePlugin.success('服务商已创建')
    }
    providerVisible.value = false
    await loadBase()
  } finally {
    saving.value = false
  }
  return true
}

async function submitModel() {
  const valid = await modelFormRef.value?.validate()
  if (valid !== true || !modelForm.providerId) return false
  saving.value = true
  try {
    if (editingModelId.value) {
      await updatePlatformModel(editingModelId.value, {
        modelName: modelForm.modelName.trim(),
        description: modelForm.description.trim() || undefined,
        contextWindow: optionalTokenLimit(modelForm.contextWindow),
        maxOutputTokens: optionalTokenLimit(modelForm.maxOutputTokens),
        status: modelForm.status,
      })
      MessagePlugin.success('模型已更新')
    } else {
      await createPlatformModel({
        providerId: modelForm.providerId,
        modelCode: modelForm.modelCode.trim(),
        modelName: modelForm.modelName.trim(),
        description: modelForm.description.trim() || undefined,
        contextWindow: optionalTokenLimit(modelForm.contextWindow),
        maxOutputTokens: optionalTokenLimit(modelForm.maxOutputTokens),
      })
      MessagePlugin.success('模型已创建')
    }
    modelVisible.value = false
    await loadModels()
  } finally {
    saving.value = false
  }
  return true
}

async function submitImport() {
  if (!modelProviderId.value || !selectedUpstream.value.length) return false
  saving.value = true
  try {
    await importUpstreamModels(modelProviderId.value, selectedUpstream.value)
    MessagePlugin.success('已导入上游模型')
    importVisible.value = false
    await loadModels()
  } finally {
    saving.value = false
  }
  return true
}

async function syncLimits() {
  if (!modelProviderId.value) {
    MessagePlugin.warning('请先选择服务商')
    return
  }
  saving.value = true
  try {
    const { data } = await syncMissingModelLimits(modelProviderId.value)
    const count = data.data ?? 0
    MessagePlugin.success(count ? `已按上游更新 ${count} 个模型的参数` : '上游未返回上下文或最大输出，已保持为暂无')
    await loadModels()
  } finally {
    saving.value = false
  }
}

async function submitKey() {
  const valid = await keyFormRef.value?.validate()
  if (valid !== true || !keyForm.providerId) return false
  saving.value = true
  try {
    await createPlatformCredential({
      providerId: keyForm.providerId,
      credentialName: keyForm.credentialName.trim(),
      apiKey: keyForm.apiKey.trim(),
    })
    MessagePlugin.success('平台密钥已保存')
    keyVisible.value = false
    await loadCredentials()
  } finally {
    saving.value = false
  }
  return true
}

function confirmDeleteProvider(row: PlatformProviderVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除服务商',
    body: `将同时删除「${row.providerName}」下的模型与密钥，确定继续？`,
    theme: 'danger',
    onConfirm: async () => {
      await deletePlatformProvider(row.id)
      MessagePlugin.success('服务商已删除')
      dialog.destroy()
      if (modelProviderId.value === row.id) modelProviderId.value = undefined
      if (credentialProviderId.value === row.id) credentialProviderId.value = undefined
      await loadBase()
    },
  })
}

function confirmDeleteModel(row: PlatformModelVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除模型',
    body: `确定删除「${row.modelName}」？`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePlatformModel(row.id)
      MessagePlugin.success('模型已删除')
      dialog.destroy()
      await loadModels()
    },
  })
}

function confirmDeleteKey(row: PlatformCredentialVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除密钥',
    body: `确定删除「${row.credentialName}」？`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePlatformCredential(row.id)
      MessagePlugin.success('密钥已删除')
      dialog.destroy()
      await loadCredentials()
    },
  })
}

watch(tab, (value) => {
  if (value === 'keys') loadCredentials()
  if (value === 'models') loadModels()
})

onMounted(loadBase)
</script>

<style scoped>
.models-card {
  overflow: hidden;
}

.tab-body {
  margin-top: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.hint {
  margin: 0 0 12px;
  color: var(--box-muted, #8f959e);
  font-size: 13px;
  line-height: 1.6;
}

.import-search {
  width: 100%;
  margin-bottom: 10px;
}

.import-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.import-count {
  margin-left: auto;
  color: var(--box-muted, #8f959e);
  font-size: 12px;
}

.import-list {
  max-height: 420px;
  overflow-y: auto;
  padding: 8px 12px;
  border: 1px solid var(--td-component-border, #dcdcdc);
  border-radius: 6px;
}

.import-list :deep(.t-checkbox-group) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 2px;
}

.import-list :deep(.t-checkbox) {
  margin-right: 0;
  width: 100%;
  padding: 6px 8px;
  border-radius: 4px;
}

.import-list :deep(.t-checkbox):hover {
  background: var(--td-bg-color-container-hover, #f3f3f3);
}

.import-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
}

.import-limits {
  margin-left: 8px;
  color: var(--box-muted, #8f959e);
  font-size: 12px;
  white-space: nowrap;
}

.import-imported {
  margin-left: 8px;
  color: var(--box-muted, #8f959e);
  font-size: 12px;
}
</style>
