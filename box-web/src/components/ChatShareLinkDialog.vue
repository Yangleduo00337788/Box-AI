<template>
  <t-dialog
    v-model:visible="visible"
    attach="body"
    header="分享链接已创建"
    width="480px"
    :footer="false"
  >
    <p class="chat-share-link-dialog__hint">复制链接发送给他人查看（只读）。</p>
    <t-input :value="shareUrl" readonly>
      <template #suffix>
        <t-button variant="text" size="small" @click="copyLink">复制</t-button>
      </template>
    </t-input>
  </t-dialog>
</template>

<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next'

const props = defineProps<{
  shareUrl: string
}>()

const visible = defineModel<boolean>('visible', { default: false })

async function copyLink() {
  if (!props.shareUrl) return
  try {
    await navigator.clipboard.writeText(props.shareUrl)
    MessagePlugin.success('链接已复制')
  } catch {
    MessagePlugin.error('复制失败')
  }
}
</script>

<style scoped>
.chat-share-link-dialog__hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--td-text-color-secondary);
}
</style>
