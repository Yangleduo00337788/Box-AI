<template>
  <div>
    <page-header
      title="工作流"
      desc="可视化编排 Agent 工作流，支持调试与发布"
      :back-to="backTo"
      :back-label="backLabel"
    >
      <template #actions>
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          新建工作流
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <div v-if="items.length" class="grid">
        <article v-for="item in items" :key="item.id" class="card" @click="openEditor(item)">
          <div class="card__head">
            <h3>{{ item.name }}</h3>
            <t-tag size="small" variant="light">{{ item.status }}</t-tag>
          </div>
          <p>{{ item.description || '暂无描述' }}</p>
          <div class="card__meta">v{{ item.draftVersionNo ?? 1 }}</div>
        </article>
      </div>
      <resource-manage-empty v-else category="workflows" @create="openCreate" />
    </t-loading>

    <t-dialog v-model:visible="dialogVisible" header="新建工作流" :footer="false" width="480px">
      <t-form :data="form" label-align="top" @submit="submit">
        <t-form-item label="名称"><t-input v-model="form.name" /></t-form-item>
        <t-form-item label="描述"><t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" /></t-form-item>
        <t-form-item><t-button theme="primary" type="submit" :loading="saving">创建并编辑</t-button></t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import ResourceManageEmpty from '@/components/ResourceManageEmpty.vue'
import { useResourceManageBack } from '@/composables/useResourceManageBack'
import { createWorkflow, listWorkflows, type WorkflowVO } from '@/api/workflow'

const { backTo, backLabel } = useResourceManageBack('workflows')

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const items = ref<WorkflowVO[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', description: '' })

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

async function submit() {
  if (!form.value.name.trim()) return
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

load()
</script>

<style scoped>
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.card { padding: 16px; border: 1px solid var(--td-component-border); border-radius: 12px; cursor: pointer; }
.card:hover { border-color: var(--td-brand-color); }
.card__head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.card__head h3 { margin: 0; font-size: 16px; }
.card p { margin: 0 0 12px; color: var(--td-text-color-secondary); font-size: 13px; }
.card__meta { font-size: 12px; color: var(--td-text-color-placeholder); }
</style>
