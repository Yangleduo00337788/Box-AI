<template>
  <t-popup
    v-model:visible="visible"
    trigger="click"
    placement="top-right"
    :show-arrow="false"
    :disabled="disabled"
    overlay-class-name="model-picker-overlay"
    :overlay-inner-style="{ padding: '0', borderRadius: '12px' }"
  >
    <button
      type="button"
      class="model-picker__trigger"
      data-testid="chat-model-picker"
      :class="{ 'model-picker__trigger--disabled': disabled }"
      :disabled="disabled"
    >
      <span>{{ currentLabel }}</span>
      <t-icon name="chevron-down" size="16px" />
    </button>
    <template #content>
      <div class="model-picker">
        <div class="model-picker__tabs">
          <button
            type="button"
            class="model-picker__tab"
            :class="{ 'model-picker__tab--active': tab === 'quality' }"
            @click="tab = 'quality'"
          >
            高质量模型
          </button>
          <button
            type="button"
            class="model-picker__tab"
            :class="{ 'model-picker__tab--active': tab === 'multi' }"
            @click="tab = 'multi'"
          >
            多模态模型
          </button>
        </div>

        <p class="model-picker__group">{{ tab === 'multi' ? '视觉、生图、语音及其他模态' : '文本对话' }}</p>
        <div class="model-picker__list">
          <t-popup
            v-for="item in visibleModels"
            :key="item.value"
            trigger="hover"
            placement="right-top"
            :show-arrow="false"
            :delay="[180, 80]"
            overlay-class-name="model-picker-tip-overlay"
            :overlay-inner-style="{ padding: '0', borderRadius: '12px' }"
          >
            <button
              type="button"
              class="model-picker__item"
              :class="{ 'model-picker__item--active': modelValue === item.value }"
              @click.stop="select(item.value)"
            >
              <span v-if="item.value === 'auto'" class="model-picker__brand model-picker__auto-icon" aria-hidden="true">
                <t-icon name="high-level-filled" size="20px" />
              </span>
              <img
                v-else-if="item.logo"
                :src="item.logo"
                alt=""
                class="model-picker__brand"
              />
              <span v-else class="model-picker__logo" :style="{ background: item.color }">
                {{ item.label.slice(0, 1) }}
              </span>
              <span class="model-picker__name">{{ item.label }}</span>
              <t-tag v-if="item.streaming" size="small" variant="light" class="model-picker__tag">流式</t-tag>
              <t-icon v-if="modelValue === item.value" name="check" class="model-picker__check" />
            </button>
            <template #content>
              <div class="model-picker__tip">
                <div class="model-picker__tip-head">
                  <strong>{{ item.label }}</strong>
                  <t-tag v-if="item.kindLabel" size="small" variant="light" theme="primary">{{ item.kindLabel }}</t-tag>
                </div>
                <p>{{ item.description }}</p>
                <div class="model-picker__meta">
                  <span v-if="item.provider">{{ item.provider }}</span>
                  <span v-if="item.contextWindow">上下文 {{ formatTokens(item.contextWindow) }}</span>
                  <span v-if="item.streaming">支持流式输出</span>
                </div>
              </div>
            </template>
          </t-popup>
          <t-empty
            v-if="!visibleModels.length"
            size="small"
            :description="tab === 'multi' ? '暂无多模态模型' : '暂无可用的文本对话模型'"
          />
        </div>

        <button type="button" class="model-picker__custom" @click="goCustom">
          <t-icon name="add" />
          添加自定义模型
        </button>
      </div>
    </template>
  </t-popup>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { PlatformModelVO } from '@/api/platform'
import { getAvatarColor } from '@/utils/format'
import { resolveModelBrandLogo } from '@/utils/modelBrandLogo'
import { classifyModelChatKind, isMultimodalCapability, modelCapabilityLabel } from '@/utils/modelCapability'

