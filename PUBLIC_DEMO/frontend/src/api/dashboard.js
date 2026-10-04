import { http } from './http.js'
export const getOverview = (year) => http.get('/admin/dashboard/overview', { params: { year } }).then(r => r.data)
export const getMonthlyCollection = (year) => http.get('/admin/dashboard/monthly-collection', { params: { year } }).then(r => r.data)
export const getFeeComposition = (year) => http.get('/admin/dashboard/fee-composition', { params: { year } }).then(r => r.data)
export const getBillStatus = (year) => http.get('/admin/dashboard/bill-status', { params: { year } }).then(r => r.data)
export const getOccupancy = () => http.get('/admin/dashboard/occupancy').then(r => r.data)
export const getOverdueTop = () => http.get('/admin/dashboard/overdue-top', { params: { limit: 10 } }).then(r => r.data)
export const getResidenceDashboard = () => http.get('/admin/dashboard/residence').then(r => r.data)
export const getRentalDashboard = (year) => http.get('/admin/dashboard/rentals', { params: { year } }).then(r => r.data)
