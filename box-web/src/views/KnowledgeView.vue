<template>
  <resource-manage-page
    category="knowledge"
    v-model:keyword="keyword"
    :total="items.length"
    :filtered="filtered.length"
    :loading="loading"
    :can-create="can(PermissionCodes.KNOWLEDGE_CREATE)"
    embedded
    @create="openCreate"
  >
    <div v-if="filtered.length" class="plugin-market__list">
      <resource-item-card
        v-for="item in filtered"
        :key="item.id"
        :title="item.name"
        :description="item.description"
        icon="folder"
        tone="sky"
        clickable
        @click="openDetail(item)"
      >
        <template #tags>
          <t-tag size="small" variant="light" theme="success">工作空间</t-tag>
          <t-tag size="small" variant="light" :theme="statusTheme(item.status || 'READY')">
            {{ statusLabel(item.status || 'READY') }}
          </t-tag>
        </template>
        <template #meta>{{ item.documentCount ?? 0 }} 篇文档 · {{ item.chunkCount ?? 0 }} 个分块</template>
        <template #actions>
          <t-button theme="primary" size="small" @click="openDetail(item)">打开</t-button>
          <t-button v-if="can(PermissionCodes.KNOWLEDGE_UPDATE)" variant="outline" theme="default" size="small" @click="openEdit(item)">
            编辑
          </t-button>
          <t-button
            v-if="can(PermissionCodes.KNOWLEDGE_DELETE)"
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
    <t-empty v-else-if="keyword.trim()" description="没有匹配的知识库" />
    <resource-manage-empty v-else-if="!loading" category="knowledge" @create="openCreate" />

    <template #dialogs>
    <t-dialog
      v-model:visible="formVisible"
      :header="editingKb ? '编辑知识库' : '新建知识库'"
      width="480px"
      :confirm-btn="{ content: editingKb ? '保存' : '创建', loading: saving }"
      :close-on-overlay-click="false"
      @confirm="submitForm"
    >
      <p v-if="!editingKb" class="resource-create-hint">创建后本工作空间成员均可检索和使用。</p>
      <t-form :data="form" label-align="top">
        <t-form-item label="名称"><t-input v-model="form.name" maxlength="128" placeholder="例如：产品手册" /></t-form-item>
        <t-form-item label="描述">
          <t-textarea
            v-model="form.description"
            :autosize="{ minRows: 2, maxRows: 4 }"
            placeholder="说明收录范围，方便同事选用"
          />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-drawer v-model:visible="detailVisible" :header="activeKb?.name || '文档'" size="720px">
      <div v-if="canUpload" class="toolbar">
        <input
          ref="fileRef"
          type="file"
          class="hidden-input"
          multiple
          :accept="KNOWLEDGE_DOCUMENT_ACCEPT"
          @change="onUpload"
        />
        <t-button theme="primary" :loading="uploadingCount > 0" @click="fileRef?.click()">
          {{ uploadingCount > 0 ? `上传中 (${uploadingCount})` : '上传文档' }}
        </t-button>
        <t-button variant="outline" @click="urlVisible = true">从 URL 导入</t-button>
        <span class="upload-hint">{{ KNOWLEDGE_DOCUMENT_UPLOAD_HINT }}</span>
      </div>
      <t-table row-key="rowKey" :data="documentRows" :columns="docColumns" size="small" :bordered="true" stripe>
        <template #empty><t-empty description="暂无文档" /></template>
        <template #docStatus="{ row }">
          <div v-if="row.isPending || PROCESSING_STATUSES.has(row.status)" class="doc-status-progress">
            <t-progress :percentage="rowDisplayProgress(row)" size="small" :label="false" />
            <span class="doc-status-label">
              {{ statusLabel(row.status) }} · {{ rowDisplayProgress(row) }}%
            </span>
          </div>
          <t-tag v-else :theme="statusTheme(row.status)" variant="light" size="small">
            {{ statusLabel(row.status) }}
          </t-tag>
        </template>
        <template #docError="{ row }">
          <span v-if="row.status === 'FAILED' && row.errorMessage" class="doc-error">{{ row.errorMessage }}</span>
        </template>
        <template #docOp="{ row }">
          <t-space v-if="!row.isPending" size="small">
            <t-button variant="text" theme="primary" @click="openChunks(row)">查看分块</t-button>
            <t-button
              v-if="canUpload && (row.status === 'FAILED' || row.status === 'QUEUED' || row.status === 'PARSING')"
              variant="text"
              theme="warning"
              :loading="retryingId === row.id"
              @click="retryDocument(row.id)"
            >
              重试
            </t-button>
            <t-button
              v-if="canDeleteDocument"
              variant="text"
              theme="danger"
              :loading="deletingDocId === row.id"
              @click="removeDocument(row)"
            >
              删除
            </t-button>
          </t-space>
        </template>
      </t-table>

      <section class="rag-test">
        <h3>RAG 测试</h3>
        <t-space direction="vertical" style="width: 100%">
          <t-textarea v-model="ragQuery" placeholder="输入测试问题" :autosize="{ minRows: 2, maxRows: 4 }" />
          <t-space>
            <t-input-number v-model="ragTopK" :min="1" :max="20" theme="column" label="Top K" />
            <t-button variant="outline" :loading="searching" @click="runRagTest">检索</t-button>
            <t-button theme="primary" :loading="answering" @click="runTestAnswer">生成回答</t-button>
          </t-space>
        </t-space>
        <div v-if="testAnswer" class="rag-answer">
          <h4>回答</h4>
          <p>{{ testAnswer }}</p>
        </div>
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

    <t-dialog
      v-model:visible="urlVisible"
      header="从 URL 导入"
      :confirm-btn="{ content: '导入', loading: urlImporting }"
      @confirm="submitUrlImport"
    >
      <t-form label-width="88px">
        <t-form-item label="网页地址">
          <t-input v-model="importUrl" placeholder="https://example.com/docs/guide" />
        </t-form-item>
        <t-form-item label="同步周期">
          <t-input v-model="importCron" placeholder="可选，如 0 0 * * *" />
        </t-form-item>
      </t-form>
    </t-dialog>
    </template>
  </resource-manage-page>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
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
import {
  createKnowledgeBase,
  deleteKnowledgeBase,
  deleteKnowledgeDocument,
  listKnowledgeBases,
  listDocumentChunks,
  listKnowledgeDocuments,
  importKnowledgeUrl,
  retryKnowledgeDocument,
  searchKnowledge,
  testKnowledgeAnswer,
  updateKnowledgeBase,
  uploadKnowledgeDocument,
  type KnowledgeBaseVO,
  type KnowledgeChunkVO,
  type KnowledgeDocumentVO,
  type KnowledgeSearchHit,
} from '@/api/knowledge'
import {
  KNOWLEDGE_DOCUMENT_ACCEPT,
  KNOWLEDGE_DOCUMENT_UPLOAD_HINT,
} from '@/constants/knowledgeDocumentUpload'

