<template>
  <div class="plugin-catalog-page admin-page">
    <page-header title="插件市场" desc="管理插件分类与上架插件，安装时按配置在工作空间创建对应资源。" />

    <t-card :bordered="false" class="admin-tab-card">
      <t-tabs v-model="tab">
        <t-tab-panel value="plugins" label="插件" />
        <t-tab-panel value="categories" label="分类" />
      </t-tabs>

      <div class="tab-body">
        <div v-if="tab === 'plugins'" class="toolbar">
          <t-space>
            <t-button theme="primary" @click="openPluginCreate">新建插件</t-button>
            <t-select
              v-model="pluginCategoryFilter"
              :options="categoryOptions"
              placeholder="按分类筛选"
              clearable
              style="width: 180px"
            />
          </t-space>
        </div>
        <t-table
          v-if="tab === 'plugins'"
          row-key="id"
          :data="filteredPlugins"
          :columns="pluginColumns"
          :loading="loading"
          hover
        >
          <template #empty>
            <t-empty :description="plugins.length ? '没有匹配的插件' : '暂无插件'" />
          </template>
        </t-table>

        <div v-if="tab === 'categories'" class="toolbar">
          <t-button theme="primary" @click="openCategoryCreate">新建分类</t-button>
        </div>
        <t-table
          v-if="tab === 'categories'"
          row-key="value"
          :data="categories"
          :columns="categoryColumns"
          :loading="categoryLoading"
          hover
        >
          <template #empty>
            <t-empty description="暂无分类" />
          </template>
        </t-table>
      </div>
    </t-card>

    <t-dialog
      v-model:visible="pluginDialogVisible"
      :header="pluginEditing ? '编辑插件' : '新建插件'"
      width="720px"
      :confirm-btn="{ content: pluginEditing ? '保存' : '创建', loading: pluginSaving }"
      @confirm="onSavePlugin"
    >
      <t-form ref="pluginFormRef" :data="pluginForm" :rules="pluginRules" label-width="108px">
        <t-form-item v-if="!pluginEditing" label="编码" name="pluginCode">
          <t-input v-model="pluginForm.pluginCode" placeholder="如 wf-custom-1" />
        </t-form-item>
        <t-form-item label="分类" name="category">
          <t-select v-model="pluginForm.category" :options="categoryOptions" placeholder="选择分类" />
        </t-form-item>
        <t-form-item label="标题" name="title">
          <t-input v-model="pluginForm.title" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="pluginForm.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="排序" name="sortOrder">
          <t-input-number v-model="pluginForm.sortOrder" :min="0" theme="column" />
        </t-form-item>
        <t-form-item v-if="pluginEditing" label="状态" name="status">
          <t-radio-group v-model="pluginForm.status">
            <t-radio value="LISTED">上架</t-radio>
            <t-radio value="UNLISTED">下架</t-radio>
          </t-radio-group>
        </t-form-item>

        <template v-if="pluginForm.category === 'tools'">
          <t-form-item label="请求方法" name="method">
            <t-select v-model="pluginForm.method" :options="methodOptions" />
          </t-form-item>
          <t-form-item label="请求地址" name="url">
            <t-input v-model="pluginForm.url" placeholder="https://api.example.com/..." />
          </t-form-item>
          <t-form-item label="超时(ms)" name="timeoutMs">
            <t-input-number v-model="pluginForm.timeoutMs" :min="1000" :step="1000" theme="column" />
          </t-form-item>
        </template>
        <template v-else-if="pluginForm.category === 'mcp'">
          <t-form-item label="传输方式" name="transportType">
            <t-select v-model="pluginForm.transportType" :options="transportOptions" />
          </t-form-item>
          <t-form-item label="服务地址" name="endpointUrl">
            <t-input v-model="pluginForm.endpointUrl" placeholder="https://mcp.example.com" />
          </t-form-item>
          <t-form-item label="鉴权" name="authType">
            <t-select v-model="pluginForm.authType" :options="authOptions" />
          </t-form-item>
        </template>
        <t-form-item v-else-if="pluginForm.category === 'workflows'" label="工作流定义" name="definitionJson">
          <t-textarea
            v-model="pluginForm.definitionJson"
            :autosize="{ minRows: 6, maxRows: 14 }"
            placeholder="工作流 JSON，留空则使用默认开始/输出节点"
          />
        </t-form-item>
        <t-form-item v-else-if="pluginForm.category === 'skills'" label="技能说明" name="instructions">
          <t-textarea
            v-model="pluginForm.instructions"
            :autosize="{ minRows: 4, maxRows: 10 }"
            placeholder="安装后写入知识库描述，供智能体按约定执行"
          />
        </t-form-item>
        <p v-else-if="pluginForm.category === 'knowledge'" class="form-hint">
          安装后会在工作空间创建一个同名知识库，可再上传文档。
        </p>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="categoryDialogVisible"
      :header="categoryEditing ? '编辑分类' : '新建分类'"
      width="560px"
      :confirm-btn="{ content: categoryEditing ? '保存' : '创建', loading: categorySaving }"
      @confirm="onSaveCategory"
    >
      <t-form ref="categoryFormRef" :data="categoryForm" :rules="categoryRules" label-width="96px">
        <t-form-item v-if="!categoryEditing" label="编码" name="categoryCode">
          <t-input v-model="categoryForm.categoryCode" placeholder="如 workflows" />
        </t-form-item>
        <t-form-item label="名称" name="label">
          <t-input v-model="categoryForm.label" />
        </t-form-item>
        <t-form-item label="描述" name="description">
          <t-textarea v-model="categoryForm.description" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="排序" name="sortOrder">
          <t-input-number v-model="categoryForm.sortOrder" :min="0" theme="column" />
        </t-form-item>
        <t-form-item v-if="categoryEditing" label="状态" name="status">
          <t-radio-group v-model="categoryForm.status">
            <t-radio value="ACTIVE">启用</t-radio>
            <t-radio value="INACTIVE">停用</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createPlugin,
  createPluginCategory,
  deletePlugin,
  deletePluginCategory,
  fetchPluginCategories,
  fetchPlugins,
  updatePlugin,
  updatePluginCategory,
  type AdminPluginCatalogVO,
  type PluginCategoryVO,
} from '@/api/plugin'

