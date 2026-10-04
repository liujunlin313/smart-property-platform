import { http } from './http.js'

export async function getMapScene() {
  const response = await http.get('/admin/map/scene')
  return response.data
}

export async function syncMissingMapLocations() {
  const response = await http.post('/admin/map/locations/sync-missing')
  return response.data
}

export async function searchSpatialObjects(keyword) {
  const response = await http.get('/admin/map/search', { params: { keyword } })
  return response.data
}

export async function getSpatialBoundary(subjectType, subjectId) {
  const response = await http.get(`/admin/map/boundaries/${subjectType}/${subjectId}`)
  return response.data
}

export async function saveSpatialBoundary(subjectType, subjectId, payload) {
  const response = await http.put(`/admin/map/boundaries/${subjectType}/${subjectId}`, payload)
  return response.data
}

export async function searchNearbyPois(params) {
  const response = await http.get('/admin/map/search/nearby', { params })
  return response.data
}

export async function searchPoiCandidates(keyword) {
  const response = await http.get('/admin/map/search/poi-candidates', { params: { keyword } })
  return response.data
}

export async function reverseManagedCommunities(payload) {
  const response = await http.post('/admin/map/search/reverse-communities', payload)
  return response.data
}
