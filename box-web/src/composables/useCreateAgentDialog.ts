import { ref } from 'vue'

const visible = ref(false)

export function useCreateAgentDialog() {
  function openCreateAgentDialog() {
    visible.value = true
  }

  return {
    visible,
    openCreateAgentDialog,
  }
}
