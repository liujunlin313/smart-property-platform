<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { parkingAdminApi } from '../../api/stageH.js'
import { apiMessage } from '../../api/http.js'
import PageHeader from '../../components/common/PageHeader.vue'
import SectionCard from '../../components/common/SectionCard.vue'
import StatCard from '../../components/common/StatCard.vue'
import PaginationBar from '../../components/common/PaginationBar.vue'
import StatusTag from '../../components/common/StatusTag.vue'
import { displayEnum, formatMoney } from '../../constants/presentation.js'

const summary = ref({ pendingCount: 0, activeCount: 0, expiringSoonCount: 0 })
const activeTab = ref('applications'), loading = ref(false), apps = ref([]), permits = ref([]), appTotal = ref(0), permitTotal = ref(0)
const appQuery = reactive({ keyword: '', status: '', page: 1, pageSize: 20 })
const permitQuery = reactive({ keyword: '', status: '', page: 1, pageSize: 20 })
const cards = () => [['待审核申请', summary.value.pendingCount], ['当前有效车证', summary.value.activeCount], ['30 天内到期', summary.value.expiringSoonCount]]
async function loadSummary () { summary.value = (await parkingAdminApi.summary()).data }
async function loadApps () { loading.value = true; try { const page = (await parkingAdminApi.applications(appQuery)).data; apps.value = page.records; appTotal.value = page.total } catch (e) { ElMessage.error(apiMessage(e, '车证申请加载失败')) } finally { loading.value = false } }
async function loadPermits () { loading.value = true; try { const page = (await parkingAdminApi.permits(permitQuery)).data; permits.value = page.records; permitTotal.value = page.total } catch (e) { ElMessage.error(apiMessage(e, '车证列表加载失败')) } finally { loading.value = false } }
async function load () { await Promise.all([loadSummary(), activeTab.value === 'applications' ? loadApps() : loadPermits()]) }
async function review (row, decision) { try { const rejecting = decision === 'REJECT'; const result = rejecting ? await ElMessageBox.prompt('请填写拒绝原因', '拒绝车证申请', { inputPattern: /\S+/, inputErrorMessage: '拒绝原因不能为空' }) : await ElMessageBox.confirm(`确认通过 ${row.holderName} 的车证申请吗？`, '审核车证申请', { type: 'warning' }); await parkingAdminApi.review(row.applicationId, { decision, rejectionReason: rejecting ? result.value : null }); ElMessage.success(rejecting ? '申请已拒绝' : '车证已签发'); load() } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error(apiMessage(e, '审核失败')) } }
function reset (query) { Object.assign(query, { keyword: '', status: '', page: 1 }); load() }
onMounted(load)
</script>

<template>
  <section class="parking-page">
    <PageHeader title="车证管理" description="审核负责范围内的居民车证申请；本模块管理人和车辆的通行权益，不分配固定车位。" />
    <div class="stats"><StatCard v-for="[label, value] in cards()" :key="label" :label="label" :value="value" /></div>
    <SectionCard title="车证业务" description="月费与有效期均由后端按申请时的资格、数量和日期校验。">
      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane label="申请审核" name="applications">
          <div class="filters"><el-input v-model="appQuery.keyword" clearable placeholder="姓名、车牌、小区或房产" @keyup.enter="appQuery.page = 1; loadApps()" /><el-select v-model="appQuery.status" clearable placeholder="申请状态"><el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已拒绝" value="REJECTED" /></el-select><el-button type="primary" @click="appQuery.page = 1; loadApps()">查询</el-button><el-button @click="reset(appQuery)">重置</el-button></div>
          <el-table v-loading="loading" :data="apps" class="business-table"><el-table-column prop="applicationNo" label="申请单号" min-width="150" /><el-table-column prop="holderName" label="申请人" min-width="95" /><el-table-column prop="plateNumber" label="车牌号" min-width="110" /><el-table-column prop="communityName" label="小区" min-width="120" /><el-table-column prop="fullLocation" label="关联房产" min-width="190" /><el-table-column label="资格来源" min-width="110"><template #default="{ row }">{{ displayEnum(row.eligibilitySource) }}</template></el-table-column><el-table-column label="期限 / 报价" min-width="175"><template #default="{ row }">{{ row.requestedMonths }} 个月 · ¥{{ formatMoney(row.quotedTotalAmount) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column><el-table-column label="操作" width="155" fixed="right"><template #default="{ row }"><template v-if="row.status === 'PENDING'"><el-button link type="primary" @click="review(row, 'APPROVE')">通过</el-button><el-button link type="danger" @click="review(row, 'REJECT')">拒绝</el-button></template><span v-else>—</span></template></el-table-column></el-table>
          <PaginationBar v-model:page="appQuery.page" v-model:page-size="appQuery.pageSize" :total="appTotal" @change="loadApps" />
        </el-tab-pane>
        <el-tab-pane label="已签发车证" name="permits">
          <div class="filters"><el-input v-model="permitQuery.keyword" clearable placeholder="姓名、车牌、小区或房产" @keyup.enter="permitQuery.page = 1; loadPermits()" /><el-select v-model="permitQuery.status" clearable placeholder="车证状态"><el-option label="有效" value="ACTIVE" /><el-option label="已过期" value="EXPIRED" /></el-select><el-button type="primary" @click="permitQuery.page = 1; loadPermits()">查询</el-button><el-button @click="reset(permitQuery)">重置</el-button></div>
          <el-table v-loading="loading" :data="permits" class="business-table"><el-table-column prop="permitNo" label="车证编号" min-width="145" /><el-table-column prop="holderName" label="持证人" min-width="95" /><el-table-column prop="plateNumber" label="车牌号" min-width="110" /><el-table-column prop="communityName" label="小区" min-width="120" /><el-table-column prop="fullLocation" label="关联房产" min-width="190" /><el-table-column label="月费 / 月数" min-width="140"><template #default="{ row }">¥{{ formatMoney(row.monthlyRate) }} / {{ row.chargedMonths }} 月</template></el-table-column><el-table-column label="有效期" min-width="175"><template #default="{ row }">{{ row.validFrom }} 至 {{ row.validTo }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column></el-table>
          <PaginationBar v-model:page="permitQuery.page" v-model:page-size="permitQuery.pageSize" :total="permitTotal" @change="loadPermits" />
        </el-tab-pane>
      </el-tabs>
    </SectionCard>
  </section>
</template>

<style scoped>
.parking-page{display:grid;gap:18px}.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.filters{display:flex;gap:10px;margin-bottom:16px}.filters .el-input{max-width:340px}.filters .el-select{width:140px}@media(max-width:760px){.stats{grid-template-columns:1fr}.filters{flex-wrap:wrap}.filters .el-input{max-width:none;width:100%}}
</style>
