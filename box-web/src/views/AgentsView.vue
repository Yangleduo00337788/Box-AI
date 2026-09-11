<template>
  <div>
    <page-header title="智能体" desc="创建和管理你的 AI 智能体，配置模型、知识库与工具">
      <template #actions>
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          创建智能体
        </t-button>
      </template>
    </page-header>

    <t-loading :loading="loading" size="small">
      <div v-if="agents.length" class="agent-grid">
        <article
          v-for="item in agents"
          :key="item.id"
          class="agent-card"
          @click="openBuilder(item)"
        >
          <div class="agent-card__head">
            <t-avatar size="48px" shape="round" class="agent-card__avatar">
              {{ item.name.slice(0, 1).toUpperCase() }}
            </t-avatar>
            <t-tag
              :theme="item.status === 'PUBLISHED' ? 'success' : 'default'"
              variant="light"
              size="small"
            >
              {{ statusLabel(item.status) }}
            </t-tag>
          </div>
          <h3 class="agent-card__title">{{ item.name }}</h3>
          <p class="agent-card__desc">{{ item.description || '暂无描述' }}</p>
          <div class="agent-card__meta">
            <span>{{ displayModelName(item) }}</span>
            <span>v{{ item.draftVersion ?? 1 }}</span>
          </div>
          <div class="agent-card__footer">
            <span class="agent-card__time">{{ formatTime(item.updatedAt) }}</span>
            <t-button
              theme="danger"
              variant="text"
              size="small"
              @click.stop="removeAgent(item)"
            >
              删除
            </t-button>
          </div>
        </article>
      </div>

      <div v-else class="agent-empty">
        <t-empty description="还没有智能体">
          <template #action>
            <t-button theme="primary" @click="openCreate">创建第一个智能体</t-button>
          </template>
        </t-empty>
      </div>
    </t-loading>
  </div>

  <t-dialog
    v-model:visible="dialogVisible"
    :header="'创建智能体'"
    :footer="false"
    width="520px"
  >
    <t-form :data="form" :rules="rules" label-align="top" @submit="submitForm">
      <t-form-item label="名称" name="name">
        <t-input v-model="form.name" placeholder="给你的智能体起个名字" maxlength="128" />
      </t-form-item>
      <t-form-item label="描述" name="description">
        <t-textarea
          v-model="form.description"
          placeholder="简要描述智能体的用途"
          :autosize="{ minRows: 3, maxRows: 6 }"
          maxlength="500"
        />
      </t-form-item>
      <t-form-item label="平台模型" name="platformModelId">
        <t-select
          v-model="form.platformModelId"
          :options="platformModelOptions"
          placeholder="请选择平台模型"
        />
      </t-form-item>
      <t-form-item>
        <t-space>
          <t-button theme="primary" type="submit" :loading="saving">
            创建
          </t-button>
          <t-button variant="outline" @click="dialogVisible = false">取消</t-button>
        </t-space>
      </t-form-item>
    </t-form>
  </t-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import type { FormProps } from 'tdesign-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import {
  createAgent,
  deleteAgent,
  listAgents,
  type AgentVO,
} from '@/api/agent'
import { listPlatformModels, type PlatformModelVO } from '@/api/platform'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const agents = ref<AgentVO[]>([])
const platformModels = ref<PlatformModelVO[]>([])

const form = reactive({
  name: '',
  description: '',
  platformModelId: undefined as number | undefined,
})

const rules: FormProps['rules'] = {
  name: [{ required: true, message: '请输入名称' }],
  platformModelId: [{ required: true, message: '请选择平台模型' }],
}

const platformModelOptions = computed(() =>
  platformModels.value
    .filter((item) => item.status === 1)
    .map((item) => ({
      label: item.providerName ? `${item.modelName}（${item.providerName}）` : item.modelName,
      value: item.id,
    })),
)

function displayModelName(item: AgentVO) {
  if (item.modelSource === 'BYOK') {
    return item.modelName ? `${item.modelName}（自带密钥）` : '自带密钥'
  }
  return item.platformModelName || item.modelName || '未配置模型'
}

function statusLabel(status: string) {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'ARCHIVED') return '已归档'
  return '草稿'
}

function formatTime(value: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadPlatformModels() {
  const { data } = await listPlatformModels()
  platformModels.value = data.data || []
}

async function loadAgents() {
  loading.value = true
  try {
    const { data } = await listAgents()
    agents.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openBuilder(item: AgentVO) {
  router.push(`/agents/${item.id}/builder`)
}

function openCreate() {
  form.name = ''
  form.description = ''
  form.platformModelId = platformModelOptions.value[0]?.value
  dialogVisible.value = true
}

const submitForm: FormProps['onSubmit'] = async ({ validateResult }) => {
  if (validateResult !== true) return
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      platformModelId: form.platformModelId,
    }
    const { data } = await createAgent(payload)
    MessagePlugin.success('智能体创建成功')
    dialogVisible.value = false
    if (data.data?.id) {
      router.push(`/agents/${data.data.id}/builder`)
    } else {
      await loadAgents()
    }
  } finally {
    saving.value = false
  }
}

function removeAgent(item: AgentVO) {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: `确定删除智能体「${item.name}」吗？此操作不可恢复。`,
    confirmBtn: '删除',
    cancelBtn: '取消',
    theme: 'warning',
    onConfirm: async () => {
      await deleteAgent(item.id)
      MessagePlugin.success('删除成功')
      dialog.hide()
      await loadAgents()
    },
  })
}

onMounted(async () => {
  await Promise.all([loadPlatformModels(), loadAgents()])
})
</script>

<style scoped>
.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.agent-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-md);
  background: var(--box-surface);
  box-shadow: var(--box-shadow-card);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.agent-card:hover {
  border-color: var(--td-gray-color-4);
  box-shadow: var(--box-shadow-soft);
  transform: translateY(-2px);
}

.agent-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.agent-card__avatar {
  background: var(--td-gray-color-2);
  color: var(--box-ink);
  font-weight: 600;
}

.agent-card__title {
  margin: 0;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.agent-card__desc {
  margin: 0;
  min-height: 40px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.agent-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px solid var(--box-border);
}

.agent-card__time {
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.agent-empty {
  padding: 48px 0;
  border: 1px solid var(--box-border);
  border-radius: var(--box-radius-lg);
  background: var(--box-surface);
}
</style>
