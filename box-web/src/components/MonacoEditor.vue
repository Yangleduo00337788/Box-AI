<template>
  <div ref="containerRef" class="monaco-editor-host" :style="{ height }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import loader from '@monaco-editor/loader'
import type { editor } from 'monaco-editor'

const props = withDefaults(
  defineProps<{
    modelValue: string
    language?: string
    height?: string
    readOnly?: boolean
  }>(),
  {
    language: 'markdown',
    height: '280px',
    readOnly: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const containerRef = ref<HTMLDivElement | null>(null)
let monacoEditor: editor.IStandaloneCodeEditor | null = null
let monacoApi: typeof import('monaco-editor') | null = null

onMounted(async () => {
  if (!containerRef.value) return
  monacoApi = await loader.init()
  monacoEditor = monacoApi.editor.create(containerRef.value, {
    value: props.modelValue || '',
    language: props.language,
    readOnly: props.readOnly,
    minimap: { enabled: false },
    automaticLayout: true,
    fontSize: 13,
    lineNumbers: 'on',
    scrollBeyondLastLine: false,
    wordWrap: 'on',
  })
  monacoEditor.onDidChangeModelContent(() => {
    emit('update:modelValue', monacoEditor?.getValue() || '')
  })
})

watch(
  () => props.modelValue,
  (value) => {
    if (!monacoEditor) return
    const current = monacoEditor.getValue()
    if (value !== current) {
      monacoEditor.setValue(value || '')
    }
  },
)

watch(
  () => props.language,
  (language) => {
    if (!monacoEditor || !monacoApi) return
    const model = monacoEditor.getModel()
    if (model) {
      monacoApi.editor.setModelLanguage(model, language)
    }
  },
)

onBeforeUnmount(() => {
  monacoEditor?.dispose()
  monacoEditor = null
})
</script>

<style scoped>
.monaco-editor-host {
  width: 100%;
  border: 1px solid var(--td-component-border);
  border-radius: 8px;
  overflow: hidden;
}
</style>
