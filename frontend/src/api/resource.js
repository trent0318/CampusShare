import request from './request'

// 资源分页（登录）。普通用户仅能看已上架；管理员看全部
export function pageResources(params) {
  return request.get('/resources', { params })
}

export function getResource(id) {
  return request.get(`/resources/${id}`)
}

// —— 以下均为 ADMIN 专属 ——
export function createResource(data) {
  return request.post('/admin/resources', data)
}

export function updateResource(id, data) {
  return request.put(`/admin/resources/${id}`, data)
}

export function deleteResource(id) {
  return request.delete(`/admin/resources/${id}`)
}

export function changeResourceStatus(id, status) {
  return request.put(`/admin/resources/${id}/status`, { status })
}

export function auditResource(id, approve, reason) {
  return request.put(`/admin/resources/${id}/audit`, { approve, reason })
}
