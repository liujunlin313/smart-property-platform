import { http } from './http.js'
export const getBills = (params) => http.get('/admin/bills', { params }).then(r => r.data)
export const getBillDetail = (id) => http.get(`/admin/bills/${id}`).then(r => r.data)
