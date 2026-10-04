import { http } from './http.js'
export const getResidents = params => http.get('/admin/residents', { params }).then(r => r.data)
export const getResidentSummary = () => http.get('/admin/residents/summary').then(r => r.data)
export const getResidentDetail = id => http.get(`/admin/residents/${id}`).then(r => r.data)
