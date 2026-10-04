<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCommunities } from '../../api/community.js'
import { getProperties, getPropertyDetail } from '../../api/property.js'
import { apiMessage } from '../../api/http.js'
import EmptyState from '../../components/common/EmptyState.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import PaginationBar from '../../components/common/PaginationBar.vue'
import SectionCard from '../../components/common/SectionCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'
import { displayEnum, displayLayoutType, enumLabels } from '../../constants/presentation.js'

const loading = ref(false), communities = ref([]), rows = ref([]), total = ref(0), drawer = ref(false), detail = ref(null)
const q = reactive({ communityId: null, occupancyStatus: '', keyword: '', page: 1, pageSize: 20 })
async function load() { loading.value = true; try { const result = await getProperties(q); rows.value = result.records; total.value = result.total } catch (error) { ElMessage.error(apiMessage(error, '房产列表加载失败')) } finally { loading.value = false } }
async function open(row) { try { detail.value = await getPropertyDetail(row.propertyUnitId); drawer.value = true } catch (error) { ElMessage.error(apiMessage(error)) } }
function reset() { Object.assign(q, { communityId: null, occupancyStatus: '', keyword: '', page: 1 }); load() }
onMounted(async () => { try { communities.value = await getCommunities() } catch (error) { ElMessage.error(apiMessage(error)) }; load() })
</script>

<template>
  <section class="property-page">
    <PageHeader title="房产管理" description="按小区、入住状态和关键字查找房产；点击任意行可查看产权、居住与计量信息。" />
    <SectionCard title="房产台账" :description="`当前筛选结果共 ${total} 套房产`">
      <div class="filters"><el-input v-model="q.keyword" clearable placeholder="房号、小区、楼栋或单元" @keyup.enter="q.page = 1; load()" /><el-select v-model="q.communityId" clearable placeholder="全部小区"><el-option v-for="community in communities" :key="community.communityId" :label="community.communityName" :value="community.communityId" /></el-select><el-select v-model="q.occupancyStatus" clearable placeholder="入住状态"><el-option v-for="(label, value) in enumLabels" v-show="['OWNER_OCCUPIED','TENANT_OCCUPIED','AVAILABLE_FOR_RENT','VACANT','UNKNOWN'].includes(value)" :key="value" :label="label" :value="value" /></el-select><el-button type="primary" @click="q.page = 1; load()">查询</el-button><el-button @click="reset">重置</el-button></div>
      <EmptyState v-if="!loading && rows.length === 0" title="没有符合条件的房产" description="可以调整筛选条件，或前往物业工作台维护资产层级。" />
      <el-table v-else v-loading="loading" :data="rows" class="business-table" @row-click="open"><el-table-column prop="propertyNo" label="房产编号" min-width="120" /><el-table-column prop="communityName" label="小区" min-width="135" /><el-table-column prop="buildingName" label="楼栋" min-width="110" /><el-table-column prop="unitName" label="单元" min-width="90" /><el-table-column label="户型" min-width="100"><template #default="{ row }">{{ displayLayoutType(row.layoutType) }}</template></el-table-column><el-table-column label="建筑面积" min-width="105"><template #default="{ row }">{{ row.grossArea || '未填' }}{{ row.grossArea ? ' ㎡' : '' }}</template></el-table-column><el-table-column label="入住状态" min-width="120"><template #default="{ row }"><StatusTag :value="row.occupancyStatus" /></template></el-table-column><el-table-column label="操作" width="95" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="open(row)">查看详情</el-button></template></el-table-column></el-table>
      <PaginationBar v-model:page="q.page" v-model:page-size="q.pageSize" :total="total" @change="load" />
    </SectionCard>
    <el-drawer v-model="drawer" size="min(560px,100%)" title="房产详情"><template v-if="detail"><el-descriptions :column="1" border><el-descriptions-item label="房产">{{ detail.propertyNo }}</el-descriptions-item><el-descriptions-item label="位置">{{ detail.communityName }} · {{ detail.buildingName }} · {{ detail.unitName }}</el-descriptions-item><el-descriptions-item label="户型 / 面积">{{ displayLayoutType(detail.layoutType) }} / {{ detail.grossArea || '未填' }}{{ detail.grossArea ? '㎡' : '' }}</el-descriptions-item><el-descriptions-item label="入住状态"><StatusTag :value="detail.occupancyStatus" /></el-descriptions-item></el-descriptions><h3>当前产权人</h3><el-table :data="detail.owners"><el-table-column prop="fullName" label="姓名" /><el-table-column prop="phone" label="电话" /><el-table-column label="关系"><template #default="{ row }">{{ displayEnum(row.relationshipRole) }}</template></el-table-column></el-table><h3>当前居住人</h3><el-table :data="detail.residents"><el-table-column prop="fullName" label="姓名" /><el-table-column prop="phone" label="电话" /><el-table-column label="居住关系"><template #default="{ row }">{{ displayEnum(row.relationshipRole) }}</template></el-table-column></el-table><h3>有效计量表</h3><el-table :data="detail.activeMeters"><el-table-column prop="feeName" label="类型" /><el-table-column prop="meterNo" label="表号" /><el-table-column prop="initialReading" label="初始读数" /></el-table></template></el-drawer>
  </section>
</template>

<style scoped>
.property-page{display:grid;gap:18px}.filters{display:grid;grid-template-columns:1.45fr repeat(2,minmax(130px,1fr)) auto auto;gap:10px;margin-bottom:16px}h3{margin:22px 0 10px;color:var(--ink);font-size:15px}@media(max-width:900px){.filters{grid-template-columns:1fr 1fr}}@media(max-width:560px){.filters{grid-template-columns:1fr}}
</style>
