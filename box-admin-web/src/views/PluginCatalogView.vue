<template>
  <div class="plugin-catalog-page admin-page">
    <page-header title="插件市场" desc="官方插件由平台管理员上架，全平台可用；工作空间插件由成员自行上传，仅本空间共享。" />

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
      width="800px"
      :confirm-btn="{ content: pluginEditing ? '保存' : '创建', loading: pluginSaving }"
      @confirm="onSavePlugin"
    >
      <t-form ref="pluginFormRef" :data="pluginForm" :rules="pluginRules" label-width="108px">
        <plugin-resource-create-fields
          v-model:category="pluginForm.category"
          v-model:title="pluginForm.title"
          v-model:description="pluginForm.description"
          v-model:manifest="manifestDraft"
          v-model:plugin-code="pluginForm.pluginCode"
          v-model:sort-order="pluginForm.sortOrder"
          :show-plugin-code="!pluginEditing"
          show-sort-order
          :category-options="categoryOptions"
          :upload-asset="uploadCatalogAsset"
          :workflow-editor-host="boxAdminWorkflowEditorHost"
          :workflow-platform-models="workflowPlatformModels"
        />
        <t-form-item v-if="pluginEditing" label="状态" name="status">
          <t-radio-group v-model="pluginForm.status" :disabled="pluginForm.reviewStatus !== 'APPROVED'">
            <t-radio value="LISTED">上架</t-radio>
            <t-radio value="UNLISTED">下架</t-radio>
          </t-radio-group>
        </t-form-item>
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

    <t-dialog
      v-model:visible="rolloutVisible"
      header="灰度放量"
      width="520px"
      :confirm-btn="{ content: '保存', loading: rolloutSaving }"
      @confirm="saveRollout"
    >
      <t-form label-width="110px">
        <t-form-item label="可见范围">
          <t-radio-group v-model="rolloutForm.visibility">
            <t-radio value="GLOBAL">全平台</t-radio>
            <t-radio value="TENANT">指定租户</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item v-if="rolloutForm.visibility === 'TENANT'" label="租户 ID">
          <t-input v-model="rolloutForm.tenantIds" placeholder="多个 ID 用逗号分隔，如 1,2,3" />
          <p class="rollout-hint">仅列出的租户可在 C 端插件市场看到该插件。</p>
        </t-form-item>
        <t-form-item v-else label="灰度百分比">
          <t-input-number v-model="rolloutForm.rolloutPercent" :min="0" :max="100" theme="column" />
          <p class="rollout-hint">全平台模式下按租户 ID 与插件 ID 稳定分桶，0 表示全隐藏。</p>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import PluginResourceCreateFields from '@box/ui/components/PluginResourceCreateFields.vue'
