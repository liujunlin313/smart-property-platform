<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Location, RefreshRight, Search } from '@element-plus/icons-vue'
import { apiMessage } from '../../api/http.js'
import { getMapScene, getSpatialBoundary, reverseManagedCommunities, saveSpatialBoundary, searchNearbyPois, searchPoiCandidates, searchSpatialObjects, syncMissingMapLocations } from '../../api/map.js'
import AmapMap from '../../components/map/AmapMap.vue'

const loading = ref(false)
const syncing = ref(false)
const scene = ref({ communities: [], buildings: [], meta: {} })
const mapReady = ref(false)
const mapError = ref('')
const mapRef = ref(null)
const searchText = ref('')
const selectedObject = ref(null)
const selectedBoundary = ref(null)
const nearbyLoading = ref(false)
const poiLoading = ref(false)
const nearbyResult = ref(null)
const poiCandidates = ref([])
const selectedPoi = ref(null)
const reverseResult = ref(null)
const nearbyForm = reactive({ communityId: null, keyword: '', radiusMeters: 3000 })
const poiKeyword = ref('')
const locatedBuildings = computed(() => (scene.value.buildings || []).filter(item => item.locationReady).length)
const mapCommunities = computed(() => scene.value.communities || [])

async function loadScene() {
  loading.value = true
  try { scene.value = await getMapScene() } catch (error) { ElMessage.error(apiMessage(error, '空间地图数据加载失败')) } finally { loading.value = false }
}
async function syncMissing() {
  try {
    await ElMessageBox.confirm('将只对当前缺失坐标的 ACTIVE 楼栋调用高德定位；已有坐标不会被覆盖。是否继续？', '补全缺失楼栋坐标', { type: 'warning', confirmButtonText: '开始补全', cancelButtonText: '取消' })
  } catch { return }
  syncing.value = true
  try {
    const result = await syncMissingMapLocations()
    await loadScene()
    ElMessage.success(`定位完成：更新 ${result.updated} 栋，失败 ${result.failed} 栋${result.duplicateCoordinateGroups?.length ? '；已记录重复坐标告警' : ''}`)
  } catch (error) { ElMessage.error(apiMessage(error, '坐标补全失败')) } finally { syncing.value = false }
}
async function searchSuggestions(query, callback) {
  if (!query?.trim()) return callback([])
  try { callback((await searchSpatialObjects(query)).map(item => ({ value: item.label, ...item }))) } catch { callback([]) }
}
async function chooseObject(item) {
  selectedObject.value = item; selectedBoundary.value = null
  const focused = item.subjectType === 'COMMUNITY' ? mapRef.value?.focusCommunity(item.subjectId) : mapRef.value?.focusBuilding(item.subjectId)
  if (!focused) ElMessage.warning('该对象暂未具备可用中心坐标')
  try { const boundary = await getSpatialBoundary(item.subjectType, item.subjectId); selectedBoundary.value = boundary; if (boundary?.geoJson) mapRef.value?.showBoundary(boundary.geoJson) } catch (error) { if (error?.response?.status !== 404) ElMessage.error(apiMessage(error, '读取空间边界失败')) }
}
function beginCalibration() { if (!selectedObject.value) return ElMessage.info('请先搜索并选择一个小区或楼栋'); if (!mapRef.value?.startBoundaryDrawing()) ElMessage.error('地图绘制工具尚未准备完成') }
async function boundaryDrawn(geoJson) {
  const target = selectedObject.value; if (!target) return
  try { await ElMessageBox.confirm('确认保存刚绘制的真实边界吗？保存不会改动既有中心坐标。', '确认空间边界', { type: 'warning', confirmButtonText: '保存边界', cancelButtonText: '放弃' }); selectedBoundary.value = await saveSpatialBoundary(target.subjectType, target.subjectId, { geoJson, version: selectedBoundary.value?.version ?? 0 }); mapRef.value?.showBoundary(selectedBoundary.value.geoJson); ElMessage.success('空间边界已保存') } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(apiMessage(error, '边界保存失败')) }
}
async function runNearbySearch() {
  if (!nearbyForm.communityId || !nearbyForm.keyword.trim()) return ElMessage.info('请选择小区并输入设施关键字')
  nearbyLoading.value = true
  try {
    nearbyResult.value = await searchNearbyPois({ ...nearbyForm, keyword: nearbyForm.keyword.trim() })
    mapRef.value?.showSearchPois(nearbyResult.value.pois, true)
    if (!nearbyResult.value.pois?.length) ElMessage.info('当前半径内未找到匹配设施')
  } catch (error) { ElMessage.error(apiMessage(error, '周边设施搜索失败')) } finally { nearbyLoading.value = false }
}
async function loadPoiCandidates() {
  if (!poiKeyword.value.trim()) return ElMessage.info('请输入 POI 名称或关键字')
  poiLoading.value = true; selectedPoi.value = null; reverseResult.value = null
  try {
    poiCandidates.value = await searchPoiCandidates(poiKeyword.value.trim())
    mapRef.value?.showSearchPois(poiCandidates.value, false)
    if (!poiCandidates.value.length) ElMessage.info('未找到可定位的 POI')
  } catch (error) { ElMessage.error(apiMessage(error, 'POI 搜索失败')) } finally { poiLoading.value = false }
}
async function selectPoi(poi) {
  selectedPoi.value = poi; poiLoading.value = true
  try {
    mapRef.value?.showSearchPois([poi], true)
    reverseResult.value = await reverseManagedCommunities({ keyword: poiKeyword.value.trim(), poiId: poi.poiId })
  } catch (error) { ElMessage.error(apiMessage(error, '反查管理小区失败')) } finally { poiLoading.value = false }
}
function showPoiOnMap(poi) { mapRef.value?.showSearchPois([poi], true) }
function focusCommunityOnMap(community) { mapRef.value?.focusCommunity(community.communityId) }
onMounted(loadScene)
</script>

