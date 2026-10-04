import { computed, reactive } from 'vue'
import { http, onAuthUpdate, setAccessToken } from '../api/http.js'

const state = reactive({
  account: null,
  ready: false,
  backendOnline: true
})

function applyTokenResponse(payload) {
  if (!payload) {
    state.account = null
    setAccessToken(null)
    return
  }
  state.account = payload.account
  setAccessToken(payload.accessToken)
}

onAuthUpdate(applyTokenResponse)

export function useAuth() {
  const authenticated = computed(() => Boolean(state.account))
  const isAdmin = computed(() => state.account?.roles?.includes('SYSTEM_ADMIN'))
  const isViewer = computed(() => state.account?.roles?.includes('DEMO_VIEWER'))
  const isStaff = computed(() => state.account?.roles?.includes('PROPERTY_STAFF'))
  const isResident = computed(() => state.account?.roles?.includes('PERSON_USER'))

  async function login(credentials) {
    const { data } = await http.post('/v1/auth/login', credentials)
    applyTokenResponse(data)
    state.backendOnline = true
    state.ready = true
    return data
  }

  async function restore(force = false) {
    if (state.ready && !force) return authenticated.value
    try {
      const { data } = await http.post('/v1/auth/refresh', {}, { skipClientReport: true })
      applyTokenResponse(data)
      state.backendOnline = true
    } catch {
      applyTokenResponse(null)
    } finally {
      state.ready = true
    }
    return authenticated.value
  }

  async function logout() {
    try {
      await http.post('/v1/auth/logout')
    } finally {
      applyTokenResponse(null)
      state.ready = true
    }
  }

  function markBackendOffline() {
    applyTokenResponse(null)
    state.backendOnline = false
    state.ready = true
  }

  return { state, authenticated, isAdmin, isViewer, isStaff, isResident, login, restore, logout, markBackendOffline }
}
