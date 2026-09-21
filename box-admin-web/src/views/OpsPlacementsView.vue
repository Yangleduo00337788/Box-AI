<template>
  <div class="ops-placements-page admin-page">
    <page-header
      title="运营位"
      desc="按投放端区分：C 端顶栏公告与右下角轮播；B 端管理后台顶栏公告与首页 Banner。消息通知会汇总待办与 B 端公告。"
    >
      <template #actions>
        <t-button theme="primary" @click="openCreate">新建运营位</t-button>
      </template>
    </page-header>

    <t-card :bordered="false" class="admin-card">
      <div class="admin-toolbar">
        <t-select v-model="audienceFilter" :options="audienceOptions" placeholder="投放端" clearable style="width: 120px" />
        <t-select v-model="slotFilter" :options="slotFilterOptions" placeholder="投放位置" clearable style="width: 180px" />
        <t-select v-model="kindFilter" :options="kindOptions" placeholder="类型" clearable style="width: 140px" />
      </div>
      <t-table row-key="id" :data="filteredItems" :columns="columns" :loading="loading" hover>
        <template #empty>
          <t-empty description="暂无运营位" />
        </template>
      </t-table>
    </t-card>

    <t-dialog
      v-model:visible="dialogVisible"
      :header="editing ? '编辑运营位' : '新建运营位'"
      width="720px"
      :confirm-btn="{ content: editing ? '保存' : '发布', loading: saving }"
      @confirm="onSave"
    >
      <t-form ref="formRef" :data="form" :rules="rules" label-width="108px">
        <t-form-item label="投放端" name="audience">
          <t-radio-group v-model="form.audience">
            <t-radio value="C">C 端（用户）</t-radio>
            <t-radio value="B">B 端（管理后台）</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="投放位置" name="slot">
          <t-select v-model="form.slot" :options="formSlotOptions" />
        </t-form-item>
        <t-form-item label="类型" name="kind">
          <t-select v-model="form.kind" :options="kindOptions" :disabled="isCreativeSlot" />
          <div class="ops-kind-hint">{{ slotHint }}</div>
        </t-form-item>
        <t-form-item label="标题" name="title">
          <t-input v-model="form.title" />
        </t-form-item>
        <t-form-item v-if="!isCreativeSlot" label="图标" name="iconName">
          <div class="ops-icon-field">
            <div class="ops-icon-presets">
              <button
                v-for="icon in iconPresets"
                :key="icon"
                type="button"
                class="ops-icon-preset"
                :class="{ 'is-active': !form.iconUrl && !form.iconSvg && form.iconName === icon }"
                :title="icon"
                @click="selectPresetIcon(icon)"
              >
                <t-icon :name="icon" />
              </button>
            </div>
            <div class="ops-icon-actions">
              <input
                ref="iconFileRef"
                type="file"
                class="ops-icon-file"
                accept=".png,.jpg,.jpeg,.svg,image/png,image/jpeg,image/svg+xml"
                @change="onIconFile"
              />
              <t-button variant="outline" :loading="uploadingIcon" @click="openIconPicker">上传图标</t-button>
              <t-button variant="text" theme="default" @click="clearIcon">无图标</t-button>
              <span class="ops-icon-hint">可不选图标。也可上传 PNG / JPG / SVG，或粘贴 SVG 代码后保存</span>
            </div>
            <t-textarea
              v-model="form.iconSvg"
              placeholder="粘贴 SVG 代码，例如 <svg viewBox=&quot;0 0 24 24&quot;>...</svg>"
              :autosize="{ minRows: 3, maxRows: 6 }"
              @change="onSvgInput"
            />
            <div v-if="form.iconUrl" class="ops-icon-custom">
              <img :src="form.iconUrl" alt="" />
              <span>已使用上传图标</span>
            </div>
            <div v-else-if="form.iconSvg" class="ops-icon-custom">
              <span class="ops-icon-svg-preview" v-html="form.iconSvg" />
              <span>已使用 SVG 代码，请点保存才会写入</span>
            </div>
            <div v-else-if="form.iconName" class="ops-icon-custom">
              <t-icon :name="form.iconName" />
              <span>已使用预设图标</span>
            </div>
            <div v-else class="ops-icon-custom">当前无图标，C 端不显示图标</div>
          </div>
        </t-form-item>
        <t-form-item v-if="isCreativeSlot" label="投放图片" name="imageUrl">
          <div class="ops-icon-field">
            <input
              ref="imageFileRef"
              type="file"
              class="ops-icon-file"
              accept=".png,.jpg,.jpeg,image/png,image/jpeg"
              @change="onImageFile"
            />
            <div class="ops-icon-actions">
              <t-button variant="outline" :loading="uploadingImage" @click="imageFileRef?.click()">上传图片</t-button>
              <span class="ops-icon-hint">必填，PNG / JPG，单张不超过 8MB；建议 16:9 横图（C 端约 200×112 展示）</span>
            </div>
            <div v-if="form.imageUrl" class="ops-image-preview">
              <img :src="form.imageUrl" alt="" />
            </div>
          </div>
        </t-form-item>
        <t-form-item
          v-if="form.slot !== 'CHAT_BANNER' && form.slot !== 'ADMIN_BANNER'"
          label="正文"
          name="body"
        >
          <t-textarea v-model="form.body" :autosize="{ minRows: 2, maxRows: 4 }" />
        </t-form-item>
        <t-form-item label="跳转链接" name="linkUrl">
          <t-input v-model="form.linkUrl" placeholder="/plugin-market 或 https://..." />
        </t-form-item>
        <t-form-item v-if="!isCreativeSlot" label="链接文案" name="linkLabel">
          <t-input v-model="form.linkLabel" placeholder="了解更多" />
        </t-form-item>
        <t-form-item v-if="!isCreativeSlot" label="提示样式" name="theme">
          <t-select v-model="form.theme" :options="themeOptions" />
        </t-form-item>
        <t-form-item label="排序" name="sortOrder">
          <t-input-number v-model="form.sortOrder" :min="0" theme="column" />
        </t-form-item>
        <t-form-item label="可关闭" name="dismissible">
          <t-switch v-model="form.dismissible" />
          <div v-if="isCreativeSlot" class="ops-kind-hint">图片位在 C 端右下角展示关闭按钮；关闭后本机不再显示该条。</div>
        </t-form-item>
        <t-form-item v-if="editing" label="状态" name="status">
          <t-radio-group v-model="form.status">
            <t-radio value="LISTED">上架</t-radio>
            <t-radio value="UNLISTED">下架</t-radio>
          </t-radio-group>
        </t-form-item>
        <t-form-item label="开始时间" name="startsAt">
          <t-date-picker
            v-model="form.startsAt"
            enable-time-picker
            allow-input
            clearable
            format="YYYY-MM-DD HH:mm:ss"
            value-type="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </t-form-item>
        <t-form-item label="结束时间" name="endsAt">
          <t-date-picker
            v-model="form.endsAt"
            enable-time-picker
            allow-input
            clearable
            format="YYYY-MM-DD HH:mm:ss"
            value-type="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </t-form-item>
      </t-form>
    </t-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { DialogPlugin, Link, MessagePlugin, Tag } from 'tdesign-vue-next'
