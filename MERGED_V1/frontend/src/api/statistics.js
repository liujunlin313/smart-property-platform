import { http } from './http.js'
export const getMonthlyStatistics = (year) => http.get('/admin/statistics/monthly', { params: { year } }).then(r => r.data)
export const getAnnualStatistics = (year) => http.get('/admin/statistics/annual', { params: { year } }).then(r => r.data)
export const getMonthlyCommunities = (year, month) => http.get('/admin/statistics/monthly/communities', { params: { year, month } }).then(r => r.data)
export const getMonthlyDrilldown = (year, month, communityId) => http.get('/admin/statistics/monthly/drilldown', { params: { year, month, communityId } }).then(r => r.data)
