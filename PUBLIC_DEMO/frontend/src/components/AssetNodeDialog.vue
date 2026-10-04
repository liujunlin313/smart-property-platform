<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiMessage } from '../api/http.js'
import {
  createBuilding, createBuildingUnit, createCommunity,
  updateBuilding, updateBuildingUnit, updateCommunity
} from '../api/assets.js'

const props = defineProps({
  type: { type: String, required: true },
  mode: { type: String, default: 'create' },
  parent: { type: Object, default: null },
  initial: { type: Object, default: null }
})
const visible = defineModel('visible', { type: Boolean, default: false })
const emit = defineEmits(['saved'])
const formRef = ref()
const saving = ref(false)
const form = reactive({})

const typeNames = { community: '小区', building: '楼栋', unit: '单元' }
const title = computed(() => `${props.mode === 'create' ? '新增' : '编辑'}${typeNames[props.type]}`)

function reset() {
  Object.keys(form).forEach(key => delete form[key])
  const source = props.initial || {}
  if (props.type === 'community') {
    Object.assign(form, {
      communityCode: source.communityCode || '',
      communityName: source.communityName || '',
      addressLine: source.addressLine || '',
      longitude: source.longitude ?? null,
      latitude: source.latitude ?? null,
      completionYear: source.completionYear ?? null,
      status: source.status || 'ACTIVE',
      version: source.version,
      changeReason: ''
    })
  } else if (props.type === 'building') {
    Object.assign(form, {
      communityId: source.communityId || props.parent?.id,
      buildingNo: source.buildingNo || '',
      buildingName: source.buildingName || '',
      totalFloors: source.totalFloors ?? null,
      longitude: source.longitude ?? null,
      latitude: source.latitude ?? null,
      status: source.status || 'ACTIVE',
      version: source.version,
      changeReason: ''
    })
  } else {
    Object.assign(form, {
      buildingId: source.buildingId || props.parent?.id,
      unitNo: source.unitNo || '',
      unitName: source.unitName || '',
      status: source.status || 'ACTIVE',
      version: source.version,
      changeReason: ''
    })
  }
}

watch(visible, (open) => { if (open) reset() })

const rules = computed(() => {
  if (props.type === 'community') {
    return {
      communityCode: [{ required: true, message: '请输入稳定业务编码', trigger: 'blur' }],
      communityName: [{ required: true, message: '请输入小区名称', trigger: 'blur' }],
      addressLine: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
    }
  }
  if (props.type === 'building') {
    return { buildingNo: [{ required: true, message: '请输入楼栋号', trigger: 'blur' }] }
  }
  return { unitNo: [{ required: true, message: '请输入单元号', trigger: 'blur' }] }
})

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    let saved
    if (props.type === 'community') {
      saved = props.mode === 'create'
        ? await createCommunity(form)
        : await updateCommunity(props.initial.communityId, form)
    } else if (props.type === 'building') {
      saved = props.mode === 'create'
        ? await createBuilding(form)
        : await updateBuilding(props.initial.buildingId, form)
    } else {
      saved = props.mode === 'create'
        ? await createBuildingUnit(form)
        : await updateBuildingUnit(props.initial.buildingUnitId, form)
    }
    ElMessage.success(`${typeNames[props.type]}已保存`)
    visible.value = false
    emit('saved', saved)
  } catch (error) {
    ElMessage.error(apiMessage(error))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="580px" destroy-on-close>
    <div class="dialog-context" v-if="parent">
      <span>所属层级</span><b>{{ parent.label }}</b>
    </div>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <template v-if="type === 'community'">
        <div class="form-grid">
          <el-form-item label="小区业务编码" prop="communityCode">
            <el-input v-model="form.communityCode" :disabled="mode === 'edit'" placeholder="如 ZGC_ACADEMY" />
          </el-form-item>
          <el-form-item label="小区名称" prop="communityName">
            <el-input v-model="form.communityName" placeholder="请输入现实名称" />
          </el-form-item>
        </div>
        <el-form-item label="详细地址" prop="addressLine">
          <el-input v-model="form.addressLine" placeholder="省 / 市 / 区 / 街道 / 门牌" />
        </el-form-item>
        <div class="form-grid thirds">
          <el-form-item label="经度（GCJ-02）"><el-input-number v-model="form.longitude" :precision="7" :controls="false" /></el-form-item>
          <el-form-item label="纬度（GCJ-02）"><el-input-number v-model="form.latitude" :precision="7" :controls="false" /></el-form-item>
          <el-form-item label="建成年份"><el-input-number v-model="form.completionYear" :min="1800" :max="2200" :controls="false" /></el-form-item>
        </div>
      </template>

      <template v-else-if="type === 'building'">
        <div class="form-grid">
          <el-form-item label="楼栋号" prop="buildingNo">
            <el-input v-model="form.buildingNo" :disabled="mode === 'edit'" placeholder="如 1" />
          </el-form-item>
          <el-form-item label="显示名称"><el-input v-model="form.buildingName" placeholder="如 1号楼" /></el-form-item>
        </div>
        <div class="form-grid thirds">
          <el-form-item label="总楼层"><el-input-number v-model="form.totalFloors" :min="1" :max="200" :controls="false" /></el-form-item>
          <el-form-item label="楼栋经度"><el-input-number v-model="form.longitude" :precision="7" :controls="false" /></el-form-item>
          <el-form-item label="楼栋纬度"><el-input-number v-model="form.latitude" :precision="7" :controls="false" /></el-form-item>
        </div>
        <p class="coordinate-note">楼栋坐标将用于后续地图标注；房产不重复保存坐标，而是继承楼栋位置。</p>
      </template>

      <template v-else>
        <div class="form-grid">
          <el-form-item label="单元号" prop="unitNo">
            <el-input v-model="form.unitNo" :disabled="mode === 'edit'" placeholder="如 1" />
          </el-form-item>
          <el-form-item label="显示名称"><el-input v-model="form.unitName" placeholder="如 1单元" /></el-form-item>
        </div>
      </template>

      <div class="form-grid" :class="{ single: mode === 'create' }">
        <el-form-item label="使用状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="mode === 'edit'" label="修改说明">
          <el-input v-model="form.changeReason" placeholder="选填，将记录到审计日志" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存{{ typeNames[type] }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-context { display:flex; justify-content:space-between; margin:-6px 0 20px; padding:12px 14px; color:#627d98; background:#f3f7f8; border-radius:10px; }
.dialog-context b { color:#24475a; }
.form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }
.form-grid.thirds { grid-template-columns:repeat(3,minmax(0,1fr)); }
.form-grid.single { grid-template-columns:1fr; }
.coordinate-note { margin:-6px 0 18px; color:#7a8e99; font-size:12px; line-height:1.7; }
:deep(.el-input-number) { width:100%; }
</style>
