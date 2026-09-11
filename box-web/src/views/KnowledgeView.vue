<template>
  <div>
    <page-header title="知识库" desc="上传文档、自动分块并建立向量索引，供 Agent RAG 检索">
      <template #actions>
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          新建知识库
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-table row-key="id" :data="items" :columns="columns" :bordered="true" stripe hover>
        <template #empty><t-empty description="暂无知识库" /></template>
        <template #op="{ row }">
          <t-space>
            <t-button variant="text" theme="primary" @click="openDetail(row)">文档</t-button>
            <t-button variant="text" theme="danger" @click="remove(row.id)">删除</t-button>
          </t-space>
        </template>
      </t-table>
    </t-loading>

    <t-dialog v-model:visible="createVisible" header="新建知识库" :footer="false" width="480px">
      <t-form :data="form" label-align="top" @submit="submitCreate">
        <t-form-item label="名称"><t-input v-model="form.name" maxlength="128" /></t-form-item>
        <t-form-item label="描述"><t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" /></t-form-item>
        <t-form-item><t-button theme="primary" type="submit" :loading="saving">创建</t-button></t-form-item>
      </t-form>
    </t-dialog>

    <t-drawer v-model:visible="detailVisible" :header="activeKb?.name || '文档'" size="640px">
      <div class="toolbar">
        <input ref="fileRef" type="file" class="hidden-input" accept=".txt,.md,.json,.csv,.log,text/plain" @change="onUpload" />
        <t-button theme="primary" :loading="uploading" @click="fileRef?.click()">上传文档</t-button>
      </div>
      <t-table row-key="id" :data="documents" :columns="docColumns" size="small" :bordered="true" stripe>
        <template #empty><t-empty description="暂无文档" /></template>
      </t-table>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listKnowledgeBases,
  listKnowledgeDocuments,
  uploadKnowledgeDocument,
  type KnowledgeBaseVO,
  type KnowledgeDocumentVO,
} from '@/api/knowledge'

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const items = ref<KnowledgeBaseVO[]>([])
const documents = ref<KnowledgeDocumentVO[]>([])
const createVisible = ref(false)
const detailVisible = ref(false)
const activeKb = ref<KnowledgeBaseVO | null>(null)
const fileRef = ref<HTMLInputElement | null>(null)
const form = ref({ name: '', description: '' })

const columns = [
  { colKey: 'name', title: '名称' },
  { colKey: 'documentCount', title: '文档', width: 80 },
  { colKey: 'chunkCount', title: '分块', width: 80 },
  { colKey: 'status', title: '状态', width: 100 },
  { colKey: 'op', title: '操作', width: 160 },
]

const docColumns = [
  { colKey: 'fileName', title: '文件名' },
  { colKey: 'chunkCount', title: '分块', width: 80 },
  { colKey: 'status', title: '状态', width: 100 },
]

async function load() {
  loading.value = true
  try {
    const { data } = await listKnowledgeBases()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', description: '' }
  createVisible.value = true
}

async function submitCreate() {
  if (!form.value.name.trim()) return
  saving.value = true
  try {
    await createKnowledgeBase({ name: form.value.name.trim(), description: form.value.description.trim() || undefined })
    createVisible.value = false
    MessagePlugin.success('知识库已创建')
    await load()
  } finally {
    saving.value = false
  }
}

async function openDetail(row: KnowledgeBaseVO) {
  activeKb.value = row
  detailVisible.value = true
  const { data } = await listKnowledgeDocuments(row.id)
  documents.value = data.data || []
}

async function onUpload(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !activeKb.value) return
  uploading.value = true
  try {
    await uploadKnowledgeDocument(activeKb.value.id, file)
    MessagePlugin.success('文档已上传并开始索引')
    const { data } = await listKnowledgeDocuments(activeKb.value.id)
    documents.value = data.data || []
    await load()
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function remove(id: number) {
  await deleteKnowledgeBase(id)
  MessagePlugin.success('已删除')
  await load()
}

load()
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.hidden-input { display: none; }
</style>
