<template>
  <div>
    <page-header
      title="模型"
      desc="管理工作空间自定义模型（BYOK）；对话中的平台模型在聊天页选择"
    />

    <t-card :bordered="true" hover-shadow class="models-card">
      <t-alert
        v-if="!byokEnabled"
        theme="info"
        title="暂未启用自定义模型"
        message="当前工作空间使用平台模型池。升级企业专业版后可配置自带密钥（BYOK）与自定义模型。"
        class="byok-hint"
      />

      <template v-else>
        <t-tabs v-model="tab">
          <t-tab-panel value="providers" label="服务商" />
          <t-tab-panel value="models" label="自定义模型" />
          <t-tab-panel value="keys" label="API 密钥" />
        </t-tabs>

        <div class="tab-body">
          <div v-if="tab === 'providers'" class="toolbar">
            <t-button theme="primary" @click="openProvider">
              <template #icon><t-icon name="add" /></template>
              新建服务商
            </t-button>
          </div>
          <t-table
            v-if="tab === 'providers'"
            row-key="id"
            :data="providers"
            :columns="providerColumns"
            :bordered="true"
            stripe
            hover
          >
            <template #empty>
              <t-empty description="暂无服务商，请先创建" />
            </template>
            <template #op="{ row }">
              <t-button theme="danger" variant="text" @click="removeProvider(row.id)">删除</t-button>
            </template>
          </t-table>

          <div v-if="tab === 'models'" class="toolbar">
            <t-button theme="primary" @click="openModel">
              <template #icon><t-icon name="add" /></template>
              新建自定义模型
            </t-button>
          </div>
          <t-table
            v-if="tab === 'models'"
            row-key="id"
            :data="models"
            :columns="modelColumns"
            :bordered="true"
            stripe
            hover
          >
            <template #empty>
              <t-empty description="暂无自定义模型，请先创建服务商与模型" />
            </template>
            <template #op="{ row }">
              <t-space>
                <t-button variant="text" theme="primary" @click="openTest(row)">测试</t-button>
                <t-button theme="danger" variant="text" @click="removeModel(row.id)">删除</t-button>
              </t-space>
            </template>
          </t-table>

          <div v-if="tab === 'keys'" class="toolbar">
            <t-button theme="primary" @click="openKey">
              <template #icon><t-icon name="add" /></template>
              新增密钥
            </t-button>
          </div>
          <t-table
            v-if="tab === 'keys'"
            row-key="id"
            :data="credentials"
            :columns="keyColumns"
            :bordered="true"
            stripe
            hover
          >
            <template #empty>
              <t-empty description="暂无密钥，请先添加" />
            </template>
            <template #op="{ row }">
              <t-button theme="danger" variant="text" @click="removeKey(row.id)">删除</t-button>
            </template>
          </t-table>
        </div>
      </template>
    </t-card>
  </div>

  <t-dialog v-model:visible="providerVisible" header="新建服务商" :footer="false" width="520px">
    <t-form :data="providerForm" :rules="providerRules" label-align="top" @submit="submitProvider">
      <t-form-item label="服务商编码" name="providerCode">
        <t-input v-model="providerForm.providerCode" placeholder="如 deepseek、qwen、openai" />
      </t-form-item>
      <t-form-item label="服务商名称" name="providerName">
        <t-input v-model="providerForm.providerName" placeholder="如 DeepSeek" />
      </t-form-item>
      <t-form-item label="接口地址" name="baseUrl">
        <t-input v-model="providerForm.baseUrl" placeholder="https://api.deepseek.com/v1" />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit" :loading="saving">保存</t-button>
          <t-button variant="outline" @click="providerVisible = false">取消</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>

  <t-dialog v-model:visible="modelVisible" header="新建自定义模型" :footer="false" width="520px">
    <t-form :data="modelForm" :rules="modelRules" label-align="top" @submit="submitModel">
      <t-form-item label="所属服务商" name="providerId">
        <t-select v-model="modelForm.providerId" :options="providerOptions" placeholder="请选择服务商" />
      </t-form-item>
      <t-form-item label="模型编码" name="modelCode">
        <t-input v-model="modelForm.modelCode" placeholder="如 deepseek-chat" />
      </t-form-item>
      <t-form-item label="显示名称" name="modelName">
        <t-input v-model="modelForm.modelName" placeholder="如 DeepSeek 对话模型" />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit" :loading="saving">保存</t-button>
          <t-button variant="outline" @click="modelVisible = false">取消</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>

  <t-dialog v-model:visible="keyVisible" header="新增 API 密钥" :footer="false" width="520px">
    <t-form :data="keyForm" :rules="keyRules" label-align="top" @submit="submitKey">
      <t-form-item label="所属服务商" name="providerId">
        <t-select v-model="keyForm.providerId" :options="providerOptions" placeholder="请选择服务商" />
      </t-form-item>
      <t-form-item label="密钥名称" name="credentialName">
        <t-input v-model="keyForm.credentialName" placeholder="如 生产环境密钥" />
      </t-form-item>
      <t-form-item label="API 密钥" name="apiKey">
        <t-input v-model="keyForm.apiKey" type="password" placeholder="密钥将加密存储，不会明文显示" />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit" :loading="saving">保存</t-button>
          <t-button variant="outline" @click="keyVisible = false">取消</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>

  <t-dialog v-model:visible="testVisible" header="模型测试" :footer="false" width="640px">
    <t-space direction="vertical" style="width: 100%" :size="16">
      <t-textarea
        v-model="testMessage"
        placeholder="输入测试提示词"
        :autosize="{ minRows: 4, maxRows: 8 }"
      />
      <t-button theme="primary" :loading="testing" @click="runTest">发送测试</t-button>
      <t-card v-if="testResult" title="模型回复" size="small" :bordered="true">
        <div class="result">{{ testResult }}</div>
      </t-card>
      <t-empty v-else description="发送后将在此显示模型回复" />
    </t-space>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import type { FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import {
  createCredential,
  createModel,
  createProvider,
  deleteCredential,
  deleteModel,
  deleteProvider,
  listCredentials,
  listModels,
  listProviders,
  testChat,
  type CredentialVO,
  type ModelVO,
  type ProviderVO,
} from '@/api/model'
import { fetchPlatformCapabilities } from '@/api/platform'

const tab = ref('models')
const byokEnabled = ref(false)
const saving = ref(false)
const testing = ref(false)
const providers = ref<ProviderVO[]>([])
const models = ref<ModelVO[]>([])
const credentials = ref<CredentialVO[]>([])

const providerVisible = ref(false)
const modelVisible = ref(false)
const keyVisible = ref(false)
const testVisible = ref(false)
const testMessage = ref('用一句话介绍 Box 平台')
const testResult = ref('')
const testingModelId = ref<number | null>(null)

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
  modelType: 'CHAT',
  supportStreaming: true,
})
const keyForm = reactive({
  providerId: undefined as number | undefined,
  credentialName: '',
  apiKey: '',
})

