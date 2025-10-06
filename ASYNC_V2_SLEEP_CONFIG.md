# Async V2 Endpoint Sleep Configuration

## Overview

The **async V2 endpoint** `GET /api/products/v2/{id}` now includes a configurable sleep that runs **in the taskExecutor thread pool** before returning the response. This is perfect for:

- ✅ **Testing Thread Pool behavior** - See taskExecutor threads in action!
- ✅ **Load testing async endpoints** - Simulate sustained async workload
- ✅ **Performance comparison** - Compare sync vs async under load
- ✅ **Thread pool monitoring** - Watch the Thread Pool Dashboard come alive!

## 🎯 Key Difference: This Sleep USES TaskExecutor!

### Sync Endpoint (V1) - Uses Tomcat Threads
```java
@GetMapping("/{id}")  // ❌ Does NOT use taskExecutor
public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
    Thread.sleep(productApiSleepMs);  // Blocks TOMCAT thread
    return ResponseEntity.ok(response);
}
```
**Thread**: `http-nio-8080-exec-1` (Tomcat)  
**Dashboard**: Shows in Tomcat Dashboard only

### Async Endpoint (V2) - Uses TaskExecutor! ✅
```java
@GetMapping("/v2/{id}")  // ✅ USES taskExecutor
public CompletableFuture<ResponseEntity<ProductResponse>> getProductByIdV2(@PathVariable Long id) {
    // Sleep happens in taskExecutor thread!
    Thread.sleep(productApiV2SleepMs);  // Blocks TASKEXECUTOR thread
    return ResponseEntity.ok(response);
}
```
**Thread**: `async-1`, `async-2`, etc. (TaskExecutor)  
**Dashboard**: Shows in **Thread Pool Dashboard**! 🎉

## Configuration

### Property
```properties
# application.properties
product.api.v2.sleep.ms=1000
```

### Parameters
- **Property Name**: `product.api.v2.sleep.ms`
- **Type**: `long` (milliseconds)
- **Default**: `0` (no sleep)
- **Location**: Runs in `taskExecutor` thread pool
- **Timing**: Before returning response, after all processing

### Examples

**No sleep:**
```properties
product.api.v2.sleep.ms=0
```

**1 second sleep (recommended for testing):**
```properties
product.api.v2.sleep.ms=1000
```

**3 seconds sleep (high load testing):**
```properties
product.api.v2.sleep.ms=3000
```

**5 seconds sleep (extreme load testing):**
```properties
product.api.v2.sleep.ms=5000
```

## 🎯 NOW You'll See Thread Pool Activity!

### Test Commands

**Generate load on V2 async endpoint:**
```bash
# This WILL show activity in Thread Pool Dashboard!
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```

**Watch Thread Pool Dashboard in real-time:**
```bash
# Terminal 1: Open dashboard
open http://localhost:8080/dashboard/threadpool

# Terminal 2: Generate load
ab -n 2000 -c 100 http://localhost:8080/api/products/v2/1
```

**Monitor metrics:**
```bash
# Watch metrics update
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq'
```

## Request Flow

### With `product.api.v2.sleep.ms=2000`

```
Time    Action                              Thread Type
-----   ------                              -----------
0ms     Request received                    Tomcat (http-nio-8080-exec-1)
5ms     Async task submitted                → TaskExecutor
10ms    Fetch product from DB               TaskExecutor (async-1)
15ms    DB connection released              ✅ Connection back to pool
20ms    Call Mock API (3s delay)            TaskExecutor (async-1)
3020ms  Mock API response received          TaskExecutor (async-1)
3020ms  Start sleep (2000ms) ⏰             TaskExecutor (async-1) ← HOLDS THREAD
5020ms  Sleep complete                      TaskExecutor (async-1)
5020ms  Build response                      TaskExecutor (async-1)
5025ms  Return to client                    Tomcat thread

Total: ~5 seconds
TaskExecutor thread held: ~5 seconds (during API + sleep)
DB connection held: ~5ms only! ✅
```

## 📊 What You'll See in Dashboards

### Thread Pool Dashboard (http://localhost:8080/dashboard/threadpool)

**Before load test:**
```
Active Threads: 0
Pool Size: 0
Queue Size: 0
Completed Tasks: 0
```

