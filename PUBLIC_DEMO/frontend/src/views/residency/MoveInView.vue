<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { residenceApi } from '../../api/stageH.js'
import { apiMessage } from '../../api/http.js'
import PageHeader from '../../components/common/PageHeader.vue'
import SectionCard from '../../components/common/SectionCard.vue'

const people = ref([]), properties = ref([]), loadingPeople = ref(false), loadingProperties = ref(false), submitting = ref(false)
const parkingIneligibleReasons = [
  { value: 'OWNER_NOT_GRANTED', label: '产权人未提供停车权益' },
  { value: 'NO_AVAILABLE_QUOTA', label: '该房屋当前无可用停车申请名额' },
  { value: 'LEASE_EXCLUDES_PARKING', label: '租赁约定不包含停车权益' },
  { value: 'OTHER', label: '其他' }
]
const form = ref({ personId: null, moveInType: 'TENANT', propertyUnitId: null, startDate: new Date().toISOString().slice(0, 10), expectedEndDate: '', parkingApplicationEligible: true, parkingIneligibleReasonCode: '', parkingEligibilityRemark: '' })
const create = ref({ fullName: '', gender: 'UNKNOWN', phone: '' })
async function find() { loadingPeople.value = true; try { people.value = (await residenceApi.people('')).data } catch (error) { ElMessage.error(apiMessage(error)) } finally { loadingPeople.value = false } }
async function available() { properties.value = []; form.value.propertyUnitId = null; if (!form.value.personId) return; loadingProperties.value = true; try { properties.value = (await residenceApi.available(form.value.personId, form.value.moveInType)).data } catch (error) { ElMessage.error(apiMessage(error)) } finally { loadingProperties.value = false } }
async function add() { if (!create.value.fullName.trim()) { ElMessage.warning('请先填写人员姓名'); return }; try { const person = (await residenceApi.createPerson(create.value)).data; people.value.unshift(person); form.value.personId = person.personId; create.value = { fullName: '', gender: 'UNKNOWN', phone: '' }; ElMessage.success('人员已新增，请选择可办理的房产') } catch (error) { ElMessage.error(apiMessage(error)) } }
async function submit() { const tenant = form.value.moveInType === 'TENANT'; if (tenant && !form.value.parkingApplicationEligible && !form.value.parkingIneligibleReasonCode) { ElMessage.warning('请选择不允许申请车证的原因'); return }; submitting.value = true; try { await residenceApi.moveIn({ ...form.value, expectedEndDate: form.value.expectedEndDate || null, parkingApplicationEligible: tenant ? form.value.parkingApplicationEligible : null, parkingIneligibleReasonCode: tenant && !form.value.parkingApplicationEligible ? form.value.parkingIneligibleReasonCode : null, parkingEligibilityRemark: tenant ? form.value.parkingEligibilityRemark || null : null }); ElMessage.success('入住办理成功'); properties.value = []; form.value.propertyUnitId = null; await available() } catch (error) { ElMessage.error(apiMessage(error)) } finally { submitting.value = false } }
watch(() => [form.value.personId, form.value.moveInType], available)
watch(() => form.value.parkingApplicationEligible, eligible => { if (eligible) form.value.parkingIneligibleReasonCode = '' })
find()
</script>

<template>
  <section class="move-in-page">
    <PageHeader title="入住办理" description="先选择人员，再根据入住性质查看可办理的房产；此处新增人员不会创建登录账号。" />
    <div class="form-grid">
      <SectionCard title="1. 选择人员" description="可选择既有人员，或先建立一条基础人员档案。">
        <el-form label-position="top"><el-form-item label="已有人员"><el-select v-model="form.personId" :loading="loadingPeople" filterable placeholder="按姓名或手机号选择"><el-option v-for="person in people" :key="person.personId" :value="person.personId" :label="`${person.fullName}${person.phone ? ' · ' + person.phone : ''}`" /></el-select></el-form-item></el-form>
        <div class="divider-label">或新增人员</div>
        <el-form label-position="top"><el-form-item label="姓名"><el-input v-model="create.fullName" placeholder="请输入姓名" /></el-form-item><el-form-item label="手机号（可选）"><el-input v-model="create.phone" placeholder="便于后续联系" /></el-form-item><el-button @click="add">新增人员并选择</el-button></el-form>
      </SectionCard>
      <SectionCard title="2. 填写入住信息" description="系统会按人员与入住性质展示可办理的房产。">
        <el-form label-position="top"><el-form-item label="入住性质"><el-radio-group v-model="form.moveInType"><el-radio value="TENANT">租赁入住</el-radio><el-radio value="OWNER_OCCUPY">产权人自住</el-radio></el-radio-group></el-form-item><div class="date-row"><el-form-item label="入住日期"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" /></el-form-item><el-form-item v-if="form.moveInType === 'TENANT'" label="预计结束日期（可选）"><el-date-picker v-model="form.expectedEndDate" value-format="YYYY-MM-DD" /></el-form-item></div><el-form-item label="可办理房产"><el-select v-model="form.propertyUnitId" :loading="loadingProperties" filterable :disabled="!form.personId" placeholder="请先选择人员"><el-option v-for="property in properties" :key="property.propertyUnitId" :value="property.propertyUnitId" :label="`${property.communityName} · ${property.buildingName} · ${property.unitName} · ${property.propertyNo}`" /></el-select></el-form-item><p v-if="form.personId && !loadingProperties && properties.length === 0" class="form-help">该人员在当前入住性质下没有可办理的房产。</p><el-button type="primary" :loading="submitting" :disabled="!form.propertyUnitId" @click="submit">确认办理入住</el-button></el-form>
      </SectionCard>
      <SectionCard v-if="form.moveInType === 'TENANT'" title="3. 车证申请资格" description="该资格仅随本次租赁入住的房产保存，居民后续可自行提交车辆和车证申请。">
        <el-form label-position="top"><el-form-item label="是否允许申请车证"><el-radio-group v-model="form.parkingApplicationEligible"><el-radio :value="true">允许申请</el-radio><el-radio :value="false">暂不允许</el-radio></el-radio-group></el-form-item><el-form-item v-if="!form.parkingApplicationEligible" label="不具备资格原因" required><el-select v-model="form.parkingIneligibleReasonCode" placeholder="请选择原因"><el-option v-for="reason in parkingIneligibleReasons" :key="reason.value" :label="reason.label" :value="reason.value" /></el-select></el-form-item><el-form-item label="备注（可选）"><el-input v-model="form.parkingEligibilityRemark" maxlength="300" show-word-limit /></el-form-item></el-form>
      </SectionCard>
    </div>
  </section>
</template>

<style scoped>
.move-in-page{display:grid;gap:18px}.form-grid{display:grid;grid-template-columns:minmax(290px,.85fr) minmax(420px,1.35fr);gap:18px}.el-select,.el-date-picker{width:100%}.divider-label{display:flex;align-items:center;gap:10px;margin:8px 0 14px;color:var(--muted);font-size:13px}.divider-label:before,.divider-label:after{content:'';height:1px;flex:1;background:var(--line)}.date-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-help{margin:-3px 0 14px;color:#a56609;font-size:13px}@media(max-width:860px){.form-grid,.date-row{grid-template-columns:1fr}}
</style>
