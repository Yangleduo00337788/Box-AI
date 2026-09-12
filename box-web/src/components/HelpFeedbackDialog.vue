<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="帮助与反馈"
    :footer="false"
    width="480px"
  >
    <t-loading :loading="loading" size="small">
      <div v-if="content" class="help-dialog">
        <section class="help-dialog__section">
          <h3 class="help-dialog__title">快捷操作</h3>
          <ul class="help-dialog__list">
            <li><kbd>Ctrl</kbd> + <kbd>K</kbd> 打开全局搜索</li>
            <li><kbd>Enter</kbd> 发送消息（可在通用设置中关闭）</li>
          </ul>
        </section>

        <section class="help-dialog__section">
          <h3 class="help-dialog__title">常用入口</h3>
          <div class="help-dialog__links">
            <t-button variant="outline" @click="go('/chat')">开始对话</t-button>
            <t-button variant="outline" @click="go('/models')">配置模型</t-button>
            <t-button variant="outline" @click="go('/settings/profile')">账号设置</t-button>
          </div>
        </section>

        <section class="help-dialog__section">
          <h3 class="help-dialog__title">反馈</h3>
          <p class="help-dialog__desc">遇到问题或有产品建议，欢迎通过邮件联系我们。</p>
          <t-input :value="content.supportEmail" readonly>
            <template #suffix>
              <t-button variant="text" size="small" @click="copyEmail">复制</t-button>
            </template>
          </t-input>
        </section>
      </div>
    </t-loading>
  </t-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { fetchSystemContent, type SystemContentVO } from '@/api/system'

const visible = defineModel<boolean>('visible', { default: false })
const router = useRouter()
const loading = ref(false)
const content = ref<SystemContentVO | null>(null)

watch(visible, async (open) => {
  if (!open || content.value) return
  loading.value = true
  try {
    const { data } = await fetchSystemContent()
    content.value = data.data
  } finally {
    loading.value = false
  }
})

function go(path: string) {
  visible.value = false
  router.push(path)
}

async function copyEmail() {
  if (!content.value?.supportEmail) return
  await navigator.clipboard.writeText(content.value.supportEmail)
  MessagePlugin.success('已复制邮箱')
}
</script>

<style scoped>
.help-dialog__section {
  margin-bottom: 20px;
}

.help-dialog__section:last-child {
  margin-bottom: 0;
}

.help-dialog__title {
  margin: 0 0 8px;
  font: var(--td-font-title-small);
  color: var(--box-ink);
}

.help-dialog__desc {
  margin: 0 0 10px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
}

.help-dialog__list {
  margin: 0;
  padding-left: 18px;
  font: var(--td-font-body-small);
  color: var(--box-muted);
  line-height: 1.8;
}

.help-dialog__links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

kbd {
  display: inline-block;
  min-width: 1.4em;
  padding: 1px 6px;
  border: 1px solid var(--box-border);
  border-radius: 4px;
  background: #f5f6f7;
  font-size: 12px;
  text-align: center;
}
</style>
