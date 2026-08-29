# CampusShare 校园资源共享与预约平台

一个基于 Spring Boot 的校园资源共享与预约系统。学生可以浏览并预约学校的物品（相机、球拍等）和场地（自习室、会议室等），使用后签到、完成、评价；管理员负责资源审核、上架下架、用户管理与数据统计。

**核心业务：资源预约。** 围绕"同一资源同一时段不能重复预约"这一核心约束，实现了时间冲突检测、并发不超卖、防重复提交、缓存一致性等能力。

---

## 技术栈

| 分类 | 技术 | 用途 |
| --- | --- | --- |
| 语言 | Java 17 | 主语言 |
| 框架 | Spring Boot 3.2.5 | 应用框架 |
| Web | Spring MVC | REST 接口 |
| 安全 | Spring Security + JWT（jjwt 0.12.5） | 认证授权、无状态登录 |
| 校验 | Spring Validation | 参数校验（@Valid） |
| 持久层 | MyBatis 3.0.3 | 数据访问（XML 动态 SQL） |
| 数据库 | MySQL 8 / 9 | 主存储 |
| 缓存 | Redis + Spring Data Redis | 缓存 / 防重复提交 / Lua 原子预检 / 限流 |
| 构建 | Maven | 依赖与打包 |
| 测试 | JUnit 5 + Spring Boot Test | 单元 / 集成 / 并发测试 |
| 其他 | Lombok | 减少样板代码 |

> 只列出项目**实际使用**的技术，未实现的（如消息队列、分布式锁中间件、监控等）不列。

---

## 系统架构

```
Client（HTTP / JSON）
   │
   ▼
Controller 层（参数校验、路由）
   │
   ▼
Service 层（业务逻辑、事务、鉴权、操作日志）
   │
   ▼
Mapper 层（MyBatis XML SQL）
   │
   ▼
MySQL（主存储，最终事实）
```

Redis 作为**加速层**挂在 Service 层旁边，负责四件事：

- **缓存**：资源详情（Cache Aside 模式）
- **防重复提交**：SETNX + TTL
- **并发预检**：Lua 脚本原子判断时段冲突
- **限流**：令牌桶

> 原则：**Redis 挡流量，MySQL 定事实。** 如果Redis挂了，还有MySQL兜底，只是效率会减慢。

---

## 功能模块

- **用户**：注册、登录、查看个人信息、修改资料、修改密码
- **权限**：JWT 无状态认证 + 角色控制（USER / ADMIN）
- **资源**：分类管理、资源 CRUD、审核（通过/驳回）、上架/下架
- **预约**：创建预约、时段冲突检测、我的预约、取消、签到、完成
- **评价**：对已完成的预约打分评价（一次预约只能评一次）
- **管理**：用户管理（启用/禁用）、预约管理、数据统计（总览 + 热门排行）
- **操作日志**：管理员敏感操作全记录
- **定时任务**：超时未签到自动过期、到点自动完成

---

## 核心技术难点

1. **时间段冲突检测**：区间重叠算法 `s1 < e2 && s2 < e1`，首尾相接不算冲突。
2. **并发预约不超卖**：四层防御——令牌桶限流 → SETNX 防重复 → Redis Lua 原子预检 → MySQL 行锁 + 唯一约束兜底。
3. **Redis 缓存一致性**：Cache Aside（读回填、写后删缓存），只缓存已上架资源，避免越权泄露。
4. **防重复提交**：同一用户短时间内连点同一时段，靠 SETNX 幂等拦截。
5. **事务**：锁行（FOR UPDATE）+ 插入在同一事务，失败整体回滚并释放 Redis 占位。
6. **定时任务幂等**：条件 UPDATE（`WHERE status = '...'`）保证重复执行不重复处理。

---

## 性能测试（并发正确性）

> 环境无 JMeter，并发压测用 JUnit `@SpringBootTest` + 真实 MySQL/Redis 实现（1000 线程同时起跑）。