import type { FormInstanceFunctions, FormProps, PrimaryTableCol } from 'tdesign-vue-next'
import PageHeader from '@box/ui/components/PageHeader.vue'
import { uploadAdminImage } from '@/api/asset'
import {
  createOpsPlacement,
  deleteOpsPlacement,
  fetchOpsPlacements,
  updateOpsPlacement,
  type OpsAudience,
  type OpsKind,
  type OpsPlacementVO,
  type OpsSlotAny,
} from '@/api/ops'

const loading = ref(false)
const items = ref<OpsPlacementVO[]>([])
const audienceFilter = ref<OpsAudience | ''>('')
const slotFilter = ref<OpsSlotAny | ''>('')
const kindFilter = ref<OpsKind | ''>('')
const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstanceFunctions>()
const iconFileRef = ref<HTMLInputElement>()
const imageFileRef = ref<HTMLInputElement>()
const uploadingIcon = ref(false)
const uploadingImage = ref(false)
const iconPresets = [
  'star',
  'gift',
  'notification',
  'lightbulb',
  'shop',
  'tools',
  'rocket',
  'check-circle',
  'info-circle',
  'thumb-up',
  'heart',
  'flag',
]

const audienceOptions = [
  { label: 'C 端', value: 'C' },
  { label: 'B 端', value: 'B' },
]
const cSlotOptions = [
  { label: '顶栏公告', value: 'CHAT_HOME' },
  { label: '全站顶栏', value: 'GLOBAL_ALERT' },
  { label: '右下角 Banner', value: 'CHAT_BANNER' },
  { label: '右下角广告', value: 'CHAT_AD' },
]
const bSlotOptions = [
  { label: '管理后台顶栏', value: 'ADMIN_HEADER' },
  { label: '管理后台 Banner', value: 'ADMIN_BANNER' },
]
const formSlotOptions = computed(() => (form.audience === 'B' ? bSlotOptions : cSlotOptions))
const slotFilterOptions = computed(() => [...cSlotOptions, ...bSlotOptions])
const kindOptions = [
  { label: '公告', value: 'ANNOUNCEMENT' },
  { label: '推荐', value: 'PROMO' },
  { label: 'Banner', value: 'BANNER' },
  { label: '广告', value: 'AD' },
]
const themeOptions = [
  { label: '信息', value: 'info' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '错误', value: 'error' },
]
const audienceLabel: Record<string, string> = { C: 'C 端', B: 'B 端' }
const slotLabel: Record<string, string> = {
  CHAT_HOME: '顶栏公告',
  GLOBAL_ALERT: '全站顶栏',
  CHAT_BANNER: '右下角 Banner',
  CHAT_AD: '右下角广告',
  ADMIN_HEADER: '管理后台顶栏',
  ADMIN_BANNER: '管理后台 Banner',
}
const kindLabel: Record<string, string> = {
  ANNOUNCEMENT: '公告',
  PROMO: '推荐',
  BANNER: 'Banner',
  AD: '广告',
}

