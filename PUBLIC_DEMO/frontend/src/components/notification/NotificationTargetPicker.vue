<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchNotificationTargets } from '../../api/notification.js'
import { apiMessage } from '../../api/http.js'

const props = defineProps({ modelValue: { type: Array, default: () => [] }, placeholder: { type: String, default: '点击浏览或搜索小区、楼栋、房产、人员' } })
const emit = defineEmits(['update:modelValue'])
const targetLabels = { COMMUNITY: '小区', BUILDING: '楼栋', PROPERTY: '房产', PERSON: '人员' }
const targetType = ref('ALL')
const keyword = ref('')
const searchResults = ref([])
const searching = ref(false)
let searchTimer

const requestTargets = params => searchNotificationTargets({ limit: 30, ...params })
async function loadNode(node, resolve) {
  try {
    const params = node.level === 0 ? {} : { parentType: node.data.targetType, parentId: node.data.targetId }
    const rows = await requestTargets(params)
    resolve(rows.map(row => ({ ...row, nodeKey: `${row.targetType}-${row.targetId}`, leaf: row.targetType === 'PERSON' })))
  } catch (error) {
    ElMessage.error(apiMessage(error, '接收对象加载失败'))
    resolve([])
  }
}
function addTarget(item) {
  const exists = props.modelValue.some(value => value.targetType === item.targetType && String(value.targetId) === String(item.targetId))
  if (exists) return ElMessage.info('该对象已在当前接收列表中')
  emit('update:modelValue', [...props.modelValue, item])
}
function removeTarget(index) { emit('update:modelValue', props.modelValue.filter((_, current) => current !== index)) }
function display(item) { return `${targetLabels[item.targetType]} · ${item.label}${item.description ? ` · ${item.description}` : ''}` }
function onKeywordInput() {
  clearTimeout(searchTimer)
  const value = keyword.value.trim()
  if (!value) { searchResults.value = []; return }
  searchTimer = setTimeout(async () => {
    searching.value = true
    try { searchResults.value = await requestTargets({ keyword: value, targetType: targetType.value }) }
    catch (error) { ElMessage.error(apiMessage(error, '接收对象搜索失败')); searchResults.value = [] }
    finally { searching.value = false }
  }, 180)
}
function onTypeChange() { if (keyword.value.trim()) onKeywordInput() }
</script>

<template>
  <div class="target-picker">
    <el-popover trigger="click" placement="bottom-start" :width="460" @show="keyword = ''; searchResults = []">
      <template #reference><el-input :placeholder="placeholder" readonly><template #suffix><span class="browse-icon">⌄</span></template></el-input></template>
      <div class="picker-head"><el-select v-model="targetType" class="type-select" @change="onTypeChange"><el-option label="全部" value="ALL"/><el-option v-for="(label, value) in targetLabels" :key="value" :label="label" :value="value"/></el-select><el-input v-model="keyword" clearable placeholder="输入关键词联想搜索" @input="onKeywordInput"/></div>
      <div v-if="keyword.trim()" v-loading="searching" class="search-results"><button v-for="item in searchResults" :key="`${item.targetType}-${item.targetId}-${item.communityId}`" class="candidate" type="button" @click="addTarget(item)"><b>{{ targetLabels[item.targetType] }}</b><span>{{ item.label }}</span><small v-if="item.description">{{ item.description }}</small></button><el-empty v-if="!searching && !searchResults.length" description="未找到匹配对象" :image-size="64"/></div>
      <div v-else class="tree-wrap"><p>从可访问小区开始浏览，可逐级展开楼栋、房产和住户。</p><el-tree lazy :load="loadNode" node-key="nodeKey" :props="{ label: 'label', isLeaf: 'leaf' }" empty-text="当前范围内没有可选对象" @node-click="addTarget"><template #default="{ data }"><span class="tree-label"><b>{{ targetLabels[data.targetType] }}</b><span>{{ data.label }}</span></span></template></el-tree></div>
    </el-popover>
    <div class="tags"><el-tag v-for="(item,index) in modelValue" :key="`${item.targetType}-${item.targetId}`" closable @close="removeTarget(index)">{{ display(item) }}</el-tag></div>
  </div>
</template>

<style scoped>
.target-picker{display:grid;gap:8px}.picker-head{display:grid;grid-template-columns:110px 1fr;gap:8px}.type-select{width:100%}.tree-wrap{max-height:320px;overflow:auto}.tree-wrap p{margin:10px 0 8px;color:#78909a;font-size:12px}.tree-label{display:flex;gap:8px;align-items:center;min-width:0}.tree-label b,.candidate b{flex:none;color:#168175;font-size:11px}.tree-label span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.search-results{display:grid;max-height:320px;overflow:auto;padding-top:8px}.candidate{display:grid;grid-template-columns:44px minmax(0,1fr);gap:4px 8px;padding:9px;border:0;border-bottom:1px solid #edf2f3;background:#fff;text-align:left;cursor:pointer}.candidate:hover{background:#f2f8f7}.candidate span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:#244956}.candidate small{grid-column:2;color:#7b9098;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.tags{display:flex;flex-wrap:wrap;gap:7px}.browse-icon{color:#5c7d86;font-size:18px}
</style>
