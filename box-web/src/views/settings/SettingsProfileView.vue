<template>
  <div class="settings-page">
    <h1 class="settings-page__title">个人信息</h1>
    <div class="profile-card">
      <t-avatar size="72px" class="profile-card__avatar">{{ avatarText }}</t-avatar>
      <t-form :data="form" label-align="top" class="profile-card__form">
        <t-form-item label="显示名称" name="nickname" required-mark>
          <t-input v-model="form.nickname" placeholder="给你自己起个名字" maxlength="64" />
        </t-form-item>
        <t-form-item label="用户名">
          <t-input :value="auth.user?.username" disabled />
        </t-form-item>
        <t-form-item label="个性签名">
          <t-textarea
            v-model="form.bio"
            placeholder="介绍一下你自己"
            :maxlength="255"
            :autosize="{ minRows: 3, maxRows: 4 }"
          />
        </t-form-item>
        <t-form-item label="所属企业">
          <t-input :value="auth.tenant?.name || '—'" disabled />
        </t-form-item>
        <t-form-item label="企业身份">
          <t-input :value="roleLabel" disabled />
        </t-form-item>
        <t-form-item label="用户 ID">
          <t-input :value="String(auth.user?.id || '')" disabled>
            <template #suffix>
              <t-button variant="text" size="small" @click="copyId">复制</t-button>
            </template>
          </t-input>
        </t-form-item>
        <div class="profile-card__actions">
          <t-button variant="outline" @click="resetForm">取消</t-button>
          <t-button theme="primary" :loading="saving" @click="save">保存</t-button>
        </div>
      </t-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { useAuthStore } from '@/stores/auth'
import { roleName } from '@/utils/role'

const auth = useAuthStore()
const saving = ref(false)
const form = reactive({
  nickname: auth.user?.nickname || '',
  bio: auth.user?.bio || '',
})

const avatarText = computed(() => (auth.user?.nickname || auth.user?.email || 'U').slice(0, 1).toUpperCase())
const roleLabel = computed(() => roleName(auth.currentWorkspace?.roleCode))

watch(
  () => auth.user,
  (user) => {
    if (!user) return
    form.nickname = user.nickname || ''
    form.bio = user.bio || ''
  },
  { immediate: true },
)

function resetForm() {
  form.nickname = auth.user?.nickname || ''
  form.bio = auth.user?.bio || ''
}

async function copyId() {
  if (!auth.user?.id) return
  await navigator.clipboard.writeText(String(auth.user.id))
  MessagePlugin.success('已复制用户 ID')
}

async function save() {
  const nickname = form.nickname.trim()
  if (!nickname) {
    MessagePlugin.warning('请填写显示名称')
    return
  }
  saving.value = true
  try {
    await auth.updateProfile({
      nickname,
      bio: form.bio.trim() || undefined,
    })
    MessagePlugin.success('已保存')
  } catch (error) {
    MessagePlugin.error(extractApiError(error, '保存失败'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page__title {
  margin: 0 0 24px;
  font: var(--td-font-title-large);
  color: var(--box-ink);
}

.profile-card {
  display: flex;
  gap: 32px;
  width: 100%;
  padding: 28px;
  border-radius: 16px;
  background: #f7f8fa;
}

.profile-card__avatar {
  flex-shrink: 0;
  background: var(--td-brand-color);
  color: #fff;
  font-weight: 600;
}

.profile-card__form {
  flex: 1;
  min-width: 0;
}

.profile-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}
</style>
