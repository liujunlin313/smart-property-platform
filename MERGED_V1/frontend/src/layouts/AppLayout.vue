<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Bell, Coin, DataAnalysis, Grid, Histogram, House, Location, OfficeBuilding, Setting, Tickets, User, UserFilled, Van
} from '@element-plus/icons-vue'
import { useAuth } from '../stores/auth.js'
import { monitorState } from '../observability/clientMonitor.js'
import { accessApi } from '../api/stageH.js'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const title = computed(() => route.meta.title || '多小区智慧物业管理系统')
const account = computed(() => auth.state.account)
const isSystemAdmin = computed(() => account.value?.roles?.includes('SYSTEM_ADMIN'))
const identityText = computed(() => {
  const roles = account.value?.roles || []
  if (roles.includes('SYSTEM_ADMIN')) return '系统管理员'
  if (roles.includes('PROPERTY_STAFF')) return '物业工作人员'
  if (roles.includes('PERSON_USER')) return '居民用户'
  if (roles.includes('DEMO_VIEWER')) return '演示观察员'
  return '已登录用户'
})
const scopeNames = ref([])
onMounted(async () => {
  if (!isSystemAdmin.value) {
    try { scopeNames.value = (await accessApi.scope()).data.map(item => item.communityName) } catch { scopeNames.value = [] }
  }
})
const canSee = roles => !roles || roles.some(role => account.value?.roles?.includes(role))
const statusText = computed(() => {
  if (!monitorState.backendOnline) return '后端离线'
  return monitorState.status === 'DEGRADED' ? '降级运行 · BYPASS' : '服务正常'
})
const navigationGroups = [
  { label: '工作台', items: [['/assets', '物业工作台', Grid, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']]] },
  { label: '资产与空间', items: [['/properties', '房产管理', House, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/map', '空间地图', Location, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']]] },
  { label: '居民服务', items: [['/residents', '人员与居住', User, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/residents/manage', '办理入住', User, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/incidents', '居民异常', Bell, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']]] },
  { label: '费用管理', items: [['/fees', '物业收费', Coin, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/bills', '账单明细', Tickets, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']]] },
  { label: '物业运营', items: [['/notifications', '物业通知', Bell, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/parking-permits', '车证管理', Van, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/rentals', '租赁挂牌', Tickets, ['SYSTEM_ADMIN']]] },
  { label: '数据分析', items: [['/dashboard', '数据总览', DataAnalysis, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/statistics/monthly', '月度统计', Histogram, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']], ['/statistics/annual', '年度统计', DataAnalysis, ['SYSTEM_ADMIN', 'PROPERTY_STAFF']]] }
]

async function logout() {
  await ElMessageBox.confirm('确认退出当前登录账号吗？', '退出登录', { type: 'warning' })
  await auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <button class="brand" @click="router.push('/assets')">
        <span class="brand-icon"><OfficeBuilding /></span>
        <span><b>栖云物业</b><small>SMART PROPERTY</small></span>
      </button>

      <nav class="navigation" aria-label="管理端业务导航">
        <section v-for="group in navigationGroups" :key="group.label" class="nav-group">
          <p>{{ group.label }}</p>
          <button v-for="item in group.items.filter(item => canSee(item[3]))" :key="item[0]" :class="['nav-item', { active: route.path === item[0] }]" @click="router.push(item[0])">
            <el-icon><component :is="item[2]" /></el-icon><span>{{ item[1] }}</span>
          </button>
        </section>
      </nav>

      <div class="sidebar-bottom">
        <div class="delivery-card"><span>当前身份</span><b>{{ identityText }}</b><p>{{ isSystemAdmin ? '可查看全部小区数据' : (scopeNames.length ? `负责 ${scopeNames.join('、')}` : '数据范围由系统授权决定') }}</p></div>
        <button class="nav-item settings" :class="{ active: route.path === '/settings' }" @click="router.push('/settings')">
          <el-icon><Setting /></el-icon><span>系统设置</span>
        </button>
      </div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div><p class="eyebrow">SMART PROPERTY</p><h1 v-if="!route.meta.pageHeader">{{ title }}</h1><small v-if="!isSystemAdmin && scopeNames.length" class="scope">当前管理范围：{{ scopeNames.join('、') }}</small></div>
        <div class="top-actions">
          <span :class="['service-status', { offline: !monitorState.backendOnline }]"><i />{{ statusText }}</span>
          <button class="icon-button" aria-label="消息通知" @click="router.push('/notifications')"><el-icon><Bell /></el-icon></button>
          <el-dropdown trigger="click">
            <button class="profile-button"><span class="avatar"><el-icon><UserFilled /></el-icon></span><span><b>{{ account?.username || '用户' }}</b><small>{{ identityText }}</small></span></button>
            <template #dropdown><el-dropdown-menu><el-dropdown-item @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
      </header>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell{min-height:100vh;display:grid;grid-template-columns:248px minmax(0,1fr);background:#f2f5f6}.sidebar{position:sticky;top:0;height:100vh;box-sizing:border-box;display:flex;flex-direction:column;padding:20px 14px;color:#c6dcdb;background:linear-gradient(180deg,#103943,#094e49)}.brand{display:flex;align-items:center;gap:12px;padding:6px 10px 28px;color:inherit;background:transparent;border:0;border-bottom:1px solid rgba(255,255,255,.1);text-align:left;cursor:pointer}.brand-icon{width:41px;height:41px;display:grid;place-items:center;color:#12645d;background:#eefaf7;border-radius:12px;font-size:25px}.brand b,.brand small{display:block}.brand b{color:#fff;font-size:17px}.brand small{margin-top:3px;color:#80aaa7;font:700 9px/1.5 Consolas,monospace;letter-spacing:.12em}.navigation{display:flex;flex-direction:column;gap:7px;padding-top:24px}.nav-item{width:100%;min-height:45px;display:flex;align-items:center;gap:11px;padding:0 12px;color:#b7cecd;background:transparent;border:0;border-radius:10px;text-align:left;cursor:pointer}.nav-item span{flex:1}.nav-item small{color:#8eb8b4;font:11px Consolas,monospace}.nav-item.active,.nav-item:hover{color:#fff;background:rgba(115,215,198,.16);box-shadow:inset 3px 0 #72d7c6}.sidebar-bottom{margin-top:auto}.delivery-card{margin:0 3px 12px;padding:14px;border:1px solid rgba(255,255,255,.12);border-radius:12px;background:rgba(4,33,37,.27)}.delivery-card span,.delivery-card b,.delivery-card p{display:block}.delivery-card span{color:#91aeac;font-size:10px}.delivery-card b{margin-top:7px;color:#88dfcf;font:700 16px/1.2 Consolas,monospace}.delivery-card p{margin:5px 0 0;color:#bfd5d3;font-size:12px}.workspace{min-width:0;padding:26px 31px 31px}.topbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:23px}.eyebrow{margin:0 0 6px;color:#087c70;font:700 12px/1.2 Consolas,monospace;letter-spacing:.14em}.topbar h1{margin:0;color:#102a43;font:500 30px/1.15 Georgia,'Noto Serif SC',serif}.top-actions{display:flex;align-items:center;gap:12px}.service-status{display:flex;align-items:center;gap:7px;padding:8px 11px;color:#5d7b85;background:#fff;border:1px solid #dce7ea;border-radius:999px;font-size:12px}.service-status i{width:8px;height:8px;border-radius:50%;background:#21a97d;box-shadow:0 0 0 4px #e2f4ed}.service-status.offline i{background:#dc7a42;box-shadow:0 0 0 4px #fff1e5}.icon-button{width:39px;height:39px;display:grid;place-items:center;color:#53727a;background:#fff;border:1px solid #dce7ea;border-radius:10px;cursor:pointer;font-size:18px}.profile-button{display:flex;align-items:center;gap:9px;padding:5px 12px 5px 6px;color:#203c4c;background:#fff;border:1px solid #dce7ea;border-radius:11px;cursor:pointer;text-align:left}.profile-button b,.profile-button small{display:block}.profile-button b{font-size:13px}.profile-button small{margin-top:2px;color:#82959d;font-size:10px}.avatar{width:31px;height:31px;display:grid;place-items:center;color:#fff;background:#167d72;border-radius:9px}@media(max-width:1120px){.workspace{padding:22px}.sidebar{width:220px}.app-shell{grid-template-columns:220px minmax(0,1fr)}}
.scope{display:block;margin-top:6px;color:#4c7771;font-size:12px}.navigation{gap:18px;padding:22px 0 12px;overflow:auto}.nav-group{display:grid;gap:4px}.nav-group>p{margin:0 10px 5px;color:#77a39f;font-size:10px;font-weight:700;letter-spacing:.1em}.nav-item{min-height:39px;border-radius:8px;font-size:13px}.nav-item:hover{background:rgba(115,215,198,.1);box-shadow:none}.nav-item.active{background:rgba(115,215,198,.17);box-shadow:none}.delivery-card b{font:700 13px/1.45 "Microsoft YaHei",sans-serif}.workspace{padding:30px 34px 36px}.topbar{margin-bottom:28px}.topbar h1{font-family:"PingFang SC","Microsoft YaHei",sans-serif;font-size:27px;font-weight:700;letter-spacing:-.02em}.eyebrow{display:none}@media(max-width:980px){.app-shell{grid-template-columns:72px minmax(0,1fr)}.sidebar{width:auto;padding:18px 10px}.brand{justify-content:center;padding:4px 0 18px}.brand>span:last-child,.nav-group>p,.nav-item span,.delivery-card,.settings span{display:none}.navigation{align-items:center}.nav-group{width:100%}.nav-item{justify-content:center;padding:0}.workspace{padding:22px}.topbar{align-items:flex-start;gap:12px}.top-actions .service-status{display:none}}@media(max-width:680px){.app-shell{display:block}.sidebar{position:static;width:100%;height:auto;padding:10px 14px}.brand{display:none}.navigation{display:flex;overflow:auto;padding:0;gap:4px}.nav-group{display:contents}.nav-item{width:42px;flex:0 0 42px}.sidebar-bottom{display:none}.workspace{padding:20px 14px}.topbar h1{font-size:23px}.profile-button>span:last-child,.icon-button{display:none}}
</style>
