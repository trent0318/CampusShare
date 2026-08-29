// 与后端枚举一一对应，勿改动后端映射关系
export const RESOURCE_STATUS = {
  0: '待审核',
  1: '已上架',
  2: '已下架',
  3: '已驳回'
}

export const RESERVATION_STATUS = {
  CONFIRMED: '已确认',
  IN_USE: '使用中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  EXPIRED: '已过期'
}

export const RESOURCE_TYPE = {
  ITEM: '物品',
  VENUE: '场地'
}
