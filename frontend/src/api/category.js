import request from './request'

// 分类列表（公开接口，无需 token）
export function listCategories(type) {
  return request.get('/categories', { params: type ? { type } : {} })
}
