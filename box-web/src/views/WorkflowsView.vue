<template>
  <resource-manage-page
    category="workflows"
    v-model:keyword="keyword"
    :total="items.length"
    :filtered="filtered.length"
    :loading="loading"
    :can-create="can(PermissionCodes.WORKFLOW_CREATE)"
    @create="openCreate"
  >
    <div v-if="filtered.length" class="resource-manage__list">
      <resource-item-card
        v-for="item in filtered"
        :key="item.id"
        :title="item.name"
        :description="item.description"
        icon="tree-square-dot"
        tone="ink"
        clickable
        @click="openEditor(item)"
      >
        <template #tags>
          <t-tag size="small" variant="light" :theme="item.status === 'PUBLISHED' ? 'success' : 'default'">
            {{ workflowStatusLabel(item.status) }}
          </t-tag>
        </template>
        <template #meta>草稿 v{{ item.draftVersionNo ?? 1 }}</template>
      </resource-item-card>
    </div>
    <t-empty v-else-if="keyword.trim()" description="没有匹配的工作流" />
    <resource-manage-empty v-else-if="!loading" category="workflows" @create="openCreate" />

    <template #dialogs>
      <t-dialog
        v-model:visible="dialogVisible"
        header="新建工作流"
        width="480px"
        :confirm-btn="{ content: '创建并编辑', loading: saving }"
        :close-on-overlay-click="false"
        @confirm="submit"
      >
        <p class="resource-create-hint">创建后本工作空间成员均可使用和编辑。</p>
        <t-form :data="form" label-align="top">
          <t-form-item label="名称">
            <t-input v-model="form.name" placeholder="例如：客服工单处理" />
          </t-form-item>
          <t-form-item label="描述">
            <t-textarea
              v-model="form.description"
              :autosize="{ minRows: 2, maxRows: 4 }"
              placeholder="说明用途，方便同事识别"
            />
          </t-form-item>
        </t-form>
      </t-dialog>
    </template>
  </resource-manage-page>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import ResourceItemCard from '@/components/ResourceItemCard.vue'
import ResourceManageEmpty from '@/components/ResourceManageEmpty.vue'
import ResourceManagePage from '@/components/ResourceManagePage.vue'
import { useOpenCreateFromQuery } from '@/composables/useOpenCreateFromQuery'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { usePermission } from '@/composables/usePermission'
import { PermissionCodes } from '@/constants/permissions'
import { filterResourcesByKeyword } from '@/constants/resourceManage'
import { createWorkflow, listWorkflows, type WorkflowVO } from '@/api/workflow'

const { can } = usePermission()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const items = ref<WorkflowVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', description: '' })

const filtered = computed(() =>
  filterResourcesByKeyword(items.value, keyword.value, (item) => [item.name, item.description, item.status]),
)

async function load() {
  loading.value = true
  try {
    const { data } = await listWorkflows()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', description: '' }
  dialogVisible.value = true
}

useOpenCreateFromQuery(openCreate)

async function submit() {
  if (!form.value.name.trim()) return false
  saving.value = true
  try {
    const { data } = await createWorkflow({
      name: form.value.name.trim(),
      description: form.value.description.trim() || undefined,
    })
    dialogVisible.value = false
    if (data.data?.id) {
      router.push(`/workflows/${data.data.id}/editor`)
    }
    await load()
  } finally {
    saving.value = false
  }
}

function openEditor(item: WorkflowVO) {
  router.push(`/workflows/${item.id}/editor`)
}

function workflowStatusLabel(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'DRAFT') return '草稿'
  return status || '草稿'
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