const { can } = usePermission()

const canUpload = computed(() => can(PermissionCodes.KNOWLEDGE_UPLOAD))
const canDeleteDocument = computed(() => can(PermissionCodes.KNOWLEDGE_DELETE))

const UPLOAD_CONCURRENCY = 3

type DocumentRow = KnowledgeDocumentVO & {
  rowKey: string
  isPending?: boolean
  uploadProgress?: number
}

const loading = ref(false)
const saving = ref(false)
const uploadingCount = ref(0)
const pendingUploads = ref<Record<string, { fileName: string; progress: number }>>({})
const urlVisible = ref(false)
const urlImporting = ref(false)
const importUrl = ref('')
const importCron = ref('')
const items = ref<KnowledgeBaseVO[]>([])
const keyword = ref('')
const documents = ref<KnowledgeDocumentVO[]>([])
const formVisible = ref(false)
const editingKb = ref<KnowledgeBaseVO | null>(null)
const detailVisible = ref(false)
const activeKb = ref<KnowledgeBaseVO | null>(null)
const fileRef = ref<HTMLInputElement | null>(null)
const form = ref({ name: '', description: '' })
const ragQuery = ref('')
const ragTopK = ref(5)
const searching = ref(false)
const answering = ref(false)
const retryingId = ref<number | null>(null)
const deletingDocId = ref<number | null>(null)
const searchHits = ref<KnowledgeSearchHit[]>([])
const testAnswer = ref('')
const chunkVisible = ref(false)
const chunksLoading = ref(false)
const chunks = ref<KnowledgeChunkVO[]>([])
const activeDoc = ref<KnowledgeDocumentVO | null>(null)

const filtered = computed(() =>
  filterResourcesByKeyword(items.value, keyword.value, (item) => [item.name, item.description, item.status]),
)

const docColumns = [
  { colKey: 'fileName', title: '文件名' },
  { colKey: 'chunkCount', title: '分块', width: 80 },
  { colKey: 'docStatus', title: '进度', width: 180 },
  { colKey: 'docError', title: '失败原因', ellipsis: true },
  { colKey: 'docOp', title: '操作', width: 220 },
]

