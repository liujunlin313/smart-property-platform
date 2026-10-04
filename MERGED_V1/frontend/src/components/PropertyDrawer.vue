<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, UploadFilled } from '@element-plus/icons-vue'
import { apiMessage } from '../api/http.js'
import { createProperty, fetchFloorPlanBlob, updateProperty, uploadFloorPlan } from '../api/assets.js'
import { displayLayoutType } from '../constants/presentation.js'

const props = defineProps({
  mode: { type: String, default: 'create' },
  parentUnit: { type: Object, default: null },
  initial: { type: Object, default: null },
  layouts: { type: Array, default: () => [] },
  readOnly: { type: Boolean, default: false }
})
const visible = defineModel('visible', { type: Boolean, default: false })
const emit = defineEmits(['saved'])
const formRef = ref()
const saving = ref(false)
const uploadProgress = ref(0)
const selectedFile = ref(null)
const previewUrl = ref(null)
const floorPlanSource = ref(null)
const fileInput = ref()
const form = reactive({})

const title = computed(() => {
  if (props.readOnly) return '房产详情'
  return props.mode === 'create' ? '录入整套房产' : '编辑房产'
})
const selectedLayout = computed(() => props.layouts.find(item => item.layoutTemplateId === form.layoutTemplateId))
const floorPlanDescription = computed(() => {
  const layout = selectedLayout.value?.layoutName || '当前户型'
  const area = form.grossArea == null ? '' : ` · ${Number(form.grossArea).toFixed(2).replace(/\.00$/, '')}㎡`
  if (floorPlanSource.value === 'PROPERTY' || floorPlanSource.value === 'PROPERTY_PENDING') return '房产专属户型图'
  if (floorPlanSource.value === 'VARIANT') return `标准户型参考图${area}`
  if (floorPlanSource.value === 'TEMPLATE') return '户型模板参考图'
  return `${layout}${area}`
})

function reset() {
  Object.keys(form).forEach(key => delete form[key])
  const source = props.initial || {}
  Object.assign(form, {
    buildingUnitId: source.buildingUnitId || props.parentUnit?.id,
    layoutTemplateId: source.layoutTemplateId || null,
    propertyNo: source.propertyNo || '',
    floorNo: source.floorNo ?? null,
    grossArea: source.grossArea ?? null,
    usableArea: source.usableArea ?? null,
    propertyUse: source.propertyUse || 'RESIDENTIAL',
    status: source.status || 'DRAFT',
    occupancyStatus: ['VACANT', 'OWNER_OCCUPIED'].includes(source.occupancyStatus) ? source.occupancyStatus : 'UNKNOWN',
    version: source.version,
    changeReason: ''
  })
  selectedFile.value = null
  floorPlanSource.value = source.floorPlanSource || null
  uploadProgress.value = 0
  revokePreview()
  if (source.floorPlanFileId) loadPreview(source.propertyUnitId)
}

watch(visible, open => { if (open) reset() })
watch(() => form.grossArea, value => {
  if (value == null || value === '') form.status = 'DRAFT'
})

function chooseFile(event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('户型图不能超过 10MB')
    return
  }
  selectedFile.value = file
  floorPlanSource.value = 'PROPERTY_PENDING'
  revokePreview()
  previewUrl.value = URL.createObjectURL(file)
}

async function loadPreview(propertyId) {
  try {
    previewUrl.value = await fetchFloorPlanBlob(propertyId)
  } catch {
    previewUrl.value = null
  }
}

function revokePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
}

onBeforeUnmount(revokePreview)

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const saved = props.mode === 'create'
      ? await createProperty(form)
      : await updateProperty(props.initial.propertyUnitId, form)
    if (selectedFile.value) {
      await uploadFloorPlan(saved.propertyUnitId, selectedFile.value, event => {
        uploadProgress.value = Math.round((event.loaded / event.total) * 100)
      })
    }
    ElMessage.success(form.status === 'DRAFT' ? '房产草稿已保存' : '房产已保存')
    visible.value = false
    emit('saved', saved)
  } catch (error) {
    ElMessage.error(apiMessage(error))
  } finally {
    saving.value = false
  }
}

