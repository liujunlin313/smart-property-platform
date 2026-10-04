<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Compass, EditPen, Fold, House, OfficeBuilding, Plus, Refresh, Search
} from '@element-plus/icons-vue'
import AssetNodeDialog from '../components/AssetNodeDialog.vue'
import PropertyDrawer from '../components/PropertyDrawer.vue'
import { apiMessage } from '../api/http.js'
import {
  getBuilding, getBuildings, getBuildingUnit, getBuildingUnits,
  getCommunities, getCommunity, getLayoutTemplates, getProperties,
  getProperty, getSummary
} from '../api/assets.js'
import { useAuth } from '../stores/auth.js'
import { displayLayoutType } from '../constants/presentation.js'

const auth = useAuth()
const treeRef = ref()
const loadingTree = ref(false)
const loadingProperties = ref(false)
const hierarchy = ref([])
const selectedNode = ref(null)
const properties = ref([])
const layouts = ref([])
const total = ref(0)
const summary = reactive({ communityCount: 0, buildingCount: 0, buildingUnitCount: 0, propertyCount: 0, activePropertyCount: 0, draftPropertyCount: 0 })
const filters = reactive({ keyword: '', status: '', occupancyStatus: '', layoutTemplateId: null, page: 1, pageSize: 20 })

const nodeDialog = reactive({ visible: false, type: 'community', mode: 'create', parent: null, initial: null })
const propertyDrawer = reactive({ visible: false, mode: 'create', parentUnit: null, initial: null, readOnly: false })

const selectedTitle = computed(() => selectedNode.value?.label || '全部房产')
const selectedMeta = computed(() => {
  const node = selectedNode.value
  if (!node) return '跨小区资产总览'
  if (node.type === 'community') return `${node.raw.addressLine} · ${node.raw.buildingCount} 栋楼`
  if (node.type === 'building') return `${node.communityName} · ${node.raw.unitCount} 个单元`
  return `${node.communityName} / ${node.buildingName} · ${node.raw.propertyCount} 套房产`
})
const breadcrumb = computed(() => selectedNode.value?.trail || ['全部资产'])
const addChildLabel = computed(() => ({ community: '新增楼栋', building: '新增单元', unit: '录入房产' })[selectedNode.value?.type])

const statusMap = {
  DRAFT: { label: '草稿', type: 'warning' },
  ACTIVE: { label: '启用', type: 'success' },
  INACTIVE: { label: '停用', type: 'info' },
  ARCHIVED: { label: '归档', type: 'info' }
}
const occupancyMap = {
  UNKNOWN: '未确认', VACANT: '无人居住', OWNER_OCCUPIED: '房东自住',
  AVAILABLE_FOR_RENT: '正在招租', TENANT_OCCUPIED: '租客居住'
}

onMounted(async () => {
  await Promise.all([loadSummary(), loadLayouts(), loadHierarchy()])
  await loadProperties()
})

async function loadSummary() {
  try { Object.assign(summary, await getSummary()) } catch (error) { ElMessage.error(apiMessage(error, '统计数据加载失败')) }
}

async function loadLayouts() {
  try { layouts.value = await getLayoutTemplates() } catch (error) { ElMessage.error(apiMessage(error, '户型字典加载失败')) }
}