const form = reactive({
  audience: 'C' as OpsAudience,
  slot: 'CHAT_HOME' as OpsSlotAny,
  kind: 'ANNOUNCEMENT' as OpsKind,
  title: '',
  body: '',
  linkUrl: '',
  linkLabel: '',
  iconName: '',
  iconUrl: '',
  iconSvg: '',
  imageUrl: '',
  theme: 'info',
  dismissible: true,
  status: 'LISTED',
  sortOrder: 0,
  startsAt: '' as string,
  endsAt: '' as string,
})

const isCreativeSlot = computed(
  () => form.slot === 'CHAT_BANNER' || form.slot === 'CHAT_AD' || form.slot === 'ADMIN_BANNER',
)

const slotHint = computed(() => {
  if (form.slot === 'GLOBAL_ALERT') {
    return '展示在 C 端页面最上方顶栏，与「顶栏公告」合并排序；多条用数字切换。提示样式决定顶栏颜色。'
  }
  if (form.slot === 'CHAT_BANNER') {
    return '与「右下角广告」合并为右下角小卡片轮播（约 200×112）。类型固定为 Banner，需上传图片。'
  }
  if (form.slot === 'CHAT_AD') {
    return '与「右下角 Banner」合并为右下角小卡片轮播。类型固定为广告，需上传图片。'
  }
  if (form.slot === 'ADMIN_HEADER') {
    return '展示在 B 端管理后台顶栏下方，并进入消息通知；多条可切换。'
  }
  if (form.slot === 'ADMIN_BANNER') {
    return 'B 端首页区域图片 Banner，需上传图片。'
  }
  return '与「全站顶栏」一起在 C 端顶栏展示；公告 / 推荐样式与可关闭行为同顶栏规则。'
})

const rules: FormProps['rules'] = {
  slot: [{ required: true, message: '请选择投放位置' }],
  kind: [{ required: true, message: '请选择类型' }],
  title: [{ required: true, message: '请输入标题' }],
  imageUrl: [
    {
      validator: () => {
        if (!isCreativeSlot.value) return { result: true, message: '' }
        return form.imageUrl.trim()
          ? { result: true, message: '' }
          : { result: false, message: '请上传投放图片', type: 'error' }
      },
    },
  ],
}

watch(
  () => form.audience,
  (audience) => {
    if (audience === 'B' && !bSlotOptions.some((opt) => opt.value === form.slot)) {
      form.slot = 'ADMIN_HEADER'
      form.kind = 'ANNOUNCEMENT'
    }
    if (audience === 'C' && !cSlotOptions.some((opt) => opt.value === form.slot)) {
      form.slot = 'CHAT_HOME'
      form.kind = 'ANNOUNCEMENT'
    }
  },
)

