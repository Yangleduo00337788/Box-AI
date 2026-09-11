<template>
  <div class="plugin-catalog-page">
    <page-header title="插件市场" desc="管理插件分类与上架插件，供 C 端用户浏览安装。" />

    <t-card :bordered="true">
      <t-tabs v-model="tab">
        <t-tab-panel value="plugins" label="插件" />
        <t-tab-panel value="categories" label="分类" />
      </t-tabs>

      <div class="tab-body">
        <div v-if="tab === 'plugins'" class="toolbar">
          <t-button theme="primary" @click="openPluginCreate">新建插件</t-button>
        </div>
        <t-table
          v-if="tab === 'plugins'"
          row-key="id"
          :data="plugins"
          :columns="pluginColumns"
          :loading="loading"
          bordered
          stripe
        />

        <div v-if="tab === 'categories'" class="toolbar">
          <t-button theme="primary" @click="openCategoryCreate">新建分类</t-button>
        </div>
        <t-table
          v-if="tab === 'categories'"
          row-key="value"
          :data="categories"
          :columns="categoryColumns"
          :loading="categoryLoading"
          bordered
          stripe
        />
      </div>
    </t-card>

    <t-dialog
      v-model:visible="pluginDialogVisible"
      :header="pluginEditing ? '编辑插件' : '新建插件'"
      width="640px"
      :confirm-btn="{ content: pluginEditing ? '保存' : '创建', loading: pluginSaving }"
      @confirm="onSavePlugin"
    >
      <t-form ref="pluginFormRef" :data="pluginForm" :rules="pluginRules" label-width="96px">
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
import { MessagePlugin } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createPlugin,
  createPluginCategory,
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
const categories = ref<PluginCategoryVO[]>([])

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

const pluginForm = reactive({
  pluginCode: '',
  category: '',
  title: '',
  description: '',
  sortOrder: 0,
  status: 'LISTED',
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

const pluginColumns: PrimaryTableCol<AdminPluginCatalogVO>[] = [
  { colKey: 'title', title: '标题', minWidth: 160 },
  { colKey: 'pluginCode', title: '编码', width: 140 },
  { colKey: 'category', title: '分类', width: 100 },
  { colKey: 'installCount', title: '安装数', width: 90 },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) => (row.status === 'LISTED' ? '上架' : '下架'),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 80,
    cell: (_, { row }) =>
      h('a', { href: 'javascript:void(0)', onClick: () => openPluginEdit(row) }, '编辑'),
  },
]

const categoryColumns: PrimaryTableCol<PluginCategoryVO>[] = [
  { colKey: 'label', title: '名称', minWidth: 120 },
  { colKey: 'value', title: '编码', width: 140 },
  { colKey: 'desc', title: '描述', ellipsis: true },
  {
    colKey: 'actions',
    title: '操作',
    width: 80,
    cell: (_, { row }) =>
      h('a', { href: 'javascript:void(0)', onClick: () => openCategoryEdit(row) }, '编辑'),
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
  })
  pluginDialogVisible.value = true
}

async function onSavePlugin() {
  const valid = await pluginFormRef.value?.validate()
  if (valid !== true) return
  pluginSaving.value = true
  try {
    if (pluginEditing.value && pluginEditingId.value) {
      await updatePlugin(pluginEditingId.value, {
        category: pluginForm.category,
        title: pluginForm.title,
        description: pluginForm.description,
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
</style>
