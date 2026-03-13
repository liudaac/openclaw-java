# OpenClaw Cron Module

企业级定时任务调度模块，对标 Node.js 版 OpenClaw 的 Cron 系统。

## 特性

- ✅ **Cron 表达式解析** - 支持标准 Unix、Quartz、Spring 格式
- ✅ **持久化存储** - SQLite 存储，重启不丢失
- ✅ **任务隔离执行** - 子进程隔离，安全可控
- ✅ **完整状态机** - 6 种状态：PENDING、RUNNING、PAUSED、COMPLETED、FAILED、CANCELLED
- ✅ **执行历史** - 完整的执行记录和统计
- ✅ **重试机制** - 可配置重试次数和间隔
- ✅ **时区支持** - 支持任意时区

## 架构

```
openclaw-cron/
├── model/           # 领域模型
│   ├── CronJob.java
│   ├── JobStatus.java
│   ├── JobStatusMachine.java
│   └── JobExecution.java
├── store/           # 持久化层
│   ├── CronJobStore.java
│   └── SQLiteCronJobStore.java
├── executor/        # 执行器
│   ├── JobExecutor.java
│   └── IsolatedJobExecutor.java
├── scheduler/       # 调度器
│   └── CronExpressionParser.java
└── service/         # 服务层
    └── CronService.java
```

## 依赖

```xml
<dependency>
    <groupId>com.cronutils</groupId>
    <artifactId>cron-utils</artifactId>
    <version>9.2.1</version>
</dependency>
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.44.1.0</version>
</dependency>
```

## 使用示例

```java
@Autowired
private CronService cronService;

// 创建定时任务
CronJob job = cronService.createJob(
    "daily-backup",
    "0 0 2 * * *",  // 每天凌晨 2 点
    "backup.sh"
).join();

// 列出所有任务
List<CronJob> jobs = cronService.listJobs().join();

// 暂停任务
cronService.pauseJob(job.getId()).join();

// 恢复任务
cronService.resumeJob(job.getId()).join();

// 立即触发
cronService.triggerJob(job.getId()).join();

// 删除任务
cronService.deleteJob(job.getId()).join();

// 查看执行历史
List<JobExecution> history = cronService.getJobHistory(job.getId(), 10).join();
```

## 与原版对比

| 功能 | Node.js 原版 | Java 版 |
|------|-------------|---------|
| Cron 表达式 | ✅ node-cron | ✅ cron-utils |
| 持久化 | ✅ SQLite | ✅ SQLite |
| 隔离执行 | ✅ 子进程 | ✅ 子进程 |
| 状态机 | ✅ 6 状态 | ✅ 6 状态 |
| WebSocket 通知 | ✅ 有 | ⚠️ 待实现 |
| 分布式调度 | ✅ Redis | ⚠️ 待实现 |

## 待实现

- [ ] WebSocket 实时通知
- [ ] 分布式调度 (Redis)
- [ ] DAG 任务依赖
- [ ] 任务分片
