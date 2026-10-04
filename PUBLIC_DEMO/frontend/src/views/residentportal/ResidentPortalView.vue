<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuth } from '../../stores/auth.js'
import { residentApi } from '../../api/stageH.js'
import { apiMessage } from '../../api/http.js'
import { displayEnum, formatMoney, formatMonth } from '../../constants/presentation.js'
import EmptyState from '../../components/common/EmptyState.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import SectionCard from '../../components/common/SectionCard.vue'
import StatCard from '../../components/common/StatCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'

const route = useRoute(), router = useRouter(), auth = useAuth()
const data = ref({ summary: {}, residences: [], properties: [], bills: [], notifications: [], incidents: [] })
const loading = ref(false), form = ref({ propertyUnitId: null, category: 'LEAK', description: '' })
const page = computed(() => route.meta.portal || 'home')
const accountName = computed(() => auth.state.account?.username || '居民用户')
const titles = { home: '居民服务', residences: '当前居所', properties: '我的房产', bills: '我的账单', notifications: '我的通知', incidents: '异常上报' }
const descriptions = { home: '查看与自己有关的居住、账单和物业服务信息。', residences: '以下信息仅展示你当前有效的居住关系。', properties: '这里展示你名下的产权房产。', bills: '账单金额与状态均来自当前物业出账记录。', notifications: '重要缴费、物业服务及社区消息会显示在这里。', incidents: '提交后，物业工作人员会按流程跟进处理。' }
const firstResidence = computed(() => data.value.residences[0])
const outstandingAmount = computed(() => data.value.bills.reduce((sum, item) => sum + Number(item.outstandingAmount || 0), 0))
const billTrend = computed(() => { const rows = [...data.value.bills].slice(0, 6).reverse(); const maximum = Math.max(...rows.map(item => Number(item.receivableAmount || 0)), 1); return rows.map(item => ({ ...item, ratio: Math.max(8, Number(item.receivableAmount || 0) / maximum * 100) })) })
const quickLinks = [['/resident/bills', '查看账单'], ['/resident/incidents', '异常上报'], ['/resident/notifications', '我的通知']]

async function load() { loading.value = true; try { const [summary, residences, properties, bills, notifications, incidents] = await Promise.all([residentApi.summary(), residentApi.residences(), residentApi.properties(), residentApi.bills(), residentApi.notifications(), residentApi.incidents()]); data.value = { summary: summary.data, residences: residences.data, properties: properties.data, bills: bills.data, notifications: notifications.data, incidents: incidents.data }; if (!form.value.propertyUnitId) form.value.propertyUnitId = data.value.residences[0]?.propertyUnitId } catch (error) { ElMessage.error(apiMessage(error, '居民服务数据加载失败')) } finally { loading.value = false } }
async function report() { if (!form.value.propertyUnitId || !form.value.description.trim()) return ElMessage.warning('请选择关联居所并填写问题描述'); try { await residentApi.report(form.value); ElMessage.success('异常上报已提交，物业工作人员会按流程处理'); form.value = { propertyUnitId: data.value.residences[0]?.propertyUnitId, category: 'LEAK', description: '' }; load() } catch (error) { ElMessage.error(apiMessage(error, '异常上报失败')) } }
onMounted(load); watch(page, load)
</script>

