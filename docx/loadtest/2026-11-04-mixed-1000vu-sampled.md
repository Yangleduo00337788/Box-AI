# Box 压测报告：mixed 1000 VU（带主机采样）

| 项 | 内容 |
|----|------|
| 日期 | 2026-11-04 18:50–18:52（UTC+8） |
| 对照 | 同日 18:39 那一轮 **没有** Redis/MySQL/JDK 采样，见 [2026-11-04-mixed-1000vu.md](./2026-11-04-mixed-1000vu.md) |
| 目标 | 本机 `http://localhost:8080`，Java PID `2216`，profile `dev` |
| k6 | `.\run.ps1 -Scenario mixed -Vus 1000 -Duration 2m` |
| k6 摘要 | `loadtest/k6/results/mixed-20261104-185005.json` |
| 采样 | `loadtest/k6/sample-host.ps1`，间隔名义 5s；原始目录 `loadtest/k6/results/samples/mixed-1000vu-20261104-185000/`（gitignore） |
| JFR | 同目录 `box-loadtest.jfr`（52 MB，`settings=profile`） |

---

## 1. 先回答「上次开了没」

**第一次 1000 VU（18:39）没有开采样。** 当时只有 k6 HTTP 指标。

这一轮（18:50）才并行采集：

| 源 | 采了什么 |
|----|----------|
| JDK | `jstat -gcutil`、`GC.heap_info`、`jcmd Thread.print -l`、JFR |
| 进程 | WorkingSet、CPU 累计 |
| MySQL | `SHOW GLOBAL STATUS`（连接/Questions/慢查询/InnoDB 锁）、`PROCESSLIST` |
| Redis | `INFO stats/clients/memory`、`SLOWLOG GET 16` |
| 连接 | 8080 套接字数量 |

采样本身偏重：每次 tick 打全量线程栈 + Windows `Get-NetTCPConnection`，单 tick 被拉到 **12～76 秒**，2 分钟里只落到 **6 个点**，并明显加重了 JVM。因此 **本轮 k6 数字差于未采样那一轮，不能拿来和 18:39 比容量**，只用来看瓶颈形态。

---

## 2. 本轮 k6（采样干扰下）

阈值 **未通过**。

| 指标 | 阈值 | 18:39 无采样 | **18:50 有采样** |
|------|------|--------------|------------------|
| 失败率 | < 1% | 0.75% | **7.79%** |
| 检查通过率 | > 99% | 99.24% | **92.20%** |
| p95 | < 800ms | 283ms | **741ms** |
| 最大延迟 | — | 1.12s | **23.1s** |
| 吞吐 | — | 3457 req/s | 2131 req/s |
| HTTP 次数 | — | 417,208 | 257,687 |

失败仍以开闸瞬间 `connection refused` 为主，叠加线程 dump 导致 Tomcat 工作线程长时间停顿。

---

## 3. 时间线（series.csv）

| 时刻 | Java RSS | MySQL Questions | 估算 8080 套接字 | 备注 |
|------|----------|-----------------|------------------|------|
| 18:50:01 | 890 MB | 3,425,933 | 1 | 压测刚启动 |
| 18:50:10 | 1015 MB | 3,493,387 | 1002 | 约 +7.5k MySQL QPS |
| 18:51:26 | 967 MB | 4,499,439 | **2257** | 峰值；约 +13k MySQL QPS |
| 18:52:16 | 1024 MB | 5,120,155 | 1833 | k6 已结束，Questions 几乎不再涨 |
| 18:52:41 | 1034 MB | 5,120,174 | 1833 | 冷却 |

进程 RSS 从 890 MB 顶到 **1034 MB**。Java CPU 累计从 967s 升到 1658s（该进程从 18:16 启动算起的累计 CPU 时间）。

---

## 4. JDK / 栈

### 4.1 堆与 GC（G1）