import {
  applyManifestJsonToDraft,
  buildManifestJsonFromDraft,
  emptyManifestDraft,
  validateManifestDraft,
} from '@box/ui/plugin/pluginCatalogManifest'
import type { WorkflowEditorPlatformModel } from '@box/ui/workflow/workflowEditorHost'
import { fetchPlatformModels } from '@/api/platform'
import { boxAdminWorkflowEditorHost } from '@/utils/boxAdminWorkflowEditorHost'
import {
  createPlugin,
  uploadPluginCatalogAsset,
  createPluginCategory,
  deletePlugin,
  deletePluginCategory,
  fetchPluginCategories,
  fetchPlugins,
  updatePlugin,
  updatePluginCategory,
  updatePluginReview,
  updatePluginRollout,
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
const rolloutVisible = ref(false)
const rolloutSaving = ref(false)
const rolloutTargetId = ref<number | null>(null)
const rolloutForm = reactive({
  visibility: 'GLOBAL',
  tenantIds: '',
  rolloutPercent: 100,
})

const manifestDraft = ref(emptyManifestDraft())
const workflowPlatformModels = ref<WorkflowEditorPlatformModel[]>([])

async function ensureWorkflowEditorContext() {
  if (workflowPlatformModels.value.length) return
  try {
    const { data } = await fetchPlatformModels()
    workflowPlatformModels.value = (data.data || []).filter((item) => item.status === 1)
  } catch {
    workflowPlatformModels.value = []
  }
}

watch(pluginDialogVisible, (open) => {
  if (open) void ensureWorkflowEditorContext()
})

const pluginForm = reactive({
  pluginCode: '',
  category: '',
  title: '',
  description: '',
  sortOrder: 0,
  status: 'LISTED',
  reviewStatus: 'PENDING_REVIEW',
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

async function uploadCatalogAsset(file: File) {
  const { data } = await uploadPluginCatalogAsset(file)
  return data.data
}

function categoryLabel(code?: string) {
  return categories.value.find((item) => item.value === code)?.label || code || '-'
}

const pluginColumns: PrimaryTableCol<AdminPluginCatalogVO>[] = [
  { colKey: 'title', title: '标题', minWidth: 160 },
  { colKey: 'pluginCode', title: '编码', width: 140 },
  { colKey: 'category', title: '分类', width: 100, cell: (_, { row }) => categoryLabel(row.category) },
  {
    colKey: 'sourceType',
    title: '来源',
    width: 100,
    cell: (_, { row }) => (row.sourceType === 'USER' ? '工作空间' : '官方'),
  },
  { colKey: 'installCount', title: '安装数', width: 90 },
  {
    colKey: 'visibility',
    title: '可见范围',
    width: 100,
    cell: (_, { row }) => (row.visibility === 'TENANT' ? '指定租户' : '全平台'),
  },
  {
    colKey: 'rolloutPercent',
    title: '灰度%',
    width: 80,
    cell: (_, { row }) => (row.visibility === 'TENANT' ? '—' : `${row.rolloutPercent ?? 100}%`),
  },
  {
    colKey: 'status',
    title: '上架',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 'LISTED' ? 'success' : 'default', variant: 'light' }, () =>
        row.status === 'LISTED' ? '上架' : '下架',
      ),
  },
  {
    colKey: 'reviewStatus',
    title: '审核',
    width: 100,
    cell: (_, { row }) => {
      const status = row.reviewStatus || 'PENDING_REVIEW'
      const theme = status === 'APPROVED' ? 'success' : status === 'REJECTED' ? 'danger' : 'warning'
      const label = status === 'APPROVED' ? '已通过' : status === 'REJECTED' ? '已拒绝' : '待审核'
      return h(Tag, { theme, variant: 'light' }, () => label)
    },
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 320,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openPluginEdit(row) }, () => '编辑'),
        row.sourceType !== 'USER' && row.reviewStatus !== 'APPROVED'
          ? h(Link, { theme: 'success', hover: 'color', onClick: () => reviewPlugin(row, 'APPROVED') }, () => '通过')
          : null,
        row.sourceType !== 'USER' && row.reviewStatus !== 'REJECTED'
          ? h(Link, { theme: 'danger', hover: 'color', onClick: () => reviewPlugin(row, 'REJECTED') }, () => '拒绝')
          : null,
        row.sourceType !== 'USER'
          ? h(Link, { theme: 'primary', hover: 'color', onClick: () => openRollout(row) }, () => '灰度')
          : null,
        row.reviewStatus === 'APPROVED' && row.status !== 'LISTED'
          ? h(Link, { theme: 'primary', hover: 'color', onClick: () => listPlugin(row) }, () => '上架')
          : null,
        row.reviewStatus === 'APPROVED' && row.status === 'LISTED'
          ? h(Link, { theme: 'warning', hover: 'color', onClick: () => unlistPlugin(row) }, () => '下架')
          : null,
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
    reviewStatus: 'PENDING_REVIEW',
  })
  manifestDraft.value = emptyManifestDraft()
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
    reviewStatus: row.reviewStatus || 'PENDING_REVIEW',
  })
  manifestDraft.value = applyManifestJsonToDraft(row.manifestJson)
  pluginDialogVisible.value = true
}

async function onSavePlugin() {
  const valid = await pluginFormRef.value?.validate()
  if (valid !== true) return
  const manifestError = validateManifestDraft(pluginForm.category, manifestDraft.value)
  if (manifestError) {
    MessagePlugin.warning(manifestError)
    return
  }
  let manifestJson: string
  try {
    manifestJson = buildManifestJsonFromDraft(pluginForm.category, manifestDraft.value)
  } catch (e) {
    MessagePlugin.warning(e instanceof Error ? e.message : '资源配置无效')
    return
  }
  pluginSaving.value = true
  try {
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
      MessagePlugin.success('已创建，通过审核后才会出现在 C 端市场')
    }
    pluginDialogVisible.value = false
    await loadPlugins()
  } finally {
    pluginSaving.value = false
  }
}

async function reviewPlugin(row: AdminPluginCatalogVO, reviewStatus: 'APPROVED' | 'REJECTED') {
  await updatePluginReview(row.id, reviewStatus)
  MessagePlugin.success(reviewStatus === 'APPROVED' ? '已通过审核' : '已拒绝')
  await loadPlugins()
}

function openRollout(row: AdminPluginCatalogVO) {
  rolloutTargetId.value = row.id
  rolloutForm.visibility = row.visibility || 'GLOBAL'
  rolloutForm.tenantIds = (row.tenantIdsJson || '').replace(/[\[\]\s]/g, '')
  rolloutForm.rolloutPercent = row.rolloutPercent ?? 100
  rolloutVisible.value = true
}

async function saveRollout() {
  if (!rolloutTargetId.value) return false
  if (rolloutForm.visibility === 'TENANT' && !rolloutForm.tenantIds.trim()) {
    MessagePlugin.warning('请填写至少一个租户 ID')
    return false
  }
  rolloutSaving.value = true
  try {
    await updatePluginRollout(rolloutTargetId.value, {
      visibility: rolloutForm.visibility,
      tenantIds: rolloutForm.visibility === 'TENANT' ? rolloutForm.tenantIds : undefined,
      rolloutPercent: rolloutForm.rolloutPercent,
    })
    MessagePlugin.success('灰度配置已保存')
    rolloutVisible.value = false
    await loadPlugins()
  } finally {
    rolloutSaving.value = false
  }
  return true
}

async function listPlugin(row: AdminPluginCatalogVO) {
  await updatePlugin(row.id, { status: 'LISTED' })
  MessagePlugin.success('插件已上架')
  await loadPlugins()
}

async function unlistPlugin(row: AdminPluginCatalogVO) {
  await updatePlugin(row.id, { status: 'UNLISTED' })
  MessagePlugin.success('插件已下架')
  await loadPlugins()
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
