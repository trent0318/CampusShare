# 并发预约与时间冲突方案

## 1. 要解决的问题

1 个名额的资源（如一个自习室），多个用户并发预约**同一时段**，必须**恰好 1 人成功**，不能超卖、不能重复。

更具体地说，两个请求的时段只要**有重叠**，就只能有 1 个成功；**首尾相接不算重叠**（10:00-12:00 和 12:00-14:00 都能约）。

## 2. 时间冲突判定：区间重叠算法

判断两个时段 `[s1, e1)` 和 `[s2, e2)` 是否重叠：

```
s1 < e2  且  s2 < e1
```

- `s1 < e2`：A 的开始早于 B 的结束
- `s2 < e1`：B 的开始早于 A 的结束

两个条件同时成立才算重叠。用**严格小于**，所以端点相等（首尾相接）不算重叠。

SQL 里的冲突查询（`countConflict`）：

```sql
SELECT COUNT(*) FROM reservation
WHERE resource_id = #{resourceId}
  AND status IN ('CONFIRMED', 'IN_USE')
  AND start_time < #{endTime}
  AND end_time > #{startTime}
```

## 3. 四层防御

并发预约不是靠单一手段，而是层层设防、逐层兜底：

```
1000 个并发请求抢同一时段
   │
   ├─ ① 令牌桶限流（Redis）       每用户独立 key，防单用户刷接口
   ├─ ② 防重复提交（Redis SETNX） 同一用户同一时段 3 秒内只放行一次
   ├─ ③ Lua 原子预检（Redis ZSET）时段区间重叠判断，单线程原子执行
   └─ ④ 事务兜底（MySQL）        行锁 + 冲突查询 + 唯一键，最终事实
```

### ① 令牌桶限流

每用户一个令牌桶（容量 10，每秒补充 1 个）。超过就拒绝（429）。作用是防**单个用户**高频刷接口，不是并发主防线。

### ② 防重复提交（SETNX + TTL）

Key：`campusshare:dedup:reserve:{userId}:{resourceId}:{startTime}:{endTime}`，`SETNX` 成功才放行，3 秒过期。防的是**同一个用户**连点提交按钮产生重复请求。

### ③ Lua 原子预检（核心）

Redis 的 ZSET 存「某资源已占用的时段」：`member = startTime`，`score = endTime`。

Lua 脚本里做两件事（**单线程原子执行**）：

1. `ZRANGEBYSCORE` 取出所有 `endTime > 当前startTime` 的已占时段；
2. 逐个判断是否 `member(startTime) < 当前endTime`，有重叠就返回 0（拒绝）。

无重叠才 `ZADD` 占位并返回 1（放行）。

**为什么用 Lua？** 因为「查重叠 + 占位」必须原子完成。如果分成两条 Redis 命令，中间会有别的请求插进来，两个请求可能同时判断"没重叠"、同时占位，就超卖了。Lua 保证这一整段在 Redis 单线程里一次跑完。

### ④ MySQL 事务兜底

过了 ③ 的请求进入事务：

1. `SELECT ... FOR UPDATE` 锁住资源行（串行化同一资源的并发）；
2. `countConflict` 再查一次时间冲突（**MySQL 是最终权威**）；
3. `countActiveByUser` 查用户有效预约是否超上限；
4. `INSERT`（`uk_user_slot` 唯一键做最后兜底）。

任何一步失败，回滚事务并释放 Redis 占位。

### 降级设计

Redis 任何一步异常都**降级放行**（不报错），把判断交给 MySQL。所以 Redis 挂了系统仍正确，只是少了"快速拦截"这一层。这是本项目反复强调的原则：**Redis 挡流量，MySQL 定事实。**

## 4. 为什么 Redis 占位不能替代 MySQL 判断

Redis 的时段占位（ZSET）有 24 小时 TTL，只是"缓存式"的快速预检，可能丢、可能过期。真正决定"能不能预约"的是 MySQL 里的预约记录和 `countConflict` 查询。Redis 负责快，MySQL 负责准。

## 5. 压测结果

环境无 JMeter，用 JUnit `@SpringBootTest` + 真实 MySQL/Redis，`CountDownLatch` 让 N 个线程同时起跑：

| 并发数 | 成功 | 失败 |
| --- | --- | --- |
| 100 | 1 | 99 |
| 500 | 1 | 499 |
| 1000 | 1 | 999 |

**1 个名额、1000 并发，恰好 1 人成功，无超卖。** 测试代码见 `src/test/java/com/campusshare/ReservationConcurrencyTest.java`。

## 6. 相关代码

- `ReservationServiceImpl.createReservation`：预约主流程（四层防御）
- `RESERVE_SLOT_LUA`：时段冲突 Lua 脚本
- `TOKEN_BUCKET_LUA`：令牌桶 Lua 脚本
- `ReservationMapper.countConflict`：MySQL 冲突查询
- `reservation.uk_user_slot`：唯一约束兜底