async function loadHierarchy(preserveKey = selectedNode.value?.key) {
  loadingTree.value = true
  try {
    const communities = await getCommunities()
    const nodes = await Promise.all(communities.map(async community => {
      const buildings = await getBuildings({ communityId: community.communityId })
      const buildingNodes = await Promise.all(buildings.map(async building => {
        const units = await getBuildingUnits({ buildingId: building.buildingId })
        return {
          key: `building-${building.buildingId}`,
          type: 'building', id: building.buildingId,
          label: building.buildingName || `${building.buildingNo}号楼`,
          subtitle: `${building.unitCount} 单元 · ${building.propertyCount} 套`, raw: building,
          communityName: community.communityName,
          trail: [community.communityName, building.buildingName || `${building.buildingNo}号楼`],
          children: units.map(unit => ({
            key: `unit-${unit.buildingUnitId}`,
            type: 'unit', id: unit.buildingUnitId,
            label: unit.unitName || `${unit.unitNo}单元`,
            subtitle: `${unit.propertyCount} 套`, raw: unit,
            communityName: community.communityName,
            buildingName: building.buildingName || `${building.buildingNo}号楼`,
            trail: [community.communityName, building.buildingName || `${building.buildingNo}号楼`, unit.unitName || `${unit.unitNo}单元`],
            children: []
          }))
        }
      }))
      return {
        key: `community-${community.communityId}`,
        type: 'community', id: community.communityId,
        label: community.communityName,
        subtitle: `${community.buildingCount} 栋 · ${community.propertyCount} 套`, raw: community,
        communityName: community.communityName,
        trail: [community.communityName], children: buildingNodes
      }
    }))
    hierarchy.value = nodes
    const restored = findNode(nodes, preserveKey) || nodes[0] || null
    selectedNode.value = restored
    await nextTick()
    if (restored) treeRef.value?.setCurrentKey(restored.key)
  } catch (error) {
    ElMessage.error(apiMessage(error, '资产层级加载失败'))
  } finally {
    loadingTree.value = false
  }
}

function findNode(nodes, key) {
  if (!key) return null
  for (const node of nodes) {
    if (node.key === key) return node
    const child = findNode(node.children || [], key)
    if (child) return child
  }
  return null
}

function scopeParams() {
  const node = selectedNode.value
  if (!node) return {}
  if (node.type === 'community') return { communityId: node.id }
  if (node.type === 'building') return { buildingId: node.id }
  return { buildingUnitId: node.id }
}

async function loadProperties() {
  loadingProperties.value = true
  try {
    const data = await getProperties({
      ...scopeParams(),
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      occupancyStatus: filters.occupancyStatus || undefined,
      layoutTemplateId: filters.layoutTemplateId || undefined,
      page: filters.page,
      pageSize: filters.pageSize,
      sortBy: 'updatedAt',
      sortDirection: 'desc'
    })
    properties.value = data.items
    total.value = data.total
  } catch (error) {
    ElMessage.error(apiMessage(error, '房产列表加载失败'))
  } finally {
    loadingProperties.value = false
  }
}

async function selectNode(node) {
  selectedNode.value = node
  filters.page = 1
  await loadProperties()
}

function search() { filters.page = 1; loadProperties() }
function resetFilters() { Object.assign(filters, { keyword: '', status: '', occupancyStatus: '', layoutTemplateId: null, page: 1 }); loadProperties() }

function addCommunity() {
  Object.assign(nodeDialog, { visible: true, type: 'community', mode: 'create', parent: null, initial: null })
}

function addChild() {
  const node = selectedNode.value
  if (!node) return
  if (node.type === 'unit') return openPropertyCreate(node)
  Object.assign(nodeDialog, {
    visible: true,
    type: node.type === 'community' ? 'building' : 'unit',
    mode: 'create',
    parent: node,
    initial: null
  })
}

async function editSelected() {
  if (!selectedNode.value) return
  const node = selectedNode.value
  try {
    const initial = node.type === 'community'
      ? await getCommunity(node.id)
      : node.type === 'building'
        ? await getBuilding(node.id)
        : await getBuildingUnit(node.id)
    Object.assign(nodeDialog, { visible: true, type: node.type, mode: 'edit', parent: null, initial })
  } catch (error) {
    ElMessage.error(apiMessage(error))
  }
}

function openPropertyCreate(unitNode = selectedNode.value) {
  if (!unitNode || unitNode.type !== 'unit') {
    ElMessage.warning('请先在左侧选择一个楼栋单元')
    return
  }
  Object.assign(propertyDrawer, {
    visible: true, mode: 'create', readOnly: false, initial: null,
    parentUnit: { id: unitNode.id, label: unitNode.label, communityName: unitNode.communityName, buildingName: unitNode.buildingName }
  })
}

async function openProperty(row, edit = false) {
  try {
    const initial = await getProperty(row.propertyUnitId)
    Object.assign(propertyDrawer, {
      visible: true,
      mode: 'edit',
      readOnly: !edit || !auth.isAdmin.value,
      initial,
      parentUnit: null
    })
  } catch (error) {
    ElMessage.error(apiMessage(error))
  }
}

