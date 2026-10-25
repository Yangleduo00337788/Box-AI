<script setup lang="ts">
import { ref } from 'vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import { screenshotImage } from '@/utils/assets'

const nodes = ref([
  { id: '1', type: 'default', position: { x: 40, y: 80 }, label: '开始' },
  { id: '2', type: 'default', position: { x: 220, y: 40 }, label: 'LLM 归纳' },
  { id: '3', type: 'default', position: { x: 220, y: 140 }, label: '知识库检索' },
  { id: '4', type: 'default', position: { x: 420, y: 90 }, label: 'Switch' },
  { id: '5', type: 'default', position: { x: 600, y: 50 }, label: 'HTTP 回调' },
  { id: '6', type: 'default', position: { x: 600, y: 150 }, label: '结束' },
])

const edges = ref([
  { id: 'e1-2', source: '1', target: '2' },
  { id: 'e1-3', source: '1', target: '3' },
  { id: 'e2-4', source: '2', target: '4' },
  { id: 'e3-4', source: '3', target: '4' },
  { id: 'e4-5', source: '4', target: '5', label: '命中' },
  { id: 'e4-6', source: '4', target: '6', label: '默认' },
])

const shot = screenshotImage('workflow-editor.png')
</script>

<template>
  <div class="demo-page">
    <header class="demo-page__header">
      <h1>工作流编排</h1>
      <p>基于 Vue Flow 的示意画布，可拖拽节点；完整编辑器见下方产品截图。</p>
    </header>
    <t-row :gutter="16">
      <t-col :span="12">
        <t-card title="示意画布（可交互）">
          <div class="flow-wrap">
            <vue-flow v-model:nodes="nodes" v-model:edges="edges" fit-view-on-init>
              <background />
              <controls />
            </vue-flow>
          </div>
          <t-space style="margin-top: 12px">
            <t-button theme="primary">校验</t-button>
            <t-button variant="outline">调试运行</t-button>
            <t-button variant="outline">发布</t-button>
          </t-space>
        </t-card>
      </t-col>
      <t-col :span="12">
        <div class="demo-shot">
          <img :src="shot" alt="工作流编辑器" loading="lazy" />
        </div>
      </t-col>
    </t-row>
  </div>
</template>

<style scoped>
.flow-wrap {
  height: 360px;
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
  overflow: hidden;
}
</style>
