import axios from 'axios'

let accessToken = null
let refreshPromise = null
const authListeners = new Set()
const failureListeners = new Set()

// One authenticated HTTP core serves both the retained /api/v1 APIs and /api/admin APIs.
export const http = axios.create({ baseURL: '/api', timeout: 15000, withCredentials: true })

export function setAccessToken(token) { accessToken = token || null }
export function onAuthUpdate(listener) { authListeners.add(listener); return () => authListeners.delete(listener) }
export function onHttpFailure(listener) { failureListeners.add(listener); return () => failureListeners.delete(listener) }

function notifyAuth(payload) { authListeners.forEach(listener => listener(payload)) }
function notifyFailure(error) {
  if (!error?.config?.skipClientReport) failureListeners.forEach(listener => listener(error))
}

function unwrapEnvelope(response) {
  const envelope = response.data
  if (envelope && typeof envelope === 'object' && Object.prototype.hasOwnProperty.call(envelope, 'success')) {
    if (!envelope.success) return Promise.reject({ response: { ...response, data: envelope }, config: response.config })
    response.data = envelope.data
  }
  return response
}

function isAuthRequest(config) {
  const url = config?.url || ''
  return url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout')
}

async function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = http.post('/v1/auth/refresh', {}, { _refreshRequest: true, skipClientReport: true })
      .then(response => {
        const payload = response.data
        setAccessToken(payload?.accessToken)
        notifyAuth(payload)
        return payload
      })
      .finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

http.interceptors.request.use(config => {
  config.headers = config.headers || {}
  if (accessToken && !config.headers.Authorization) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

http.interceptors.response.use(unwrapEnvelope, async error => {
  const original = error?.config
  const unauthorized = error?.response?.status === 401
  if (unauthorized && original && !original._retry && !original._refreshRequest && !isAuthRequest(original)) {
    original._retry = true
    try {
      const token = await refreshAccessToken()
      original.headers = original.headers || {}
      original.headers.Authorization = `Bearer ${token.accessToken}`
      return http(original)
    } catch (refreshError) {
      setAccessToken(null)
      notifyAuth(null)
      notifyFailure(refreshError)
      return Promise.reject(refreshError)
    }
  }
  notifyFailure(error)
  return Promise.reject(error)
})

export function apiMessage(error, fallback = '请求失败，请稍后重试') {
  return error?.response?.data?.message || fallback
}
