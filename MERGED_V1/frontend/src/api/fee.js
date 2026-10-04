import { http } from './http.js'
export const getActiveFeeTypes = () => http.get('/admin/fees/types').then(r => r.data)
export const getFeeStandards = params => http.get('/admin/fees/standards', { params }).then(r => r.data)
export const getPayments = params => http.get('/admin/fees/payments', { params }).then(r => r.data)
export const getOverdueOverview = params => http.get('/admin/fees/overdue', { params }).then(r => r.data)
export const getOverdueBills = params => http.get('/admin/fees/overdue/list', { params }).then(r => r.data)
