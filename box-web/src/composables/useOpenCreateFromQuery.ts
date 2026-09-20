import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/** 从插件市场「新建」带 ?create=1 进入时，打开对应资源的创建对话框 */
export function useOpenCreateFromQuery(openCreate: () => void) {
  const route = useRoute()
  const router = useRouter()

  onMounted(() => {
    if (route.query.create !== '1') return
    openCreate()
    const query = { ...route.query }
    delete query.create
    void router.replace({ path: route.path, query })
  })
}
