# OpenClaw Tasks Module

Task flow runtime and registry for OpenClaw Java Edition.

## Overview

This module implements the ClawFlow runtime, which allows work to span one or more detached tasks while behaving like one job with a single owner context.

## Features

- **Flow Management**: Create, update, and manage task flows
- **Task Execution**: Queue and run tasks within flows
- **State Machine**: Track flow and task status transitions
- **Output Storage**: Persist flow outputs between steps
- **SQLite Persistence**: Local database storage for flows and tasks

## Architecture

```
openclaw-tasks/
├── model/           # Data models (FlowRecord, TaskRecord, enums)
├── registry/        # Storage interfaces and implementations
│   ├── FlowRegistry
│   ├── TaskRegistry
│   ├── SqliteFlowRegistry
│   └── SqliteTaskRegistry
├── runtime/         # Core runtime logic
│   └── FlowRuntime
├── executor/        # Task execution
│   └── TaskExecutor
├── delivery/        # Message delivery
│   └── FlowDeliveryService
└── config/          # Spring configuration
    └── TasksConfig
```

## Usage

### Creating a Flow

```java
FlowRuntime.CreateFlowParams params = new FlowRuntime.CreateFlowParams(
    "owner-session-key",
    null,  // requesterOrigin
    "Triage inbox messages",
    TaskNotifyPolicy.STATE_CHANGES,
    "initial_step",
    null,  // createdAt
    null   // updatedAt
);
FlowRecord flow = flowRuntime.createFlow(params);
```

### Running a Task in Flow

```java
FlowRuntime.RunTaskParams taskParams = new FlowRuntime.RunTaskParams(
    flow.getFlowId(),
    TaskRuntime.ACP,
    null,  // sourceId
    null,  // childSessionKey
    null,  // parentTaskId
    "agent-1",
    null,  // runId
    "classification-task",
    "Classify inbox messages",
    false, // preferMetadata
    TaskNotifyPolicy.STATE_CHANGES,
    TaskDeliveryStatus.PENDING,
    "queued",  // launch
    null,  // startedAt
    null,  // lastEventAt
    null,  // progressSummary
    "wait_for_classification"  // currentStep
);
FlowRuntime.TaskInFlowResult result = flowRuntime.runTaskInFlow(taskParams);
```

### Setting Flow Output

```java
FlowRuntime.SetFlowOutputParams outputParams = new FlowRuntime.SetFlowOutputParams(
    flow.getFlowId(),
    "classification",
    Map.of("route", "business"),
    null  // updatedAt
);
flowRuntime.setFlowOutput(outputParams);
```

### Completing a Flow

```java
FlowRuntime.FinishFlowParams finishParams = new FlowRuntime.FinishFlowParams(
    flow.getFlowId(),
    "completed_step",
    null,  // updatedAt
    null   // endedAt
);
flowRuntime.finishFlow(finishParams);
```

## Flow Status Lifecycle

```
QUEUED -> RUNNING -> WAITING -> RUNNING -> SUCCEEDED/FAILED/CANCELLED
              |         ^
              v         |
           (task execution)
```

## Testing

Run tests with Maven:

```bash
mvn test -pl openclaw-tasks
```

## Configuration

Database path can be configured via `application.properties`:

```properties
openclaw.tasks.db.path=${user.home}/.openclaw/tasks.db
```

## Dependencies

- Spring Boot
- SQLite JDBC
- Jackson (JSON processing)
- Lombok
- JUnit 5 (testing)

## License

Same as OpenClaw Java Edition
