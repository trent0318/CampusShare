# 接口文档

## 通用说明

- **Base URL**：`http://localhost:8080`
- **数据格式**：JSON（UTF-8）
- **统一响应**：

```json
{ "code": 200, "message": "success", "data": { } }
```

- **鉴权**：请求头 `Authorization: Bearer <token>`（登录接口返回 token）
- **分页响应**：`data` 为 `{ "total": 数量, "list": [ ... ] }`

### 常见错误码

| code | 含义 |
| --- | --- |
| 200 | 成功 |
| 400 | 业务错误（参数非法、状态不符等，message 说明原因） |
| 401 | 未登录 / token 过期 |
| 403 | 无权限（越权访问） |
| 429 | 请求过于频繁（限流） |

---

## 1. 认证 Auth

### 1.1 注册（公开）

```
POST /api/auth/register
```

请求体：

```json
{ "username": "wangwu", "password": "123456", "nickname": "王五", "phone": "13800000005" }
```

| 字段 | 必填 | 规则 |
| --- | --- | --- |
| username | 是 | 3~50 字符 |
| password | 是 | 6~100 字符 |
| nickname | 否 | 最长 50 |
| phone | 否 | 最长 20 |

### 1.2 登录（公开）

```
POST /api/auth/login
```

请求体：

```json
{ "username": "admin", "password": "123456" }
```

响应 `data`：

```json
{ "token": "<jwt>", "userId": 1, "username": "admin", "role": "ADMIN" }
```

### 1.3 当前用户（登录）

```
GET /api/auth/me
```

响应 `data`：当前用户信息（id、username、nickname、role 等）。

---

## 2. 用户 User

### 2.1 修改资料（USER）

```
PUT /api/user/profile
```

请求体：`{ "nickname": "...", "avatar": "...", "phone": "..." }`（均可选）

### 2.2 修改密码（USER）

```
PUT /api/user/password
```

请求体：`{ "oldPassword": "...", "newPassword": "..." }`

失败：`400 旧密码错误`

---

## 3. 用户管理 Admin User

### 3.1 用户分页（ADMIN）

```
GET /api/admin/users?page=1&size=10
```

### 3.2 启用/禁用用户（ADMIN）

```
PUT /api/admin/users/{id}/status
```

请求体：`{ "status": 0 }`（1 启用 / 0 禁用）

---

## 4. 分类 Category

### 4.1 分类列表（公开）

```
GET /api/categories?type=ITEM
```

`type` 可选（ITEM / VENUE），不传返回全部。

### 4.2 新建分类（ADMIN）

```
POST /api/admin/categories
```

请求体：`{ "name": "乐器", "type": "ITEM", "sort": 3 }`

### 4.3 更新分类（ADMIN）

```
PUT /api/admin/categories/{id}
```

请求体：`{ "name": "...", "type": "...", "sort": 0, "status": 1 }`（均可选）

### 4.4 删除分类（ADMIN）

```
DELETE /api/admin/categories/{id}
```

失败：`400 该分类下存在资源，无法删除`

---

## 5. 资源 Resource

### 5.1 资源分页（登录）

```
GET /api/resources?page=1&size=10&categoryId=1&type=ITEM&keyword=相机
```

| 参数 | 说明 |
| --- | --- |
| page / size | 分页，默认 1 / 10 |
| categoryId | 按分类过滤 |
| type | ITEM / VENUE |
| keyword | 名称模糊搜索 |

> 普通用户只能看到「已上架」资源（即使传 status 也会被强制覆盖为 1）；管理员可见全部。

### 5.2 资源详情（登录）

```
GET /api/resources/{id}
```

失败：`400 资源不存在` / `400 资源不存在或已下架`（普通用户读下架资源）

### 5.3 创建资源（ADMIN）

```
POST /api/admin/resources
```

请求体：

```json
{ "name": "无人机", "categoryId": 1, "type": "ITEM", "description": "...", "totalCount": 2 }
```

新资源状态为「待审核(0)」。

### 5.4 更新资源（ADMIN）

```
PUT /api/admin/resources/{id}
```

请求体：`{ "name": "...", "description": "...", "totalCount": 3 }`（均可选）

### 5.5 删除资源（ADMIN）

```
DELETE /api/admin/resources/{id}
```

### 5.6 上架/下架（ADMIN）

```
PUT /api/admin/resources/{id}/status
```

请求体：`{ "status": 1 }`（1 上架 / 2 下架）

### 5.7 审核（ADMIN）

```
PUT /api/admin/resources/{id}/audit
```

请求体：`{ "approve": true }` 或 `{ "approve": false, "reason": "图片不合规" }`

失败：`400 只有待审核的资源才能审核` / `400 驳回时必须填写原因`

---

## 6. 预约 Reservation

### 6.1 创建预约（USER）

```
POST /api/reservations
```

请求体：

```json
{ "resourceId": 1, "startTime": "2026-09-28T10:00:00", "endTime": "2026-09-28T12:00:00", "remark": "备注" }
```

