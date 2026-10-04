<script setup>
const props = defineProps({
  total: { type: Number, default: 0 }, page: { type: Number, default: 1 }, pageSize: { type: Number, default: 20 },
  pageSizes: { type: Array, default: () => [20, 50, 100] },
})
const emit = defineEmits(['update:page', 'update:pageSize', 'change'])
function updatePage(page) { emit('update:page', page); emit('change', { page, pageSize: props.pageSize, reason: 'page-change' }) }
function updatePageSize(pageSize) { emit('update:pageSize', pageSize); emit('update:page', 1); emit('change', { page: 1, pageSize, reason: 'page-size-change' }) }
</script>
<template><div class="pagination-bar"><span class="total">共 {{ total }} 条</span><el-pagination :current-page="page" :page-size="pageSize" :page-sizes="pageSizes" :total="total" background layout="sizes, prev, pager, next, jumper" @current-change="updatePage" @size-change="updatePageSize" /></div></template>
<style scoped>.pagination-bar{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-top:16px;color:#718692;font-size:12px;min-height:32px}.total{white-space:nowrap}@media(max-width:760px){.pagination-bar{align-items:flex-start;flex-direction:column}.pagination-bar :deep(.el-pagination){flex-wrap:wrap}}</style>