<template>
  <section v-loading="loading" class="map-page">
    <header class="map-header panel">
      <div><p class="eyebrow">SPATIAL MAP · GCJ-02</p><h1>管理范围空间地图</h1><p>地图底图使用高德 JS API；小区与楼栋位置、房产与入住统计均读取当前数据库。</p></div>
      <div class="header-actions"><el-button :icon="RefreshRight" @click="loadScene">刷新场景</el-button><el-button type="primary" :icon="Location" :loading="syncing" @click="syncMissing">补全缺失楼栋坐标</el-button></div>
    </header>
    <div class="map-summary"><article class="panel"><span>管理小区</span><b>{{ scene.communities?.length || 0 }}</b></article><article class="panel"><span>ACTIVE 楼栋</span><b>{{ scene.buildings?.length || 0 }}</b></article><article class="panel"><span>已定位楼栋</span><b>{{ locatedBuildings }}</b></article><article class="panel"><span>坐标系</span><b>{{ scene.meta?.coordinateSystem || 'GCJ-02' }}</b></article></div>
    <section class="panel map-panel">
      <div class="spatial-tools"><el-autocomplete v-model="searchText" :fetch-suggestions="searchSuggestions" placeholder="搜索本系统内的小区或楼栋" :prefix-icon="Search" clearable @select="chooseObject"/><el-button :icon="EditPen" :disabled="!selectedObject" @click="beginCalibration">校准所选边界</el-button><span v-if="selectedObject" class="selection-note">{{ selectedObject.label }} · {{ selectedBoundary?.geoJson ? '已加载真实边界' : '尚未校准边界，将仅高亮中心点' }}</span></div>
      <section class="poi-workbench" aria-label="高德 POI 空间检索">
        <el-tabs>
          <el-tab-pane label="从管理小区查周边设施">
            <el-form class="poi-form" @submit.prevent="runNearbySearch">
              <el-select v-model="nearbyForm.communityId" placeholder="选择管理小区"><el-option v-for="item in mapCommunities" :key="item.communityId" :label="item.communityName" :value="item.communityId" /></el-select>
              <el-input v-model="nearbyForm.keyword" placeholder="如：医院、学校、地铁" clearable @keyup.enter="runNearbySearch" />
              <el-select v-model="nearbyForm.radiusMeters" class="radius-select"><el-option label="1 公里" :value="1000" /><el-option label="3 公里" :value="3000" /><el-option label="5 公里" :value="5000" /><el-option label="10 公里" :value="10000" /></el-select>
              <el-button type="primary" :loading="nearbyLoading" native-type="submit">查询周边</el-button>
            </el-form>
            <div v-if="nearbyResult" class="poi-result"><p>以 {{ nearbyResult.communityName }} 为中心 · {{ nearbyResult.radiusMeters / 1000 }} 公里内的真实高德 POI</p><button v-for="poi in nearbyResult.pois" :key="poi.poiId" class="poi-row" @click="showPoiOnMap(poi)"><b>{{ poi.name }}</b><span>{{ poi.type || '未分类' }} · {{ poi.address || '地址未返回' }}</span></button><el-empty v-if="!nearbyResult.pois?.length" description="当前半径内暂无匹配设施" :image-size="56" /></div>
          </el-tab-pane>
          <el-tab-pane label="从 POI 反查管理小区">
            <el-form class="poi-form single" @submit.prevent="loadPoiCandidates"><el-input v-model="poiKeyword" placeholder="输入医院、地铁站、学校等 POI 名称" clearable @keyup.enter="loadPoiCandidates" /><el-button type="primary" :loading="poiLoading" native-type="submit">搜索 POI</el-button></el-form>
            <div v-if="poiCandidates.length" class="poi-result"><p>请先选择一个真实高德 POI，系统只返回你有权限管理的小区。</p><button v-for="poi in poiCandidates" :key="poi.poiId" class="poi-row" :class="{ active: selectedPoi?.poiId === poi.poiId }" @click="selectPoi(poi)"><b>{{ poi.name }}</b><span>{{ poi.type || '未分类' }} · {{ poi.address || '地址未返回' }}</span></button></div>
            <div v-if="reverseResult" class="community-result"><p><b>{{ reverseResult.poi.name }}</b> 周边的系统管理小区（直线距离）</p><button v-for="community in reverseResult.communities" :key="community.communityId" class="community-row" @click="focusCommunityOnMap(community)"><span><b>{{ community.communityName }}</b><small>{{ community.address || '地址未填写' }}</small></span><strong>{{ community.straightLineDistanceMeters >= 1000 ? `${(community.straightLineDistanceMeters / 1000).toFixed(1)} km` : `${community.straightLineDistanceMeters} m` }}</strong></button><el-empty v-if="!reverseResult.communities?.length" description="你的管理范围内暂无可定位小区" :image-size="56" /></div>
          </el-tab-pane>
        </el-tabs>
      </section>
      <AmapMap ref="mapRef" :scene="scene" @ready="mapReady=true" @error="mapError=$event" @boundary-drawn="boundaryDrawn"/>
      <footer><span><i :class="{online: mapReady, error: mapError}"/>{{ mapError || (mapReady ? '高德地图已连接' : '等待地图配置') }}</span><span>小比例尺显示小区，放大后显示楼栋；仅人工校准的真实 Polygon 才会显示为边界。</span></footer>
    </section>
  </section>
