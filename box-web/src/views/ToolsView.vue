<template>
  <div>
    <page-header
      title="工具"
      desc="配置 HTTP 工具，绑定到 Agent 后支持 Tool Calling"
      :back-to="backTo"
      :back-label="backLabel"
    >
      <template #actions>
        <t-button v-if="can(PermissionCodes.TOOL_CREATE)" theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          新建工具
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-table v-if="items.length" row-key="id" :data="items" :columns="columns" :bordered="true" stripe hover>
        <template #op="{ row }">
          <t-space>
            <t-button variant="text" theme="primary" :loading="testingId === row.id" @click="runTest(row.id)">测试</t-button>
            <t-button v-if="can(PermissionCodes.TOOL_DELETE)" variant="text" theme="danger" @click="remove(row)">删除</t-button>
          </t-space>
        </template>
      </t-table>
      <resource-manage-empty v-else category="tools" @create="openCreate" />
    </t-loading>

    <t-dialog v-model:visible="dialogVisible" header="新建 HTTP 工具" :footer="false" width="560px">
      <t-form :data="form" label-align="top" @submit="submit">
        <t-form-item label="名称"><t-input v-model="form.name" /></t-form-item>
        <t-form-item label="Tool Key"><t-input v-model="form.toolKey" placeholder="如 get_weather" /></t-form-item>
        <t-form-item label="描述"><t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" /></t-form-item>
        <t-form-item label="URL"><t-input v-model="form.url" placeholder="https://..." /></t-form-item>
        <t-form-item label="Method">
          <t-select v-model="form.method" :options="['GET', 'POST', 'PUT', 'DELETE'].map((v) => ({ label: v, value: v }))" />
        </t-form-item>
        <t-form-item><t-button theme="primary" type="submit" :loading="saving">创建</t-button></t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ResourceManageEmpty from '@/components/ResourceManageEmpty.vue'
import { useResourceManageBack } from '@/composables/useResourceManageBack'
import { usePermission } from '@/composables/usePermission'
import { confirmResourceDelete } from '@/composables/useResourceDelete'
import { PermissionCodes } from '@/constants/permissions'
import { createTool, deleteTool, listTools, testTool, type ToolVO } from '@/api/tool'

const { backTo, backLabel } = useResourceManageBack('tools')
const { can } = usePermission()

const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const items = ref<ToolVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', toolKey: '', description: '', url: '', method: 'GET' })

const columns = [
  { colKey: 'name', title: '名称' },
  { colKey: 'toolKey', title: 'Key' },
  { colKey: 'type', title: '类型', width: 90 },
  { colKey: 'description', title: '描述', ellipsis: true },
  { colKey: 'op', title: '操作', width: 160 },
]

async function load() {
  loading.value = true
  try {
    const { data } = await listTools()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', toolKey: '', description: '', url: '', method: 'GET' }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name.trim() || !form.value.toolKey.trim() || !form.value.url.trim()) return
  saving.value = true
  try {
    await createTool({
      name: form.value.name.trim(),
      toolKey: form.value.toolKey.trim(),
      description: form.value.description.trim() || undefined,
      type: 'HTTP',
      httpConfig: { method: form.value.method, url: form.value.url.trim() },
    })
    dialogVisible.value = false
    MessagePlugin.success('工具已创建')
    await load()
  } finally {
    saving.value = false
  }
}

async function runTest(id: number) {
  testingId.value = id
  try {
    const { data } = await testTool(id)
    MessagePlugin.success(`HTTP ${data.data?.statusCode} · ${data.data?.durationMs}ms`)
  } finally {
    testingId.value = null
  }
}

function remove(item: ToolVO) {
  void confirmResourceDelete({
    header: '确认删除',
    body: `确定删除工具「${item.name}」吗？`,
    resourceLabel: '工具',
    onDelete: async () => {
      await deleteTool(item.id)
    },
    onSuccess: async () => {
      MessagePlugin.success('已删除')
      await load()
    },
  })
}

load()
</script>
