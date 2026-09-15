<template>
  <t-drawer
    :visible="visible"
    :header="`${tenantName} · 工作空间`"
    size="640px"
    :footer="false"
    :close-on-overlay-click="true"
    @close="emit('close')"
  >
    <t-table
      row-key="id"
      :data="workspaces"
      :columns="columns"
      :loading="loading"
      hover
      size="small"
    >
      <template #empty>
        <t-empty description="该租户下暂无工作空间" />
      </template>
    </t-table>
  </t-drawer>
</template>

<script setup lang="ts">
import { h, ref, watch } from 'vue'
import { MessagePlugin, Tag } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import { fetchTenantWorkspaces, type AdminWorkspaceVO } from '@/api/tenant'
import { formatDateTime } from '@/utils/datetime'

const props = defineProps<{
  visible: boolean
  tenantId: number | null
  tenantName: string
}>()

const emit = defineEmits<{
  close: []
}>()

const workspaces = ref<AdminWorkspaceVO[]>([])
const loading = ref(false)

const columns: PrimaryTableCol<AdminWorkspaceVO>[] = [
  { colKey: 'id', title: 'ID', width: 72 },
  { colKey: 'name', title: '名称', minWidth: 140 },
  { colKey: 'slug', title: '标识', minWidth: 120 },
  {
    colKey: 'status',
    title: '状态',
    width: 80,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 1 ? 'success' : 'warning', variant: 'light' }, () =>
        row.status === 1 ? '启用' : '停用',
      ),
  },
  { colKey: 'ownerId', title: '所有者', width: 90 },
  {
    colKey: 'createdAt',
    title: '创建时间',
    minWidth: 160,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
]

async function load() {
  if (!props.tenantId) {
    workspaces.value = []
    return
  }
  loading.value = true
  try {
    const { data } = await fetchTenantWorkspaces(props.tenantId)
    workspaces.value = data.data || []
  } catch {
    MessagePlugin.error('加载工作空间失败')
    workspaces.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.tenantId],
  ([visible]) => {
    if (visible) {
      void load()
    }
  },
)
</script>
