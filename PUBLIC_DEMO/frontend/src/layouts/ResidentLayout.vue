<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../stores/auth.js'

const router = useRouter(), route = useRoute(), auth = useAuth()
const nav = [['/resident', '首页'], ['/resident/residences', '当前居所'], ['/resident/properties', '我的房产'], ['/resident/bills', '我的账单'], ['/resident/parking-permits', '我的车证'], ['/resident/notifications', '我的通知'], ['/resident/incidents', '异常上报']]
const username = computed(() => auth.state.account?.username || '居民用户')
async function logout() { await auth.logout(); router.replace('/login') }
</script>
<template>
  <div class="resident-shell">
    <header class="resident-topbar">
      <button class="brand" @click="router.push('/resident')"><b>栖云物业</b><small>居民服务中心</small></button>
      <nav aria-label="居民服务导航"><button v-for="item in nav" :key="item[0]" :class="{ active: route.path === item[0] }" @click="router.push(item[0])">{{ item[1] }}</button></nav>
      <el-dropdown trigger="click"><button class="resident-profile"><span>{{ username.slice(0, 1).toUpperCase() }}</span><b>{{ username }}</b></button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
    </header>
    <main class="resident-main"><router-view /></main>
  </div>
</template>
<style scoped>
.resident-shell{min-height:100vh;background:linear-gradient(180deg,#f7fbfa 0,#f2f6f5 380px);color:var(--ink-900)}.resident-topbar{position:sticky;z-index:5;top:0;height:70px;padding:0 max(22px,calc((100vw - 1240px)/2));display:flex;align-items:center;gap:34px;background:rgba(255,255,255,.94);border-bottom:1px solid var(--line);backdrop-filter:blur(12px)}.brand{display:grid;gap:2px;min-width:112px;padding:0;border:0;color:var(--teal-dark);background:transparent;text-align:left;cursor:pointer}.brand b{font-size:18px;letter-spacing:-.03em}.brand small{color:#76918f;font-size:10px;font-weight:700;letter-spacing:.08em}nav{display:flex;flex:1;gap:3px}nav button{padding:9px 12px;border:0;border-radius:8px;color:var(--ink-500);background:transparent;cursor:pointer;font-size:13px}.active,nav button:hover{color:var(--teal-dark);background:var(--teal-soft)}.resident-profile{display:flex;align-items:center;gap:8px;border:0;background:transparent;color:var(--ink-700);cursor:pointer}.resident-profile span{width:30px;height:30px;display:grid;place-items:center;border-radius:50%;color:#fff;background:var(--teal);font-size:12px}.resident-profile b{font-size:13px}.resident-main{max-width:1180px;margin:0 auto;padding:34px 22px 54px}@media(max-width:780px){.resident-topbar{height:auto;min-height:64px;flex-wrap:wrap;padding:10px 16px;gap:12px}.brand{min-width:auto}nav{order:3;width:100%;overflow:auto}.resident-profile b{display:none}.resident-main{padding:24px 14px}}
</style>