const props = withDefaults(
  defineProps<{
    modelValue: string
    models: PlatformModelVO[]
    disabled?: boolean
    autoHint?: string
  }>(),
  { disabled: false, autoHint: '' },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const router = useRouter()
const visible = ref(false)
const tab = ref<'quality' | 'multi'>('quality')

interface PickerItem {
  value: string
  label: string
  provider: string
  description: string
  contextWindow?: number
  color: string
  logo?: string
  kind: ReturnType<typeof classifyModelChatKind>
  kindLabel: string
  streaming: boolean
}

const autoItem = computed<PickerItem>(() => ({
  value: 'auto',
  label: 'Auto',
  provider: '',
  description: props.autoHint || '自动选用当前可用、已配置密钥的平台模型。',
  color: '#1f2329',
  kind: 'text',
  kindLabel: '自动调度',
  streaming: true,
}))

const mapped = computed<PickerItem[]>(() =>
  props.models.map((item) => {
    const kind = classifyModelChatKind(item.modelCode, item.modelName, item.description)
    return {
      value: String(item.id),
      label: item.modelName,
      provider: item.providerName || '',
      description: item.description || '平台托管模型，对话将扣减工作空间配额。',
      contextWindow: item.contextWindow,
      color: getAvatarColor(item.modelName),
      logo: resolveModelBrandLogo(item.modelCode, item.modelName, item.providerName),
      kind,
      kindLabel: modelCapabilityLabel(kind),
      streaming: Boolean(item.supportStreaming),
    }
  }),
)

const visibleModels = computed(() => {
  if (tab.value === 'multi') {
    return mapped.value.filter((item) => isMultimodalCapability(item.kind))
  }
  return [autoItem.value, ...mapped.value.filter((item) => item.kind === 'text')]
})

const currentLabel = computed(() => {
  if (props.modelValue === 'auto') return 'Auto'
  return mapped.value.find((item) => item.value === props.modelValue)?.label || 'Auto'
})

function formatTokens(value: number) {
  if (value >= 1000) return `${Math.round(value / 1000)}K`
  return String(value)
}

function select(value: string) {
  emit('update:modelValue', value)
  visible.value = false
}

function goCustom() {
  visible.value = false
  router.push('/models')
}
</script>

<style scoped>
.model-picker__trigger {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 28px;
  padding: 0 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--box-muted);
  font-size: 13px;
  cursor: pointer;
}

.model-picker__trigger:hover {
  background: var(--box-hover);
  color: var(--box-ink);
}

.model-picker__trigger--disabled,
.model-picker__trigger--disabled:hover {
  cursor: not-allowed;
  opacity: 0.55;
  background: transparent;
  color: var(--box-muted);
}
</style>

<style>
.model-picker {
  width: 300px;
  padding: 10px 10px 8px;
}

.model-picker__tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 3px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: #f2f3f5;
}

.model-picker__tab {
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #8f959e;
  font-size: 13px;
  cursor: pointer;
}

.model-picker__tab--active {
  background: #fff;
  color: #1f2329;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.model-picker__group {
  margin: 0 8px 6px;
  font-size: 12px;
  color: #8f959e;
}

.model-picker__list {
  max-height: 280px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.model-picker__list::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.model-picker__item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  height: 40px;
  padding: 0 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #1f2329;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}

.model-picker__item:hover,
.model-picker__item--active {
  background: #f5f6f7;
}

.model-picker__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.model-picker__auto-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--box-ink);
}

.model-picker__brand {
  width: 20px;
  height: 20px;
  object-fit: contain;
  flex-shrink: 0;
  border-radius: 4px;
}

.model-picker__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-picker__tag {
  flex-shrink: 0;
}

.model-picker__check {
  color: #1f2329;
  font-size: 16px;
}

.model-picker__custom {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  height: 36px;
  margin-top: 4px;
  padding: 0 8px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #1f2329;
  font-size: 13px;
  cursor: pointer;
}

.model-picker__custom:hover {
  background: #f5f6f7;
}

.model-picker__tip {
  width: 228px;
  padding: 14px 16px;
}

.model-picker__tip strong {
  display: block;
  margin: 0;
  font-size: 14px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-picker__tip-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.model-picker__tip-head .t-tag {
  flex-shrink: 0;
}

.model-picker__tip p {
  margin: 0 0 10px;
  font-size: 12px;
  color: #8f959e;
  line-height: 1.55;
}

.model-picker__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #646a73;
}

.model-picker-overlay .t-popup__content,
.model-picker-tip-overlay .t-popup__content {
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(31, 35, 41, 0.12);
  overflow: hidden;
}
</style>
