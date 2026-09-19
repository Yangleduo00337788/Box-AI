<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    placement="center"
    header="反馈"
    width="480px"
    :confirm-btn="{ content: '提交', loading: submitting }"
    :on-confirm="onConfirm"
  >
    <p class="chat-feedback-dialog__hint">请描述这条回复的问题，帮助我们改进。</p>
    <t-textarea
      v-model="content"
      placeholder="例如：事实错误、未理解问题、格式混乱…"
      :maxlength="500"
      :autosize="{ minRows: 4, maxRows: 8 }"
    />
  </t-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { submitMessageFeedback } from '@/api/conversation'
import { extractApiError } from '@/api/apiError'

const props = defineProps<{
  conversationId: number
  messageId: number
}>()

const visible = defineModel<boolean>('visible', { default: false })
const emit = defineEmits<{ submitted: [] }>()

const content = ref('')
const submitting = ref(false)

watch(
  () => visible.value,
  (open) => {
    if (open) {
      content.value = ''
    }
  },
)

async function onConfirm() {
  const text = content.value.trim()
  if (!text) {
    MessagePlugin.warning('请填写反馈内容')
    return false
  }
  submitting.value = true
  try {
    await submitMessageFeedback(props.conversationId, props.messageId, {
      rating: 'bad',
      content: text,
    })
    MessagePlugin.success('感谢反馈')
    emit('submitted')
    return true
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '提交失败'))
    return false
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.chat-feedback-dialog__hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}
</style>
