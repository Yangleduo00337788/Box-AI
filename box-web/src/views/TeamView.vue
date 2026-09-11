<template>
  <div class="team-page">
    <page-header
      title="团队"
      :desc="isEnterprise ? '管理企业租户成员，邀请同事加入协作。' : '个人版账号无需团队管理。'"
    />

    <t-empty
      v-if="!isEnterprise"
      description="个人版账号无需团队管理，切换企业版后可邀请同事协作。"
    />

    <template v-else>
      <div class="team-toolbar">
        <t-form layout="inline" @submit="onAdd">
          <t-form-item>
            <t-input v-model="addEmail" placeholder="同事邮箱（需已注册）" clearable style="width: 280px" />
          </t-form-item>
          <t-form-item>
            <t-select v-model="addRole" :options="roleOptions" style="width: 140px" />
          </t-form-item>
          <t-form-item>
            <t-button theme="primary" type="submit" :loading="adding" :disabled="!canManage">
              邀请成员
            </t-button>
          </t-form-item>
        </t-form>
      </div>

      <t-table
        row-key="userId"
        :data="members"
        :columns="columns"
        :loading="loading"
        bordered
        stripe
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  addTenantMember,
  fetchTenantMembers,
  updateTenantMemberStatus,
  type TenantMemberVO,
} from '@/api/tenant'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const members = ref<TenantMemberVO[]>([])
const loading = ref(false)
const adding = ref(false)
const addEmail = ref('')
const addRole = ref('MEMBER')

const isEnterprise = computed(() => auth.tenant?.tenantType === 'ENTERPRISE')
const canManage = computed(() => {
  const me = members.value.find((item) => item.userId === auth.user?.id)
  return me?.roleCode === 'TENANT_ADMIN' && me?.status === 1
})

const roleOptions = [
  { label: '成员', value: 'MEMBER' },
  { label: '管理员', value: 'TENANT_ADMIN' },
]

const columns: PrimaryTableCol<TenantMemberVO>[] = [
  { colKey: 'email', title: '邮箱', minWidth: 180 },
  { colKey: 'nickname', title: '昵称', width: 120 },
  {
    colKey: 'roleCode',
    title: '角色',
    width: 100,
    cell: (_, { row }) => (row.roleCode === 'TENANT_ADMIN' ? '管理员' : '成员'),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) => (row.status === 1 ? '正常' : '停用'),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 90,
    cell: (_, { row }) => {
      if (!canManage.value || row.userId === auth.user?.id) {
        return '-'
      }
      return h(
        'a',
        {
          href: 'javascript:void(0)',
          onClick: () => toggleStatus(row),
        },
        row.status === 1 ? '停用' : '启用',
      )
    },
  },
]

async function loadMembers() {
  if (!isEnterprise.value) return
  loading.value = true
  try {
    const { data } = await fetchTenantMembers()
    members.value = data.data
  } finally {
    loading.value = false
  }
}

async function onAdd() {
  if (!addEmail.value.trim()) {
    MessagePlugin.warning('请输入成员邮箱')
    return
  }
  adding.value = true
  try {
    await addTenantMember({
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
  const nextStatus = row.status === 1 ? 0 : 1
  await updateTenantMemberStatus(row.userId, nextStatus)
  MessagePlugin.success(nextStatus === 1 ? '已启用' : '已停用')
  await loadMembers()
}

onMounted(() => {
  loadMembers()
})
</script>

<style scoped>
.team-toolbar {
  margin-bottom: 16px;
}

.team-page :deep(.t-empty) {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}
</style>
