<template>
  <div class="settings-page">
    <h1 class="settings-page__title">角色与权限</h1>
    <p class="settings-page__desc">管理工作空间自定义角色，并为角色分配细粒度权限。</p>

    <div class="settings-card">
      <div class="toolbar">
        <t-button theme="primary" @click="openCreate">
          <template #icon><t-icon name="add" /></template>
          创建角色
        </t-button>
      </div>

      <t-table row-key="id" :data="roles" :columns="columns" :loading="loading" bordered stripe>
        <template #builtIn="{ row }">
          <t-tag :theme="row.builtIn ? 'default' : 'primary'" variant="light" size="small">
            {{ row.builtIn ? '内置' : '自定义' }}
          </t-tag>
        </template>
        <template #permissions="{ row }">
          <span class="perm-count">{{ row.permissionCodes.length }} 项权限</span>
        </template>
        <template #op="{ row }">
          <t-space>
            <t-button variant="text" @click="openEdit(row)">编辑</t-button>
            <t-button v-if="!row.builtIn" variant="text" theme="danger" @click="removeRole(row.id)">删除</t-button>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editingId ? '编辑角色' : '创建角色'"
      :confirm-btn="{ content: '保存', loading: saving }"
      width="640px"
      @confirm="submitForm"
    >
      <t-form label-align="top">
        <t-form-item label="角色名称">
          <t-input v-model="form.roleName" maxlength="64" />
        </t-form-item>
        <t-form-item label="描述">
          <t-textarea v-model="form.description" :autosize="{ minRows: 2, maxRows: 4 }" maxlength="255" />
        </t-form-item>
        <t-form-item label="权限">
          <t-checkbox-group v-model="form.permissionCodes" class="perm-grid">
            <t-checkbox v-for="item in permissions" :key="item.permissionCode" :value="item.permissionCode">
              {{ item.permissionName }}
            </t-checkbox>
          </t-checkbox-group>
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import type { PrimaryTableCol } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { useReloadOnWorkspaceChange } from '@/composables/useReloadOnWorkspaceChange'
import {
  createRole,
  deleteRole,
  listPermissions,
  listRoles,
  updateRole,
  type PermissionVO,
  type RoleVO,
} from '@/api/role'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const roles = ref<RoleVO[]>([])
const permissions = ref<PermissionVO[]>([])

const form = reactive({
  roleName: '',
  description: '',
  permissionCodes: [] as string[],
})

const columns: PrimaryTableCol<RoleVO>[] = [
  { colKey: 'roleName', title: '角色名称' },
  { colKey: 'roleCode', title: '编码', width: 160 },
  { colKey: 'builtIn', title: '类型', width: 90 },
  { colKey: 'permissions', title: '权限', width: 120 },
  { colKey: 'op', title: '操作', width: 160 },
]

async function loadData() {
  loading.value = true
  try {
    const [rolesRes, permsRes] = await Promise.all([listRoles(), listPermissions()])
    roles.value = rolesRes.data.data || []
    permissions.value = permsRes.data.data || []
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '加载角色失败'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.roleName = ''
  form.description = ''
  form.permissionCodes = []
  dialogVisible.value = true
}

function openEdit(row: RoleVO) {
  editingId.value = row.id
  form.roleName = row.roleName
  form.description = row.description || ''
  form.permissionCodes = [...row.permissionCodes]
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.roleName.trim()) {
    MessagePlugin.warning('请输入角色名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      roleName: form.roleName.trim(),
      description: form.description.trim() || undefined,
      permissionCodes: form.permissionCodes,
    }
    if (editingId.value) {
      await updateRole(editingId.value, payload)
    } else {
      await createRole(payload)
    }
    dialogVisible.value = false
    await loadData()
    MessagePlugin.success('保存成功')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

async function removeRole(id: number) {
  try {
    await deleteRole(id)
    await loadData()
    MessagePlugin.success('已删除')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '删除失败'))
  }
}

onMounted(loadData)
useReloadOnWorkspaceChange(loadData)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.perm-count {
  color: var(--box-muted);
  font-size: 13px;
}
</style>
