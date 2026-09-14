<template>
  <div class="users-page admin-page">
    <page-header title="用户管理" desc="查看平台用户，新建平台管理员，启停账号。停用后无法登录。">
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建管理员</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <div class="admin-toolbar">
        <t-input v-model="keyword" clearable placeholder="搜索邮箱、昵称或账号" style="width: 240px" />
        <t-select v-model="userType" :options="typeOptions" placeholder="账号类型" clearable style="width: 160px" />
        <t-select v-model="statusFilter" :options="statusOptions" placeholder="状态" clearable style="width: 120px" />
        <t-button theme="primary" :loading="loading" @click="onSearch">查询</t-button>
      </div>
      <t-table
        row-key="id"
        :data="users"
        :columns="columns"
        :loading="loading"
        hover
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <template #empty>
          <t-empty description="暂无用户" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      header="新建平台管理员"
      width="480px"
      :confirm-btn="{ content: '创建', loading: saving }"
      @confirm="onCreate"
    >
      <t-form ref="formRef" :data="form" :rules="rules" label-width="88px">
        <t-form-item label="邮箱" name="email">
          <t-input v-model="form.email" placeholder="用于登录管理端" />
        </t-form-item>
        <t-form-item label="密码" name="password">
          <t-input v-model="form.password" type="password" placeholder="至少 8 位" />
        </t-form-item>
        <t-form-item label="昵称" name="nickname">
          <t-input v-model="form.nickname" placeholder="可选，默认取邮箱前缀" />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PageInfo, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  createPlatformAdmin,
  listPlatformUsers,
  updatePlatformUserStatus,
  type PlatformUserVO,
} from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/datetime'

const auth = useAuthStore()
const loading = ref(false)
const users = ref<PlatformUserVO[]>([])
const keyword = ref('')
const userType = ref('')
const statusFilter = ref<number | ''>('')
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstanceFunctions>()
const form = reactive({
  email: '',
  password: '',
  nickname: '',
})
const rules: FormProps['rules'] = {
  email: [
    { required: true, message: '请输入邮箱' },
    { email: { ignore_max_length: true }, message: '邮箱格式不正确' },
  ],
  password: [
    { required: true, message: '请输入密码' },
    { min: 8, message: '密码至少 8 位' },
  ],
}

const typeOptions = [
  { label: '租户用户', value: 'TENANT_USER' },
  { label: '平台管理员', value: 'PLATFORM_ADMIN' },
]
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 },
]

const typeLabel: Record<string, string> = {
  TENANT_USER: '租户用户',
  PLATFORM_ADMIN: '平台管理员',
}

const columns: PrimaryTableCol<PlatformUserVO>[] = [
  { colKey: 'id', title: 'ID', width: 80 },
  { colKey: 'email', title: '邮箱', minWidth: 200, cell: (_, { row }) => row.email || '-' },
  { colKey: 'nickname', title: '昵称', width: 140, cell: (_, { row }) => row.nickname || '-' },
  { colKey: 'username', title: '账号', width: 140, cell: (_, { row }) => row.username || '-' },
  {
    colKey: 'userType',
    title: '类型',
    width: 120,
    cell: (_, { row }) =>
      h(
        Tag,
        { theme: row.userType === 'PLATFORM_ADMIN' ? 'warning' : 'default', variant: 'light' },
        () => typeLabel[row.userType || ''] || row.userType || '-',
      ),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '正常' : '停用',
      ),
  },
  {
    colKey: 'lastLoginAt',
    title: '最近登录',
    width: 180,
    cell: (_, { row }) => formatDateTime(row.lastLoginAt),
  },
  {
    colKey: 'createdAt',
    title: '注册时间',
    width: 180,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 90,
    fixed: 'right',
    cell: (_, { row }) =>
      h(
        Link,
        {
          theme: row.status === 1 ? 'warning' : 'success',
          hover: 'color',
          disabled: auth.user?.id === row.id,
          onClick: () => toggleStatus(row),
        },
        () => (row.status === 1 ? '停用' : '启用'),
      ),
  },
]

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await listPlatformUsers({
      keyword: keyword.value.trim() || undefined,
      userType: userType.value || undefined,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    const page = data.data
    users.value = page?.records || []
    pagination.total = page?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  void loadUsers()
}

function onPageChange(pageInfo: PageInfo) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  void loadUsers()
}

function toggleStatus(row: PlatformUserVO) {
  if (auth.user?.id === row.id) {
    MessagePlugin.warning('不能停用当前登录账号')
    return
  }
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 1) {
    void applyStatus(row, 1)
    return
  }
  const dialog = DialogPlugin.confirm({
    header: '停用用户',
    body: `确定停用「${row.nickname || row.email || row.id}」？停用后该账号无法登录。`,
    theme: 'warning',
    onConfirm: async () => {
      await applyStatus(row, 0)
      dialog.destroy()
    },
  })
}

async function applyStatus(row: PlatformUserVO, status: number) {
  await updatePlatformUserStatus(row.id, status)
  MessagePlugin.success(status === 1 ? '已启用' : '已停用')
  await loadUsers()
}

function openCreate() {
  form.email = ''
  form.password = ''
  form.nickname = ''
  dialogVisible.value = true
}

async function onCreate() {
  const valid = await formRef.value?.validate()
  if (valid !== true) return false
  saving.value = true
  try {
    await createPlatformAdmin({
      email: form.email.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined,
    })
    MessagePlugin.success('管理员已创建')
    dialogVisible.value = false
    pagination.current = 1
    await loadUsers()
  } finally {
    saving.value = false
  }
  return true
}

onMounted(loadUsers)
</script>
