import { http } from './http.js'

export const getSummary = () => http.get('/v1/dashboard/summary').then(r => r.data)
export const getCommunities = (params = {}) => http.get('/v1/communities', { params }).then(r => r.data)
export const getCommunity = (id) => http.get(`/v1/communities/${id}`).then(r => r.data)
export const createCommunity = (payload) => http.post('/v1/communities', payload).then(r => r.data)
export const updateCommunity = (id, payload) => http.put(`/v1/communities/${id}`, payload).then(r => r.data)

export const getBuildings = (params = {}) => http.get('/v1/buildings', { params }).then(r => r.data)
export const getBuilding = (id) => http.get(`/v1/buildings/${id}`).then(r => r.data)
export const createBuilding = (payload) => http.post('/v1/buildings', payload).then(r => r.data)
export const updateBuilding = (id, payload) => http.put(`/v1/buildings/${id}`, payload).then(r => r.data)

export const getBuildingUnits = (params = {}) => http.get('/v1/building-units', { params }).then(r => r.data)
export const getBuildingUnit = (id) => http.get(`/v1/building-units/${id}`).then(r => r.data)
export const createBuildingUnit = (payload) => http.post('/v1/building-units', payload).then(r => r.data)
export const updateBuildingUnit = (id, payload) => http.put(`/v1/building-units/${id}`, payload).then(r => r.data)

export const getProperties = (params = {}) => http.get('/v1/properties', { params }).then(r => r.data)
export const getProperty = (id) => http.get(`/v1/properties/${id}`).then(r => r.data)
export const createProperty = (payload) => http.post('/v1/properties', payload).then(r => r.data)
export const updateProperty = (id, payload) => http.put(`/v1/properties/${id}`, payload).then(r => r.data)

export const getLayoutTemplates = () => http.get('/v1/layout-templates').then(r => r.data)

export const uploadFloorPlan = (propertyId, file, onProgress) => {
  const body = new FormData()
  body.append('file', file)
  return http.post(`/v1/properties/${propertyId}/floor-plan`, body, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  }).then(r => r.data)
}

export const fetchFloorPlanBlob = (propertyId) => http.get(`/v1/properties/${propertyId}/floor-plan`, {
  responseType: 'blob'
}).then(r => URL.createObjectURL(r.data))