watch(
  () => form.slot,
  (slot) => {
    if (slot === 'CHAT_BANNER' || slot === 'ADMIN_BANNER') {
      form.kind = 'BANNER'
      form.iconName = ''
      form.iconUrl = ''
      form.iconSvg = ''
      form.dismissible = true
      return
    }
    if (slot === 'CHAT_AD') {
      form.kind = 'AD'
      form.iconName = ''
      form.iconUrl = ''
      form.iconSvg = ''
      form.dismissible = true
      return
    }
    if (form.kind === 'BANNER' || form.kind === 'AD') {
      form.kind = 'ANNOUNCEMENT'
    }
  },
)

const filteredItems = computed(() =>
  items.value.filter((item) => {
    if (audienceFilter.value && item.audience !== audienceFilter.value) return false
    if (slotFilter.value && item.slot !== slotFilter.value) return false
    if (kindFilter.value && item.kind !== kindFilter.value) return false
    return true
  }),
)

const columns: PrimaryTableCol<OpsPlacementVO>[] = [
  { colKey: 'title', title: '标题', minWidth: 160 },
  {
    colKey: 'audience',
    title: '端',
    width: 72,
    cell: (_, { row }) => audienceLabel[row.audience] || row.audience,
  },
  {
    colKey: 'slot',
    title: '位置',
    width: 156,
    cell: (_, { row }) => slotLabel[row.slot] || row.slot,
  },
  {
    colKey: 'kind',
    title: '类型',
    width: 90,
    cell: (_, { row }) =>
      h(
        Tag,
        {
          theme: row.kind === 'PROMO' || row.kind === 'AD' ? 'warning' : row.kind === 'BANNER' ? 'success' : 'primary',
          variant: 'light',
        },
        () => kindLabel[row.kind] || row.kind,
      ),
  },
  {
    colKey: 'status',
    title: '状态',
    width: 90,
    cell: (_, { row }) =>
      h(Tag, { theme: row.status === 'LISTED' ? 'success' : 'default', variant: 'light' }, () =>
        row.status === 'LISTED' ? '上架' : '下架',
      ),
  },
  { colKey: 'sortOrder', title: '排序', width: 80 },
  { colKey: 'impressions', title: '曝光', width: 90 },
  { colKey: 'clicks', title: '点击', width: 90 },
  {
    colKey: 'actions',
    title: '操作',
    width: 140,
    fixed: 'right',
    cell: (_, { row }) =>
      h('div', { class: 'admin-ops' }, [
        h(Link, { theme: 'primary', hover: 'color', onClick: () => openEdit(row) }, () => '编辑'),
        h(Link, { theme: 'danger', hover: 'color', onClick: () => confirmDelete(row) }, () => '删除'),
      ]),
  },
]

function resetForm() {
  Object.assign(form, {
    audience: 'C',
    slot: 'CHAT_HOME',
    kind: 'ANNOUNCEMENT',
    title: '',
    body: '',
    linkUrl: '',
    linkLabel: '',
    iconName: '',
    iconUrl: '',
    iconSvg: '',
    imageUrl: '',
    theme: 'info',
    dismissible: true,
    status: 'LISTED',
    sortOrder: 0,
    startsAt: '',
    endsAt: '',
  })
}

async function loadItems() {
  loading.value = true
  try {
    const { data } = await fetchOpsPlacements()
    items.value = data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: OpsPlacementVO) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    audience: row.audience || 'C',
    slot: row.slot,
    kind: row.kind,
    title: row.title,
    body: row.body || '',
    linkUrl: row.linkUrl || '',
    linkLabel: row.linkLabel || '',
    iconName: row.iconName || '',
    iconUrl: row.iconUrl || '',
    iconSvg: row.iconSvg || '',
    imageUrl: row.imageUrl || '',
    theme: row.theme || 'info',
    dismissible: row.dismissible,
    status: row.status,
    sortOrder: row.sortOrder || 0,
    startsAt: row.startsAt || '',
    endsAt: row.endsAt || '',
  })
  dialogVisible.value = true
}