const rules = {
  propertyNo: [{ required: true, message: '请输入房号', trigger: 'blur' }]
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" size="640px" destroy-on-close>
    <div class="property-context">
      <div>
        <p class="eyebrow">PROPERTY UNIT</p>
        <h3>{{ initial?.communityName || parentUnit?.communityName || '当前层级' }} · {{ initial?.buildingName || parentUnit?.buildingName || '' }}</h3>
        <span>{{ initial?.unitName || parentUnit?.label || '请选择单元' }}</span>
      </div>
      <el-tag :type="form.status === 'ACTIVE' ? 'success' : form.status === 'DRAFT' ? 'warning' : 'info'">
        {{ { ACTIVE:'正式', DRAFT:'草稿', INACTIVE:'停用', ARCHIVED:'归档' }[form.status] }}
      </el-tag>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="readOnly">
      <div class="section-title">门牌与户型</div>
      <div class="form-grid thirds">
        <el-form-item label="房号" prop="propertyNo">
          <el-input v-model="form.propertyNo" :disabled="mode === 'edit' || readOnly" placeholder="如 101" />
        </el-form-item>
        <el-form-item label="所在楼层">
          <el-input-number v-model="form.floorNo" :min="-20" :max="200" :controls="false" />
        </el-form-item>
        <el-form-item label="用途">
          <el-select v-model="form.propertyUse">
            <el-option label="住宅" value="RESIDENTIAL" />
            <el-option label="商业" value="COMMERCIAL" />
            <el-option label="办公" value="OFFICE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="户型模板">
        <el-select v-model="form.layoutTemplateId" clearable filterable placeholder="选择有限枚举式户型" style="width:100%">
          <el-option v-for="item in layouts" :key="item.layoutTemplateId" :value="item.layoutTemplateId"
                     :label="`${item.layoutName} · ${item.standardArea || '面积未定'}㎡`" />
        </el-select>
      </el-form-item>
      <div v-if="selectedLayout" class="layout-hint">
        <b>{{ selectedLayout.layoutName }}</b><span class="mono">{{ displayLayoutType(selectedLayout.layoutType) }}</span><p>{{ selectedLayout.description }}</p>
      </div>

      <div class="section-title">面积与当前状态</div>
      <div class="form-grid">
        <el-form-item label="建筑面积（㎡）">
          <el-input-number v-model="form.grossArea" :min="0.01" :precision="2" :controls="false" placeholder="未填时保存为草稿" />
        </el-form-item>
        <el-form-item label="套内面积（㎡）">
          <el-input-number v-model="form.usableArea" :min="0.01" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="资料状态">
          <el-select v-model="form.status" :disabled="!form.grossArea">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="正式启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
            <el-option label="归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="居住状态（Phase 2A）">
          <el-select v-model="form.occupancyStatus">
            <el-option label="未确认" value="UNKNOWN" />
            <el-option label="无人居住" value="VACANT" />
            <el-option label="房东自住" value="OWNER_OCCUPIED" />
          </el-select>
        </el-form-item>
      </div>
      <p class="draft-note" v-if="!form.grossArea">建筑面积“未填”会以 NULL 保存，数据库自动将房产限制为草稿，不能伪造为 0㎡。</p>

      <div class="section-title floor-plan-heading">
        <span>整套房户型图</span>
        <small v-if="previewUrl">{{ floorPlanDescription }}</small>
      </div>
      <div class="floor-plan-box" :class="{ empty: !previewUrl }">
        <el-image v-if="previewUrl" :src="previewUrl" :preview-src-list="[previewUrl]" fit="contain"
                  preview-teleported :z-index="3000" alt="整套房户型图" />
        <div v-else class="empty-plan"><el-icon><Picture /></el-icon><b>暂无可用户型图</b><span>优先显示房产专属图，再匹配相同户型与建筑面积的标准图，最后回退户型模板图。</span></div>
      </div>
      <p v-if="previewUrl" class="preview-hint">点击图片可查看完整大图</p>
      <div v-if="!readOnly" class="upload-row">
        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="chooseFile" />
        <el-button :icon="UploadFilled" @click="fileInput.click()">选择 JPEG / PNG / WebP</el-button>
        <span>{{ selectedFile?.name || '上传专属图可覆盖当前模板展示；单张图片最大 10MB' }}</span>
      </div>
      <el-progress v-if="uploadProgress" :percentage="uploadProgress" :stroke-width="8" />

      <el-form-item v-if="mode === 'edit' && !readOnly" label="修改说明" class="reason-field">
        <el-input v-model="form.changeReason" type="textarea" :rows="2" placeholder="选填，将随本次修改写入审计记录" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">{{ readOnly ? '关闭' : '取消' }}</el-button>
      <el-button v-if="!readOnly" type="primary" :loading="saving" @click="submit">保存房产</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.property-context { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:26px; padding:18px; background:linear-gradient(135deg,#edf7f5,#f8fafb); border:1px solid #dceae8; border-radius:14px; }
.property-context h3 { margin:0 0 5px; color:#193b4b; font-size:18px; }.property-context span { color:#6d8290; font-size:13px; }
.section-title { margin:26px 0 16px; padding-bottom:9px; color:#193b4b; font-weight:800; border-bottom:1px solid #e5ecef; }
.floor-plan-heading { display:flex; align-items:baseline; justify-content:space-between; gap:16px; }.floor-plan-heading small { color:#62817c; font-size:12px; font-weight:600; }
.form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }.form-grid.thirds { grid-template-columns:repeat(3,minmax(0,1fr)); }
:deep(.el-input-number), :deep(.el-select) { width:100%; }
.layout-hint { margin:-5px 0 8px; padding:12px 14px; background:#f5f8fa; border-radius:10px; color:#5f7481; font-size:12px; }.layout-hint b { color:#254b5b; margin-right:10px; }.layout-hint .mono { color:#167d72; }.layout-hint p { margin:7px 0 0; }
.draft-note { margin:-4px 0 12px; padding:10px 12px; color:#8a6323; background:#fff8e9; border-radius:9px; font-size:12px; }
.floor-plan-box { height:300px; overflow:hidden; display:grid; place-items:center; background:#f6f8f9; border:1px solid #dce6eb; border-radius:14px; }.floor-plan-box :deep(.el-image) { width:100%; height:100%; background:white; }.floor-plan-box :deep(.el-image__inner) { object-fit:contain; cursor:zoom-in; }.floor-plan-box.empty { border-style:dashed; }
.empty-plan { display:flex; flex-direction:column; align-items:center; gap:8px; color:#7a8f9a; text-align:center; }.empty-plan .el-icon { color:#79aaa3; font-size:38px; }.empty-plan span { max-width:360px; font-size:12px; line-height:1.6; }
.preview-hint { margin:8px 0 0; color:#71848f; font-size:12px; text-align:right; }
.upload-row { display:flex; align-items:center; gap:12px; margin:12px 0; }.upload-row span { overflow:hidden; color:#71848f; font-size:12px; text-overflow:ellipsis; white-space:nowrap; }
.reason-field { margin-top:24px; }
@media (max-width:640px) { .floor-plan-heading { align-items:flex-start; flex-direction:column; gap:4px; }.floor-plan-box { height:260px; } }
</style>