const tab = ref('plugins')
const loading = ref(false)
const categoryLoading = ref(false)
const plugins = ref<AdminPluginCatalogVO[]>([])
const pluginCategoryFilter = ref<string>('')
const categories = ref<PluginCategoryVO[]>([])

const filteredPlugins = computed(() => {
  if (!pluginCategoryFilter.value) return plugins.value
  return plugins.value.filter((item) => item.category === pluginCategoryFilter.value)
})

const pluginDialogVisible = ref(false)
const pluginSaving = ref(false)
const pluginEditing = ref(false)
const pluginEditingId = ref<number | null>(null)
const pluginFormRef = ref<FormInstanceFunctions>()

const categoryDialogVisible = ref(false)
const categorySaving = ref(false)
const categoryEditing = ref(false)
const categoryEditingCode = ref('')
const categoryFormRef = ref<FormInstanceFunctions>()

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

const emptyManifest = {
  method: 'GET',
  url: '',
  timeoutMs: 10000,
  transportType: 'HTTP',
  endpointUrl: '',
  authType: 'NONE',
  definitionJson: '',
  instructions: '',
}

const pluginForm = reactive({
  pluginCode: '',
  category: '',
  title: '',
  description: '',
  sortOrder: 0,
  status: 'LISTED',
  ...emptyManifest,
})

const categoryForm = reactive({
  categoryCode: '',
  label: '',
  description: '',
  sortOrder: 0,
  status: 'ACTIVE',
})

