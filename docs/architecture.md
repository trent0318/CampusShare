# 系统架构

## 1. 整体架构

CampusShare 是典型的**分层单体架构**（前后端分离，后端提供 REST API）：

```
Client（浏览器 / Postman / 前端应用）
   │  HTTP + JSON
   ▼
┌─────────────────────────────────────────────┐
│              Spring Boot 应用                 │
│                                              │
│  Controller 层 —— 参数校验、路由、统一响应      │
│        │                                     │
│  Service 层 —— 业务逻辑、事务、鉴权、操作日志    │
│        │                │                    │
│  Mapper 层          Redis 加速层              │
│  (MyBatis XML)      (缓存/防重/限流/Lua)      │
│        │                                     │
└────────┼─────────────────────────────────────┘
         ▼
      MySQL（主存储，最终事实）
```

**核心原则：Redis 挡流量，MySQL 定事实。** Redis 只做加速与预检，任何 Redis 故障都降级放行，最终正确性由 MySQL 的约束与事务保证。

## 2. 分层职责

| 层 | 职责 | 不该做什么 |
| --- | --- | --- |
| Controller | 接收参数、`@Valid` 校验、调 Service、包装 `Result` | 不写业务逻辑、不直接碰 Mapper |
| Service | 业务规则、事务边界、越权校验、操作日志埋点 | 不处理 HTTP 细节 |
| Mapper | 只做数据访问（SQL） | 不写业务判断 |
| Entity/DTO/VO | 数据载体 | 三者分离：DB 实体 / 入参 / 出参 |

分层带来的好处：

- **可测试**：Service 层可脱离 HTTP 直接注入测试（Phase 7 的测试就是这么写的）。
- **职责清晰**：越权校验在 Service 层统一做，不依赖 Controller 记得加。

## 3. 包结构

```
com.campusshare
├── controller/          # 接口层
│   └── admin/           #   管理员接口（/api/admin/**）
├── service/             # 业务接口
│   └── impl/            #   业务实现
├── mapper/              # MyBatis Mapper 接口
├── entity/              # 数据库实体
├── dto/                 # 请求入参（带校验注解）
├── vo/                  # 响应出参
├── security/            # JWT + Spring Security 配置
├── task/                # 定时任务
├── common/              # 通用类（Result、PageResult）
├── exception/           # 全局异常处理
└── utils/               # 工具类（SecurityUtil）
```

## 4. 关键设计决策

### 4.1 逻辑外键（不用数据库 FOREIGN KEY）

表之间用「列名引用 + 索引」表达关联，**不加数据库层外键约束**。原因：

1. 避免跨表锁与高并发下的死锁风险；
2. 与主流开源项目（NewBee Mall、苍穹外卖）一致；
3. 引用完整性由 Service 层业务代码保证（删分类前先查是否有资源引用）。

> 副作用：测试可以用 user 表里不存在的 userId 插预约数据（预约表无外键），实现测试数据隔离。

### 4.2 无状态 JWT + 服务层二次鉴权

- **URL 层**：`/api/admin/**` 由 Spring Security 的 `hasRole("ADMIN")` 拦截。
- **服务层**：敏感操作（查看/取消预约、签到/完成、评价）再校验一次「当前用户是否拥有该资源」，防止水平越权（A 用户操作 B 用户的数据）。

只靠 URL 拦截不够——URL 只能挡住"非管理员访问管理接口"，挡不住"普通用户 A 访问普通用户 B 的数据"。所以服务层必须二次校验。

### 4.3 预约状态机

```
CONFIRMED ──签到──▶ IN_USE ──完成──▶ COMPLETED
    │                                     │
    ├──取消──▶ CANCELLED                  └──▶ 评价（本人 + 一次）
    │
    └──超时未签到（定时任务）──▶ EXPIRED
```

状态流转都靠「条件 UPDATE」（`WHERE status = 旧状态`）保证原子与幂等，不允许乱跳。

## 5. 核心流程：创建预约

```
POST /api/reservations
   │
   ├─ ① 时间合法性：开始 < 结束 且 开始 > 现在
   ├─ ② 令牌桶限流（Redis，每用户）
   ├─ ③ 防重复提交（Redis SETNX，3s TTL）
   ├─ ④ 时段冲突预检（Redis Lua 原子判断，ZSET）
   ├─ ⑤ 事务兜底（MySQL）
   │      a. FOR UPDATE 锁资源行
   │      b. countConflict 查时间冲突
   │      c. countActiveByUser 查有效预约上限
   │      d. INSERT（uk_user_slot 唯一键兜底）
   │      任一失败 → 回滚 + 释放 Redis 占位
   └─ 返回预约 id
```

详细并发设计见 [concurrency.md](concurrency.md)，Redis 细节见 [redis.md](redis.md)。

## 6. 定时任务

`ReservationTask` 每 60 秒执行两个任务：

| 任务 | 触发条件 | 动作 |
| --- | --- | --- |
| `expireNoShow` | `CONFIRMED` 且 `start_time < now - 宽限期` | 转 `EXPIRED`，释放 Redis 占位 |
| `autoComplete` | `IN_USE` 且 `end_time < now` | 转 `COMPLETED`，释放 Redis 占位 |

两个任务都用条件 UPDATE 保证幂等，重复扫描不会重复处理。
