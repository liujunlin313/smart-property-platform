<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiMessage } from '../../api/http.js'
import { getNotificationDetail, getSentNotifications, sendNotification } from '../../api/notification.js'
import { getActiveFeeTypes } from '../../api/fee.js'
import PaginationBar from '../../components/common/PaginationBar.vue'
import NotificationTargetPicker from '../../components/notification/NotificationTargetPicker.vue'

const loading = ref(false); const sending = ref(false); const history = ref({ records: [], total: 0 }); const detail = ref(null); const detailVisible = ref(false); const feeTypes = ref([])
const form = reactive({ title: '', category: 'PROPERTY_NOTICE', feeTypeId: null, content: '', to: [], cc: [] })
const categoryLabels = { PROPERTY_NOTICE: '物业通知', MAINTENANCE: '维修施工', BILLING: '收费通知', EMERGENCY: '紧急通知', OTHER: '其他' }
const query = reactive({ page: 1, pageSize: 10 })
const toCount = computed(() => form.to.length); const ccCount = computed(() => form.cc.length)
async function loadHistory() { loading.value = true; try { history.value = await getSentNotifications(query) } catch (error) { ElMessage.error(apiMessage(error, '发送记录加载失败')) } finally { loading.value = false } }
function payloadTargets(items) { return items.reduce((groups, item) => { const group = groups.find(value => value.targetType === item.targetType); if (group) group.targetIds.push(item.targetId); else groups.push({ targetType: item.targetType, targetIds: [item.targetId] }); return groups }, []) }
async function send() {
  if (!form.title.trim() || !form.content.trim() || !form.to.length) return ElMessage.warning('请填写标题、正文并选择至少一个主要接收对象')
  sending.value = true
  try { await sendNotification({ title: form.title, content: form.content, category: form.category, feeTypeId: form.category === 'BILLING' ? form.feeTypeId : null, toTargets: payloadTargets(form.to), ccTargets: payloadTargets(form.cc) }); ElMessage.success('通知已发送；重复人员已按 TO 优先自动合并'); Object.assign(form, { title: '', category: 'PROPERTY_NOTICE', feeTypeId: null, content: '', to: [], cc: [] }); query.page = 1; await loadHistory() } catch (error) { ElMessage.error(apiMessage(error, '通知发送失败')) } finally { sending.value = false }
}
async function openDetail(row) { try { detail.value = await getNotificationDetail(row.notificationId); detailVisible.value = true } catch (error) { ElMessage.error(apiMessage(error, '通知详情读取失败')) } }
function onPageChange(value) { query.page = value.page; query.pageSize = value.pageSize; loadHistory() }
onMounted(async () => { await Promise.all([loadHistory(), getActiveFeeTypes().then(rows => { feeTypes.value = rows })]) })
</script>