const pluginRules: FormProps['rules'] = {
  pluginCode: [{ required: true, message: '请输入编码' }],
  category: [{ required: true, message: '请选择分类' }],
  title: [{ required: true, message: '请输入标题' }],
}

const categoryRules: FormProps['rules'] = {
  categoryCode: [{ required: true, message: '请输入编码' }],
  label: [{ required: true, message: '请输入名称' }],
}

const categoryOptions = computed(() =>
  categories.value.map((item) => ({ label: item.label, value: item.value })),
)

function categoryLabel(code?: string) {
  return categories.value.find((item) => item.value === code)?.label || code || '-'
}

const pluginColumns: PrimaryTableCol<AdminPluginCatalogVO>[] = [
  { colKey: 'title', title: '标题', minWidth: 160 },
  { colKey: 'pluginCode', title: '编码', width: 140 },
  { colKey: 'category', title: '分类', width: 100, cell: (_, { row }) => categoryLabel(row.category) },
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
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openPluginEdit(row) }, () => '编辑'),
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDeletePlugin(row) }, () => '删除'),
      ]),
  },
]

const categoryColumns: PrimaryTableCol<PluginCategoryVO>[] = [
  { colKey: 'label', title: '名称', minWidth: 120 },
  { colKey: 'value', title: '编码', width: 140 },
  { colKey: 'desc', title: '描述', ellipsis: true },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openCategoryEdit(row) }, () => '编辑'),
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDeleteCategory(row) }, () => '删除'),
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

function applyManifest(raw?: string) {
  const data = parseManifest(raw)
  pluginForm.method = String(data.method || 'GET')
  pluginForm.url = String(data.url || '')
  pluginForm.timeoutMs = Number(data.timeoutMs || 10000)
  pluginForm.transportType = String(data.transportType || 'HTTP')
  pluginForm.endpointUrl = String(data.endpointUrl || '')
  pluginForm.authType = String(data.authType || 'NONE')
  pluginForm.definitionJson = String(data.definitionJson || '')
  pluginForm.instructions = String(data.instructions || '')
}

function buildManifestJson() {
  const category = pluginForm.category
  if (category === 'tools') {
    return JSON.stringify({
      type: 'HTTP',
      method: pluginForm.method,
      url: pluginForm.url.trim(),
      timeoutMs: pluginForm.timeoutMs,
    })
  }
  if (category === 'mcp') {
    return JSON.stringify({
      transportType: pluginForm.transportType,
      endpointUrl: pluginForm.endpointUrl.trim(),
      authType: pluginForm.authType,
    })
  }
  if (category === 'workflows') {
    return JSON.stringify({
      definitionJson: pluginForm.definitionJson.trim(),
    })
  }
  if (category === 'skills') {
    return JSON.stringify({
      instructions: pluginForm.instructions.trim(),
    })
  }
  return JSON.stringify({})
}

