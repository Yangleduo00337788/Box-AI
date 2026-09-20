<template>
  <resource-manage-page
    category="mcp"
    v-model:keyword="keyword"
    :total="items.length"
    :filtered="filtered.length"
    :loading="loading"
    :can-create="can(PermissionCodes.TOOL_CREATE)"
    @create="openCreate"
  >
    <div v-if="filtered.length" class="resource-manage__list">
      <resource-item-card
        v-for="item in filtered"
        :key="item.id"
        :title="item.name"
        :description="item.description || item.endpointUrl"
        icon="server"
        tone="violet"
      >
        <template #tags>
          <t-tag size="small" variant="light">{{ item.transportType || 'SSE' }}</t-tag>
        </template>
        <template #meta>
          {{ item.serverKey }}
          <template v-if="item.lastSyncAt"> · 同步于 {{ item.lastSyncAt }}</template>
        </template>
        <template #actions>
          <t-space>
            <t-button variant="text" theme="primary" :loading="syncingId === item.id" @click="sync(item.id)">
              同步
            </t-button>
            <t-button variant="text" theme="danger" @click="remove(item)">
              删除
            </t-button>
          </t-space>
        </template>
      </resource-item-card>
    </div>
    <t-empty v-else-if="keyword.trim()" description="没有匹配的 MCP" />
    <resource-manage-empty v-else-if="!loading" category="mcp" @create="openCreate" />

    <template #dialogs>
      <t-dialog
        v-model:visible="dialogVisible"
        header="添加 MCP Server"
        width="560px"
        :confirm-btn="{ content: '添加', loading: saving }"
        :close-on-overlay-click="false"
        @confirm="submit"
      >
        <p class="resource-create-hint">添加后本工作空间成员均可同步并使用该 Server 的工具。</p>
        <t-form :data="form" label-align="top">
          <t-form-item label="名称">
            <t-input v-model="form.name" placeholder="例如：GitHub MCP" />
          </t-form-item>
          <t-form-item label="Server Key">
            <t-input v-model="form.serverKey" placeholder="github_mcp" />
          </t-form-item>
          <t-form-item label="传输方式">
            <t-select v-model="form.transportType" :options="transportOptions" />
          </t-form-item>
          <t-form-item label="Endpoint URL">
            <t-input v-model="form.endpointUrl" placeholder="https://..." />
          </t-form-item>
          <t-form-item label="描述">
            <t-textarea
              v-model="form.description"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="说明提供哪些能力"
            />
          </t-form-item>
        </t-form>
      </t-dialog>
    </template>
  </resource-manage-page>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import ResourceItemCard from '@/components/ResourceItemCard.vue'
import ResourceManageEmpty from '@/components/ResourceManageEmpty.vue'
import ResourceManagePage from '@/components/ResourceManagePage.vue'
import { useOpenCreateFromQuery } from '@/composables/useOpenCreateFromQuery'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { usePermission } from '@/composables/usePermission'
import { confirmResourceDelete } from '@/composables/useResourceDelete'
import { PermissionCodes } from '@/constants/permissions'
import { filterResourcesByKeyword } from '@/constants/resourceManage'
import { createMcpServer, deleteMcpServer, listMcpServers, syncMcpServer, type McpServerVO } from '@/api/mcp'

const { can } = usePermission()
const loading = ref(false)
const saving = ref(false)
const syncingId = ref<number | null>(null)
const keyword = ref('')
const items = ref<McpServerVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', serverKey: '', endpointUrl: '', description: '', transportType: 'SSE' })
const transportOptions = [
  { label: 'SSE', value: 'SSE' },
  { label: 'HTTP', value: 'HTTP' },
]

const filtered = computed(() =>
  filterResourcesByKeyword(items.value, keyword.value, (item) => [
    item.name,
    item.serverKey,
    item.endpointUrl,
    item.description,
    item.transportType,
  ]),
)

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
  form.value = { name: '', serverKey: '', endpointUrl: '', description: '', transportType: 'SSE' }
  dialogVisible.value = true
}

useOpenCreateFromQuery(openCreate)

async function submit() {
  if (!form.value.name.trim() || !form.value.serverKey.trim() || !form.value.endpointUrl.trim()) return false
  saving.value = true
  try {
    await createMcpServer({
      name: form.value.name.trim(),
      serverKey: form.value.serverKey.trim(),
      endpointUrl: form.value.endpointUrl.trim(),
      description: form.value.description.trim() || undefined,
      transportType: form.value.transportType,
    })
    dialogVisible.value = false
    MessagePlugin.success('MCP Server 已添加，本空间成员均可使用')
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

function remove(item: McpServerVO) {
  void confirmResourceDelete({
    header: '确认删除',
    body: `确定删除 MCP「${item.name}」吗？删除后本空间将无法再同步使用。`,
    resourceLabel: 'MCP',
    onDelete: async () => {
      await deleteMcpServer(item.id)
    },
    onSuccess: async () => {
      MessagePlugin.success('已删除')
      await load()
    },
  })
}

load()
useReloadOnWorkspaceChange(load)
</script>

<style scoped>
.resource-create-hint {
  margin: 0 0 16px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}
</style>
