<template>
  <button
    type="button"
    class="image-picker"
    :disabled="uploading"
    :aria-label="hint"
    :style="{ width: size, height: size }"
    @click.stop="pick"
  >
    <span class="image-picker__frame">
      <t-avatar :size="size" shape="circle" :image="preview || undefined">
        {{ fallbackText }}
      </t-avatar>
      <span class="image-picker__overlay">{{ hint }}</span>
    </span>
    <input
      ref="inputRef"
      type="file"
      accept="image/png,image/jpeg"
      class="image-picker__input"
      @change="onFile"
    />
  </button>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { uploadImageAsset } from '@/api/asset'
import { useAuthStore } from '@/stores/auth'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    fallbackText?: string
    hint?: string
    size?: string
    persist?: 'asset' | 'avatar'
  }>(),
  {
    modelValue: '',
    fallbackText: '图',
    hint: '更换头像',
    size: '56px',
    persist: 'asset',
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  uploaded: [url: string]
}>()

const inputRef = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const preview = ref(props.modelValue)
const auth = useAuthStore()

watch(
  () => props.modelValue,
  (value) => {
    preview.value = value || ''
  },
)

function pick() {
  inputRef.value?.click()
}

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    let url = ''
    if (props.persist === 'avatar') {
      await auth.uploadAvatar(file)
      url = auth.user?.avatarUrl || ''
    } else {
      const { data } = await uploadImageAsset(file)
      url = data.data?.url || ''
    }
    preview.value = url
    emit('update:modelValue', url)
    emit('uploaded', url)
    MessagePlugin.success(props.persist === 'avatar' ? '头像已更新' : '图片已上传')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '图片上传失败'))
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-picker {
  display: inline-flex;
  flex-shrink: 0;
  align-self: flex-start;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  overflow: hidden;
  border-radius: 50%;
}

.image-picker:disabled {
  cursor: wait;
  opacity: 0.7;
}

.image-picker__frame {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
}

.image-picker__frame :deep(.t-avatar) {
  width: 100% !important;
  height: 100% !important;
}

.image-picker__frame :deep(.t-avatar__image),
.image-picker__frame :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.image-picker__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 10px;
  line-height: 1.2;
  text-align: center;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.image-picker:hover .image-picker__overlay,
.image-picker:focus-visible .image-picker__overlay {
  opacity: 1;
}

.image-picker__input {
  display: none;
}
</style>