const providerRules: FormProps['rules'] = {
  providerCode: [{ required: true, message: '请输入服务商编码' }],
  providerName: [{ required: true, message: '请输入服务商名称' }],
}
const modelRules: FormProps['rules'] = {
  providerId: [{ required: true, message: '请选择服务商' }],
  modelCode: [{ required: true, message: '请输入模型编码' }],
  modelName: [{ required: true, message: '请输入显示名称' }],
}
const keyRules: FormProps['rules'] = {
  providerId: [{ required: true, message: '请选择服务商' }],
  credentialName: [{ required: true, message: '请输入密钥名称' }],
  apiKey: [{ required: true, message: '请输入 API 密钥' }],
}

const providerOptions = computed(() =>
  providers.value.map((item) => ({ label: item.providerName, value: item.id })),
)

const providerColumns: PrimaryTableCol[] = [
  { colKey: 'providerName', title: '名称', width: 160 },
  { colKey: 'providerCode', title: '编码', width: 140 },
  { colKey: 'providerType', title: '类型', width: 160 },
  { colKey: 'baseUrl', title: '接口地址', ellipsis: true },
  { colKey: 'op', title: '操作', width: 100, fixed: 'right' },
]
const modelColumns: PrimaryTableCol[] = [
  { colKey: 'modelName', title: '名称', width: 180 },
  { colKey: 'modelCode', title: '编码', width: 180 },
  { colKey: 'providerName', title: '服务商', width: 140 },
  { colKey: 'modelType', title: '类型', width: 100 },
  { colKey: 'op', title: '操作', width: 160, fixed: 'right' },
]
const keyColumns: PrimaryTableCol[] = [
  { colKey: 'credentialName', title: '名称', width: 160 },
  { colKey: 'providerName', title: '服务商', width: 140 },
  { colKey: 'maskedApiKey', title: '密钥', ellipsis: true },
  { colKey: 'op', title: '操作', width: 100, fixed: 'right' },
]

