import { http } from './http.js'
export const getCommunities = () => http.get('/admin/communities').then(r => r.data)
