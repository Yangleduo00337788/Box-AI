<template>
  <div class="settings-page">
    <h1 class="settings-page__title">个人信息</h1>
    <p class="settings-page__desc">头像和显示名称会出现在对话、团队与工作空间菜单中。</p>

    <section class="settings-card profile-card">
      <image-picker
        :model-value="auth.user?.avatarUrl"
        :fallback-text="avatarText"
        hint="点击更换头像"
        size="72px"
        persist="avatar"
      />
      <t-form :data="form" label-align="top" class="profile-card__form">
        <t-form-item label="显示名称" name="nickname" required-mark>
          <t-input v-model="form.nickname" placeholder="给你自己起个名字" maxlength="64" />
        </t-form-item>
        <t-form-item label="个性签名">
          <t-textarea
            v-model="form.bio"
            placeholder="介绍一下你自己"
            :maxlength="255"
            :autosize="{ minRows: 3, maxRows: 4 }"
          />
        </t-form-item>
        <div class="profile-card__actions">
          <t-button variant="outline" @click="resetForm">取消</t-button>
          <t-button theme="primary" :loading="saving" @click="save">保存</t-button>
        </div>
      </t-form>
    </section>

    <section class="settings-card">
      <h2 class="settings-card__heading">账号信息</h2>
      <t-descriptions :column="1" item-layout="horizontal">
        <t-descriptions-item label="用户名">{{ auth.user?.username || '—' }}</t-descriptions-item>
        <t-descriptions-item label="所属企业">{{ auth.tenant?.name || '—' }}</t-descriptions-item>
        <t-descriptions-item label="当前空间角色">{{ roleLabel }}</t-descriptions-item>
        <t-descriptions-item label="用户 ID">
          <span>{{ auth.user?.id || '—' }}</span>
          <t-button variant="text" size="small" @click="copyId">复制</t-button>
        </t-descriptions-item>
      </t-descriptions>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { extractApiError } from '@/api/apiError'
import { useAuthStore } from '@/stores/auth'
import { roleName } from '@/utils/role'
import ImagePicker from '@/components/ImagePicker.vue'

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
.profile-card {
  display: flex;
  align-items: flex-start;
  gap: 32px;
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

.settings-card__heading {
  margin: 0 0 16px;
  font: var(--td-font-title-small);
}

@media (max-width: 640px) {
  .profile-card {
    flex-direction: column;
  }
}
</style>
