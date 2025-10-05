# Thread Pool Monitoring Guide

## Overview

The Thread Pool Monitoring feature provides real-time insights into the TaskExecutor thread pool that handles asynchronous operations in the application. This helps you understand thread utilization, queue behavior, and overall concurrency patterns.

## Quick Access

- **Dashboard**: http://localhost:8080/dashboard/threadpool
- **API Metrics**: http://localhost:8080/api/threadpool/metrics
- **Real-time Stream**: http://localhost:8080/api/threadpool/stream

## Features

### 1. Real-time Dashboard
Beautiful web interface with:
- 📊 Live metrics cards showing active threads, pool size, queue size, and completed tasks
- 📈 Real-time charts tracking thread activity over time
- 🎯 Utilization progress bars for thread pool and queue
- ⚠️ Automatic warnings when utilization exceeds thresholds
- 🔄 Updates every second via Server-Sent Events

### 2. REST API Endpoints

#### Get Thread Pool Metrics
```
GET /api/threadpool/metrics
```
Returns current thread pool statistics including:
- Active thread count
- Pool size (current and maximum)
- Queue size and capacity
- Task counts (total and completed)
- Utilization percentages

#### Get Detailed Information
```
GET /api/threadpool/details
```
Returns comprehensive thread pool information:
- Basic info (name, class)
- Configuration (core size, max size, keep-alive time)
- Current state (all metrics)
- Status (shutdown, terminated, etc.)

#### Get Simple Status
```
GET /api/threadpool/status
```
Returns quick status check:
- Running status
- Active threads
- Pool size
- Queue size
- Health indicator

#### Real-time Stream
```
GET /api/threadpool/stream
```
Server-Sent Events stream with updates every second.
Perfect for dashboards and real-time monitoring tools.

#### Health Check
```
GET /api/threadpool/health
```
Simple health check endpoint.

## Key Metrics Explained

### Thread Metrics

- **Active Count**: Threads currently executing tasks
- **Pool Size**: Current number of threads in the pool
- **Core Pool Size**: Minimum number of threads kept alive
- **Maximum Pool Size**: Maximum allowed threads
- **Largest Pool Size**: Highest number of threads ever reached

### Queue Metrics

- **Queue Size**: Tasks waiting to be executed
- **Queue Capacity**: Total queue capacity
- **Queue Remaining Capacity**: Available queue space
- **Queue Utilization %**: Percentage of queue capacity in use

### Task Metrics

- **Task Count**: Total number of tasks ever submitted
- **Completed Task Count**: Total tasks successfully completed

### Utilization Metrics

- **Thread Utilization %**: (Active Count / Maximum Pool Size) × 100
- **Queue Utilization %**: (Queue Size / Queue Capacity) × 100

## Dashboard Features

### Visual Indicators

- **Status Dot**: 
  - 🟢 Green = Connected and streaming
  - 🔴 Red = Connection error

- **Utilization Bars**:
  - Green (0-60%): Healthy utilization
  - Yellow (61-80%): Moderate utilization
  - Red (>80%): High utilization

### Automatic Warnings

The dashboard shows warnings when:
- Thread utilization exceeds 80%
- Queue utilization exceeds 80%

### Real-time Charts

- **Thread Activity Chart**: Tracks active threads and pool size over last 60 seconds
- **Queue Activity Chart**: Tracks queue size over last 60 seconds

## Usage Examples

### View Dashboard
```bash
# Start the application
mvn spring-boot:run

# Open in browser
open http://localhost:8080/dashboard/threadpool
```

### Get Metrics via API
```bash
# Get current metrics
curl http://localhost:8080/api/threadpool/metrics | jq

# Get detailed info
curl http://localhost:8080/api/threadpool/details | jq

# Get simple status
curl http://localhost:8080/api/threadpool/status | jq
```

### Stream Real-time Data
```bash
# Stream metrics (press Ctrl+C to stop)
curl -N http://localhost:8080/api/threadpool/stream
```

## Configuration

The thread pool is configured in the main application class with `@EnableAsync` and a TaskExecutor bean. Current configuration:
- Core Pool Size: 5
- Max Pool Size: 10
- Queue Capacity: 25

You can adjust these in `ConnectionPoolDemoApplication.java`.

## Performance Tips

### Healthy Patterns
- Thread utilization stays below 70%
- Queue size remains low or zero
- No warning messages on dashboard

### Warning Signs
- ⚠️ Thread utilization consistently above 80%
- ⚠️ Queue size growing continuously
- ⚠️ Many tasks waiting in queue

### Optimization Strategies

1. **High Thread Utilization**:
   - Increase maximum pool size
   - Optimize task execution time
   - Review if tasks should be async

2. **High Queue Utilization**:
   - Increase queue capacity
   - Increase thread pool size
   - Implement backpressure

3. **Low Utilization**:
   - Reduce pool size to save resources
   - Consider fewer threads for better resource efficiency

## Integration with HikariCP Monitoring

The thread pool monitoring complements the HikariCP dashboard:
- **HikariCP Dashboard**: http://localhost:8080/dashboard/hikari
- **Thread Pool Dashboard**: http://localhost:8080/dashboard/threadpool

Together, they provide complete visibility into:
- Database connection usage (HikariCP)
- Async task execution (Thread Pool)
- Overall application concurrency

## Troubleshooting

### Dashboard Not Loading
1. Check application is running: `curl http://localhost:8080/api/threadpool/health`
2. Check browser console for errors
3. Verify port 8080 is accessible

### No Real-time Updates
1. Check SSE connection in browser Network tab
2. Verify `/api/threadpool/stream` endpoint is accessible
3. Check for firewall/proxy issues

### High CPU Usage
- If thread utilization is very high, consider:
  - Increasing thread pool size
  - Adding rate limiting
  - Implementing circuit breakers

## Comparison with HikariCP Monitoring

| Feature | Thread Pool | HikariCP |
|---------|------------|----------|
| **Monitors** | Async task execution | Database connections |
| **Key Metric** | Active threads | Active connections |
| **Queue** | Task queue | Connection wait queue |
| **Purpose** | Concurrency management | DB resource management |
| **Dashboard** | /dashboard/threadpool | /dashboard/hikari |

## API Response Examples

### Metrics Response
```json
{
  "poolName": "TaskExecutor",
  "corePoolSize": 5,
  "maximumPoolSize": 10,
  "activeCount": 3,
  "poolSize": 5,
  "largestPoolSize": 8,
  "taskCount": 1523,
  "completedTaskCount": 1520,
  "queueSize": 2,
  "queueRemainingCapacity": 23,
  "queueCapacity": 25,
  "utilizationPercent": 30,
  "queueUtilizationPercent": 8,
  "isShutdown": false,
  "timestamp": 1696521600000
}
```

## Best Practices

1. **Monitor Regularly**: Check dashboard during load testing
2. **Set Alerts**: Monitor utilization in production
3. **Tune Configuration**: Adjust pool size based on workload
4. **Compare Patterns**: Use with HikariCP monitoring for full picture
5. **Document Baseline**: Know your normal operating metrics

## Related Documentation

- [HikariCP Monitoring Guide](REALTIME_DASHBOARD_GUIDE.md)
- [Async API Guide](ASYNC_API_GUIDE.md)
- [Home Endpoint Guide](HOME_ENDPOINT_GUIDE.md)

---

**Thread Pool Monitoring Dashboard** - Real-time insights into async task execution! 🚀

