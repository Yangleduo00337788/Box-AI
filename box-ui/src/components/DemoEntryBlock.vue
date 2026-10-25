<template>
  <div v-if="enabled" class="demo-entry">
    <div class="demo-entry__row">
      <div class="demo-entry__label">
        <span class="demo-entry__title">演示环境</span>
        <span class="demo-entry__chip">{{ portalLabel }}</span>
      </div>
      <a
        :href="peerUrl"
        class="demo-entry__peer"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ switchLabel }}
        <t-icon name="chevron-right" class="demo-entry__peer-icon" />
      </a>
    </div>
    <p class="demo-entry__desc">任意账号登录即可体验，数据为前端模拟。</p>
    <div class="demo-entry__repos">
      <source-repo-links size="sm" :ghost="true" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { peerDemoLoginUrl } from '../constants/demoPortal'
import SourceRepoLinks from './SourceRepoLinks.vue'

const props = withDefaults(
  defineProps<{
    portal?: 'consumer' | 'admin'
  }>(),
  { portal: 'consumer' },
)

const enabled = import.meta.env.VITE_BOX_DEMO === 'true'

const portalLabel = computed(() => (props.portal === 'admin' ? '管理端' : '用户端'))
const switchLabel = computed(() => (props.portal === 'admin' ? '用户端' : '管理端'))
const peerUrl = computed(() => peerDemoLoginUrl(props.portal))
</script>

<style scoped>
.demo-entry {
  margin: 0 0 20px;
  padding: 14px 16px 12px;
  border-radius: 12px;
  border: 1px solid var(--box-border);
  background: var(--td-gray-color-1);
}

.demo-entry__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.demo-entry__label {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.demo-entry__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--box-ink);
}

.demo-entry__chip {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  color: var(--box-muted);
  background: var(--box-surface);
  border: 1px solid var(--box-border);
}

.demo-entry__peer {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  margin: 0;
  padding: 4px 2px 4px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: var(--box-muted);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}

.demo-entry__peer:hover {
  color: var(--box-ink);
  background: color-mix(in srgb, var(--box-surface) 70%, transparent);
}

.demo-entry__peer-icon {
  font-size: 14px;
  opacity: 0.65;
}

.demo-entry__desc {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--box-muted);
}

.demo-entry__repos {
  display: flex;
  justify-content: flex-start;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--box-border);
}
</style>
