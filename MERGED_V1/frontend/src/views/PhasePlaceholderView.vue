<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Connection, Files, Lock, OfficeBuilding } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const title = computed(() => route.meta.title)
const stage = computed(() => route.meta.stage)
const description = computed(() => route.meta.description)
</script>

<template>
  <section class="placeholder panel">
    <div class="stage"><span />{{ stage }}</div>
    <div class="symbol"><OfficeBuilding /></div>
    <p class="eyebrow">ROUTE READY</p>
    <h2>{{ title }}</h2>
    <p class="description">{{ description }}</p>
    <div class="readiness">
      <div><el-icon><Connection /></el-icon><span><b>路由已接通</b><small>菜单、鉴权与页面刷新均可到达</small></span></div>
      <div><el-icon><Lock /></el-icon><span><b>权限边界保留</b><small>正式接口仍由后端角色权限决定</small></span></div>
      <div><el-icon><Files /></el-icon><span><b>业务尚未伪造</b><small>当前只提供后续开发入口，不展示假数据</small></span></div>
    </div>
    <el-button :icon="ArrowLeft" data-action="返回资产工作台" @click="router.push('/assets')">返回资产工作台</el-button>
  </section>
</template>

<style scoped>
.placeholder { min-height:calc(100vh - 132px); padding:72px; display:flex; flex-direction:column; align-items:flex-start; justify-content:center; position:relative; overflow:hidden; }
.placeholder::after { content:""; position:absolute; width:540px; height:540px; right:-180px; top:-200px; border:1px solid #d8ebe7; border-radius:50%; box-shadow:0 0 0 80px #f4faf8,0 0 0 160px #f8fbfa; }
.stage { position:absolute; top:30px; right:32px; z-index:1; padding:8px 12px; color:#5f777f; background:#f4f8f8; border:1px solid #dde8e9; border-radius:999px; font-size:12px; }.stage span { display:inline-block; width:7px; height:7px; margin-right:7px; border-radius:50%; background:#d39b36; }
.symbol { width:58px; height:58px; display:grid; place-items:center; margin-bottom:26px; color:#167d72; background:#e7f4f1; border-radius:17px; font-size:28px; }.placeholder h2 { margin:0; color:#153c4b; font:500 44px/1.2 Georgia,"Noto Serif SC",serif; }.description { max-width:690px; margin:18px 0 32px; color:#657d88; font-size:17px; line-height:1.8; }
.readiness { width:min(760px,100%); display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-bottom:34px; }.readiness > div { min-height:106px; padding:18px; display:flex; gap:12px; background:#f7faf9; border:1px solid #e2ebec; border-radius:13px; }.readiness .el-icon { flex:0 0 auto; margin-top:2px; color:#168779; font-size:19px; }.readiness b,.readiness small { display:block; }.readiness b { color:#294d59; font-size:13px; }.readiness small { margin-top:7px; color:#84969d; font-size:11px; line-height:1.5; }
</style>