async function afterSaved() {
  await Promise.all([loadSummary(), loadHierarchy()])
  await loadProperties()
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
</script>

<template>
  <div class="assets-view">
      <section class="summary-grid">
        <article class="summary-card primary"><div><span>管理小区</span><b>{{ summary.communityCount }}</b><small>个有效小区</small></div><el-icon><Compass /></el-icon></article>
        <article class="summary-card"><div><span>楼栋规模</span><b>{{ summary.buildingCount }}</b><small>{{ summary.buildingUnitCount }} 个单元</small></div><el-icon><OfficeBuilding /></el-icon></article>
        <article class="summary-card"><div><span>房产总量</span><b>{{ summary.propertyCount }}</b><small>{{ summary.activePropertyCount }} 套已启用</small></div><el-icon><House /></el-icon></article>
        <article class="summary-card draft"><div><span>待补资料</span><b>{{ summary.draftPropertyCount }}</b><small>面积未填的草稿</small></div><el-icon><EditPen /></el-icon></article>
      </section>

      <section class="asset-layout">
        <aside class="hierarchy-panel panel">
          <div class="panel-heading">
            <div><p class="eyebrow">HIERARCHY</p><h2>资产层级</h2></div>
            <el-button v-if="auth.isAdmin.value" circle :icon="Plus" title="新增小区" @click="addCommunity" />
          </div>
          <div class="hierarchy-tools"><el-input placeholder="在层级中定位" :prefix-icon="Search" disabled /><el-button :icon="Refresh" @click="loadHierarchy()" /></div>
          <el-scrollbar class="tree-scroll" v-loading="loadingTree">
            <el-tree ref="treeRef" :data="hierarchy" node-key="key" default-expand-all highlight-current :expand-on-click-node="false" @node-click="selectNode">
              <template #default="{ data }">
                <div class="tree-node">
                  <span class="node-icon" :class="data.type"><el-icon><OfficeBuilding v-if="data.type === 'community' || data.type === 'building'" /><Fold v-else /></el-icon></span>
                  <span class="node-copy"><b>{{ data.label }}</b><small>{{ data.subtitle }}</small></span>
                </div>
              </template>
            </el-tree>
            <div v-if="!loadingTree && !hierarchy.length" class="tree-empty"><OfficeBuilding /><b>还没有小区</b><span>管理员可以从这里建立第一条资产链</span></div>
          </el-scrollbar>
          <div class="selection-card" v-if="selectedNode">
            <span>当前节点</span><b>{{ selectedNode.label }}</b><small>{{ selectedNode.subtitle }}</small>
            <div v-if="auth.isAdmin.value"><el-button size="small" @click="editSelected">编辑</el-button><el-button size="small" type="primary" @click="addChild">{{ addChildLabel }}</el-button></div>
          </div>
        </aside>

        <section class="property-panel panel">
          <div class="property-heading">
            <div>
              <el-breadcrumb separator="/"><el-breadcrumb-item v-for="item in breadcrumb" :key="item">{{ item }}</el-breadcrumb-item></el-breadcrumb>
              <h2>{{ selectedTitle }}</h2><p>{{ selectedMeta }}</p>
            </div>
            <el-button v-if="auth.isAdmin.value && selectedNode?.type === 'unit'" type="primary" :icon="Plus" @click="openPropertyCreate()">录入房产</el-button>
            <el-tag v-else-if="auth.isViewer.value" type="info" effect="plain">只读演示模式</el-tag>
          </div>

          <div class="filter-bar">
            <el-input v-model="filters.keyword" :prefix-icon="Search" clearable placeholder="房号、小区、楼栋或户型" @keyup.enter="search" />
            <el-select v-model="filters.status" clearable placeholder="资料状态"><el-option label="草稿" value="DRAFT" /><el-option label="启用" value="ACTIVE" /><el-option label="停用" value="INACTIVE" /><el-option label="归档" value="ARCHIVED" /></el-select>
            <el-select v-model="filters.occupancyStatus" clearable placeholder="居住状态"><el-option label="未确认" value="UNKNOWN" /><el-option label="无人居住" value="VACANT" /><el-option label="房东自住" value="OWNER_OCCUPIED" /></el-select>
            <el-select v-model="filters.layoutTemplateId" clearable filterable placeholder="户型"><el-option v-for="layout in layouts" :key="layout.layoutTemplateId" :value="layout.layoutTemplateId" :label="layout.layoutName" /></el-select>
            <el-button type="primary" @click="search">查询</el-button><el-button @click="resetFilters">重置</el-button>
          </div>

          <div class="table-wrap">
            <el-table :data="properties" v-loading="loadingProperties" row-key="propertyUnitId" height="100%" @row-dblclick="row => openProperty(row)">
              <el-table-column label="房产门牌" min-width="210">
                <template #default="{ row }"><div class="property-name"><span class="house-icon"><House /></span><div><b>{{ row.buildingName || `${row.buildingNo}号楼` }} · {{ row.unitName || `${row.unitNo}单元` }} · {{ row.propertyNo }}</b><small>{{ row.communityName }}</small></div></div></template>
              </el-table-column>
              <el-table-column label="楼层" width="78"><template #default="{ row }">{{ row.floorNo ?? '—' }}</template></el-table-column>
              <el-table-column label="户型" min-width="150"><template #default="{ row }"><b class="layout-name">{{ row.layoutName || '未选择' }}</b><small class="layout-code">{{ displayLayoutType(row.layoutType) }}</small></template></el-table-column>
              <el-table-column label="面积" width="130"><template #default="{ row }"><span v-if="row.grossArea"><b>{{ row.grossArea }}</b> ㎡</span><span v-else class="area-missing">未填</span><small class="usable" v-if="row.usableArea">套内 {{ row.usableArea }}㎡</small></template></el-table-column>
              <el-table-column label="资料状态" width="105"><template #default="{ row }"><el-tag size="small" :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.label }}</el-tag></template></el-table-column>
              <el-table-column label="居住状态" width="110"><template #default="{ row }">{{ occupancyMap[row.occupancyStatus] }}</template></el-table-column>
              <el-table-column label="户型图" width="90"><template #default="{ row }"><span :class="['file-indicator', { ready: row.floorPlanFileId }]">{{ row.floorPlanFileId ? '已上传' : '未上传' }}</span></template></el-table-column>
              <el-table-column label="更新时间" width="125"><template #default="{ row }">{{ formatDate(row.updatedAt) }}</template></el-table-column>
              <el-table-column label="操作" width="145" fixed="right">
                <template #default="{ row }"><el-button link type="primary" @click="openProperty(row)">详情</el-button><el-button v-if="auth.isAdmin.value" link type="primary" @click="openProperty(row, true)">编辑</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="当前层级没有符合条件的房产" /></template>
            </el-table>
          </div>
          <div class="pagination-row"><span>共 {{ total }} 套房产</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.pageSize" layout="prev, pager, next" :total="total" @current-change="loadProperties" /></div>
        </section>
      </section>
    <AssetNodeDialog v-model:visible="nodeDialog.visible" :type="nodeDialog.type" :mode="nodeDialog.mode" :parent="nodeDialog.parent" :initial="nodeDialog.initial" @saved="afterSaved" />
    <PropertyDrawer v-model:visible="propertyDrawer.visible" :mode="propertyDrawer.mode" :parent-unit="propertyDrawer.parentUnit" :initial="propertyDrawer.initial" :layouts="layouts" :read-only="propertyDrawer.readOnly" @saved="afterSaved" />
  </div>
</template>

<style scoped>
.summary-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:15px; margin-bottom:17px; }.summary-card { min-height:122px; padding:20px 21px; display:flex; justify-content:space-between; align-items:center; background:white; border:1px solid #dce6e9; border-radius:16px; box-shadow:0 9px 22px rgba(29,54,69,.05); }.summary-card span,.summary-card small { display:block; color:#718692; font-size:12px; }.summary-card b { display:block; margin:8px 0 6px; color:#14394a; font:600 32px/1 Georgia; }.summary-card > .el-icon { width:43px; height:43px; color:#167d72; background:#e6f4f1; border-radius:13px; font-size:21px; }.summary-card.primary { color:white; background:linear-gradient(135deg,#146b66,#168779); border:0; }.summary-card.primary span,.summary-card.primary small { color:#c5e7e1; }.summary-card.primary b { color:white; }.summary-card.primary > .el-icon { color:white; background:rgba(255,255,255,.15); }.summary-card.draft > .el-icon { color:#b98129; background:#fff3df; }
.asset-layout { height:calc(100vh - 235px); min-height:590px; display:grid; grid-template-columns:300px minmax(0,1fr); gap:17px; }.hierarchy-panel,.property-panel { min-height:0; display:flex; flex-direction:column; }.hierarchy-panel { padding:20px 16px 16px; }.panel-heading,.property-heading { display:flex; justify-content:space-between; align-items:flex-start; }.panel-heading { padding:0 5px; }.panel-heading h2,.property-heading h2 { margin:0; color:#183b4a; font-size:20px; }.hierarchy-tools { display:grid; grid-template-columns:1fr auto; gap:7px; margin:18px 0 11px; }.tree-scroll { flex:1; min-height:0; }.tree-node { width:100%; padding:5px 0; display:flex; align-items:center; gap:9px; }.node-icon { width:31px; height:31px; flex:0 0 auto; display:grid; place-items:center; color:#4c7d7a; background:#e9f3f1; border-radius:9px; }.node-icon.building { color:#58778b; background:#edf2f5; }.node-icon.unit { width:27px; height:27px; color:#7a8792; background:#f1f3f5; }.node-copy { min-width:0; flex:1; }.node-copy b,.node-copy small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.node-copy b { color:#294b59; font-size:13px; }.node-copy small { margin-top:2px; color:#8a9aa2; font-size:10px; }.selection-card { margin-top:11px; padding:13px; background:#f3f7f7; border:1px solid #e1e9eb; border-radius:12px; }.selection-card > span,.selection-card > small { display:block; color:#78909a; font-size:10px; }.selection-card > b { display:block; margin:4px 0; color:#204859; }.selection-card > div { margin-top:10px; display:flex; justify-content:flex-end; }.tree-empty { height:240px; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:8px; color:#83949c; text-align:center; }.tree-empty svg { width:32px; }.tree-empty span { max-width:190px; font-size:11px; line-height:1.5; }
.property-panel { padding:20px 20px 13px; }.property-heading { padding-bottom:17px; border-bottom:1px solid #e8eef0; }.property-heading h2 { margin-top:9px; font-size:23px; }.property-heading p { margin:5px 0 0; color:#738994; font-size:12px; }.filter-bar { display:grid; grid-template-columns:minmax(190px,1.6fr) repeat(3,minmax(120px,1fr)) auto auto; gap:9px; padding:15px 0; }.table-wrap { flex:1; min-height:0; border:1px solid #e3eaed; border-radius:12px; overflow:hidden; }.pagination-row { padding-top:12px; display:flex; justify-content:space-between; align-items:center; color:#71858f; font-size:12px; }
.property-name { display:flex; align-items:center; gap:10px; }.house-icon { width:34px; height:34px; flex:0 0 auto; display:grid; place-items:center; color:#167d72; background:#e8f4f2; border-radius:10px; }.property-name b,.property-name small { display:block; }.property-name b { color:#214455; font-size:13px; }.property-name small { margin-top:4px; color:#8797a0; font-size:10px; }.layout-name,.layout-code,.usable { display:block; }.layout-name { color:#31505e; font-size:12px; }.layout-code,.usable { margin-top:3px; color:#8b9aa1; font-size:10px; }.area-missing { padding:3px 7px; color:#a27127; background:#fff4dc; border-radius:6px; font-size:11px; }.file-indicator { color:#9aa5aa; font-size:11px; }.file-indicator.ready { color:#167d72; font-weight:700; }
:deep(.el-tree-node__content) { height:auto; min-height:42px; border-radius:9px; }:deep(.el-tree-node.is-current > .el-tree-node__content) { background:#e6f2f0; }:deep(.el-table th.el-table__cell) { color:#627987; background:#f7f9fa; font-size:11px; }:deep(.el-table td.el-table__cell) { padding:11px 0; }:deep(.el-breadcrumb__inner) { color:#83949c !important; font-size:11px; font-weight:400 !important; }
@media (max-width:1350px) { .asset-layout { grid-template-columns:280px minmax(0,1fr); }.filter-bar { grid-template-columns:1.4fr repeat(3,1fr); }.filter-bar .el-button { margin-left:0; }.summary-card { padding:17px; } }
</style>
