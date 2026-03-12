# OpenClaw Gateway (Java)

Gateway service for OpenClaw Java Edition, compatible with Node.js version 2026.3.9.

## Features

- **Node Registry**: Manage connected nodes
- **Work Queue**: Priority-based work queue
- **Work Dispatcher**: Distribute work to nodes
- **Heartbeat**: Node health monitoring

## Architecture

```
openclaw-gateway/
├── GatewayService      # Main gateway interface
├── node/               # Node registry
├── queue/              # Work queue
└── work/               # Work dispatcher
```

## Usage

### Initialize Gateway

```java
GatewayService gateway = new DefaultGatewayService();

GatewayConfig config = GatewayConfig.builder()
    .port(8080)
    .maxNodes(100)
    .queueCapacity(10000)
    .workerThreads(10)
    .build();

gateway.initialize(config).join();
```

### Register Node

```java
NodeRegistry registry = gateway.getNodeRegistry();

NodeInfo node = NodeInfo.builder()
    .id("node-1")
    .name("Worker Node 1")
    .host("192.168.1.100")
    .port(8081)
    .capabilities(List.of("agent", "tool"))
    .build();

RegistrationResult result = registry.registerNode(node).join();
```

### Submit Work

```java
WorkItem work = WorkItem.of(
    WorkType.AGENT_RUN,
    "Hello, process this".getBytes()
);

String workId = gateway.submitWork(work).join();

// Check status
WorkStatus status = gateway.getWorkStatus(workId).join();
```

### Work Queue

```java
WorkQueue queue = gateway.getWorkQueue();

// Enqueue
queue.enqueue(work).join();

// Dequeue
Optional<WorkItem> item = queue.dequeue().join();

// Get stats
QueueStats stats = queue.getStats().join();
System.out.println("Queue utilization: " + stats.utilizationPercent() + "%");
```

### Work Dispatcher

```java
WorkDispatcher dispatcher = gateway.getWorkDispatcher();

// Set strategy
dispatcher.setStrategy(DispatchStrategy.LEAST_LOADED);

// Dispatch
DispatchResult result = dispatcher.dispatch(work).join();
```

## Work Types

- `AGENT_RUN` - Run an agent
- `AGENT_SPAWN` - Spawn a subagent
- `CHANNEL_MESSAGE` - Process channel message
- `TOOL_EXECUTION` - Execute a tool
- `MEMORY_UPDATE` - Update memory
- `SYSTEM_MAINTENANCE` - System maintenance

## Dispatch Strategies

- `ROUND_ROBIN` - Distribute evenly
- `LEAST_LOADED` - Send to least busy node
- `RANDOM` - Random selection
- `AFFINITY` - Stick to same node
- `PRIORITY` - Priority-based

## Statistics

- **Total Java files**: 5
- **Queue capacity**: Configurable
- **Node limit**: Configurable
- **Dispatch strategies**: 5

## Next Steps

1. Add persistent queue storage
2. Implement node auto-discovery
3. Add load balancing metrics
4. Implement work retry logic
