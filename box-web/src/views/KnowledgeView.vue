<template>
  <div>
    <page-header
      title="知识库"
      desc="上传文档、自动分块并建立向量索引，供 Agent RAG 检索"
      :back-to="backTo"
      :back-label="backLabel"
    >
      <template #actions>
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          新建知识库
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <t-table v-if="items.length" row-key="id" :data="items" :columns="columns" :bordered="true" stripe hover>
        <template #op="{ row }">
          <t-space>
            <t-button variant="text" theme="primary" @click="openDetail(row)">文档</t-button>
            <t-button variant="text" theme="danger" @click="remove(row.id)">删除</t-button>
          </t-space>
        </template>
      </t-table>
      <resource-manage-empty v-else category="knowledge" @create="openCreate" />
    </t-loading>

    <t-dialog v-model:visible="createVisible" header="新建知识库" :footer="false" width="480px">
      <t-form :data="form" label-align="top" @submit="submitCreate">
        <t-form-item label="名称"><t-input v-model="form.name" maxlength="128" /></t-form-item>
        <t-form-item label="描述"><t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" /></t-form-item>
        <t-form-item><t-button theme="primary" type="submit" :loading="saving">创建</t-button></t-form-item>
      </t-form>
    </t-dialog>

    <t-drawer v-model:visible="detailVisible" :header="activeKb?.name || '文档'" size="720px">
      <div class="toolbar">
        <input
          ref="fileRef"
          type="file"
          class="hidden-input"
          accept=".txt,.md,.json,.csv,.log,.pdf,.docx,text/plain,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
          @change="onUpload"
        />
        <t-button theme="primary" :loading="uploading" @click="fileRef?.click()">上传文档</t-button>
        <span class="upload-hint">支持 TXT、MD、PDF、DOCX</span>
      </div>
      <t-table row-key="id" :data="documents" :columns="docColumns" size="small" :bordered="true" stripe>
        <template #empty><t-empty description="暂无文档" /></template>
        <template #docOp="{ row }">
          <t-button variant="text" theme="primary" @click="openChunks(row)">查看分块</t-button>
        </template>
      </t-table>

      <section class="rag-test">
        <h3>RAG 检索测试</h3>
        <t-space direction="vertical" style="width: 100%">
          <t-textarea v-model="ragQuery" placeholder="输入测试问题" :autosize="{ minRows: 2, maxRows: 4 }" />
          <t-space>
            <t-input-number v-model="ragTopK" :min="1" :max="20" theme="column" label="Top K" />
            <t-button theme="primary" :loading="searching" @click="runRagTest">检索</t-button>
          </t-space>
        </t-space>
        <t-table
          v-if="searchHits.length"
          row-key="chunkId"
          :data="searchHits"
          :columns="searchColumns"
          size="small"
          :bordered="true"
          stripe
          class="rag-test__table"
        />
      </section>
    </t-drawer>

    <t-drawer v-model:visible="chunkVisible" :header="activeDoc?.fileName || '文档分块'" size="640px">
      <t-loading :loading="chunksLoading" size="small">
        <t-table
          v-if="chunks.length"
          row-key="id"
          :data="chunks"
          :columns="chunkColumns"
          size="small"
          :bordered="true"
          stripe
        >
          <template #chunkContent="{ row }">
            <div class="chunk-content">{{ row.content }}</div>
          </template>
        </t-table>
        <t-empty v-else description="暂无分块数据" />
      </t-loading>
    </t-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ResourceManageEmpty from '@/components/ResourceManageEmpty.vue'
import { useResourceManageBack } from '@/composables/useResourceManageBack'
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  listKnowledgeBases,
  listDocumentChunks,
  listKnowledgeDocuments,
  searchKnowledge,
  uploadKnowledgeDocument,
  type KnowledgeBaseVO,
  type KnowledgeChunkVO,
  type KnowledgeDocumentVO,
  type KnowledgeSearchHit,
} from '@/api/knowledge'

const { backTo, backLabel } = useResourceManageBack('knowledge')

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
const ragQuery = ref('')
const ragTopK = ref(5)
const searching = ref(false)
const searchHits = ref<KnowledgeSearchHit[]>([])
const chunkVisible = ref(false)
const chunksLoading = ref(false)
const chunks = ref<KnowledgeChunkVO[]>([])
const activeDoc = ref<KnowledgeDocumentVO | null>(null)

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
  { colKey: 'docOp', title: '操作', width: 100 },
]

const chunkColumns = [
  { colKey: 'chunkIndex', title: '#', width: 56 },
  { colKey: 'tokenCount', title: 'Token', width: 72 },
  { colKey: 'chunkContent', title: '内容' },
]

const searchColumns = [
  { colKey: 'score', title: '分数', width: 80 },
  { colKey: 'chunkIndex', title: '分块', width: 80 },
  { colKey: 'content', title: '内容', ellipsis: true },
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
  ragQuery.value = ''
  searchHits.value = []
  const { data } = await listKnowledgeDocuments(row.id)
  documents.value = data.data || []
}

async function runRagTest() {
  if (!activeKb.value || !ragQuery.value.trim()) return
  searching.value = true
  try {
    const { data } = await searchKnowledge(activeKb.value.id, ragQuery.value.trim(), ragTopK.value)
    searchHits.value = data.data || []
    if (!searchHits.value.length) {
      MessagePlugin.info('未检索到相关内容')
    }
  } finally {
    searching.value = false
  }
}

async function openChunks(doc: KnowledgeDocumentVO) {
  activeDoc.value = doc
  chunkVisible.value = true
  chunksLoading.value = true
  chunks.value = []
  try {
    const { data } = await listDocumentChunks(doc.id)
    chunks.value = data.data || []
  } finally {
    chunksLoading.value = false
  }
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
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.upload-hint {
  font-size: 12px;
  color: var(--box-muted);
}
.hidden-input { display: none; }
.chunk-content {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  max-height: 160px;
  overflow-y: auto;
}
.rag-test { margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--td-component-border); }
.rag-test h3 { margin: 0 0 12px; font-size: 15px; }
.rag-test__table { margin-top: 12px; }
</style>
