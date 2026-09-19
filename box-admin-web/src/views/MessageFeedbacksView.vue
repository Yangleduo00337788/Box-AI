<template>
  <div class="feedback-page admin-page">
    <page-header
      title="消息反馈"
      desc="汇总 C 端点赞与点踩：点踩可回复并通知用户；点赞用于观察优质回复与 Agent 表现，无需回复。"
    />

    <t-card :bordered="false" class="admin-card">
      <div class="admin-toolbar">
        <t-select
          v-model="filters.rating"
          clearable
          placeholder="反馈类型"
          style="width: 140px"
          :options="ratingOptions"
        />
        <t-select
          v-model="filters.status"
          clearable
          placeholder="处理状态"
          style="width: 160px"
          :options="statusOptions"
        />
        <t-button theme="primary" :loading="loading" @click="onSearch">查询</t-button>
      </div>
      <t-table
        row-key="id"
        :data="rows"
        :columns="columns"
        :loading="loading"
        hover
        :pagination="pagination"
        @page-change="onPageChange"
        @row-click="({ row }) => openDetail(row as AdminMessageFeedbackVO)"
      >
        <template #empty>
          <t-empty description="暂无反馈" />
        </template>
      </t-table>
    </t-card>

    <t-drawer v-model:visible="detailVisible" header="反馈详情" size="560px" :footer="false">
      <t-loading :loading="detailLoading" size="small">
        <t-descriptions v-if="detail" :column="1" bordered>
          <t-descriptions-item label="时间">{{ formatDateTime(detail.createdAt) }}</t-descriptions-item>
          <t-descriptions-item label="类型">
            <t-tag :theme="ratingTheme(detail.rating)" variant="light">{{ ratingLabel(detail.rating) }}</t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="状态">
            <t-tag :theme="statusTheme(detail)" variant="light">{{ statusLabel(detail) }}</t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="用户">{{ userLabel(detail) }}</t-descriptions-item>
          <t-descriptions-item label="工作空间">#{{ detail.workspaceId }}</t-descriptions-item>
          <t-descriptions-item label="会话">#{{ detail.conversationId }}</t-descriptions-item>
          <t-descriptions-item label="消息">#{{ detail.messageId }}</t-descriptions-item>
          <t-descriptions-item v-if="detail.rating === 'bad'" label="用户说明">
            <pre class="feedback-detail__pre">{{ detail.content || '（无说明）' }}</pre>
          </t-descriptions-item>
          <t-descriptions-item label="关联 AI 回复">
            <pre class="feedback-detail__pre">{{ detail.messageContent || '（无内容）' }}</pre>
          </t-descriptions-item>
          <t-descriptions-item v-if="detail.adminReply" label="管理员回复">
            <pre class="feedback-detail__pre">{{ detail.adminReply }}</pre>
            <p v-if="detail.adminRepliedAt" class="feedback-detail__meta">
              {{ formatDateTime(detail.adminRepliedAt) }}
            </p>
          </t-descriptions-item>
        </t-descriptions>
        <div v-if="detail && detail.rating === 'bad'" class="feedback-detail__actions">
          <t-button
            v-if="detail.status === 'PENDING'"
            theme="primary"
            @click="openReplyFromDetail('create')"
          >
            回复用户
          </t-button>
          <t-button
            v-else-if="detail.status === 'REPLIED'"
            variant="outline"
            @click="openReplyFromDetail('edit')"
          >
            修改回复
          </t-button>
        </div>
      </t-loading>
    </t-drawer>

    <t-dialog
      v-model:visible="replyVisible"
      :header="replyMode === 'edit' ? '修改回复' : '回复用户'"
      width="520px"
      :confirm-btn="{ content: replyMode === 'edit' ? '更新并通知' : '发送站内信', loading: replying }"
      :on-confirm="onConfirmReply"
    >
      <template v-if="activeRow">
        <p class="feedback-reply__meta">
          用户：{{ userLabel(activeRow) }} · 会话 #{{ activeRow.conversationId }}
        </p>
        <div v-if="activeRow.rating === 'bad'" class="feedback-reply__block">
          <div class="feedback-reply__label">用户反馈</div>
          <p class="feedback-reply__text">{{ activeRow.content || '（无说明）' }}</p>
        </div>
        <div v-if="activeRow.messageExcerpt" class="feedback-reply__block">
          <div class="feedback-reply__label">关联 AI 回复</div>
          <p class="feedback-reply__text feedback-reply__text--muted">{{ activeRow.messageExcerpt }}</p>
        </div>
        <t-textarea
          v-model="replyText"
          placeholder="输入回复内容，将发送至用户站内信"
          :maxlength="2000"
          :autosize="{ minRows: 4, maxRows: 8 }"
        />
      </template>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { Button, Tag } from 'tdesign-vue-next'
