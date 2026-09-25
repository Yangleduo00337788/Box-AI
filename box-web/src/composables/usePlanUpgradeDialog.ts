import { ref } from 'vue'

const upgradeVisible = ref(false)
const successListeners = new Set<() => void>()

/** 全局居中升级弹窗（挂 body，避免挤在侧栏额度按钮容器内） */
export function usePlanUpgradeDialog() {
  function openPlanUpgrade() {
    upgradeVisible.value = true
  }

  function closePlanUpgrade() {
    upgradeVisible.value = false
  }

  function onPlanUpgradeSuccess() {
    successListeners.forEach((fn) => fn())
  }

  function subscribePlanUpgradeSuccess(listener: () => void) {
    successListeners.add(listener)
    return () => successListeners.delete(listener)
  }

  return {
    upgradeVisible,
    openPlanUpgrade,
    closePlanUpgrade,
    onPlanUpgradeSuccess,
    subscribePlanUpgradeSuccess,
  }
}