async function loadCapabilities() {
  const { data } = await fetchPlatformCapabilities()
  byokEnabled.value = data.data?.byokEnabled === true
}

async function loadByok() {
  const [p, m, k] = await Promise.all([listProviders(), listModels(), listCredentials()])
  providers.value = p.data.data || []
  models.value = m.data.data || []
  credentials.value = k.data.data || []
}

async function loadAll() {
  await loadCapabilities()
  if (byokEnabled.value) {
    await loadByok()
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
  modelVisible.value = true
}

function openKey() {
  keyForm.providerId = providers.value[0]?.id
  keyForm.credentialName = ''
  keyForm.apiKey = ''
  keyVisible.value = true
}

function openTest(row: ModelVO) {
  testingModelId.value = row.id
  testResult.value = ''
  testVisible.value = true
}

const submitProvider: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  saving.value = true
  try {
    await createProvider(providerForm)
    MessagePlugin.success('服务商创建成功')
    providerVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

const submitModel: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true || !modelForm.providerId) return
  saving.value = true
  try {
    await createModel({
      providerId: modelForm.providerId,
      modelCode: modelForm.modelCode,
      modelName: modelForm.modelName,
      modelType: modelForm.modelType,
      supportStreaming: modelForm.supportStreaming,
    })
    MessagePlugin.success('模型创建成功')
    modelVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

const submitKey: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true || !keyForm.providerId) return
  saving.value = true
  try {
    await createCredential({
      providerId: keyForm.providerId,
      credentialName: keyForm.credentialName,
      apiKey: keyForm.apiKey,
    })
    MessagePlugin.success('密钥已加密保存')
    keyVisible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

function confirmDelete(body: string, action: () => Promise<unknown>) {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body,
    confirmBtn: '删除',
    cancelBtn: '取消',
    theme: 'warning',
    onConfirm: async () => {
      await action()
      await loadAll()
      MessagePlugin.success('删除成功')
      dialog.hide()
    },
  })
}

function removeProvider(id: number) {
  confirmDelete('删除服务商后，关联的模型与密钥需手动清理。', () => deleteProvider(id))
}

function removeModel(id: number) {
  confirmDelete('确认删除该模型？', () => deleteModel(id))
}

function removeKey(id: number) {
  confirmDelete('确认删除该密钥？', () => deleteCredential(id))
}

async function runTest() {
  if (!testingModelId.value) return
  testing.value = true
  try {
    const { data } = await testChat(testingModelId.value, testMessage.value)
    testResult.value = data.data.content
  } finally {
    testing.value = false
  }
}

onMounted(loadAll)
useReloadOnWorkspaceChange(loadAll)
</script>

<style scoped>
.models-card {
  overflow: hidden;
}

.tab-body {
  margin-top: 16px;
}

.byok-hint {
  margin-bottom: 0;
}

.toolbar {
  margin-bottom: 16px;
}

.result {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  color: var(--box-ink);
}
</style>
