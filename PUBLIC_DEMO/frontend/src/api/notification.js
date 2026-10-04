import { http } from './http.js'

export async function sendNotification(payload) { const response = await http.post('/admin/notifications', payload); return response.data }
export async function getSentNotifications(params) { const response = await http.get('/admin/notifications', { params }); return response.data }
export async function getNotificationDetail(id) { const response = await http.get(`/admin/notifications/${id}`); return response.data }
export async function searchNotificationTargets(params = {}) { const response = await http.get('/admin/notifications/targets', { params: typeof params === 'string' ? { keyword: params } : params }); return response.data }
