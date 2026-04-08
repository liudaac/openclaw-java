# Java版 OpenClaw Phase 4 迭代计划 - 2026-04-08

## 目标

处理剩余的高优先级任务：
1. ACP Discord 恢复和重置流程基础
2. Memory Wiki 基础结构准备

## 任务1: ACP Discord 恢复和重置流程基础

### 分析
原版提交 `f6124f3e17` 包含：
- ACPX 更新到 0.5.2
- Discord 斜杠命令后使用 follow-up replies
- ACP 绑定重置通过 Gateway 服务路由
- 统一绑定重置权限
- 修复 Claude 启动和重置恢复

### Java版当前状态
- ACP 模块仅有基础审批分类功能
- 缺少完整的 ACP 控制平面实现
- 缺少 Discord 集成

### 实施步骤

#### 1.1 创建 ACP 绑定管理基础
- 创建 `AcpBindingManager` 接口
- 定义绑定生命周期方法
- 支持重置和恢复流程

#### 1.2 创建 Discord 集成基础
- 创建 `DiscordAcpAdapter` 类
- 支持 follow-up replies
- 集成到 Gateway 服务

#### 1.3 更新 Gateway 服务
- 添加绑定重置路由支持
- 统一权限检查

## 任务2: Memory Wiki 基础结构准备

### 分析
原版提交 `947a43dae3` 包含：
- Belief-layer digests 功能
- 兼容迁移支持
- Public artifacts 模块
- Memory Core dreaming phases 重构
- Plugin SDK memory-core 类型

### Java版当前状态
- Memory 模块有基础配置
- 缺少 Wiki 功能
- 缺少 dreaming 实现
- 缺少 belief-layer 支持

### 实施步骤

#### 2.1 扩展 Memory 配置
- 添加 Wiki 配置支持
- 添加 belief-layer 配置

#### 2.2 创建 Memory Wiki 基础类
- 创建 `MemoryWikiService` 接口
- 创建 `BeliefLayerDigest` 类
- 创建 `PublicArtifact` 类

#### 2.3 准备 dreaming 扩展点
- 在 Memory 模块中添加 dreaming 扩展接口
- 为后续完整实现做准备

## 实施顺序

1. ACP 绑定管理基础
2. Memory Wiki 基础结构
3. 单元测试

## 预期产出

- ACP 绑定管理接口和基础实现
- Memory Wiki 基础类和配置
- 相关单元测试

## 说明

由于原版变更是重大功能更新，Java 版需要分阶段实现。本次迭代专注于基础结构和接口定义，为后续完整实现做准备。

---

*计划创建时间: 2026-04-08*