<template>
  <section v-loading="loading" class="resident-page">
    <PageHeader :title="titles[page]" :description="descriptions[page]" />
    <template v-if="page === 'home'">
      <div class="home-summary">
        <section class="welcome-card"><p>您好，{{ accountName || '居民用户' }}</p><h2>{{ firstResidence?.fullLocation || '欢迎使用居民服务中心' }}</h2><span>{{ firstResidence ? `${displayEnum(firstResidence.residenceRole)} · ${displayEnum(firstResidence.occupancyStatus)}` : '居住与物业服务信息会在这里汇总展示。' }}</span></section>
        <div class="compact-stats"><StatCard label="当前居所" :value="data.summary.residenceCount || 0" helper="有效居住关系"/><StatCard label="待缴账单" :value="data.summary.outstandingBillCount || 0" :helper="outstandingAmount ? `合计 ¥ ${formatMoney(outstandingAmount)}` : '当前无待缴金额'" tone="warning"/><StatCard label="未读通知" :value="data.summary.unreadNotificationCount || 0" helper="及时查看物业消息"/></div>
      </div>
      <div class="home-grid">
        <SectionCard title="本月账单" description="账单状态和金额以物业出账记录为准。"><template #action><el-button link type="primary" @click="router.push('/resident/bills')">查看全部</el-button></template><div v-if="billTrend.length" class="bill-trend"><div v-for="item in billTrend" :key="item.billId" class="trend-item"><span class="bar" :style="{height:`${item.ratio}%`}"></span><b>{{ item.billingMonth }}月</b><small>¥ {{ formatMoney(item.receivableAmount) }}</small></div></div><EmptyState v-else title="当前没有账单记录" description="物业出账后，账单与缴费状态会在这里显示。" /></SectionCard>
        <SectionCard title="最近通知" description="物业发送的重要服务、缴费与社区消息。"><template #action><el-button link type="primary" @click="router.push('/resident/notifications')">查看全部</el-button></template><div v-if="data.notifications.length" class="notification-list"><article v-for="item in data.notifications.slice(0,3)" :key="item.notificationId"><div><StatusTag :value="item.category"/><b>{{ item.title }}</b></div><small>{{ item.sentAt }} · {{ item.readAt ? '已读' : '未读' }}</small></article></div><EmptyState v-else title="当前没有新的物业通知" description="重要缴费、物业服务及社区消息会在这里显示。" /></SectionCard>
      </div>
      <SectionCard title="快捷服务" description="常用操作会在新页面中完成。"><div class="quick-actions"><el-button v-for="item in quickLinks" :key="item[0]" @click="router.push(item[0])">{{ item[1] }}</el-button></div></SectionCard>
    </template>

    <template v-else-if="page === 'residences'">
      <div v-if="data.residences.length" class="property-cards"><article v-for="item in data.residences" :key="item.residenceId" class="residence-card"><div><p>{{ item.fullLocation }}</p><StatusTag :value="item.occupancyStatus"/></div><dl><div><dt>入住身份</dt><dd>{{ displayEnum(item.residenceRole) }}</dd></div><div><dt>入住日期</dt><dd>{{ item.startDate || '—' }}</dd></div><div><dt>预计结束</dt><dd>{{ item.expectedEndDate || '长期居住' }}</dd></div></dl></article></div><EmptyState v-else title="暂无当前居所" description="完成入住办理后，当前居所会显示在这里。" />
    </template>

    <template v-else-if="page === 'properties'">
      <div v-if="data.properties.length" class="property-cards"><article v-for="item in data.properties" :key="item.propertyUnitId" class="residence-card"><div><p>{{ item.fullLocation }}</p><StatusTag :value="item.occupancyStatus"/></div><dl><div><dt>建筑面积</dt><dd>{{ item.grossArea ? `${item.grossArea} ㎡` : '未填' }}</dd></div><div><dt>当前状态</dt><dd>{{ displayEnum(item.occupancyStatus) }}</dd></div></dl></article></div><EmptyState v-else title="暂无产权房产" description="产权关系生效后，相应房产会显示在这里。" />
    </template>

    <template v-else-if="page === 'bills'">
      <div class="bill-summary"><StatCard label="当前待缴" :value="`¥ ${formatMoney(outstandingAmount)}`" :helper="`${data.summary.outstandingBillCount || 0} 条待缴账单`" tone="warning"/><StatCard label="账单记录" :value="data.bills.length" helper="当前可查询的账单"/></div>
      <SectionCard title="账单列表" description="已缴、部分缴费、未缴与逾期状态均按实际账单展示。"><el-table v-if="data.bills.length" :data="data.bills"><el-table-column prop="fullLocation" label="房产" min-width="250"/><el-table-column label="账期" width="120"><template #default="{row}">{{ formatMonth(row.billingYear,row.billingMonth) }}</template></el-table-column><el-table-column label="应收" width="130"><template #default="{row}">¥ {{ formatMoney(row.receivableAmount) }}</template></el-table-column><el-table-column label="待缴" width="130"><template #default="{row}">¥ {{ formatMoney(row.outstandingAmount) }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.status"/></template></el-table-column></el-table><EmptyState v-else title="当前没有账单记录" description="物业完成出账后，账单会显示在这里。" /></SectionCard>
    </template>

    <template v-else-if="page === 'notifications'">
      <SectionCard title="通知列表" description="点击浏览每一条与自己有关的物业消息。"><div v-if="data.notifications.length" class="notification-list full"><article v-for="item in data.notifications" :key="item.notificationId"><div><StatusTag :value="item.category"/><b>{{ item.title }}</b></div><small>{{ item.sentAt }} · {{ item.readAt ? '已读' : '未读' }}</small></article></div><EmptyState v-else title="当前没有新的物业通知" description="重要缴费、物业服务及社区消息会在这里显示。" /></SectionCard>
    </template>

    <template v-else-if="page === 'incidents'">
      <div class="incident-grid"><SectionCard title="提交异常" description="请尽量说明问题位置和情况，便于物业工作人员跟进。"><el-form label-position="top"><el-form-item label="关联居所"><el-select v-model="form.propertyUnitId" placeholder="请选择关联居所"><el-option v-for="item in data.residences" :key="item.propertyUnitId" :value="item.propertyUnitId" :label="item.fullLocation"/></el-select></el-form-item><el-form-item label="异常类别"><el-select v-model="form.category"><el-option label="漏水" value="LEAK"/><el-option label="用电" value="ELECTRICITY"/><el-option label="燃气" value="GAS"/><el-option label="公共设施" value="PUBLIC_FACILITY"/><el-option label="其他" value="OTHER"/></el-select></el-form-item><el-form-item label="问题描述"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="请描述问题现象、出现时间及需要协助的内容。"/></el-form-item><el-button type="primary" @click="report">提交异常上报</el-button></el-form></SectionCard><SectionCard title="我的上报" description="物业处理进度会同步显示在这里。"><el-table v-if="data.incidents.length" :data="data.incidents"><el-table-column prop="fullLocation" label="位置" min-width="180"/><el-table-column label="类别" width="110"><template #default="{row}"><StatusTag :value="row.category"/></template></el-table-column><el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip/><el-table-column label="状态" width="110"><template #default="{row}"><StatusTag :value="row.status"/></template></el-table-column></el-table><EmptyState v-else title="你还没有提交异常" description="如遇到居住或公共设施问题，可从左侧表单提交。" /></SectionCard></div>
    </template>
  </section>
