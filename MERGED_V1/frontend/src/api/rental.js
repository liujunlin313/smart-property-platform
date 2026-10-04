import { http } from './http.js'
export const getRentals = params => http.get('/admin/rentals', { params }).then(r => r.data)
export const getRentalSummary = year => http.get('/admin/rentals/summary', { params: { year } }).then(r => r.data)
export const getRentalDetail = id => http.get(`/admin/rentals/${id}`).then(r => r.data)
export const getRentalAgencies = () => http.get('/admin/rentals/agencies').then(r => r.data)
