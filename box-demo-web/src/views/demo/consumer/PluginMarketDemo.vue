<script setup lang="ts">
import { ref } from 'vue'

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'tools', label: '工具' },
  { key: 'workflows', label: '工作流' },
  { key: 'knowledge', label: '知识库' },
  { key: 'mcp', label: 'MCP' },
]

const active = ref('all')

const items = [
  { title: '查询订单 HTTP', type: '工具', source: '官方', desc: '对接 ERP 订单状态' },
  { title: '客服分流工作流', type: '工作流', source: '官方', desc: 'Switch + 知识库 + 人工' },
  { title: '产品 FAQ', type: '知识库', source: '工作空间', desc: '已安装 · 42 分块' },
  { title: 'CRM MCP Server', type: 'MCP', source: '官方', desc: '客户与商机读写' },
]
</script>

<template>
  <div class="demo-page">
    <header class="demo-page__header">
      <h1>插件市场</h1>
      <p>工具、工作流、知识库与 MCP 统一发现与安装（模拟）。</p>
    </header>

    <t-tabs v-model="active">
      <t-tab-panel v-for="t in tabs" :key="t.key" :value="t.key" :label="t.label" />
    </t-tabs>

    <t-row :gutter="[16, 16]" style="margin-top: 16px">
      <t-col v-for="item in items" :key="item.title" :xs="12" :md="6">
        <t-card :title="item.title" hover-shadow>
          <t-space size="small">
            <t-tag size="small" variant="light">{{ item.type }}</t-tag>
            <t-tag size="small" theme="primary" variant="outline">{{ item.source }}</t-tag>
          </t-space>
          <p class="card-desc">{{ item.desc }}</p>
          <t-button size="small" theme="primary" variant="text">打开</t-button>
        </t-card>
      </t-col>
    </t-row>
  </div>
</template>

<style scoped>
.card-desc {
  margin: 12px 0 8px;
  font-size: 14px;
  color: var(--td-text-color-secondary);
}
</style>