**During load test (50 concurrent requests):**
```
Active Threads: 20        ← ✅ NOW YOU SEE ACTIVITY!
Pool Size: 20             ← ✅ Threads created
Queue Size: 30            ← ✅ Requests waiting
Completed Tasks: 150      ← ✅ Growing counter
Utilization: 100%         ← ✅ Pool is busy!
```

### Tomcat Dashboard (http://localhost:8080/dashboard/tomcat)

**During load test:**
```
Current Threads: 50       ← Tomcat threads handling requests
Busy Threads: 50          ← All busy submitting async tasks
Max Threads: 400          ← Still has capacity
```

### HikariCP Dashboard (http://localhost:8080/dashboard/hikari)

**During load test:**
```
Active Connections: 5     ← Only during DB queries
Idle Connections: 5       ← Connections released quickly
Total Connections: 10     ← Pool not exhausted! ✅
```

## Use Cases

### 1. Testing Thread Pool Under Load

**Configuration:**
```properties
product.api.v2.sleep.ms=2000
```

**Test:**
```bash
# Generate sustained load
ab -n 5000 -c 100 -t 60 http://localhost:8080/api/products/v2/1
```

**Observe:**
- Thread Pool Dashboard shows high activity
- Active threads increase to max pool size
- Queue fills up when pool is exhausted
- Completed task count grows

### 2. Comparing Sync vs Async Performance

**Setup:**
```properties
# Sync endpoint sleep
product.api.sleep.ms=2000

# Async endpoint sleep
product.api.v2.sleep.ms=2000
```

**Test Sync (V1):**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/1
# Watch: Tomcat Dashboard
# Observe: Tomcat threads maxed out
```

**Test Async (V2):**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
# Watch: Thread Pool Dashboard
# Observe: TaskExecutor threads maxed out, Tomcat threads free!
```

### 3. Testing Thread Pool Exhaustion

**Configuration:**
```properties
# Long sleep to hold threads
product.api.v2.sleep.ms=10000
```

**Test:**
```bash
# Flood with requests
ab -n 10000 -c 200 http://localhost:8080/api/products/v2/1
```

**Observe:**
- Thread Pool fills up (20 threads by default)
- Queue starts growing (50 capacity by default)
- Requests start timing out when queue is full
- HikariCP pool remains healthy (connections released quickly)

### 4. Optimal Configuration Testing

Find the sweet spot for your application:

```properties
# Test different values
product.api.v2.sleep.ms=500   # Low
product.api.v2.sleep.ms=1000  # Medium
product.api.v2.sleep.ms=2000  # High
```

Monitor:
- Response times
- Thread pool utilization
- Queue depth
- Error rates

## Implementation Details

### Code Location
- **File**: `ProductServiceAsync.java`
- **Method**: `getProductWithMockApiAsync(Long id)`
- **Execution**: Inside `thenCombine()` lambda (taskExecutor thread)

### Code Snippet
```java
// Step 3: Combine results
return productFuture.thenCombine(mockApiFuture, (product, mockApiResponse) -> {
    log.info("[ASYNC] Combining results for product {} - Total time: {}ms", id, totalTime);
    
    // Configurable sleep before returning response (runs in taskExecutor thread)
    if (productApiV2SleepMs > 0) {
        try {
            log.info("[ASYNC-V2] Sleeping for {} ms in taskExecutor thread: {}", 
                productApiV2SleepMs, Thread.currentThread().getName());
            Thread.sleep(productApiV2SleepMs);
            log.info("[ASYNC-V2] Sleep completed in thread: {}", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            log.error("[ASYNC-V2] Thread interrupted during sleep: {}", e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted during async sleep", e);
        }
    }
    
    return ProductResponse.builder()
        // ... build response
        .build();
});
```

### Thread Names
You'll see these thread names in logs:
- `async-1`, `async-2`, `async-3`, etc. (TaskExecutor threads)
- NOT `http-nio-8080-exec-X` (Tomcat threads)

### Log Output Example

