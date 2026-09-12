<template>
  <div class="trace-tree">
    <div v-for="span in tree" :key="span.spanId" class="trace-tree__node" :style="{ marginLeft: `${span.depth * 12}px` }">
      <div class="trace-tree__head">
        <strong>{{ span.name }}</strong>
        <t-tag size="small" variant="light">{{ span.spanType }}</t-tag>
        <span class="trace-tree__meta">{{ span.durationMs ?? 0 }} ms</span>
      </div>
      <pre v-if="span.inputJson" class="trace-tree__json">输入：{{ formatJson(span.inputJson) }}</pre>
      <pre v-if="span.outputJson" class="trace-tree__json">输出：{{ formatJson(span.outputJson) }}</pre>
      <p v-if="span.errorMessage" class="trace-tree__error">{{ span.errorMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TraceSpanVO } from '@/api/execution'

const props = defineProps<{
  spans: TraceSpanVO[]
}>()

interface TreeSpan extends TraceSpanVO {
  depth: number
}

const tree = computed(() => buildTree(props.spans || []))

function buildTree(spans: TraceSpanVO[]): TreeSpan[] {
  const byId = new Map(spans.map((span) => [span.spanId, span]))
  const depthCache = new Map<string, number>()

  function depthOf(span: TraceSpanVO): number {
    if (depthCache.has(span.spanId)) {
      return depthCache.get(span.spanId)!
    }
    if (!span.parentSpanId || !byId.has(span.parentSpanId)) {
      depthCache.set(span.spanId, 0)
      return 0
    }
    const value = depthOf(byId.get(span.parentSpanId)!) + 1
    depthCache.set(span.spanId, value)
    return value
  }

  return spans
    .map((span) => ({ ...span, depth: depthOf(span) }))
    .sort((a, b) => a.depth - b.depth || a.spanId.localeCompare(b.spanId))
}

function formatJson(raw: string) {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}
</script>

<style scoped>
.trace-tree {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trace-tree__node {
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
  padding: 10px;
  background: var(--td-bg-color-container);
}

.trace-tree__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.trace-tree__meta {
  margin-left: auto;
  color: var(--td-text-color-secondary);
  font-size: 12px;
}

.trace-tree__json {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.trace-tree__error {
  margin: 6px 0 0;
  color: var(--td-error-color);
  font-size: 12px;
}
</style>