const documentRows = computed<DocumentRow[]>(() => {
  const pending = Object.entries(pendingUploads.value).map(([key, item]) => ({
    rowKey: key,
    isPending: true,
    id: 0,
    knowledgeBaseId: activeKb.value?.id ?? 0,
    name: item.fileName,
    fileName: item.fileName,
    fileType: '',
    fileSize: 0,
    chunkCount: 0,
    status: 'UPLOADING',
    uploadProgress: item.progress,
    createdAt: '',
    updatedAt: '',
  }))
  const saved = documents.value.map((doc) => ({
    ...doc,
    rowKey: String(doc.id),
    isPending: false,
  }))
  return [...pending, ...saved]
})

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
  editingKb.value = null
  form.value = { name: '', description: '' }
  formVisible.value = true
}

useOpenCreateFromQuery(openCreate)

function openEdit(row: KnowledgeBaseVO) {
  editingKb.value = row
  form.value = { name: row.name, description: row.description || '' }
  formVisible.value = true
}

async function submitForm() {
  if (!form.value.name.trim()) return false
  saving.value = true
  try {
    const name = form.value.name.trim()
    const description = form.value.description.trim() || undefined
    if (editingKb.value) {
      const { data } = await updateKnowledgeBase(editingKb.value.id, {
        name,
        description,
        icon: editingKb.value.icon,
        embeddingModelId: editingKb.value.embeddingModelId,
        rerankModelId: editingKb.value.rerankModelId,
      })
      formVisible.value = false
      MessagePlugin.success('知识库已更新')
      if (activeKb.value?.id === editingKb.value.id && data.data) {
        activeKb.value = data.data
      }
      editingKb.value = null
    } else {
      await createKnowledgeBase({ name, description })
      formVisible.value = false
      MessagePlugin.success('知识库已创建，本空间成员均可使用')
    }
    await load()
  } finally {
    saving.value = false
  }
}

const PROCESSING_STATUSES = new Set(['UPLOADING', 'QUEUED', 'PARSING', 'OCR', 'CHUNKING', 'EMBEDDING', 'INDEXING'])

let documentPollTimer: ReturnType<typeof setInterval> | null = null

function hasProcessingDocuments() {
  return Object.keys(pendingUploads.value).length > 0
    || documents.value.some((doc) => PROCESSING_STATUSES.has(doc.status))
}

function stopDocumentPolling() {
  if (documentPollTimer) {
    clearInterval(documentPollTimer)
    documentPollTimer = null
  }
}

function syncDocumentPolling() {
  if (!detailVisible.value) {
    stopDocumentPolling()
    return
  }
  if (!hasProcessingDocuments()) {
    stopDocumentPolling()
    return
  }
  if (documentPollTimer) {
    return
  }
  documentPollTimer = setInterval(async () => {
    await refreshDocuments()
    if (!hasProcessingDocuments()) {
      stopDocumentPolling()
      await load()
    }
  }, 2000)
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    UPLOADING: '上传中',
    QUEUED: '排队处理中',
    PARSING: '解析中',
    OCR: 'OCR 识别中',
    CHUNKING: '分块中',
    EMBEDDING: '向量化',
    INDEXING: '索引中',
    READY: '就绪',
    FAILED: '失败',
  }
  return labels[status] || status
}

function statusTheme(status: string) {
  if (status === 'READY') return 'success'
  if (status === 'FAILED') return 'danger'
  if (PROCESSING_STATUSES.has(status)) return 'warning'
  return 'default'
}

const STATUS_PROGRESS_FALLBACK: Record<string, number> = {
  UPLOADING: 5,
  QUEUED: 10,
  PARSING: 20,
  OCR: 45,
  CHUNKING: 60,
  EMBEDDING: 75,
  INDEXING: 90,
  READY: 100,
  FAILED: 0,
}

function rowDisplayProgress(row: DocumentRow) {
  if (row.isPending) {
    return Math.max(1, Math.min(8, Math.round((row.uploadProgress ?? 0) * 0.08)))
  }
  if (row.status === 'READY') return 100
  return row.progress ?? STATUS_PROGRESS_FALLBACK[row.status] ?? 0
}

async function refreshDocuments() {
  if (!activeKb.value) return
  const { data } = await listKnowledgeDocuments(activeKb.value.id)
  documents.value = data.data || []
  syncDocumentPolling()
}

async function openDetail(row: KnowledgeBaseVO) {
  activeKb.value = row
  detailVisible.value = true
  ragQuery.value = ''
  searchHits.value = []
  testAnswer.value = ''
  await refreshDocuments()
  syncDocumentPolling()
}

