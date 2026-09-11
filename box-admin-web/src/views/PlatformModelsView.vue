<template>
  <div class="platform-models-page">
    <page-header title="平台模型池" desc="接入上游模型服务商，配置平台密钥，供 C 端用户选用并扣配额。" />

    <t-card :bordered="true" class="models-card">
      <t-tabs v-model="tab">
        <t-tab-panel value="providers" label="服务商" />
        <t-tab-panel value="models" label="模型" />
        <t-tab-panel value="keys" label="平台密钥" />
      </t-tabs>

      <div class="tab-body">
        <div v-if="tab === 'providers'" class="toolbar">
          <t-button theme="primary" @click="openProvider">新建服务商</t-button>
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
          <t-button theme="primary" @click="openModel">新建模型</t-button>
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
              style="width: 220px"
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

    <t-dialog v-model:visible="providerVisible" header="新建服务商" width="520px" :confirm-btn="{ loading: saving }" @confirm="submitProvider">
      <t-form ref="providerFormRef" :data="providerForm" :rules="providerRules" label-width="100px">
        <t-form-item label="编码" name="providerCode">
          <t-input v-model="providerForm.providerCode" placeholder="如 openai、deepseek" />
        </t-form-item>
        <t-form-item label="名称" name="providerName">
          <t-input v-model="providerForm.providerName" placeholder="如 OpenAI" />
        </t-form-item>
        <t-form-item label="接口地址" name="baseUrl">
          <t-input v-model="providerForm.baseUrl" placeholder="https://api.openai.com/v1" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog v-model:visible="modelVisible" header="新建模型" width="520px" :confirm-btn="{ loading: saving }" @confirm="submitModel">
      <t-form ref="modelFormRef" :data="modelForm" :rules="modelRules" label-width="100px">
        <t-form-item label="服务商" name="providerId">
          <t-select v-model="modelForm.providerId" :options="providerOptions" placeholder="请选择" />
        </t-form-item>
        <t-form-item label="模型编码" name="modelCode">
          <t-input v-model="modelForm.modelCode" placeholder="如 gpt-4o-mini" />
        </t-form-item>
        <t-form-item label="显示名称" name="modelName">
          <t-input v-model="modelForm.modelName" placeholder="如 GPT-4o Mini" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="modelForm.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="上下文窗口" name="contextWindow">
          <t-input-number v-model="modelForm.contextWindow" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="最大输出" name="maxOutputTokens">
          <t-input-number v-model="modelForm.maxOutputTokens" :min="0" theme="column" />
        </t-form-item>
      </t-form>
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createPlatformCredential,
  createPlatformModel,
  createPlatformProvider,
  fetchPlatformCredentials,
  fetchPlatformModels,
  fetchPlatformProviders,
  type PlatformCredentialVO,
  type PlatformModelVO,
  type PlatformProviderVO,
} from '@/api/platform'

const tab = ref('providers')
const loading = ref(false)
const credentialLoading = ref(false)
const saving = ref(false)
const providers = ref<PlatformProviderVO[]>([])
const models = ref<PlatformModelVO[]>([])
const credentials = ref<PlatformCredentialVO[]>([])
const credentialProviderId = ref<number | undefined>()

const providerVisible = ref(false)
const modelVisible = ref(false)
const keyVisible = ref(false)
const providerFormRef = ref<FormInstanceFunctions>()
const modelFormRef = ref<FormInstanceFunctions>()
const keyFormRef = ref<FormInstanceFunctions>()

const providerForm = reactive({
  providerCode: '',
  providerName: '',
  providerType: 'OPENAI_COMPATIBLE',
  baseUrl: '',
})
const modelForm = reactive({
  providerId: undefined as number | undefined,
  modelCode: '',
  modelName: '',
  description: '',
  contextWindow: 128000,
  maxOutputTokens: 4096,
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
]

const modelColumns: PrimaryTableCol<PlatformModelVO>[] = [
  { colKey: 'modelName', title: '名称', width: 160 },
  { colKey: 'modelCode', title: '编码', width: 160 },
  { colKey: 'providerName', title: '服务商', width: 120 },
  { colKey: 'contextWindow', title: '上下文', width: 100 },
  { colKey: 'maxOutputTokens', title: '最大输出', width: 100 },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) => (row.status === 1 ? '启用' : '停用'),
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
]

async function loadBase() {
  loading.value = true
  try {
    const [providerRes, modelRes] = await Promise.all([fetchPlatformProviders(), fetchPlatformModels()])
    providers.value = providerRes.data.data || []
    models.value = modelRes.data.data || []
    if (!credentialProviderId.value && providers.value.length) {
      credentialProviderId.value = providers.value[0].id
    }
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

function openProvider() {
  providerForm.providerCode = ''
  providerForm.providerName = ''
  providerForm.baseUrl = ''
  providerVisible.value = true
}

function openModel() {
  modelForm.providerId = providers.value[0]?.id
  modelForm.modelCode = ''
  modelForm.modelName = ''
  modelForm.description = ''
  modelForm.contextWindow = 128000
  modelForm.maxOutputTokens = 4096
  modelVisible.value = true
}

function openKey() {
  keyForm.providerId = credentialProviderId.value || providers.value[0]?.id
  keyForm.credentialName = ''
  keyForm.apiKey = ''
  keyVisible.value = true
}

async function submitProvider() {
  const valid = await providerFormRef.value?.validate()
  if (valid !== true) return false
  saving.value = true
  try {
    await createPlatformProvider({
      providerCode: providerForm.providerCode.trim(),
      providerName: providerForm.providerName.trim(),
      providerType: providerForm.providerType,
      baseUrl: providerForm.baseUrl.trim() || undefined,
    })
    MessagePlugin.success('服务商已创建')
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
    await createPlatformModel({
      providerId: modelForm.providerId,
      modelCode: modelForm.modelCode.trim(),
      modelName: modelForm.modelName.trim(),
      description: modelForm.description.trim() || undefined,
      contextWindow: modelForm.contextWindow,
      maxOutputTokens: modelForm.maxOutputTokens,
    })
    MessagePlugin.success('模型已创建')
    modelVisible.value = false
    await loadBase()
  } finally {
    saving.value = false
  }
  return true
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

watch(tab, (value) => {
  if (value === 'keys') {
    loadCredentials()
  }
})

onMounted(async () => {
  await loadBase()
  if (tab.value === 'keys') {
    await loadCredentials()
  }
})
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
</style>