```
INFO  - [ASYNC] Starting non-blocking product fetch for ID: 1
INFO  - [ASYNC] Fetching product 1 from database - Thread: async-1
INFO  - [ASYNC] Product 1 fetched successfully, DB connection will be released - Thread: async-1
INFO  - [ASYNC] Product fetched, now calling mock API without holding DB connection
INFO  - [ASYNC] Calling mock API for product 1 - Thread: async-2, NO DB connection held
INFO  - [ASYNC] Mock API call completed for product 1 - Thread: async-2
INFO  - [ASYNC] Combining results for product 1 - Total time: 3015ms
INFO  - [ASYNC-V2] Sleeping for 2000 ms in taskExecutor thread: async-1
INFO  - [ASYNC-V2] Sleep completed in thread: async-1
```

## Monitoring Commands

### Real-time Thread Pool Metrics
```bash
# Watch metrics
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq ".activeCount, .poolSize, .queueSize"'
```

### Load Test with Monitoring
```bash
# Terminal 1: Monitor
while true; do
  curl -s http://localhost:8080/api/threadpool/metrics | \
    jq -r '"Active: \(.activeCount) | Pool: \(.poolSize) | Queue: \(.queueSize) | Completed: \(.completedTaskCount)"'
  sleep 1
done

# Terminal 2: Generate load
ab -n 5000 -c 100 http://localhost:8080/api/products/v2/1
```

### Check Thread Names
```bash
# See which threads are running
jstack <pid> | grep -A 5 "async-"
```

## Comparison: V1 vs V2

| Aspect | V1 (Sync) `/api/products/{id}` | V2 (Async) `/api/products/v2/{id}` |
|--------|--------------------------------|-------------------------------------|
| **Thread Type** | Tomcat HTTP threads | TaskExecutor async threads |
| **Sleep Property** | `product.api.sleep.ms` | `product.api.v2.sleep.ms` |
| **Thread Name** | `http-nio-8080-exec-X` | `async-X` |
| **Dashboard** | Tomcat Dashboard | **Thread Pool Dashboard** ✅ |
| **DB Connection** | Held during sleep ❌ | Released before sleep ✅ |
| **Scalability** | Limited by Tomcat threads (400) | Limited by TaskExecutor (20) |
| **Best For** | Simple requests | I/O-bound operations |

## Configuration Examples

### Development (No Sleep)
```properties
product.api.v2.sleep.ms=0
```

### Light Load Testing
```properties
product.api.v2.sleep.ms=1000
```

### Heavy Load Testing
```properties
product.api.v2.sleep.ms=3000

# Also increase thread pool size
# In ConnectionPoolDemoApplication.java:
executor.setMaxPoolSize(50);
executor.setQueueCapacity(100);
```

### Extreme Stress Testing
```properties
product.api.v2.sleep.ms=10000

# Keep default small pool to force exhaustion
# Max: 20 threads
# Queue: 50 capacity
```

## Troubleshooting

### Thread Pool shows zero?
- ✅ Make sure you're testing `/api/products/v2/{id}` (not `/api/products/{id}`)
- ✅ Check sleep is enabled: `product.api.v2.sleep.ms > 0`
- ✅ Generate enough load: `ab -n 1000 -c 50`

### Pool exhausted too quickly?
- Increase max pool size in `ConnectionPoolDemoApplication.java`
- Increase queue capacity
- Reduce sleep duration

### Application hanging?
- Sleep too long + too many requests = thread pool exhaustion
- Reduce `product.api.v2.sleep.ms`
- Reduce concurrent requests in load test

## Summary

✅ **Added**: Configurable sleep in async V2 endpoint  
✅ **Property**: `product.api.v2.sleep.ms` (milliseconds)  
✅ **Default**: `0` (disabled)  
✅ **Thread Pool**: Uses **taskExecutor** (async threads)  
✅ **Dashboard**: Shows in **Thread Pool Dashboard**! 🎉  
✅ **Location**: Before returning response  
✅ **Best For**: Testing async thread pool behavior  

## Quick Start

1. **Configure sleep:**
   ```properties
   product.api.v2.sleep.ms=2000
   ```

2. **Start application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Open Thread Pool Dashboard:**
   ```
   http://localhost:8080/dashboard/threadpool
   ```

4. **Generate load:**
   ```bash
   ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
   ```

5. **Watch the magic happen!** ✨
   - Active threads increase
   - Pool size grows
   - Queue fills up
   - Completed tasks counter increases

**NOW your Thread Pool Dashboard will show real activity!** 🚀
