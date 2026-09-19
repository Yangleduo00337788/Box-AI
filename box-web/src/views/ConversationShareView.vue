<template>
  <div class="conversation-share-page">
    <t-loading :loading="loading" size="small">
      <article v-if="share" class="conversation-share-card">
        <header class="conversation-share-card__header">
          <img
            class="conversation-share-card__mascot"
            src="/share/box-mascot.gif"
            alt=""
            width="120"
            height="140"
            loading="lazy"
            decoding="async"
          />
          <h1>{{ share.title }}</h1>
          <p class="conversation-share-card__meta">
            {{ formatDate(share.sharedAt) }} · 内容由 AI 生成，不能完全保证真实
          </p>
        </header>

        <box-chat-message-list
          class="conversation-share-card__messages"
          :items="listItems"
          readonly
          :auto-scroll="false"
          :show-scroll-button="false"
        />

        <footer class="conversation-share-card__footer">
          <p class="conversation-share-card__end">已经到底</p>
          <button type="button" class="conversation-share-card__report" @click="reportVisible = true">
            内容有问题，点击进行举报
          </button>
        </footer>
      </article>
      <t-empty v-else-if="!loading" description="分享不存在或已失效" />
    </t-loading>

    <t-dialog
      v-model:visible="reportVisible"
      attach="body"
      header="举报内容"
      width="440px"
      :confirm-btn="{ content: '提交' }"
      :on-confirm="onReport"
    >
      <t-textarea
        v-model="reportText"
        placeholder="请描述问题内容"
        :autosize="{ minRows: 3, maxRows: 6 }"
        :maxlength="300"
      />
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import BoxChatMessageList from '@/components/BoxChatMessageList.vue'
import { getPublicConversationShare, type SharedConversationVO } from '@/api/conversation'
import { extractApiError } from '@/api/apiError'
import { buildSharedListItems } from '@/utils/boxChatListItems'

const route = useRoute()
const loading = ref(true)
const share = ref<SharedConversationVO | null>(null)
const reportVisible = ref(false)
const reportText = ref('')

const listItems = computed(() => (share.value ? buildSharedListItems(share.value) : []))

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

async function loadShare() {
  const token = String(route.params.token || '')
  if (!token) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const { data } = await getPublicConversationShare(token)
    share.value = data.data || null
  } catch (error) {
    share.value = null
    MessagePlugin.error(extractApiError(error, '加载分享失败'))
  } finally {
    loading.value = false
  }
}

function onReport() {
  if (!reportText.value.trim()) {
    MessagePlugin.warning('请填写举报说明')
    return false
  }
  MessagePlugin.success('已收到举报，感谢反馈')
  reportVisible.value = false
  reportText.value = ''
  return true
}

onMounted(() => {
  void loadShare()
})
</script>

<style scoped>
.conversation-share-page {
  min-height: 100vh;
  background: #f0f1f3;
  padding: 40px 16px 48px;
  box-sizing: border-box;
}

.conversation-share-card {
  position: relative;
  max-width: 720px;
  margin: 0 auto;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
  padding: 32px 32px 28px;
  box-sizing: border-box;
  overflow: visible;
}

.conversation-share-card__header {
  position: relative;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--td-component-border);
}

.conversation-share-card__mascot {
  position: absolute;
  right: -28px;
  bottom: 0;
  width: 132px;
  height: auto;
  max-height: 156px;
  object-fit: contain;
  object-position: bottom center;
  pointer-events: none;
  user-select: none;
  background: transparent;
  filter: drop-shadow(0 8px 18px rgba(15, 23, 42, 0.14));
  z-index: 2;
}

.conversation-share-card__header h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.35;
  font-weight: 600;
  color: var(--td-text-color-primary);
  padding-right: 88px;
}

.conversation-share-card__meta {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}

.conversation-share-card__messages {
  margin-top: 24px;
  min-height: 120px;
  max-height: none;
}

.conversation-share-card__messages :deep(.t-chat__list) {
  max-height: none;
  overflow: visible;
  gap: 28px;
}

.conversation-share-card__footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--td-component-border);
  text-align: center;
}

.conversation-share-card__end {
  margin: 0;
  font-size: 13px;
  color: var(--td-text-color-placeholder);
}

.conversation-share-card__report {
  margin-top: 12px;
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  color: var(--td-text-color-secondary);
  text-decoration: underline;
  cursor: pointer;
}
</style>