async function loadPlugins() {
  loading.value = true
  try {
    const { data } = await fetchPlugins()
    plugins.value = data.data
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  categoryLoading.value = true
  try {
    const { data } = await fetchPluginCategories()
    categories.value = data.data
  } finally {
    categoryLoading.value = false
  }
}

function openPluginCreate() {
  pluginEditing.value = false
  pluginEditingId.value = null
  Object.assign(pluginForm, {
    pluginCode: '',
    category: categories.value[0]?.value || '',
    title: '',
    description: '',
    sortOrder: 0,
    status: 'LISTED',
    ...emptyManifest,
  })
  pluginDialogVisible.value = true
}

function openPluginEdit(row: AdminPluginCatalogVO) {
  pluginEditing.value = true
  pluginEditingId.value = row.id
  Object.assign(pluginForm, {
    pluginCode: row.pluginCode,
    category: row.category,
    title: row.title,
    description: row.description || '',
    sortOrder: row.sortOrder,
    status: row.status,
    ...emptyManifest,
  })
  applyManifest(row.manifestJson)
  pluginDialogVisible.value = true
}

async function onSavePlugin() {
  const valid = await pluginFormRef.value?.validate()
  if (valid !== true) return
  if (pluginForm.category === 'tools' && !pluginForm.url.trim()) {
    MessagePlugin.warning('工具插件需填写请求地址')
    return
  }
  if (pluginForm.category === 'mcp' && !pluginForm.endpointUrl.trim()) {
    MessagePlugin.warning('MCP 插件需填写服务地址')
    return
  }
  if (pluginForm.category === 'skills' && !pluginForm.instructions.trim()) {
    MessagePlugin.warning('Skill 插件需填写技能说明')
    return
  }
  pluginSaving.value = true
  try {
    const manifestJson = buildManifestJson()
    if (pluginEditing.value && pluginEditingId.value) {
      await updatePlugin(pluginEditingId.value, {
        category: pluginForm.category,
        title: pluginForm.title,
        description: pluginForm.description,
        manifestJson,
        sortOrder: pluginForm.sortOrder,
        status: pluginForm.status,
      })
      MessagePlugin.success('已保存')
    } else {
      await createPlugin({
        pluginCode: pluginForm.pluginCode,
        category: pluginForm.category,
        title: pluginForm.title,
        description: pluginForm.description,
        manifestJson,
        sortOrder: pluginForm.sortOrder,
      })
      MessagePlugin.success('已创建')
    }
    pluginDialogVisible.value = false
    await loadPlugins()
  } finally {
    pluginSaving.value = false
  }
}

function confirmDeletePlugin(row: AdminPluginCatalogVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除插件',
    body: `确定删除「${row.title}」？仍有工作空间安装时将无法删除。`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePlugin(row.id)
      MessagePlugin.success('插件已删除')
      dialog.destroy()
      await loadPlugins()
    },
  })
}

function openCategoryCreate() {
  categoryEditing.value = false
  categoryEditingCode.value = ''
  Object.assign(categoryForm, {
    categoryCode: '',
    label: '',
    description: '',
    sortOrder: 0,
    status: 'ACTIVE',
  })
  categoryDialogVisible.value = true
}

function openCategoryEdit(row: PluginCategoryVO) {
  categoryEditing.value = true
  categoryEditingCode.value = row.value
  Object.assign(categoryForm, {
    categoryCode: row.value,
    label: row.label,
    description: row.desc,
    sortOrder: 0,
    status: 'ACTIVE',
  })
  categoryDialogVisible.value = true
}

async function onSaveCategory() {
  const valid = await categoryFormRef.value?.validate()
  if (valid !== true) return
  categorySaving.value = true
  try {
    if (categoryEditing.value) {
      await updatePluginCategory(categoryEditingCode.value, {
        label: categoryForm.label,
        description: categoryForm.description,
        sortOrder: categoryForm.sortOrder,
        status: categoryForm.status,
      })
      MessagePlugin.success('已保存')
    } else {
      await createPluginCategory({
        categoryCode: categoryForm.categoryCode,
        label: categoryForm.label,
        description: categoryForm.description,
        sortOrder: categoryForm.sortOrder,
      })
      MessagePlugin.success('已创建')
    }
    categoryDialogVisible.value = false
    await loadCategories()
  } finally {
    categorySaving.value = false
  }
}

function confirmDeleteCategory(row: PluginCategoryVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除分类',
    body: `确定删除分类「${row.label}」？分类下仍有插件时将无法删除。`,
    theme: 'warning',
    onConfirm: async () => {
      await deletePluginCategory(row.value)
      MessagePlugin.success('分类已删除')
      dialog.destroy()
      await loadCategories()
    },
  })
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadPlugins()])
})
</script>

<style scoped>
.tab-body {
  margin-top: 16px;
}

.toolbar {
  margin-bottom: 12px;
}

.form-hint {
  margin: 0 0 8px 108px;
  font-size: 12px;
  color: var(--box-muted);
}
</style>