| 并发请求数 | 成功预约 | 失败 | 结果 |
| --- | --- | --- | --- |
| 100 | 1 | 99 | ✅ 恰好 1 人成功 |
| 500 | 1 | 499 | ✅ 恰好 1 人成功 |
| 1000 | 1 | 999 | ✅ 恰好 1 人成功 |

**1 个名额的资源，1000 个并发请求抢同一时段，最终恰好 1 人成功，无超卖、无重复。** 压测看的是数据正确性，不是 QPS。

完整测试：`mvn test`，共 19 个用例，全部通过。详见 [docs/code-review.md](docs/code-review.md)。

---

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8.x / 9.x（本机 9.5）
- Redis

### 2. 初始化数据库

**方式一（推荐，一键）**：

```bash
mysql -uroot -p --default-character-set=utf8mb4 < CampusShare.sql
```

**方式二（分步）**：

```bash
mysql -uroot -p -e "CREATE DATABASE campusshare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p campusshare < sql/schema.sql
mysql -uroot -p campusshare < sql/data.sql
```

### 3. 配置数据库连接

`src/main/resources/application.yml` 默认连接：

- MySQL：`localhost:3306`，用户名 `root`
- Redis：`localhost:6379`

密码和 JWT 密钥通过环境变量注入（**本地开发有默认值，生产必须覆盖**）：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_PASSWORD` | `123456` | MySQL 密码 |
| `REDIS_PASSWORD` | `123456` | Redis 密码 |
| `JWT_SECRET` | 开发占位值 | 生产必须设为强随机密钥，如 `openssl rand -base64 48` |

如果你的本地 MySQL/Redis 密码不是 `123456`，启动前先设置对应环境变量，例如：

```bash
export DB_PASSWORD=你的密码
export REDIS_PASSWORD=你的密码
```

### 4. 启动

```bash
mvn spring-boot:run
```

启动后访问 `http://localhost:8080`。

### 5. 测试账号（密码均为 `123456`）

| 用户名 | 角色 |
| --- | --- |
| `admin` | ADMIN（管理员） |
| `zhangsan` | USER（普通用户） |
| `lisi` | USER（普通用户） |

### 6. 跑测试

```bash
mvn test
```

> 测试连的是真实 MySQL/Redis，跑之前确保两者正在运行，且库里有至少一个「已上架」资源和一个「启用」分类（`sql/data.sql` 已内置）。

---

## 接口文档

完整接口列表见 [docs/api.md](docs/api.md)。鉴权方式：`Authorization: Bearer <token>`（登录接口返回）。

---

## 项目文档

| 文档 | 说明 |
| --- | --- |
| [docs/architecture.md](docs/architecture.md) | 系统架构与模块设计 |
| [docs/database.md](docs/database.md) | 数据库表结构与索引设计 |
| [docs/api.md](docs/api.md) | 全部接口文档 |
| [docs/concurrency.md](docs/concurrency.md) | 并发预约与时间冲突方案 |
| [docs/redis.md](docs/redis.md) | Redis 缓存/防重/限流设计 |
| [docs/development.md](docs/development.md) | 开发环境搭建与约定 |
| [docs/code-review.md](docs/code-review.md) | 代码审查报告 |

---

## 目录结构

```
CampusShare
├── sql/
│   ├── schema.sql          # 建表脚本
│   └── data.sql            # 种子数据
├── src/
│   ├── main/java/com/campusshare/
│   │   ├── controller/     # 接口层（含 admin 子包）
│   │   ├── service/        # 业务层（接口 + impl）
│   │   ├── mapper/         # MyBatis Mapper 接口
│   │   ├── entity/ dto/ vo/  # 实体、入参、出参
│   │   ├── security/       # JWT + Spring Security 配置
│   │   ├── task/           # 定时任务
│   │   └── common/ exception/ utils/  # 通用响应、异常、工具
│   └── main/resources/
│       ├── mapper/         # MyBatis XML
│       └── application.yml # 配置
├── docs/                   # 项目文档
├── CampusShare.sql         # 一键初始化脚本
└── pom.xml
```

---

## License

本项目仅用于学习与求职展示。
