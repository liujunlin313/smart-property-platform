<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
const props = defineProps({ option: { type: Object, required: true }, height: { type: String, default: '320px' } })
const el = ref(); let chart; let observer
function render() { if (chart) chart.setOption(props.option, true) }
onMounted(() => { chart = echarts.init(el.value); render(); observer = new ResizeObserver(() => chart?.resize()); observer.observe(el.value) })
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>
<template><div ref="el" :style="{ height }" /></template>