| | tick-001（空载附近） | tick-002（加压） | tick-003 | tick-006（结束后） |
|--|----------------------|------------------|----------|---------------------|
| Eden | 93.5% | 34.4% | 90.2% | 50.7% |
| Old | 63.0% | 63.1% | 66.3% | 67.4% |
| Metaspace | 98.5% | 98.5% | 98.5% | 98.5% |
| Young GC 次数 | 708 | 713 | 871 | **973** |
| Full GC | 0 | 0 | 0 | **0** |
| 并发 GC 次数 | 312 | 312 | 314 | 314 |
| GC 累计时间 | 2.38s | 2.40s | 3.06s | 3.37s |
| heap used | — | 249 MB / 494 MB | 181 MB / 488 MB | — |

结论：没有 Full GC，Young GC 在约 2.5 分钟内 +265 次，总停顿仍只有约 1s。**瓶颈不是 GC 和堆不够。**

### 4.2 Tomcat 线程

`http-nio-8080-exec-*` 数量：

| tick | RUNNABLE | BLOCKED | WAITING | TIMED_WAITING | http-nio-exec |
|------|----------|---------|---------|---------------|---------------|
| 001 空载 | 29 | 0 | 12 | 11 | **10** |
| 002 加压 | **117** | 1 | 80 | 46 | **200** |
| 003～006 | ~20 | 0 | 2 | 211 | **200** |

默认最大工作线程 **200 打满**。高峰过后大量 `TIMED_WAITING`（池内空闲），符合「队列打满 → 拒连 / 排队 → 结束后线程还在池里」。

tick-002 唯一 `BLOCKED` 栈：

```
http-nio-8080-exec-344  BLOCKED
  TomcatEmbeddedWebappClassLoader.loadClass
  io.jsonwebtoken.Jwts.parser
  com.boxai.security.jwt.JwtService.parse
  JwtAuthenticationFilter.doFilterInternal
```

JWT 解析路径上触发了 **类加载器锁**。并发一高，验票时偶发堵在 `loadClass`，不是 Redis、也不是 SQL 锁。

### 4.3 JFR

已写出 `box-loadtest.jfr`（52 MB）。可用 JDK Mission Control 打开看锁、套接字、GC。不要提交进 git。

---

## 5. MySQL

全程：

| 项 | 值 |
|----|-----|
| Threads_connected | **11**（不变） |
| Threads_running | **2** |
| Max_used_connections | 11 |
| Slow_queries | 0 |
| Innodb_row_lock_waits | 0 |
| Innodb_buffer_pool_wait_free | 0 |
| Connection_errors_max_connections | 0 |
| Aborted_connects | 1 |

Hikari 连接数打满在 11 左右，库侧几乎空闲。Questions 在高峰约 **1.2万～1.3万 QPS**，没有行锁等待。**MySQL 不是这次拒连和 p95 的原因。**

---

## 6. Redis

| 项 | 值 |
|----|-----|
| connected_clients | 2 |
| instantaneous_ops_per_sec | 0（采样瞬间） |
| total_commands_processed | ~163（tick-002） |
| used_memory | ~1.1 MB |
| rejected_connections | 0 |
| slowlog | 空 |

本场景几乎不打 Redis（登录只在 setup 一次；读列表不走限流计数）。**Redis 空闲。**

---

## 7. 结论

1. 第一次 1000 VU **没采主机**；这一轮才采到 JDK/MySQL/Redis/栈。
2. 瓶颈在 **应用进程 / Tomcat**：默认 200 工作线程打满、8080 套接字冲到 2000+、开闸 `connection refused`。偶发 JWT `ClassLoader` BLOCKED。
3. **MySQL、Redis、堆、Full GC 都不是瓶颈。**
4. 采样脚本过重，本轮失败率和延迟被采样污染。脚本已改为：线程 dump 每 N 个 tick 一次，去掉 `Get-NetTCPConnection`。下次应用：`.\sample-host.ps1 -IntervalSec 5 -DumpEvery 3 -DurationSec 180`。
