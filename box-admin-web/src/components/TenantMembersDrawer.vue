<template>
  <t-drawer
    :visible="visible"
    :header="`${tenantName} · 成员`"
    size="560px"
    :close-on-overlay-click="true"
    @close="emit('close')"
  >
    <div class="members-toolbar">
      <t-form layout="inline" @submit="onAdd">
        <t-form-item>
          <t-input v-model="addEmail" placeholder="成员邮箱" clearable />
        </t-form-item>
        <t-form-item>
          <t-select v-model="addRole" :options="roleOptions" style="width: 140px" />
        </t-form-item>
        <t-form-item>
          <t-button theme="primary" type="submit" :loading="adding">添加</t-button>
        </t-form-item>
      </t-form>
    </div>

    <t-table
      row-key="userId"
      :data="members"
      :columns="columns"
      :loading="loading"
      hover
      size="small"
    >
      <template #empty>
        <t-empty description="暂无成员" />
      </template>
    </t-table>
  </t-drawer>
</template>

<script setup lang="ts">
import { h, ref, watch } from 'vue'
import { Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import {
  addTenantMember,
  fetchTenantMembers,
  updateTenantMemberStatus,
  type TenantMemberVO,
} from '@/api/tenant'

const props = defineProps<{
  visible: boolean
  tenantId: number | null
  tenantName: string
}>()

const emit = defineEmits<{
  close: []
}>()

const members = ref<TenantMemberVO[]>([])
const loading = ref(false)
const adding = ref(false)
const addEmail = ref('')
const addRole = ref('MEMBER')

const roleOptions = [
  { label: '成员', value: 'MEMBER' },
  { label: '管理员', value: 'TENANT_ADMIN' },
]

const columns: PrimaryTableCol<TenantMemberVO>[] = [
  { colKey: 'email', title: '邮箱', minWidth: 160 },
  { colKey: 'nickname', title: '昵称', width: 100 },
  {
    colKey: 'roleCode',
    title: '角色',
    width: 100,
    cell: (_, { row }) =>
      h(Tag, { theme: row.roleCode === 'TENANT_ADMIN' ? 'primary' : 'default', variant: 'light' }, () =>
        row.roleCode === 'TENANT_ADMIN' ? '管理员' : '成员',
      ),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '正常' : '停用',
      ),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 80,
    cell: (_, { row }) =>
      h(
        Link,
        {
          theme: row.status === 1 ? 'danger' : 'primary',
          hover: 'color',
          onClick: () => toggleStatus(row),
        },
        () => (row.status === 1 ? '停用' : '启用'),
      ),
  },
]

async function loadMembers() {
  if (!props.tenantId) return
  loading.value = true
  try {
    const { data } = await fetchTenantMembers(props.tenantId)
    members.value = data.data
  } finally {
    loading.value = false
  }
}

async function onAdd() {
  if (!props.tenantId || !addEmail.value.trim()) {
    MessagePlugin.warning('请输入成员邮箱')
    return
  }
  adding.value = true
  try {
    await addTenantMember(props.tenantId, {
      email: addEmail.value.trim(),
      roleCode: addRole.value,
    })
    MessagePlugin.success('成员已添加')
    addEmail.value = ''
    await loadMembers()
  } finally {
    adding.value = false
  }
}

async function toggleStatus(row: TenantMemberVO) {
  if (!props.tenantId) return
  const nextStatus = row.status === 1 ? 0 : 1
  await updateTenantMemberStatus(props.tenantId, row.userId, nextStatus)
  MessagePlugin.success(nextStatus === 1 ? '已启用' : '已停用')
  await loadMembers()
}

watch(
  () => [props.visible, props.tenantId] as const,
  ([visible, tenantId]) => {
    if (visible && tenantId) {
      loadMembers()
    }
  },
)
</script>

<style scoped>
.members-toolbar {
  margin-bottom: 16px;
}
</style>