| 失败场景 | 响应 |
| --- | --- |
| 开始晚于结束 | `400 开始时间必须早于结束时间` |
| 时间已过去 | `400 预约时间已过去` |
| 资源不存在 | `400 资源不存在` |
| 资源未上架 | `400 资源未上架，无法预约` |
| 时段冲突 | `400 该时间段已被预约` |
| 重复提交 | `400 请勿重复提交` |
| 数量上限 | `400 预约数量已达上限` |

### 6.2 我的预约（USER）

```
GET /api/reservations/mine?status=CONFIRMED&page=1&size=10
```

### 6.3 预约详情（USER，本人或 ADMIN）

```
GET /api/reservations/{id}
```

失败：`403 无权查看该预约`

### 6.4 取消预约（USER，本人或 ADMIN）

```
DELETE /api/reservations/{id}?reason=临时有事
```

失败：`403 无权取消该预约` / `400 当前状态不能取消` / `400 预约已开始，无法取消`

### 6.5 签到（USER，本人）

```
PUT /api/reservations/{id}/checkin
```

失败：`403 只能签到自己的预约` / `400 当前状态不能签到` / `400 未到签到时间` / `400 已过签到时间`

### 6.6 完成（USER，本人）

```
PUT /api/reservations/{id}/complete
```

失败：`403 只能完成自己的预约` / `400 当前状态不能完成`

### 6.7 预约管理分页（ADMIN）

```
GET /api/admin/reservations?userId=1&resourceId=1&status=CONFIRMED&page=1&size=10
```

---

## 7. 评价 Review

### 7.1 创建评价（USER）

```
POST /api/reviews
```

请求体：

```json
{ "reservationId": 3, "rating": 5, "content": "很好用" }
```

| 失败场景 | 响应 |
| --- | --- |
| 预约不存在 | `400 预约不存在` |
| 评价别人的预约 | `403 只能评价自己的预约` |
| 预约未完成 | `400 只有已完成的预约才能评价` |
| 重复评价 | `400 该预约已评价过` |
| 评分越界 | `400 评分最低 1 分` / `400 评分最高 5 分` |

### 7.2 资源评价列表（登录）

```
GET /api/resources/{resourceId}/reviews?page=1&size=10
```

响应 `data.list` 每项含 `username`、`resourceName`、`rating`、`content` 等。

---

## 8. 统计 Admin Stats

### 8.1 数据总览（ADMIN）

```
GET /api/admin/stats/overview
```

响应 `data`：

```json
{ "resourceCount": 3, "userCount": 8, "reservationCount": 12, "completedCount": 1, "cancelledCount": 1 }
```

### 8.2 热门资源排行（ADMIN）

```
GET /api/admin/stats/hot-resources?limit=10
```

响应 `data`：按预约次数降序的数组，每项 `{ "resourceId", "resourceName", "reservationCount" }`。

---

## 附：完整接口清单

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/register | 公开 | 注册 |
| POST | /api/auth/login | 公开 | 登录 |
| GET | /api/auth/me | 登录 | 当前用户 |
| PUT | /api/user/profile | USER | 改资料 |
| PUT | /api/user/password | USER | 改密码 |
| GET | /api/admin/users | ADMIN | 用户分页 |
| PUT | /api/admin/users/{id}/status | ADMIN | 启/禁用户 |
| GET | /api/categories | 公开 | 分类列表 |
| POST | /api/admin/categories | ADMIN | 建分类 |
| PUT | /api/admin/categories/{id} | ADMIN | 改分类 |
| DELETE | /api/admin/categories/{id} | ADMIN | 删分类 |
| GET | /api/resources | 登录 | 资源分页 |
| GET | /api/resources/{id} | 登录 | 资源详情 |
| POST | /api/admin/resources | ADMIN | 建资源 |
| PUT | /api/admin/resources/{id} | ADMIN | 改资源 |
| DELETE | /api/admin/resources/{id} | ADMIN | 删资源 |
| PUT | /api/admin/resources/{id}/status | ADMIN | 上/下架 |
| PUT | /api/admin/resources/{id}/audit | ADMIN | 审核 |
| POST | /api/reservations | USER | 创建预约 |
| GET | /api/reservations/mine | USER | 我的预约 |
| GET | /api/reservations/{id} | USER/ADMIN | 预约详情 |
| DELETE | /api/reservations/{id} | USER/ADMIN | 取消预约 |
| PUT | /api/reservations/{id}/checkin | USER | 签到 |
| PUT | /api/reservations/{id}/complete | USER | 完成 |
| GET | /api/admin/reservations | ADMIN | 预约管理 |
| POST | /api/reviews | USER | 创建评价 |
| GET | /api/resources/{resourceId}/reviews | 登录 | 资源评价 |
| GET | /api/admin/stats/overview | ADMIN | 数据总览 |
| GET | /api/admin/stats/hot-resources | ADMIN | 热门排行 |
