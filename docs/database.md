# 数据库设计

## 1. 概览

- 数据库：`campusshare`（MySQL 8 / 9，InnoDB，utf8mb4）
- 表数量：**8 张**
- 建表脚本：`sql/schema.sql`；种子数据：`sql/data.sql`；一键脚本：`CampusShare.sql`

## 2. 表清单

| 表 | 说明 | 关键点 |
| --- | --- | --- |
| `user` | 用户 | BCrypt 密码、username 唯一 |
| `category` | 分类 | ITEM / VENUE 两类 |
| `resource` | 资源（物品/场地） | 状态 0 待审核/1 上架/2 下架/3 驳回 |
| `resource_image` | 资源图片 | 一对多 |
| `reservation` | 预约（核心） | 时段冲突 + 唯一约束 |
| `review` | 评价 | 一次预约只能评一次 |
| `operation_log` | 操作日志 | 管理员操作审计 |
| `system_config` | 系统配置 | 预约上限、签到宽限期等可调参数 |

## 3. 表结构

### 3.1 user（用户）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) | 登录名，`uk_username` 唯一 |
| password | VARCHAR(100) | BCrypt 加密，绝不存明文 |
| nickname / avatar / phone | - | 资料 |
| role | VARCHAR(20) | `USER` / `ADMIN` |
| status | TINYINT | 1 正常 / 0 禁用 |

### 3.2 category（分类）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| name | VARCHAR(50) | 分类名 |
| type | VARCHAR(20) | `ITEM` / `VENUE`，`idx_type` 索引 |
| sort | INT | 排序号，小的在前 |
| status | TINYINT | 1 启用 / 0 停用 |

### 3.3 resource（资源）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| name | VARCHAR(100) | 资源名 |
| category_id | BIGINT | 逻辑外键 → category.id，`idx_category` |
| type | VARCHAR(20) | `ITEM` / `VENUE` |
| description | TEXT | 描述 |
| image / location | VARCHAR(255) | 封面图 / 位置 |
| total_count | INT | 可用数量，场地一般 1 |
| status | TINYINT | 0 待审核 / 1 上架 / 2 下架 / 3 驳回 |
| audit_reason | VARCHAR(255) | 驳回原因 |

索引：`idx_category(category_id)`、`idx_type_status(type, status)`。

### 3.4 reservation（预约，核心）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 预约人，`idx_user` |
| resource_id | BIGINT | 资源 |
| reserve_date | DATE | 预约日期 |
| start_time / end_time | DATETIME | 起止时间 |
| status | VARCHAR(20) | PENDING / CONFIRMED / IN_USE / COMPLETED / CANCELLED / EXPIRED |
| checkin_time / finish_time | DATETIME | 签到 / 完成时间 |
| cancel_reason / remark | VARCHAR(255) | 取消原因 / 备注 |

索引（这是并发正确性的关键）：

| 索引 | 作用 |
| --- | --- |
| `uk_user_slot (user_id, resource_id, reserve_date, start_time, end_time)` | **唯一约束**，同一用户同一时段不重复（兜底） |
| `idx_resource_time (resource_id, start_time, end_time)` | 服务时段冲突查询 `countConflict` |
| `idx_user (user_id)` | 「我的预约」列表 |

### 3.5 review（评价）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 评价人 |
| resource_id | BIGINT | 被评资源 |
| reservation_id | BIGINT | 对应预约，`uk_reservation` **唯一**（一次预约只能评一次） |
| rating | TINYINT | 1~5 |
| content | VARCHAR(500) | 评论 |

### 3.6 operation_log（操作日志）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| user_id / username | - | 操作者（username 冗余，删用户后日志仍可读） |
| operation_type | VARCHAR(50) | 如 `CREATE_RESOURCE` |
| target_type / target_id | - | 操作对象 |
| detail | VARCHAR(500) | 详情 |
| result | VARCHAR(20) | `SUCCESS` / `FAIL` |

### 3.7 system_config（系统配置）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| config_key | VARCHAR(50) | `uk_config_key` 唯一 |
| config_value | VARCHAR(255) | 值 |
| description | VARCHAR(255) | 说明 |

内置配置：`max_active_reservations`（有效预约上限 3）、`checkin_early_minutes`（提前签到 15）、`checkin_grace_minutes`（宽限期 30）。

## 4. 设计要点

### 4.1 逻辑外键 vs 物理外键

**不用数据库 FOREIGN KEY 约束**，用「列名引用 + 索引」表达关系。好处：

- 避免外键带来的跨表锁与死锁；
- 便于测试用不存在的 userId 隔离数据；
- 引用完整性由 Service 层保证。

### 4.2 唯一约束兜底并发

两个业务规则靠唯一约束在数据库层"硬保证"，即使应用层并发漏判也不会出错：

- `review.uk_reservation`：一次预约只能评一次。
- `reservation.uk_user_slot`：同一用户同一时段不重复。

### 4.3 定时任务与索引

定时任务按 `status` 过滤扫描（`WHERE status='CONFIRMED' AND start_time < ?`）。当前数据量小、无压力；量大后可加 `idx_status_time(status, start_time)` 加速（见 code-review.md 的优化建议）。

## 5. 初始化

```bash
# 一键（建库 + 建表 + 数据）
mysql -uroot -p --default-character-set=utf8mb4 < CampusShare.sql

# 或分步
mysql -uroot -p -e "CREATE DATABASE campusshare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p campusshare < sql/schema.sql
mysql -uroot -p campusshare < sql/data.sql
```
