<template>
  <resource-manage-page
    category="tools"
    v-model:keyword="keyword"
    :total="items.length"
    :filtered="filtered.length"
    :loading="loading"
    :can-create="can(PermissionCodes.TOOL_CREATE)"
    embedded
    @create="openCreate"
  >
    <div v-if="filtered.length" class="plugin-market__list">
      <resource-item-card
        v-for="item in filtered"
        :key="item.id"
        :title="item.name"
        :description="item.description"
        icon="tools"
        tone="stone"
      >
        <template #tags>
          <t-tag size="small" variant="light" theme="success">工作空间</t-tag>
          <t-tag size="small" variant="light">{{ item.type || 'HTTP' }}</t-tag>
        </template>
        <template #meta>{{ item.toolKey }}{{ methodOf(item) ? ` · ${methodOf(item)}` : '' }}</template>
        <template #actions>
          <t-button theme="primary" size="small" :loading="testingId === item.id" @click="runTest(item.id)">
            测试
          </t-button>
          <t-button
            v-if="can(PermissionCodes.TOOL_DELETE)"
            variant="outline"
            theme="default"
            size="small"
            @click="remove(item)"
          >
            删除
          </t-button>
        </template>
      </resource-item-card>
    </div>
    <t-empty v-else-if="keyword.trim()" description="没有匹配的工具" />
    <resource-manage-empty v-else-if="!loading" category="tools" @create="openCreate" />

    <template #dialogs>
      <t-dialog
        v-model:visible="dialogVisible"
        header="新建 HTTP 工具"
        width="560px"
        :confirm-btn="{ content: '创建', loading: saving }"
        :close-on-overlay-click="false"
        @confirm="submit"
      >
        <p class="resource-create-hint">创建后本工作空间成员均可绑定到 Agent 使用。</p>
        <t-form :data="form" label-align="top">
          <t-form-item label="名称">
            <t-input v-model="form.name" placeholder="例如：查询天气" />
          </t-form-item>
          <t-form-item label="Tool Key">
            <t-input v-model="form.toolKey" placeholder="如 get_weather" />
          </t-form-item>
          <t-form-item label="描述">
            <t-textarea
              v-model="form.description"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="说明能力，便于模型选择调用"
            />
          </t-form-item>
          <t-form-item label="URL">
            <t-input v-model="form.url" placeholder="https://..." />
          </t-form-item>
          <t-form-item label="Method">
            <t-select v-model="form.method" :options="methodOptions" />
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
import { createTool, deleteTool, listTools, testTool, type ToolVO } from '@/api/tool'

const { can } = usePermission()
const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const keyword = ref('')
const items = ref<ToolVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', toolKey: '', description: '', url: '', method: 'GET' })
const methodOptions = ['GET', 'POST', 'PUT', 'DELETE'].map((value) => ({ label: value, value }))

const filtered = computed(() =>
  filterResourcesByKeyword(items.value, keyword.value, (item) => [
    item.name,
    item.toolKey,
    item.description,
    item.type,
    methodOf(item),
  ]),
)

async function load() {
  loading.value = true
  try {
    const { data } = await listTools()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function methodOf(item: ToolVO) {
  return item.httpConfig?.method || ''
}

function openCreate() {
  form.value = { name: '', toolKey: '', description: '', url: '', method: 'GET' }
  dialogVisible.value = true
}

useOpenCreateFromQuery(openCreate)

async function submit() {
  if (!form.value.name.trim() || !form.value.toolKey.trim() || !form.value.url.trim()) return false
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
    MessagePlugin.success('工具已创建，本空间成员均可使用')
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
    body: `确定删除工具「${item.name}」吗？删除后本空间将无法再绑定使用。`,
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
useReloadOnWorkspaceChange(load)
</script>

<style scoped>
.resource-create-hint {
  margin: 0 0 16px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}
</style>