async function runRagTest() {
  if (!activeKb.value || !ragQuery.value.trim()) return
  searching.value = true
  testAnswer.value = ''
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

async function runTestAnswer() {
  if (!activeKb.value || !ragQuery.value.trim()) return
  answering.value = true
  try {
    const { data } = await testKnowledgeAnswer(activeKb.value.id, ragQuery.value.trim(), ragTopK.value)
    testAnswer.value = data.data?.answer || ''
    searchHits.value = data.data?.citations || []
    if (!testAnswer.value) {
      MessagePlugin.info('未生成回答')
    }
  } catch {
    MessagePlugin.error('生成回答失败')
  } finally {
    answering.value = false
  }
}

function removeDocument(doc: KnowledgeDocumentVO) {
  void confirmResourceDelete({
    header: '确认删除',
    body: `确定删除文档「${doc.fileName}」吗？分块与索引将一并移除。`,
    resourceLabel: '文档',
    onDelete: async () => {
      deletingDocId.value = doc.id
      try {
        await deleteKnowledgeDocument(doc.id)
      } finally {
        deletingDocId.value = null
      }
    },
    onSuccess: async () => {
      MessagePlugin.success('文档已删除')
      if (activeDoc.value?.id === doc.id) {
        chunkVisible.value = false
        activeDoc.value = null
      }
      await refreshDocuments()
      await load()
    },
  })
}

async function retryDocument(documentId: number) {
  retryingId.value = documentId
  try {
    await retryKnowledgeDocument(documentId)
    MessagePlugin.success('已重新提交处理')
    await refreshDocuments()
    syncDocumentPolling()
  } catch {
    MessagePlugin.error('重试失败')
    await refreshDocuments()
  } finally {
    retryingId.value = null
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
  const files = Array.from(input.files || [])
  if (!files.length || !activeKb.value) return
  input.value = ''

  const kbId = activeKb.value.id
  let successCount = 0
  let failCount = 0
  uploadingCount.value += files.length

  const uploadOne = async (file: File) => {
    const key = `pending-${crypto.randomUUID()}`
    pendingUploads.value = {
      ...pendingUploads.value,
      [key]: { fileName: file.name, progress: 0 },
    }
    try {
      await uploadKnowledgeDocument(kbId, file, (percent) => {
        const current = pendingUploads.value[key]
        if (current) {
          pendingUploads.value = {
            ...pendingUploads.value,
            [key]: { ...current, progress: percent },
          }
        }
      })
      successCount++
    } catch {
      failCount++
      MessagePlugin.error(`「${file.name}」上传失败`)
    } finally {
      const next = { ...pendingUploads.value }
      delete next[key]
      pendingUploads.value = next
      uploadingCount.value--
    }
  }

  const queue = [...files]
  const workers = Array.from({ length: Math.min(UPLOAD_CONCURRENCY, files.length) }, async () => {
    while (queue.length) {
      const file = queue.shift()
      if (!file) break
      await uploadOne(file)
    }
  })
  await Promise.all(workers)

  await refreshDocuments()
  await load()
  syncDocumentPolling()

  if (successCount > 0) {
    const message = failCount > 0
      ? `已成功提交 ${successCount} 个文档，${failCount} 个失败`
      : `已成功提交 ${successCount} 个文档，正在后台处理`
    MessagePlugin.success(message)
  }
}

async function submitUrlImport() {
  if (!activeKb.value || !importUrl.value.trim()) {
    MessagePlugin.warning('请输入 URL')
    return false
  }
  urlImporting.value = true
  try {
    await importKnowledgeUrl(activeKb.value.id, importUrl.value.trim(), importCron.value.trim() || undefined)
    MessagePlugin.success('已开始从 URL 导入')
    urlVisible.value = false
    importUrl.value = ''
    importCron.value = ''
    await refreshDocuments()
    await load()
    syncDocumentPolling()
  } finally {
    urlImporting.value = false
  }
  return true
}

function remove(item: KnowledgeBaseVO) {
  void confirmResourceDelete({
    header: '确认删除',
    body: `确定删除知识库「${item.name}」吗？关联文档与索引将一并移除。`,
    resourceLabel: '知识库',
    onDelete: async () => {
      await deleteKnowledgeBase(item.id)
    },
    onSuccess: async () => {
      MessagePlugin.success('已删除')
      await load()
    },
  })
}

load()
useReloadOnWorkspaceChange(load)

watch(detailVisible, (visible) => {
  if (!visible) {
    stopDocumentPolling()
  } else {
    syncDocumentPolling()
  }
})

onUnmounted(stopDocumentPolling)
</script>

<style scoped>
.resource-create-hint {
  margin: 0 0 16px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}
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
.doc-error {
  color: var(--td-error-color);
  font-size: 12px;
}

.doc-status-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 140px;
}

.doc-status-label {
  font-size: 12px;
  color: var(--box-muted);
  line-height: 1.4;
}

.rag-answer {
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f8fa;
}

.rag-answer h4 {
  margin: 0 0 8px;
  font-size: 14px;
}

.rag-answer p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}

.rag-test__table { margin-top: 12px; }
</style>
