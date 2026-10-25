<script setup lang="ts">
import { ref } from 'vue'
import { screenshotImage } from '@/utils/assets'

const section = ref('prompt')
const version = ref('v3 · 已发布')
const prompt = ref('你是 Acme 公司的客服助手，回答需简洁、可追溯知识库来源。')

const sections = [
  { value: 'prompt', label: 'Prompt' },
  { value: 'model', label: '模型' },
  { value: 'knowledge', label: '知识库' },
  { value: 'tools', label: '工具' },
  { value: 'variables', label: '变量' },
]

const shot = screenshotImage('agent-builder.png')
</script>

<template>
  <div class="demo-page">
    <header class="demo-page__header">
      <h1>Agent Builder</h1>
      <p>左侧模块导航 + 中央配置 + 右侧调试预览（模拟）。</p>
    </header>

    <t-row :gutter="16">
      <t-col :span="12">
        <t-card>
          <div class="builder-layout">
            <nav class="builder-nav">
              <t-button
                v-for="s in sections"
                :key="s.value"
                :variant="section === s.value ? 'base' : 'text'"
                block
                @click="section = s.value"
              >
                {{ s.label }}
              </t-button>
            </nav>
            <div class="builder-main">
              <div class="builder-toolbar">
                <t-select v-model="version" :options="[{ label: version, value: version }]" style="width: 180px" />
                <t-space>
                  <t-button variant="outline">保存草稿</t-button>
                  <t-button theme="primary">发布</t-button>
                </t-space>
              </div>
              <div v-if="section === 'prompt'">
                <t-textarea v-model="prompt" :autosize="{ minRows: 8, maxRows: 14 }" />
              </div>
              <div v-else-if="section === 'model'">
                <t-form label-align="top">
                  <t-form-item label="模型">
                    <t-select value="gpt-4.1" :options="[{ label: 'gpt-4.1（平台池）', value: 'gpt-4.1' }]" />
                  </t-form-item>
                  <t-form-item label="温度">
                    <t-slider :value="0.3" :min="0" :max="1" :step="0.1" />
                  </t-form-item>
                </t-form>
              </div>
              <div v-else-if="section === 'knowledge'">
                <t-tag theme="primary" variant="light">产品 FAQ</t-tag>
                <t-tag theme="primary" variant="light" style="margin-left: 8px">工单知识库</t-tag>
              </div>
              <div v-else-if="section === 'tools'">
                <t-list size="small">
                  <t-list-item>HTTP · 查询订单状态</t-list-item>
                  <t-list-item>MCP · 内部 CRM</t-list-item>
                </t-list>
              </div>
              <div v-else>
                <t-table
                  size="small"
                  :data="[{ name: 'user_name', type: 'string', default: '访客' }]"
                  :columns="[
                    { colKey: 'name', title: '变量名' },
                    { colKey: 'type', title: '类型' },
                    { colKey: 'default', title: '默认值' },
                  ]"
                />
              </div>
            </div>
            <aside class="builder-debug">
              <div class="debug-title">调试预览</div>
              <div class="debug-msg">用户：定价方案有哪些？</div>
              <div class="debug-msg assistant">助手：个人版 / 团队版 / 企业版…（模拟）</div>
              <t-button size="small" theme="primary" block style="margin-top: 12px">运行调试</t-button>
            </aside>
          </div>
        </t-card>
      </t-col>
      <t-col :span="12">
        <div class="demo-shot">
          <img :src="shot" alt="Agent Builder" loading="lazy" />
        </div>
      </t-col>
    </t-row>
  </div>
</template>

<style scoped>
.builder-layout {
  display: grid;
  grid-template-columns: 120px 1fr 200px;
  gap: 16px;
  min-height: 360px;
}

.builder-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.builder-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.builder-debug {
  border-left: 1px solid var(--td-component-border);
  padding-left: 12px;
  font-size: 13px;
}

.debug-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.debug-msg {
  padding: 8px;
  border-radius: 8px;
  background: var(--td-bg-color-secondarycontainer);
  margin-bottom: 8px;
}

.debug-msg.assistant {
  background: var(--td-brand-color-1);
}

@media (max-width: 900px) {
  .builder-layout {
    grid-template-columns: 1fr;
  }
  .builder-debug {
    border-left: none;
    padding-left: 0;
    border-top: 1px solid var(--td-component-border);
    padding-top: 12px;
  }
}
</style>
