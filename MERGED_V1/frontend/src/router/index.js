import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../stores/auth.js'

const placeholder = () => import('../views/PhasePlaceholderView.vue')

export const homeFor = account => account?.roles?.includes('PERSON_USER')
  ? { name: 'resident-home' }
  : { name: 'assets' }

export function isAllowedRoute(account, target) {
  const roles = account?.roles || []
  if (target.meta?.public) return true

  // Residents have a deliberately separate portal. A missing `roles` meta on
  // a legacy back-office route must never make that route resident-accessible.
  if (roles.includes('PERSON_USER')) {
    return target.matched.some(record => record.path === '/resident')
  }

  if (target.meta?.admin && !roles.includes('SYSTEM_ADMIN')) return false
  const requiredRoles = target.meta?.roles
  return !requiredRoles || requiredRoles.some(role => roles.includes(role))
}
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true, title: '登录' } },
  { path: '/', component: () => import('../layouts/AppLayout.vue'), children: [
    { path: '', redirect: () => homeFor(useAuth().state.account) },
    { path: 'assets', name: 'assets', component: () => import('../views/AssetsView.vue'), meta: { title: '物业工作台', roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '数据总览', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'properties', name: 'admin-properties', component: () => import('../views/property/PropertyView.vue'), meta: { title: '房产管理', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'bills', name: 'admin-bills', component: () => import('../views/billing/BillView.vue'), meta: { title: '物业账单', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'statistics/monthly', name: 'monthly-statistics', component: () => import('../views/statistics/MonthlyStatisticsView.vue'), meta: { title: '月度收费统计', roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'statistics/annual', name: 'annual-statistics', component: () => import('../views/statistics/AnnualStatisticsView.vue'), meta: { title: '年度收费统计', roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'residents', name: 'residents', component: () => import('../views/resident/ResidentView.vue'), meta: { title: '人员与居住', roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'residents/manage', name: 'residence-management', component: () => import('../views/residency/MoveInView.vue'), meta: { title: '办理入住', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'incidents', name: 'incidents', component: () => import('../views/incident/IncidentView.vue'), meta: { title: '居民异常', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'rentals', name: 'rentals', component: () => import('../views/rental/RentalView.vue'), meta: { title: '租赁挂牌', admin: true } },
    { path: 'fees', name: 'fees', component: () => import('../views/billing/FeeCenterView.vue'), meta: { title: '物业收费', roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'map', name: 'map', component: () => import('../views/map/MapView.vue'), meta: { title: '空间地图', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'parking-permits', name: 'parking-permits', component: () => import('../views/parking/ParkingPermitView.vue'), meta: { title: '车证管理', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'notifications', name: 'notifications', component: () => import('../views/notification/NotificationView.vue'), meta: { title: '物业通知', pageHeader: true, roles: ['SYSTEM_ADMIN', 'PROPERTY_STAFF'] } },
    { path: 'settings', name: 'settings', component: placeholder, meta: { title: '系统设置', stage: '后续阶段', description: '系统配置和管理员设置将在这一入口继续扩展。' } }
  ] },
  { path: '/resident', component: () => import('../layouts/ResidentLayout.vue'), meta: { roles: ['PERSON_USER'] }, children: [
    { path: '', name: 'resident-home', component: () => import('../views/residentportal/ResidentPortalView.vue'), meta: { title: '居民服务', portal: 'home', roles: ['PERSON_USER'] } },
    ...['residences','properties','bills','notifications','incidents'].map(x=>({path:x,name:`resident-${x}`,component:()=>import('../views/residentportal/ResidentPortalView.vue'),meta:{title:'居民服务',portal:x,roles:['PERSON_USER']}})),
    { path: 'parking-permits', name: 'resident-parking-permits', component: () => import('../views/parking/ResidentParkingPermitView.vue'), meta: { title: '我的车证', roles: ['PERSON_USER'] } }
  ]},
  { path: '/:pathMatch(.*)*', redirect: () => homeFor(useAuth().state.account) }
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(async to => {
  const auth = useAuth()
  const authenticated = await auth.restore()
  if (to.meta.public) return authenticated ? homeFor(auth.state.account) : true
  if (!authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  return isAllowedRoute(auth.state.account, to) ? true : homeFor(auth.state.account)
})
export default router
