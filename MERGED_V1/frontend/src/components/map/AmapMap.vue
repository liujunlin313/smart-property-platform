<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { amapJsConfigured, loadAmap } from '../../map/amapLoader.js'

const props = defineProps({ scene: { type: Object, default: () => ({ communities: [], buildings: [], meta: {} }) } })
const emit = defineEmits(['ready', 'error', 'boundary-drawn'])
const container = ref(null)
const configured = computed(amapJsConfigured)
const loading = ref(true)
const error = ref('')
let AMap = null
let map = null
let communityMarkers = []
let buildingMarkers = []
let poiMarkers = []
let infoWindow = null
let selectedPolygon = null
let mouseTool = null
let hasFittedFirstScene = false

const palette = ['#167d72', '#3987c9', '#c4843e', '#8b6fb8', '#d46075', '#3e9a8f']
const colorFor = id => palette[Number(id || 0) % palette.length]

function point(item) { return [Number(item.longitude), Number(item.latitude)] }
function ready(item) { return item?.locationReady && Number.isFinite(Number(item.longitude)) && Number.isFinite(Number(item.latitude)) }
function hasSceneLocations() { return [...(props.scene?.communities || []), ...(props.scene?.buildings || [])].some(ready) }

function markerLabel(text, color) {
  const node = document.createElement('div')
  node.className = 'amap-marker-label'
  node.style.setProperty('--marker-color', color)
  node.textContent = text
  return node
}

function infoNode(building) {
  const root = document.createElement('div')
  root.className = 'map-info-window'
  const add = (tag, text) => { const element = document.createElement(tag); element.textContent = text; root.appendChild(element) }
  add('h3', `${building.communityName} · ${building.buildingName}`)
  add('p', `${building.unitCount} 个单元 · ${building.propertyCount} 套房产`)
  add('p', `入住 ${building.occupiedPropertyCount} 套 · 空置 ${building.vacantPropertyCount} 套 · 入住率 ${Number(building.occupancyRate || 0).toFixed(1)}%`)
  add('p', `地址：${building.fullAddress || '未填写'}`)
  return root
}

function communityInfoNode(community) {
  const root = document.createElement('div')
  root.className = 'map-info-window'
  const add = (tag, text) => { const element = document.createElement(tag); element.textContent = text; root.appendChild(element) }
  add('h3', community.communityName)
  add('p', `${community.buildingCount} 栋 · ${community.unitCount} 个单元 · ${community.propertyCount} 套房产`)
  add('p', `入住 ${community.occupiedPropertyCount} 套 · 空置 ${community.vacantPropertyCount} 套 · 入住率 ${Number(community.occupancyRate || 0).toFixed(1)}%`)
  add('p', `地址：${community.address || '未填写'}`)
  return root
}

function clearMarkers() {
  if (!map) return
  map.remove([...communityMarkers, ...buildingMarkers])
  communityMarkers = []
  buildingMarkers = []
}

function clearPoiMarkers() {
  if (!map) return
  map.remove(poiMarkers)
  poiMarkers = []
}

function clearSelectedBoundary() {
  if (selectedPolygon && map) map.remove(selectedPolygon)
  selectedPolygon = null
}

function applyVisibility() {
  if (!map) return
  const zoom = map.getZoom()
  const meta = props.scene?.meta || {}
  const communityMax = Number(meta.communityVisibleMaxZoom ?? 14)
  const buildingMin = Number(meta.buildingVisibleMinZoom ?? 15)
  communityMarkers.forEach(marker => marker.setMap(zoom <= communityMax ? map : null))
  buildingMarkers.forEach(marker => marker.setMap(zoom >= buildingMin ? map : null))
}

function renderScene(fit = false) {
  if (!map || !AMap) return false
  clearMarkers()
  const communities = (props.scene?.communities || []).filter(ready)
  const buildings = (props.scene?.buildings || []).filter(ready)
  communityMarkers = communities.map(community => new AMap.Marker({
    position: point(community),
    content: markerLabel(`${community.communityName} · ${community.buildingCount} 栋 · ${community.propertyCount} 套`, colorFor(community.communityId)),
    offset: new AMap.Pixel(-12, -12),
    zIndex: 20
  }))
  communityMarkers.forEach((marker, index) => {
    const community = communities[index]
    marker.on('click', () => { infoWindow.setContent(communityInfoNode(community)); infoWindow.open(map, point(community)) })
  })
  buildingMarkers = buildings.map(building => {
    const marker = new AMap.Marker({
      position: point(building),
      content: markerLabel(`${building.buildingName} · ${building.propertyCount} 套 · ${Number(building.occupancyRate || 0).toFixed(1)}%`, colorFor(building.communityId)),
      offset: new AMap.Pixel(-12, -12), zIndex: 30
    })
    marker.on('click', () => { infoWindow.setContent(infoNode(building)); infoWindow.open(map, point(building)) })
    return marker
  })
  applyVisibility()
  if (fit && [...communityMarkers, ...buildingMarkers].length) map.setFitView(communityMarkers.length ? communityMarkers : buildingMarkers, false, [64, 64, 64, 64])
  return true
}

async function initialize() {
  if (!configured.value) { loading.value = false; return }
  try {
    AMap = await loadAmap()
    await nextTick()
    map = new AMap.Map(container.value, {
      viewMode: '2D',
      zoom: 13,
      resizeEnable: true,
      showLabel: true,
      showBuildingBlock: true
    })
    infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -28), isCustom: false })
    map.addControl(new AMap.Scale())
    map.addControl(new AMap.ToolBar({ position: { right: '18px', bottom: '28px' } }))
    map.on('zoomend', applyVisibility)
    const shouldFit = hasSceneLocations() && !hasFittedFirstScene
    if (renderScene(shouldFit) && shouldFit) hasFittedFirstScene = true
    loading.value = false
    emit('ready')
  } catch (reason) {
    loading.value = false
    error.value = reason?.message || '高德地图初始化失败'
    emit('error', error.value)
  }
}