</template>

<style scoped>
.map-page{display:grid;gap:17px}.map-header{display:flex;align-items:center;justify-content:space-between;padding:23px 26px}.eyebrow{margin:0 0 7px;color:#168175;font-size:11px;font-weight:800;letter-spacing:.15em}.map-header h1{margin:0;color:#143a49;font:500 27px/1.2 Georgia,"Noto Serif SC",serif}.map-header p:not(.eyebrow){margin:9px 0 0;color:#748993;font-size:13px}.header-actions{display:flex;gap:10px}.map-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.map-summary article{padding:17px 19px}.map-summary span{display:block;color:#758b94;font-size:12px}.map-summary b{display:block;margin-top:8px;color:#173f4c;font-size:22px}.map-panel{padding:12px}.spatial-tools{display:flex;align-items:center;gap:10px;padding:2px 2px 12px}.spatial-tools :deep(.el-autocomplete){width:330px}.selection-note{color:#71858d;font-size:12px}.poi-workbench{margin:0 2px 14px;padding:0 12px;border:1px solid #e2ecea;border-radius:12px;background:#fbfdfd}.poi-workbench :deep(.el-tabs__header){margin-bottom:12px}.poi-form{display:grid;grid-template-columns:1.1fr 1.2fr .7fr auto;gap:9px;align-items:center}.poi-form.single{grid-template-columns:1fr auto}.radius-select{width:100%}.poi-result,.community-result{display:grid;gap:6px;max-height:195px;margin-top:12px;overflow:auto}.poi-result p,.community-result p{margin:0 0 4px;color:#667e88;font-size:12px}.poi-row,.community-row{display:flex;justify-content:space-between;gap:12px;width:100%;padding:9px 10px;border:1px solid #e4ecee;border-radius:8px;color:#31505b;background:#fff;text-align:left;cursor:pointer}.poi-row:hover,.poi-row.active,.community-row:hover{border-color:#7eb9ae;background:#f0f8f6}.poi-row b,.community-row b{color:#1d4b52}.poi-row span{overflow:hidden;color:#72868e;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.community-row span{display:grid;gap:3px}.community-row small{color:#72868e}.community-row strong{align-self:center;color:#167d72;font-size:13px;white-space:nowrap}.map-panel footer{display:flex;justify-content:space-between;padding:12px 10px 3px;color:#71858d;font-size:12px}.map-panel i{display:inline-block;width:7px;height:7px;margin-right:6px;border-radius:50%;background:#d49b39}.map-panel i.online{background:#1d9a77}.map-panel i.error{background:#c65858}@media(max-width:1000px){.map-header{align-items:flex-start;gap:15px;flex-direction:column}.map-summary{grid-template-columns:repeat(2,1fr)}.spatial-tools,.poi-form{align-items:stretch;grid-template-columns:1fr;flex-direction:column}.spatial-tools :deep(.el-autocomplete){width:100%}.map-panel footer{gap:8px;flex-direction:column}}
</style>
