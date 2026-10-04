import { reactive } from 'vue'
import { http, onHttpFailure } from '../api/http.js'

const HEARTBEAT_MS = 5_000
const MAX_MISSES = 3
const MAX_EVENTS = 50
const colors = { cyan: '#34c7d9', green: '#42b883', yellow: '#dca33a', red: '#e45c5c' }
const queue = []
let initialized = false
let flushing = false
let lastRoute = '/'

export const monitorState = reactive({
  backendOnline: true,
  misses: 0,
  status: 'UP',
  cacheMode: 'CHECKING',
  dependencies: null,
  lastHeartbeatAt: null
})

function safeText(value, max = 240) {
  return String(value || '').replace(/[\r\n\u001b]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, max)
}

function log(label, message, color) {
  console.log(`%c[${label}]%c ${safeText(message, 600)}`, `color:${color};font-weight:700`, 'color:inherit')
}

function event(type, action, details = {}) {
  const item = {
    type,
    action: safeText(action, 100),
    route: safeText(lastRoute, 240),
    message: safeText(details.message, 600),
    stack: safeText(details.stack, 2_000),
    method: safeText(details.method, 12),
    url: safeText(details.url, 240),
    httpStatus: details.httpStatus || null,
    timestamp: new Date().toISOString()
  }
  queue.push(item)
  if (queue.length > MAX_EVENTS) queue.splice(0, queue.length - MAX_EVENTS)
  return item
}

async function flush() {
  if (flushing || !queue.length || !monitorState.backendOnline) return
  flushing = true
  const batch = queue.splice(0, MAX_EVENTS)
  try {
    const response = await fetch('/api/v1/dev/client-events', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ events: batch })
    })
    if (!response.ok && response.status !== 404) throw new Error(`event endpoint ${response.status}`)
  } catch {
    queue.unshift(...batch.slice(-MAX_EVENTS))
    if (queue.length > MAX_EVENTS) queue.splice(0, queue.length - MAX_EVENTS)
  } finally {
    flushing = false
  }
}

function actionName(target) {
  const element = target?.closest?.('[data-action],button,a,[role="button"]')
  if (!element) return null
  return safeText(element.dataset.action || element.getAttribute('aria-label') || element.getAttribute('title') || element.textContent, 100)
}

export function initializeClientMonitoring({ app, router, auth }) {
  if (initialized) return
  initialized = true
  lastRoute = router.currentRoute.value.fullPath

  document.addEventListener('click', (browserEvent) => {
    const action = actionName(browserEvent.target)
    if (!action) return
    event('ACTION', action)
    log('UI ACTION', action, colors.cyan)
  }, true)
  document.addEventListener('submit', (browserEvent) => {
    const action = safeText(browserEvent.target?.dataset?.action || browserEvent.target?.getAttribute?.('name') || '提交表单', 100)
    event('SUBMIT', action)
    log('UI ACTION', action, colors.cyan)
  }, true)

  router.afterEach((to) => {
    lastRoute = to.fullPath
    event('ROUTE', to.meta.title || to.name || to.path)
    log('UI ACTION', `route ${to.fullPath}`, colors.cyan)
  })

  onHttpFailure((error) => {
    const item = event('HTTP_ERROR', 'HTTP 请求失败', {
      method: error.config?.method?.toUpperCase(),
      url: error.config?.url,
      httpStatus: error.response?.status,
      message: error.response?.data?.message || error.message
    })
    log('HTTP ERROR', `${item.method} ${item.url} ${item.httpStatus || 'NETWORK'} ${item.message}`, colors.red)
    flush()
  })

  app.config.errorHandler = (error, _instance, info) => {
    event('ERROR', 'Vue 运行异常', { message: `${info}: ${error.message}`, stack: error.stack })
    log('FRONTEND ERROR', `${info}: ${error.message}`, colors.red)
    flush()
  }
  window.addEventListener('error', (browserEvent) => {
    event('ERROR', '浏览器运行异常', { message: browserEvent.message, stack: browserEvent.error?.stack })
    log('FRONTEND ERROR', browserEvent.message, colors.red)
    flush()
  })
  window.addEventListener('unhandledrejection', (browserEvent) => {
    const reason = browserEvent.reason
    event('ERROR', '未处理异步异常', { message: reason?.message || reason, stack: reason?.stack })
    log('FRONTEND ERROR', reason?.message || reason, colors.red)
    flush()
  })

  async function heartbeat() {
    try {
      const { data } = await http.get('/v1/system/heartbeat', { timeout: 3_500, skipClientReport: true })
      const wasOffline = !monitorState.backendOnline
      Object.assign(monitorState, {
        backendOnline: true,
        misses: 0,
        status: data.status,
        cacheMode: data.cacheMode,
        dependencies: { mysql: data.mysql, redis: data.redis, minio: data.minio },
        lastHeartbeatAt: data.serverTime
      })
      if (wasOffline) {
        log('BACKEND OK', '后端连接已恢复，正在重新验证登录状态', colors.green)
        event('ACTION', '后端连接恢复')
        await auth.restore(true)
      }
      await flush()
    } catch (error) {
      monitorState.misses += 1
      if (monitorState.misses < MAX_MISSES || !monitorState.backendOnline) return
      monitorState.backendOnline = false
      monitorState.status = 'DOWN'
      auth.markBackendOffline()
      event('OFFLINE', '后端连接中断', { message: `连续 ${MAX_MISSES} 次心跳失败` })
      log('BACKEND OFFLINE', `连续 ${MAX_MISSES} 次心跳失败，已退出本地会话`, colors.yellow)
      const current = router.currentRoute.value
      if (current.name !== 'login') {
        await router.replace({ name: 'login', query: { offline: '1', redirect: current.fullPath } })
      }
    }
  }

  heartbeat()
  window.setInterval(heartbeat, HEARTBEAT_MS)
}
