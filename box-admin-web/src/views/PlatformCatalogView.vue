<template>
  <div class="platform-catalog-page admin-page">
    <page-header :title="title" :desc="desc">
      <template #actions>
        <t-button theme="primary" @click="openCreate">{{ createLabel }}</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <t-table row-key="id" :data="items" :columns="columns" :loading="loading" hover>
        <template #empty>
          <t-empty :description="emptyText" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editing ? `编辑${noun}` : `新建${noun}`"
      width="640px"
      :confirm-btn="{ content: editing ? '保存' : '创建', loading: saving }"
      @confirm="onSave"
    >
      <t-form ref="formRef" :data="form" :rules="rules" label-width="108px">
        <t-form-item v-if="!editing" label="编码" name="pluginCode">
          <t-input v-model="form.pluginCode" :placeholder="codePlaceholder" />
        </t-form-item>
        <t-form-item label="标题" name="title">
          <t-input v-model="form.title" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="排序" name="sortOrder">
          <t-input-number v-model="form.sortOrder" :min="0" theme="column" />
        </t-form-item>
        <t-form-item v-if="editing" label="状态" name="status">
          <t-radio-group v-model="form.status">
            <t-radio value="LISTED">上架</t-radio>
            <t-radio value="UNLISTED">下架</t-radio>
          </t-radio-group>
        </t-form-item>

        <template v-if="category === 'tools'">
          <t-form-item label="请求方法" name="method">
            <t-select v-model="form.method" :options="methodOptions" />
          </t-form-item>
          <t-form-item label="请求地址" name="url">
            <t-input v-model="form.url" placeholder="https://api.example.com/..." />
          </t-form-item>
          <t-form-item label="超时(ms)" name="timeoutMs">
            <t-input-number v-model="form.timeoutMs" :min="1000" :step="1000" theme="column" />
          </t-form-item>
        </template>
        <template v-else>
          <t-form-item label="传输方式" name="transportType">
            <t-select v-model="form.transportType" :options="transportOptions" />
          </t-form-item>
          <t-form-item label="服务地址" name="endpointUrl">
            <t-input v-model="form.endpointUrl" placeholder="https://mcp.example.com" />
          </t-form-item>
          <t-form-item label="鉴权" name="authType">
            <t-select v-model="form.authType" :options="authOptions" />
          </t-form-item>
        </template>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { createPlugin, deletePlugin, fetchPlugins, updatePlugin, type AdminPluginCatalogVO } from '@/api/plugin'

const route = useRoute()
const category = computed(() => (route.meta.catalogCategory === 'mcp' ? 'mcp' : 'tools'))
const title = computed(() => (category.value === 'mcp' ? '官方 MCP' : '官方工具'))
const desc = computed(() =>
  category.value === 'mcp'
    ? '上架平台 MCP 服务，租户可在插件市场安装到工作空间。'
    : '上架平台 HTTP 工具，租户可在插件市场安装到工作空间。',
)
const noun = computed(() => (category.value === 'mcp' ? 'MCP' : '工具'))
const createLabel = computed(() => `新建${noun.value}`)
const emptyText = computed(() => (category.value === 'mcp' ? '暂无官方 MCP' : '暂无官方工具'))
const codePlaceholder = computed(() => (category.value === 'mcp' ? '如 mcp-search' : '如 tool-http-weather'))

const loading = ref(false)
const items = ref<AdminPluginCatalogVO[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()

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

const form = reactive({
  pluginCode: '',
  title: '',
  description: '',
  sortOrder: 0,
  status: 'LISTED',
  method: 'GET',
  url: '',
  timeoutMs: 10000,
  transportType: 'HTTP',
  endpointUrl: '',
  authType: 'NONE',
})

const rules: FormProps['rules'] = {
  pluginCode: [{ required: true, message: '请输入编码' }],
  title: [{ required: true, message: '请输入标题' }],
}

const columns: PrimaryTableCol<AdminPluginCatalogVO>[] = [
  { colKey: 'title', title: '标题', minWidth: 160 },
  { colKey: 'pluginCode', title: '编码', width: 160 },
  { colKey: 'installCount', title: '安装数', width: 90 },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 'LISTED' ? 'success' : 'default', variant: 'light' }, () =>
        row.status === 'LISTED' ? '上架' : '下架',
      ),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openEdit(row) }, () => '编辑'),
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDelete(row) }, () => '删除'),
      ]),
  },
]

function parseManifest(raw?: string) {
  if (!raw) return {}
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return {}
  }
}

function resetForm() {
  Object.assign(form, {
    pluginCode: '',
    title: '',
    description: '',
    sortOrder: 0,
    status: 'LISTED',
    method: 'GET',
    url: '',
    timeoutMs: 10000,
    transportType: 'HTTP',
    endpointUrl: '',
    authType: 'NONE',
  })
}

function applyManifest(raw?: string) {
  const data = parseManifest(raw)
  form.method = String(data.method || 'GET')
  form.url = String(data.url || '')
  form.timeoutMs = Number(data.timeoutMs || 10000)
  form.transportType = String(data.transportType || 'HTTP')
  form.endpointUrl = String(data.endpointUrl || '')
  form.authType = String(data.authType || 'NONE')
}

function buildManifestJson() {
  if (category.value === 'tools') {
    return JSON.stringify({
      type: 'HTTP',
      method: form.method,
      url: form.url.trim(),
      timeoutMs: form.timeoutMs,
    })
  }
  return JSON.stringify({
    transportType: form.transportType,
    endpointUrl: form.endpointUrl.trim(),
    authType: form.authType,
  })
}

async function loadItems() {
  loading.value = true
  try {
    const { data } = await fetchPlugins(category.value)
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminPluginCatalogVO) {
  editing.value = true
  editingId.value = row.id
  form.pluginCode = row.pluginCode
  form.title = row.title
  form.description = row.description || ''
  form.sortOrder = row.sortOrder
  form.status = row.status
  applyManifest(row.manifestJson)
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate()
  if (valid !== true) return false
  if (category.value === 'tools' && !form.url.trim()) {
    MessagePlugin.warning('请填写请求地址')
    return false
  }
  if (category.value === 'mcp' && !form.endpointUrl.trim()) {
    MessagePlugin.warning('请填写服务地址')
    return false
  }
  saving.value = true
  try {
    const manifestJson = buildManifestJson()
    if (editing.value && editingId.value) {
      await updatePlugin(editingId.value, {
        category: category.value,
        title: form.title,
        description: form.description,
        manifestJson,
        sortOrder: form.sortOrder,
        status: form.status,
      })
      MessagePlugin.success('已保存')
    } else {
      await createPlugin({
        pluginCode: form.pluginCode,
        category: category.value,
        title: form.title,
        description: form.description,
        manifestJson,
        sortOrder: form.sortOrder,
      })
      MessagePlugin.success('已创建')
    }
    dialogVisible.value = false
    await loadItems()
  } finally {
    saving.value = false
  }
  return true
}

function confirmDelete(row: AdminPluginCatalogVO) {
  const dialog = DialogPlugin.confirm({
    header: `删除${noun.value}`,
    body: `确定删除「${row.title}」？仍有工作空间安装时将无法删除。`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePlugin(row.id)
      MessagePlugin.success('已删除')
      dialog.destroy()
      await loadItems()
    },
  })
}

watch(category, () => {
  void loadItems()
})

onMounted(loadItems)
</script>
