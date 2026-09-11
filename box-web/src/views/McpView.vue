<template>
  <div>
    <page-header title="MCP" desc="连接 MCP Server，同步并管理外部工具能力">
      <template #actions>
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          添加 MCP Server
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-table row-key="id" :data="items" :columns="columns" :bordered="true" stripe hover>
        <template #empty><t-empty description="暂无 MCP Server" /></template>
        <template #op="{ row }">
          <t-space>
            <t-button variant="text" theme="primary" :loading="syncingId === row.id" @click="sync(row.id)">同步</t-button>
            <t-button variant="text" theme="danger" @click="remove(row.id)">删除</t-button>
          </t-space>
        </template>
      </t-table>
    </t-loading>

    <t-dialog v-model:visible="dialogVisible" header="添加 MCP Server" :footer="false" width="560px">
      <t-form :data="form" label-align="top" @submit="submit">
        <t-form-item label="名称"><t-input v-model="form.name" /></t-form-item>
        <t-form-item label="Server Key"><t-input v-model="form.serverKey" placeholder="github_mcp" /></t-form-item>
        <t-form-item label="Endpoint URL"><t-input v-model="form.endpointUrl" placeholder="https://..." /></t-form-item>
        <t-form-item label="描述"><t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" /></t-form-item>
        <t-form-item><t-button theme="primary" type="submit" :loading="saving">添加</t-button></t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { createMcpServer, deleteMcpServer, listMcpServers, syncMcpServer, type McpServerVO } from '@/api/mcp'

const loading = ref(false)
const saving = ref(false)
const syncingId = ref<number | null>(null)
const items = ref<McpServerVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', serverKey: '', endpointUrl: '', description: '' })

const columns = [
  { colKey: 'name', title: '名称' },
  { colKey: 'serverKey', title: 'Key' },
  { colKey: 'endpointUrl', title: 'Endpoint', ellipsis: true },
  { colKey: 'transportType', title: '传输', width: 90 },
  { colKey: 'lastSyncAt', title: '最近同步', width: 180 },
  { colKey: 'op', title: '操作', width: 160 },
]

async function load() {
  loading.value = true
  try {
    const { data } = await listMcpServers()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', serverKey: '', endpointUrl: '', description: '' }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name.trim() || !form.value.serverKey.trim() || !form.value.endpointUrl.trim()) return
  saving.value = true
  try {
    await createMcpServer({
      name: form.value.name.trim(),
      serverKey: form.value.serverKey.trim(),
      endpointUrl: form.value.endpointUrl.trim(),
      description: form.value.description.trim() || undefined,
      transportType: 'SSE',
    })
    dialogVisible.value = false
    MessagePlugin.success('MCP Server 已添加')
    await load()
  } finally {
    saving.value = false
  }
}

async function sync(id: number) {
  syncingId.value = id
  try {
    await syncMcpServer(id)
    MessagePlugin.success('同步完成')
    await load()
  } finally {
    syncingId.value = null
  }
}

async function remove(id: number) {
  await deleteMcpServer(id)
  MessagePlugin.success('已删除')
  await load()
}

load()
</script>
