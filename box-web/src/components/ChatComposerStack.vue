<template>
  <div class="chat-composer-stack" :class="{ 'chat-composer-stack--solo': !showAgentRail }">
    <div class="chat-composer-stack__panel">
      <slot />
    </div>
    <div v-if="showAgentRail" class="chat-composer-stack__agent-rail">
      <slot name="agent" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 新建对话页展示 Agent/项目底栏；进行中对话可关闭 */
    showAgentRail?: boolean
  }>(),
  {
    showAgentRail: true,
  },
)
</script>

<style scoped>
.chat-composer-stack {
  width: 100%;
}

.chat-composer-stack__panel {
  position: relative;
  z-index: 2;
}

.chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender) {
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}

/* 白框：独立圆角卡片，略宽于下方灰条 */
.chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender__textarea) {
  padding: 16px 18px 14px;
  border: 1px solid var(--box-composer-border);
  border-radius: var(--box-composer-radius);
  background: var(--box-surface);
  box-shadow: var(--box-composer-shadow);
}

.chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender__textarea:hover),
.chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender__textarea--focus),
.chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender__textarea--focus:hover) {
  border-color: var(--box-composer-border);
  background: var(--box-surface);
  box-shadow: var(--box-composer-shadow);
}

.chat-composer-stack__panel :deep(.t-textarea__inner) {
  min-height: 72px;
  font-size: 15px;
  line-height: 1.6;
}

.chat-composer-stack--solo .chat-composer-stack__panel :deep(.box-chat-sender) {
  margin-bottom: 0;
}

.chat-composer-stack--solo {
  margin-bottom: 16px;
}

/* 灰条：左右内缩、上移叠在白框下，只露出底部一条 */
.chat-composer-stack__agent-rail {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  margin: -20px 14px 0;
  padding: 26px 12px 8px;
  border: 1px solid var(--box-border);
  border-radius: var(--box-composer-radius);
  background: var(--td-bg-color-secondarycontainer, rgb(247, 247, 245));
}

@media (max-width: 640px) {
  .chat-composer-stack__panel :deep(.box-chat-sender__input .t-chat-sender__textarea) {
    padding: 14px 14px 12px;
  }

  .chat-composer-stack__agent-rail {
    margin-left: 10px;
    margin-right: 10px;
    padding-top: 24px;
  }
}
</style>
