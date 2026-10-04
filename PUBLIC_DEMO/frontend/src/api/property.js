import { http } from './http.js'
export const getProperties = (params) => http.get('/admin/properties', { params }).then(r => r.data)
export const getPropertyDetail = (id) => http.get(`/admin/properties/${id}`).then(r => r.data)