function optionalText(value: string) {
  const trimmed = value.trim()
  return trimmed || undefined
}

function selectPresetIcon(name: string) {
  form.iconName = name
  form.iconUrl = ''
  form.iconSvg = ''
}

function clearIcon() {
  form.iconName = ''
  form.iconUrl = ''
  form.iconSvg = ''
  if (iconFileRef.value) iconFileRef.value.value = ''
}

function onSvgInput(value: string) {
  if (String(value || '').trim()) {
    form.iconUrl = ''
    form.iconName = ''
  }
}

function openIconPicker() {
  iconFileRef.value?.click()
}

async function onIconFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingIcon.value = true
  try {
    const { data } = await uploadAdminImage(file)
    form.iconUrl = data.data?.url || ''
    form.iconName = ''
    form.iconSvg = ''
    MessagePlugin.success('图标已上传')
  } finally {
    uploadingIcon.value = false
    input.value = ''
  }
}

async function onImageFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploadingImage.value = true
  try {
    const { data } = await uploadAdminImage(file)
    form.imageUrl = data.data?.url || ''
    MessagePlugin.success('图片已上传')
  } finally {
    uploadingImage.value = false
    input.value = ''
  }
}

async function onSave() {
  const valid = await formRef.value?.validate()
  if (valid !== true) return false
  saving.value = true
  try {
    const payload = {
      audience: form.audience,
      slot: form.slot,
      kind: form.kind,
      title: form.title.trim(),
      body: optionalText(form.body),
      linkUrl: optionalText(form.linkUrl),
      linkLabel: optionalText(form.linkLabel),
      iconName: isCreativeSlot.value ? '' : (form.iconUrl || form.iconSvg ? '' : form.iconName.trim()),
      iconUrl: isCreativeSlot.value || form.iconSvg ? '' : form.iconUrl.trim(),
      iconSvg: isCreativeSlot.value ? '' : form.iconSvg.trim(),
      imageUrl: form.imageUrl.trim(),
      theme: form.theme,
      dismissible: form.dismissible,
      sortOrder: form.sortOrder,
      startsAt: optionalText(form.startsAt),
      endsAt: optionalText(form.endsAt),
    }
    if (editing.value && editingId.value) {
      await updateOpsPlacement(editingId.value, { ...payload, status: form.status })
      MessagePlugin.success('已保存')
    } else {
      await createOpsPlacement(payload)
      MessagePlugin.success('已发布')
    }
    dialogVisible.value = false
    await loadItems()
    return true
  } catch {
    return false
  } finally {
    saving.value = false
  }
}

function confirmDelete(row: OpsPlacementVO) {
  const dialog = DialogPlugin.confirm({
    header: '删除运营位',
    body: `确定删除「${row.title}」？`,
    theme: 'warning',
    onConfirm: async () => {
      await deleteOpsPlacement(row.id)
      MessagePlugin.success('已删除')
      dialog.destroy()
      await loadItems()
    },
  })
}

onMounted(loadItems)
</script>

<style scoped>
.ops-kind-hint {
  margin-top: 6px;
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  line-height: 1.4;
}

.ops-icon-field {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.ops-icon-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ops-icon-preset {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 1px solid var(--td-border-level-2-color, #e7e7e7);
  border-radius: 8px;
  background: #fff;
  color: var(--td-text-color-primary);
  cursor: pointer;
}

.ops-icon-preset.is-active {
  border-color: var(--td-brand-color);
  background: var(--td-brand-color-light, #f2f3ff);
  color: var(--td-brand-color);
}

.ops-icon-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.ops-icon-file {
  display: none;
}

.ops-icon-hint {
  color: var(--td-text-color-placeholder);
  font-size: 12px;
}

.ops-icon-custom {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--td-text-color-secondary);
  font-size: 12px;
}

.ops-icon-custom img,
.ops-icon-svg-preview :deep(svg) {
  width: 22px;
  height: 22px;
  display: block;
  object-fit: contain;
}

.ops-image-preview {
  overflow: hidden;
  max-width: 360px;
  border: 1px solid var(--td-border-level-2-color, #e7e7e7);
  border-radius: 8px;
  background: #f5f6f8;
}

.ops-image-preview img {
  display: block;
  width: 100%;
  height: 120px;
  object-fit: cover;
}
</style>