function focusCommunity(id) {
  const target = (props.scene?.communities || []).find(item => String(item.communityId) === String(id) && ready(item))
  if (target && map) { map.setZoomAndCenter(16, point(target)); return true }
  return false
}
function focusBuilding(id) {
  const target = (props.scene?.buildings || []).find(item => String(item.buildingId) === String(id) && ready(item))
  if (target && map) { map.setZoomAndCenter(17, point(target)); return true }
  return false
}
function poiInfoNode(poi) {
  const root = document.createElement('div')
  root.className = 'map-info-window'
  const add = (tag, text) => { const element = document.createElement(tag); element.textContent = text; root.appendChild(element) }
  add('h3', poi.name || '周边设施')
  add('p', poi.type || '未分类')
  add('p', poi.address || '地址未返回')
  return root
}
function showSearchPois(pois, focusFirst = false) {
  if (!map || !AMap) return false
  clearPoiMarkers()
  const valid = (pois || []).filter(item => Number.isFinite(Number(item.longitude)) && Number.isFinite(Number(item.latitude)))
  poiMarkers = valid.map(poi => {
    const marker = new AMap.Marker({
      position: [Number(poi.longitude), Number(poi.latitude)], content: markerLabel(poi.name || '周边设施', '#b06b34'),
      offset: new AMap.Pixel(-12, -12), zIndex: 60, extData: { poiId: poi.poiId }
    })
    marker.on('click', () => { infoWindow.setContent(poiInfoNode(poi)); infoWindow.open(map, [Number(poi.longitude), Number(poi.latitude)]) })
    return marker
  })
  map.add(poiMarkers)
  if (focusFirst && poiMarkers[0]) map.setZoomAndCenter(16, poiMarkers[0].getPosition())
  return true
}
function showBoundary(geoJson) {
  if (!map || !AMap || !geoJson) return false
  try {
    const rings = JSON.parse(geoJson).coordinates
    if (!Array.isArray(rings) || !rings.length) return false
    clearSelectedBoundary()
    selectedPolygon = new AMap.Polygon({ path: rings[0], strokeColor: '#e97c26', strokeWeight: 4, fillColor: '#f5b55e', fillOpacity: .22, zIndex: 90 })
    map.add(selectedPolygon); map.setFitView([selectedPolygon], false, [86, 86, 86, 86]); return true
  } catch { return false }
}
function startBoundaryDrawing() {
  if (!map || !AMap?.MouseTool) return false
  mouseTool?.close(true); mouseTool = new AMap.MouseTool(map)
  mouseTool.on('draw', event => {
    const path = event.obj?.getPath?.() || []
    const ring = path.map(point => [point.lng, point.lat])
    if (ring.length >= 3) { ring.push([...ring[0]]); emit('boundary-drawn', JSON.stringify({ type: 'Polygon', coordinates: [ring] })) }
    mouseTool.close(true)
  })
  mouseTool.polygon({ strokeColor: '#e97c26', strokeWeight: 4, fillColor: '#f5b55e', fillOpacity: .22 }); return true
}
defineExpose({ focusCommunity, focusBuilding, showBoundary, startBoundaryDrawing, showSearchPois, clearSearchPois: clearPoiMarkers })
watch(() => props.scene, () => {
  const shouldFit = hasSceneLocations() && !hasFittedFirstScene
  if (renderScene(shouldFit) && shouldFit) hasFittedFirstScene = true
}, { deep: true })
initialize()
onBeforeUnmount(() => { mouseTool?.close(true); clearSelectedBoundary(); clearPoiMarkers(); clearMarkers(); map?.destroy(); map = null })
</script>

<template>
  <div class="map-shell">
    <div ref="container" class="amap-container" />
    <div v-if="loading" class="map-overlay">正在准备高德地图…</div>
    <div v-else-if="!configured" class="map-overlay warning"><b>地图凭据尚未配置</b><span>请在本机前端 <code>.env.local</code> 中填写高德 Web 端 JS API Key 与安全密钥。</span></div>
    <div v-else-if="error" class="map-overlay error"><b>地图加载失败</b><span>{{ error }}</span></div>
  </div>
</template>

<style scoped>
.map-shell{position:relative;min-height:630px;overflow:hidden;border-radius:14px;background:#e9f1f0}.amap-container{width:100%;height:630px}.map-overlay{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:9px;color:#55727a;background:rgba(240,247,246,.92)}.map-overlay b{color:#214e55;font-size:16px}.map-overlay span{max-width:520px;text-align:center;font-size:13px;line-height:1.7}.map-overlay code{padding:2px 5px;background:#e1eeee;border-radius:4px}.map-overlay.warning{color:#86692f}.map-overlay.error{color:#a24a4a}
</style>

<style>
.amap-marker-label{max-width:220px;padding:7px 10px;color:white;background:var(--marker-color);border:2px solid rgba(255,255,255,.9);border-radius:9px;box-shadow:0 4px 14px rgba(17,58,61,.25);font-size:12px;font-weight:650;line-height:1.35;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.map-info-window{max-width:290px;padding:2px 3px;color:#2b4c57;font:13px/1.65 system-ui,"Microsoft YaHei",sans-serif}.map-info-window h3{margin:0 0 7px;color:#153d49;font-size:15px}.map-info-window p{margin:2px 0}
</style>
