<template>
  <div class="settings-page">
    <h1 class="settings-page__title">API 密钥</h1>
    <p class="settings-page__desc">
      用于调用已发布智能体的开放 API。请求时在 Header 中携带
      <code>Authorization: Bearer ax_live_…</code>
    </p>

    <div class="settings-card">
      <div class="toolbar">
        <t-button v-if="can(PermissionCodes.API_KEY_MANAGE)" theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          创建密钥
        </t-button>
      </div>

      <t-table
        row-key="id"
        :data="apiKeys"
        :columns="columns"
        :loading="loading"
        bordered
        stripe
        hover
      >
        <template #status="{ row }">
          <t-tag :theme="row.status === 1 ? 'success' : 'default'" variant="light" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </t-tag>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-button
              v-if="row.status === 1"
              variant="text"
              theme="warning"
              @click="toggleKey(row.id, false)"
            >
              禁用
            </t-button>
            <t-button v-else variant="text" theme="primary" @click="toggleKey(row.id, true)">启用</t-button>
            <t-button variant="text" @click="rotateKey(row.id)">轮换</t-button>
            <t-button variant="text" theme="danger" @click="removeKey(row.id)">删除</t-button>
          </t-space>
        </template>
        <template #empty>
          <t-empty description="暂无 API 密钥，创建后可用于调用已发布智能体" />
        </template>
      </t-table>
    </div>

    <t-dialog
      v-model:visible="createVisible"
      header="创建 API 密钥"
      :confirm-btn="{ content: '创建', loading: creating }"
      @confirm="submitCreate"
    >
      <t-form label-align="top">
        <t-form-item label="名称">
          <t-input v-model="createForm.name" placeholder="例如：生产环境 / 测试集成" maxlength="128" />
        </t-form-item>
      </t-form>
    </t-dialog>

    <t-dialog
      v-model:visible="secretVisible"
      header="请妥善保存密钥"
      :footer="false"
      width="520px"
    >
      <t-alert theme="warning" message="密钥仅显示一次，关闭后无法再次查看完整内容。" />
      <div class="secret-box">
        <code>{{ createdSecret }}</code>
      </div>
      <t-space style="margin-top: 16px">
        <t-button theme="primary" @click="copySecret">复制密钥</t-button>
        <t-button variant="outline" @click="secretVisible = false">我已保存</t-button>
      </t-space>
    </t-dialog>

    <section class="settings-card">
      <h2 class="settings-card__heading">开发者文档</h2>
      <p class="settings-card__hint">
        OpenAPI：<a href="/v3/api-docs" target="_blank" rel="noopener">/v3/api-docs</a>
      </p>
      <pre class="code-sample">curl -X POST "{{ apiBase }}/published/agents/{agentId}/chat" \
  -H "Authorization: Bearer ax_live_xxx" \
  -H "Content-Type: application/json" \
  -d '{"message":"你好"}'</pre>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { DialogPlugin, MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { usePermission } from '@/composables/usePermission'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import { PermissionCodes } from '@/constants/permissions'
import {
  createApiKey,
  deleteApiKey,
  disableApiKey,
  enableApiKey,
  listApiKeys,
  rotateApiKey,
  type ApiKeyVO,
} from '@/api/apiKey'

const { can } = usePermission()

const loading = ref(false)
const creating = ref(false)
const apiKeys = ref<ApiKeyVO[]>([])
const createVisible = ref(false)
const secretVisible = ref(false)
const createdSecret = ref('')
const createForm = reactive({ name: '' })
const apiBase = `${window.location.origin}/api/v1`

const columns: PrimaryTableCol<ApiKeyVO>[] = [
  { colKey: 'name', title: '名称', ellipsis: true },
  { colKey: 'keyPrefix', title: '前缀', width: 140 },
  { colKey: 'status', title: '状态', width: 90 },
  { colKey: 'lastUsedAt', title: '最近使用', width: 180 },
  { colKey: 'createdAt', title: '创建时间', width: 180 },
  { colKey: 'op', title: '操作', width: 220 },
]

async function loadKeys() {
  loading.value = true
  try {
    const { data } = await listApiKeys()
    apiKeys.value = data.data || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载 API 密钥失败'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.name = ''
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.name.trim()) {
    MessagePlugin.warning('请输入密钥名称')
    return
  }
  creating.value = true
  try {
    const { data } = await createApiKey({ name: createForm.name.trim() })
    createdSecret.value = data.data?.apiKey || ''
    createVisible.value = false
    secretVisible.value = true
    await loadKeys()
    MessagePlugin.success('API 密钥已创建')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '创建失败'))
  } finally {
    creating.value = false
  }
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(createdSecret.value)
    MessagePlugin.success('已复制到剪贴板')
  } catch {
    MessagePlugin.warning('复制失败，请手动复制')
  }
}

async function toggleKey(id: number, enable: boolean) {
  try {
    if (enable) {
      await enableApiKey(id)
    } else {
      await disableApiKey(id)
    }
    await loadKeys()
    MessagePlugin.success(enable ? '已启用' : '已禁用')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '操作失败'))
  }
}

async function rotateKey(id: number) {
  try {
    const { data } = await rotateApiKey(id)
    createdSecret.value = data.data?.apiKey || ''
    secretVisible.value = true
    await loadKeys()
    MessagePlugin.success('密钥已轮换，请更新集成配置')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '轮换失败'))
  }
}

function removeKey(id: number) {
  const dialog = DialogPlugin.confirm({
    header: '确认删除',
    body: '删除后使用该密钥的集成将立即失效，此操作不可恢复。',
    confirmBtn: '删除',
    cancelBtn: '取消',
    theme: 'warning',
    onConfirm: async () => {
      try {
        await deleteApiKey(id)
        dialog.hide()
        await loadKeys()
        MessagePlugin.success('已删除')
      } catch (error) {
        MessagePlugin.error(extractApiError(error, '删除失败'))
      }
    },
  })
}

onMounted(loadKeys)
useReloadOnWorkspaceChange(loadKeys)
</script>

<style scoped>
.settings-page__desc code {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--td-bg-color-secondarycontainer, var(--box-hover));
  font-size: 13px;
}

.toolbar {
  margin-bottom: 16px;
}

.secret-box {
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: var(--td-bg-color-container);
  border: 1px solid var(--box-border);
  word-break: break-all;
}

.secret-box code {
  font-size: 13px;
}

.settings-card__heading {
  margin: 0 0 12px;
  font: var(--td-font-title-small);
}

.settings-card__hint {
  margin: 0 0 12px;
  color: var(--box-muted);
  font-size: 13px;
}

.code-sample {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: var(--td-bg-color-secondarycontainer);
  font-size: 12px;
  overflow-x: auto;
}
</style>
