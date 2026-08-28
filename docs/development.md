# 开发环境与约定

## 1. 环境准备

| 软件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 编译与运行 |
| Maven | 3.9+ | 构建 |
| MySQL | 8.x / 9.x | 主数据库（本机 9.5） |
| Redis | 3.2+ | 缓存/防重/限流（本机 3.2.100 Windows 版） |

## 2. 启动步骤

### 2.1 初始化数据库

```bash
# 一键
mysql -uroot -p --default-character-set=utf8mb4 < CampusShare.sql

# 或分步
mysql -uroot -p -e "CREATE DATABASE campusshare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p campusshare < sql/schema.sql
mysql -uroot -p campusshare < sql/data.sql
```

### 2.2 配置连接

`application.yml` 默认连 `localhost:3306`（用户 `root`）和 `localhost:6379`。密码和 JWT 密钥通过环境变量注入：

```bash
# 本地密码不是 123456 时设置
export DB_PASSWORD=你的MySQL密码
export REDIS_PASSWORD=你的Redis密码
# 生产必须设置强随机 JWT 密钥
export JWT_SECRET=$(openssl rand -base64 48)
```

### 2.3 启动

```bash
mvn spring-boot:run
```

### 2.4 验证

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

拿到 `token` 后，带 `Authorization: Bearer <token>` 访问其他接口。

## 3. 测试账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | ADMIN |
| zhangsan | 123456 | USER |
| lisi | 123456 | USER |

## 4. 运行测试

```bash
mvn test              # 全部（19 个用例，含 100/500/1000 并发）
mvn test -Dtest=ReservationServiceTest   # 只跑某个类
```

> 测试连真实 MySQL/Redis，跑之前确保两者运行，且库里有「已上架」资源 + 「启用」分类。

## 5. 代码约定

- **分层**：Controller（参数校验/路由）→ Service（业务/事务/鉴权）→ Mapper（SQL）。Controller 不写业务、不碰 Mapper。
- **响应**：统一 `Result<T>`，分页用 `PageResult<T>`，业务错误抛 `BusinessException`（带 code + message），全局异常处理器统一转 JSON。
- **命名**：表字段下划线（`create_time`），Java 驼峰（`createTime`），MyBatis `map-underscore-to-camel-case` 自动映射。
- **入参/出参分离**：`dto/`（请求，带 `@Valid` 校验注解）、`vo/`（响应）、`entity/`（DB 实体），三者不混用。
- **鉴权**：URL 层（`/api/admin/**`）+ 服务层二次校验（防水平越权）双重保障。
- **日志**：敏感管理操作走 `OperationLogService.record()`。

## 6. 项目结构速查

```
src/main/java/com/campusshare/
├── controller/     接口层（admin/ 子包放管理接口）
├── service/        业务接口 + impl/ 实现
├── mapper/         MyBatis 接口（XML 在 resources/mapper/）
├── entity/ dto/ vo/ 实体 / 入参 / 出参
├── security/       JWT + Spring Security
├── task/           定时任务
├── common/ exception/ utils/ 通用 / 异常 / 工具
```

## 7. 常见问题

**Q：启动报 "Access denied for user 'root'"？**
MySQL 密码不对，设置 `DB_PASSWORD` 环境变量。

**Q：启动报 Redis 连接失败？**
Redis 没启动，或密码不对（设置 `REDIS_PASSWORD`）。注意：Redis 连不上应用仍能启动（降级），但缓存/防重/限流不生效。

**Q：接口返回 401 "未登录或登录已过期"？**
没带 `Authorization` 头，或 token 过期（默认 24 小时），重新登录。

**Q：跑 `mvn test` 失败，提示没有上架资源？**
先执行 `sql/data.sql` 灌入种子数据（含 3 个已上架资源）。
