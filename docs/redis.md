# Redis 设计

Redis 在本项目中扮演**加速层**角色，负责四件事：缓存、防重复提交、并发预检、限流。原则是**Redis 挡流量，MySQL 定事实**——任何 Redis 故障都降级放行，不阻断主流程。

## 1. Key 设计

统一前缀 `campusshare:`，按用途分四类：

| Key | 类型 | 用途 | TTL |
| --- | --- | --- | --- |
| `campusshare:resource:detail:{id}` | String | 资源详情缓存（JSON） | 30 分钟 |
| `campusshare:dedup:reserve:{userId}:{resourceId}:{start}:{end}` | String | 防重复提交 | 3 秒 |
| `campusshare:reserve:slots:{resourceId}` | ZSET | 资源时段占用 | 24 小时 |
| `campusshare:ratelimit:{userId}:reserve` | Hash | 令牌桶限流 | 60 秒 |

## 2. 资源缓存（Cache Aside）

### 读

```
getResource(id)
  ├─ 查缓存 campusshare:resource:detail:{id}
  │     ├─ 命中 → 直接返回
  │     └─ 未命中 → 查 MySQL → 回填缓存（仅已上架资源）
  └─ ...
```

### 写

```
updateResource / changeStatus / deleteResource / audit
  └─ 先改 MySQL → 再删缓存（evict）
```

**为什么"先改库、再删缓存"？** 反过来（先删缓存再改库）在并发下会出问题：A 删了缓存 → B 读库回填旧值 → A 再改库，缓存里就是脏数据。"先改库、再删缓存"保证删完之后，下一个读请求一定回填的是最新值。

### 为什么只缓存已上架资源

下架/待审核资源**不缓存**。原因：缓存是"绕过业务判断"的快通道，如果把下架资源也缓存了，普通用户可能通过缓存读到本不该看到的资源（越权泄露）。只缓存已上架资源，从根上杜绝这个问题。

## 3. 防重复提交（SETNX）

```java
Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(3));
```

- Key 含 `userId + resourceId + startTime + endTime`，同一用户对**完全相同**的预约请求，3 秒内只会放行第一次。
- `SETNX`（setIfAbsent）是原子操作，并发下也只有一个能设置成功。

## 4. 时段占用预检（Lua + ZSET）

每个资源一个 ZSET：

- `member` = `startTime`（epoch 毫秒）
- `score` = `endTime`（epoch 毫秒）

Lua 脚本原子完成「查重叠 + 占位」，详细逻辑见 [concurrency.md](concurrency.md) 第 3.③ 节。

## 5. 令牌桶限流（Lua）

每个用户一个 Hash 存 `{ tokens, last }`：

- 每次请求先按「流逝时间 × 补充速率」补令牌；
- 有令牌则扣 1 放行，没有则拒绝（429）。

参数：容量 10，每秒补充 1 个。防止单个用户高频刷接口。

## 6. 降级策略

所有 Redis 操作都包在 try/catch 里，异常时：

| 场景 | 降级行为 |
| --- | --- |
| 缓存读失败 | 当未命中，直接查 MySQL |
| 缓存写失败 | 忽略（缓存只是加速） |
| 缓存删失败 | 忽略（最坏是缓存多活 30 分钟） |
| 防重 SETNX 失败 | 放行（交给 MySQL 冲突检测兜底） |
| Lua 预检失败 | 放行（交给 MySQL 兜底） |
| 限流失败 | 放行（放弃限流，不阻断业务） |

核心逻辑：**Redis 是"锦上添花"的加速层，绝不能因为它挂了导致核心业务不可用。** 最终正确性由 MySQL 的事务和唯一约束保证。

## 7. 相关代码

- `ResourceServiceImpl`：缓存读写（getCached / putCache / evictResourceCache）
- `ReservationServiceImpl`：防重、Lua 预检、令牌桶、释放占位
- `RESERVE_SLOT_LUA` / `TOKEN_BUCKET_LUA`：两个 Lua 脚本