<template>
  <section class="notification-page">
    <header class="page-title"><p>PROPERTY NOTICE CENTER</p><h2>发送与发送记录</h2><span>发送范围由后端按当前账号的小区作用域验证；同一人员同时命中 TO 与 CC 时仅保留 TO。</span></header>
    <div class="workspace-grid">
      <section class="panel compose"><h3>发送通知</h3><el-form label-position="top"><el-form-item label="通知标题"><el-input v-model="form.title" maxlength="160" show-word-limit placeholder="例如：1、2号楼除雪作业提醒"/></el-form-item><el-form-item label="通知类型"><el-select v-model="form.category" @change="form.feeTypeId = null"><el-option v-for="(label, value) in categoryLabels" :key="value" :label="label" :value="value"/></el-select></el-form-item><el-form-item v-if="form.category === 'BILLING'" label="收费类别"><el-select v-model="form.feeTypeId" clearable placeholder="全部收费类别"><el-option label="全部" :value="null"/><el-option v-for="item in feeTypes" :key="item.feeTypeId" :label="item.feeName" :value="item.feeTypeId"/></el-select><p class="charge-hint">收费通知按当前到期未结清账单生成每位接收人的独立数据；部分支付的多费用项账单不会伪造费用项欠费。</p></el-form-item><el-form-item :label="form.category === 'BILLING' ? '公共说明' : '通知正文'"><el-input v-model="form.content" type="textarea" :rows="6" maxlength="20000" show-word-limit placeholder="请说明时间、地点、事项与注意事项。"/></el-form-item></el-form>
        <div class="target-block"><b>主要接收对象 TO</b><NotificationTargetPicker v-model="form.to" placeholder="点击浏览或搜索主要接收对象"/></div>
        <div class="target-block"><b>抄送对象 CC</b><NotificationTargetPicker v-model="form.cc" placeholder="可选：点击浏览或搜索抄送对象"/></div>
        <p class="recipient-hint">当前选择：TO {{ toCount }} 个业务目标，CC {{ ccCount }} 个业务目标。实际人员在服务端实时展开并安全去重。</p><el-button type="primary" :loading="sending" @click="send">发送通知</el-button>
      </section>
      <section v-loading="loading" class="panel sent"><div class="section-head"><h3>已发送</h3><span>仅显示当前发送人的记录</span></div><el-table :data="history.records" empty-text="暂无已发送通知"><el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip/><el-table-column prop="category" label="类型" width="110"><template #default="{row}">{{ categoryLabels[row.category] || row.category }}</template></el-table-column><el-table-column prop="sentAt" label="发送时间" width="175"/><el-table-column label="接收人" width="110"><template #default="{row}">TO {{ row.toCount }} / CC {{ row.ccCount }}</template></el-table-column><el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="primary" @click="openDetail(row)">查看</el-button></template></el-table-column></el-table><PaginationBar :total="history.total" :page="query.page" :page-size="query.pageSize" @change="onPageChange"/></section>
    </div>
    <el-dialog v-model="detailVisible" title="通知详情" width="640px"><template v-if="detail"><h3>{{ detail.title }}</h3><p class="muted">{{ categoryLabels[detail.category] || detail.category }} · {{ detail.sentAt }} · {{ detail.senderUsername }}</p><p class="content">{{ detail.content }}</p><h4>最终接收人（{{ detail.recipients?.length || 0 }}）</h4><div class="tags"><el-tag v-for="item in detail.recipients" :key="item.personId" :type="item.recipientRole === 'TO' ? 'success' : 'info'">{{ item.recipientRole }} · {{ item.fullName }}</el-tag></div></template></el-dialog>
  </section>
</template>

<style scoped>
.notification-page{display:grid;gap:18px}.page-title p{margin:0 0 6px;color:#158176;font:700 11px/1.2 Consolas,monospace;letter-spacing:.14em}.page-title h2{margin:0;color:#143b4a;font:500 28px Georgia,"Noto Serif SC",serif}.page-title span{display:block;margin-top:8px;color:#748993;font-size:13px}.workspace-grid{display:grid;grid-template-columns:minmax(330px,.95fr) minmax(520px,1.35fr);gap:18px}.panel{padding:22px;background:#fff;border:1px solid #dce7ea;border-radius:14px;box-shadow:0 6px 22px rgba(24,65,72,.04)}.panel h3{margin:0 0 17px;color:#1b4652}.target-block{display:grid;gap:9px;margin:16px 0}.target-block b{font-size:13px;color:#335865}.tags{display:flex;flex-wrap:wrap;gap:7px}.recipient-hint,.muted,.charge-hint{color:#78909a;font-size:12px}.charge-hint{margin:8px 0 0;line-height:1.55}.section-head{display:flex;align-items:center;justify-content:space-between}.section-head span{color:#82949b;font-size:12px}.content{white-space:pre-wrap;line-height:1.8;color:#385660}.sent :deep(.pagination-bar){margin-top:14px}.sent :deep(.el-table){min-height:390px}@media(max-width:1100px){.workspace-grid{grid-template-columns:1fr}}
</style>