</template>
<style scoped>
.resident-page{display:grid;gap:18px}.home-summary{display:grid;grid-template-columns:minmax(0,1.1fr) minmax(0,1.4fr);gap:16px}.welcome-card{padding:24px;background:linear-gradient(135deg,#126a62,#1b8c7e);border-radius:16px;color:#fff;box-shadow:0 14px 30px rgba(18,106,98,.17)}.welcome-card p{margin:0;color:#d3efe9;font-size:13px}.welcome-card h2{max-width:500px;margin:10px 0 7px;font-size:22px;line-height:1.42}.welcome-card span{color:#dcf4ee;font-size:13px}.compact-stats,.bill-summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.home-grid,.incident-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.quick-actions{display:flex;flex-wrap:wrap;gap:10px}.bill-trend{height:168px;display:flex;align-items:end;gap:12px;padding:10px 2px 0}.trend-item{height:100%;display:grid;flex:1;grid-template-rows:1fr auto auto;align-items:end;gap:5px;min-width:0;text-align:center}.trend-item .bar{display:block;width:100%;max-width:38px;min-height:8px;justify-self:center;border-radius:8px 8px 3px 3px;background:linear-gradient(180deg,#5fafa3,#167d72)}.trend-item b{color:var(--ink-700);font-size:12px}.trend-item small{overflow:hidden;color:var(--ink-500);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.notification-list{display:grid;gap:10px}.notification-list article{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:11px 0;border-bottom:1px solid #edf2f1}.notification-list article:last-child{border-bottom:0}.notification-list article>div{display:flex;align-items:center;gap:8px;min-width:0}.notification-list b{overflow:hidden;color:var(--ink-700);font-size:13px;text-overflow:ellipsis;white-space:nowrap}.notification-list small{flex:0 0 auto;color:var(--ink-500);font-size:11px}.property-cards{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}.residence-card{padding:20px;background:#fff;border:1px solid var(--line);border-radius:15px;box-shadow:var(--shadow-card)}.residence-card>div{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.residence-card p{margin:0;color:var(--ink-900);font-size:16px;font-weight:700;line-height:1.55}.residence-card dl{display:flex;flex-wrap:wrap;gap:18px;margin:18px 0 0}.residence-card dt{color:var(--ink-500);font-size:11px}.residence-card dd{margin:4px 0 0;color:var(--ink-700);font-size:13px}.incident-grid{align-items:start}@media(max-width:900px){.home-summary,.home-grid,.incident-grid{grid-template-columns:1fr}.compact-stats{grid-template-columns:repeat(3,minmax(0,1fr))}}@media(max-width:620px){.compact-stats,.bill-summary,.property-cards{grid-template-columns:1fr}.notification-list article{align-items:flex-start;flex-direction:column;gap:6px}.notification-list small{padding-left:0}.welcome-card{padding:20px}}
</style>