import type { PageInfo, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import {
  getMessageFeedbackDetail,
  listMessageFeedbacks,
  replyMessageFeedback,
  type AdminMessageFeedbackDetailVO,
  type AdminMessageFeedbackVO,
} from '@/api/messageFeedback'
import { formatDateTime } from '@/utils/datetime'
import { MessagePlugin } from 'tdesign-vue-next'

const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<AdminMessageFeedbackDetailVO | null>(null)
const replying = ref(false)
const rows = ref<AdminMessageFeedbackVO[]>([])
const replyVisible = ref(false)
const replyMode = ref<'create' | 'edit'>('create')
const activeRow = ref<AdminMessageFeedbackVO | null>(null)
const replyText = ref('')
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const filters = reactive({
  rating: '' as string | undefined,
  status: '' as string | undefined,
})

const ratingOptions = [
  { label: '点踩', value: 'bad' },
  { label: '点赞', value: 'good' },
]

const statusOptions = [
  { label: '待处理', value: 'PENDING' },
  { label: '已回复', value: 'REPLIED' },
  { label: '已记录（赞）', value: 'RECORDED' },
]

function userLabel(row: AdminMessageFeedbackVO) {
  if (row.userNickname && row.userEmail) return `${row.userNickname}（${row.userEmail}）`
  return row.userNickname || row.userEmail || `#${row.userId}`
}

function ratingTheme(rating: string) {
  return rating === 'good' ? 'success' : 'danger'
}

function ratingLabel(rating: string) {
  return rating === 'good' ? '点赞' : '点踩'
}

function statusTheme(row: AdminMessageFeedbackVO) {
  if (row.rating === 'good') return 'default'
  if (row.status === 'REPLIED') return 'success'
  if (row.status === 'PENDING') return 'warning'
  return 'default'
}

function statusLabel(row: AdminMessageFeedbackVO) {
  if (row.rating === 'good') return '已记录'
  if (row.status === 'REPLIED') return '已回复'
  if (row.status === 'PENDING') return '待处理'
  return row.status || '-'
}

function renderActions(row: AdminMessageFeedbackVO) {
  const buttons = [
    h(
      Button,
      {
        size: 'small',
        variant: 'text',
        theme: 'default',
        onClick: (e: Event) => {
          e.stopPropagation()
          void openDetail(row)
        },
      },
      () => '详情',
    ),
  ]
  if (row.rating === 'bad' && row.status === 'PENDING') {
    buttons.push(
      h(
        Button,
        {
          size: 'small',
          variant: 'text',
          theme: 'primary',
          onClick: (e: Event) => {
            e.stopPropagation()
            openReply(row, 'create')
          },
        },
        () => '回复',
      ),
    )
  } else if (row.rating === 'bad' && row.status === 'REPLIED') {
    buttons.push(
      h(
        Button,
        {
          size: 'small',
          variant: 'text',
          theme: 'primary',
          onClick: (e: Event) => {
            e.stopPropagation()
            openReply(row, 'edit')
          },
        },
        () => '修改',
      ),
    )
  }
  return h('div', { class: 'feedback-page__actions' }, buttons)
}

const columns: PrimaryTableCol<AdminMessageFeedbackVO>[] = [
  {
    colKey: 'createdAt',
    title: '时间',
    width: 170,
    cell: (_, { row }) => formatDateTime(row.createdAt),
  },
  {
    colKey: 'rating',
    title: '类型',
    width: 80,
    cell: (_, { row }) =>
      h(Tag, { theme: ratingTheme(row.rating), variant: 'light' }, () => ratingLabel(row.rating)),
  },
  {
    colKey: 'user',
    title: '用户',
    minWidth: 160,
    cell: (_, { row }) => userLabel(row),
  },
  {
    colKey: 'content',
    title: '说明',
    ellipsis: true,
    minWidth: 160,
    cell: (_, { row }) => (row.rating === 'good' ? '—' : row.content || '（无说明）'),
  },
  { colKey: 'messageExcerpt', title: '关联 AI 回复', ellipsis: true, minWidth: 200 },
  {
    colKey: 'status',
    title: '状态',
    width: 100,
    cell: (_, { row }) =>
      h(Tag, { theme: statusTheme(row), variant: 'light' }, () => statusLabel(row)),
  },
  {
    colKey: 'actions',
    title: '操作',
    width: 132,
    fixed: 'right',
    cell: (_, { row }) => renderActions(row),
  },
]

async function openDetail(row: AdminMessageFeedbackVO) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const { data } = await getMessageFeedbackDetail(row.id)
    detail.value = data.data || null
  } finally {
    detailLoading.value = false
  }
}

function openReplyFromDetail(mode: 'create' | 'edit') {
  if (!detail.value) return
  detailVisible.value = false
  openReply(detail.value, mode)
}

async function load() {
  loading.value = true
  try {
    const { data } = await listMessageFeedbacks({
      rating: filters.rating || undefined,
      status: filters.status || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    rows.value = data.data?.records || []
    pagination.total = data.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  void load()
}

function onPageChange(pageInfo: PageInfo) {
  pagination.current = pageInfo.current
  pagination.pageSize = pageInfo.pageSize
  void load()
}

function openReply(row: AdminMessageFeedbackVO, mode: 'create' | 'edit') {
  activeRow.value = row
  replyMode.value = mode
  replyText.value = mode === 'edit' ? row.adminReply || '' : ''
  replyVisible.value = true
}

async function onConfirmReply() {
  if (!activeRow.value) return false
  const text = replyText.value.trim()
  if (!text) {
    MessagePlugin.warning('请填写回复内容')
    return false
  }
  replying.value = true
  try {
    await replyMessageFeedback(activeRow.value.id, text)
    MessagePlugin.success(replyMode.value === 'edit' ? '已更新，用户将收到新站内信' : '已回复，用户将收到站内信')
    replyVisible.value = false
    await load()
    return true
  } catch {
    return false
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.feedback-reply__meta {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.feedback-reply__block {
  margin-bottom: 12px;
}

.feedback-reply__label {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
  margin-bottom: 4px;
}

.feedback-reply__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--td-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.feedback-reply__text--muted {
  color: var(--td-text-color-secondary);
}

.feedback-page__muted {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.feedback-page__actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.feedback-detail__pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.55;
  max-height: 280px;
  overflow: auto;
}

.feedback-detail__meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.feedback-detail__actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
