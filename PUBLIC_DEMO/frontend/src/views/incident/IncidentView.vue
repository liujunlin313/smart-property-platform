<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { incidentApi } from '../../api/stageH.js'
import { apiMessage } from '../../api/http.js'
import EmptyState from '../../components/common/EmptyState.vue'
import PageHeader from '../../components/common/PageHeader.vue'
import SectionCard from '../../components/common/SectionCard.vue'
import StatCard from '../../components/common/StatCard.vue'
import StatusTag from '../../components/common/StatusTag.vue'

const rows = ref([])
const loading = ref(false)
const openCount = computed(() => rows.value.filter((item) => item.status === 'OPEN').length)
const processingCount = computed(() => rows.value.filter((item) => item.status === 'PROCESSING').length)
const resolvedCount = computed(() => rows.value.filter((item) => item.status === 'RESOLVED').length)

async function load() {
  loading.value = true
  try { rows.value = (await incidentApi.list()).data } catch (error) { ElMessage.error(apiMessage(error)) } finally { loading.value = false }
}
async function change(row, status) {
  try {
    await incidentApi.status(row.residentIncidentId, { status })
    ElMessage.success(status === 'PROCESSING' ? '已开始处理该事项' : '该事项已标记为已解决')
    await load()
  } catch (error) { ElMessage.error(apiMessage(error)) }
}
onMounted(load)
</script>

<template>
  <section class="incident-page">
    <PageHeader title="居民服务事项" description="集中查看居民上报的问题，及时跟进并留下清晰的处理状态。">
      <template #actions><el-button :loading="loading" @click="load">刷新列表</el-button></template>
    </PageHeader>
    <div class="stat-grid">
      <StatCard label="待处理" :value="openCount" hint="需要安排处理" tone="warning" />
      <StatCard label="处理中" :value="processingCount" hint="已有人员跟进" tone="primary" />
      <StatCard label="已解决" :value="resolvedCount" hint="本列表累计" tone="success" />
    </div>
    <SectionCard title="事项列表" description="状态变化会同步写回服务记录。">
      <EmptyState v-if="!loading && rows.length === 0" title="暂时没有居民上报事项" description="居民在服务门户提交的问题会显示在这里。" />
      <el-table v-else v-loading="loading" :data="rows" class="business-table">
        <el-table-column prop="fullLocation" label="关联房产" min-width="185" />
        <el-table-column prop="category" label="事项类别" min-width="115"><template #default="{ row }"><StatusTag :value="row.category" /></template></el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="status" label="当前状态" min-width="115"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column prop="createdAt" label="上报时间" min-width="165" />
        <el-table-column label="处理操作" width="150" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'OPEN'" link type="primary" @click="change(row, 'PROCESSING')">开始处理</el-button><el-button v-else-if="row.status === 'PROCESSING'" link type="success" @click="change(row, 'RESOLVED')">标记解决</el-button><span v-else class="muted">已完成</span></template></el-table-column>
      </el-table>
    </SectionCard>
  </section>
</template>

<style scoped>
.incident-page{display:grid;gap:18px}.stat-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.muted{color:var(--muted);font-size:13px}@media(max-width:760px){.stat-grid{grid-template-columns:1fr}}
</style>
