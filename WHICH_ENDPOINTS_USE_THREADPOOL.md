# Which Endpoints Use the Thread Pool?

## Understanding Thread Pool vs Database Connections

Your application has TWO separate resource pools:

1. **Database Connection Pool (HikariCP)** - For database queries
2. **Thread Pool (TaskExecutor)** - For async operations

They are **NOT the same thing**!

## Thread Pool Activity Map

### ✅ Endpoints That USE Thread Pool (Async)

These endpoints will show activity in the **Thread Pool Dashboard**:

#### Product Endpoints (Async)
- `GET /api/products/v2/{id}` - Uses `@Async getProductWithMockApiAsync()`
  - Example: `curl http://localhost:8080/api/products/v2/1`
  - Thread pool usage: **YES** ✅
  - Why: Calls `postmanEchoService.callMockApiAsync()` which is `@Async`

#### Customer Endpoints (Async)
- `GET /api/customers/async` - Uses `@Async getAllCustomersAsync()`
  - Example: `curl http://localhost:8080/api/customers/async`
  - Thread pool usage: **YES** ✅
  
- `GET /api/customers/async/{id}` - Uses `@Async getCustomerByIdAsync()`
  - Example: `curl http://localhost:8080/api/customers/async/1`
  - Thread pool usage: **YES** ✅
  
- `POST /api/customers/async` - Uses `@Async addNewCustomerAsync()`
  - Example: `curl -X POST http://localhost:8080/api/customers/async -H "Content-Type: application/json" -d '{"name":"Test"}'`
  - Thread pool usage: **YES** ✅

### ❌ Endpoints That DON'T Use Thread Pool (Synchronous)

These endpoints will **NOT** show in Thread Pool Dashboard (but will show in HikariCP Dashboard):

#### Product Endpoints (Sync)
- `GET /api/products/{id}` - Synchronous database query
  - Example: `curl http://localhost:8080/api/products/1`
  - Thread pool usage: **NO** ❌
  - Database connections: **YES** ✅ (Shows in HikariCP dashboard)
  
- `POST /api/products/{id}/process` - Synchronous processing
  - Example: `curl -X POST http://localhost:8080/api/products/1/process`
  - Thread pool usage: **NO** ❌
  - Database connections: **YES** ✅

- `GET /api/products` - Get all products
  - Thread pool usage: **NO** ❌
  - Database connections: **YES** ✅

- `POST /api/products` - Create product
  - Thread pool usage: **NO** ❌
  - Database connections: **YES** ✅

- `DELETE /api/products/{id}` - Delete product
  - Thread pool usage: **NO** ❌
  - Database connections: **YES** ✅

#### Customer Endpoints (Sync)
- `GET /api/customers` - Synchronous get all
  - Thread pool usage: **NO** ❌
  
- `GET /api/customers/{id}` - Synchronous get by ID
  - Thread pool usage: **NO** ❌
  
- `POST /api/customers` - Synchronous create
  - Thread pool usage: **NO** ❌
  
- `PUT /api/customers/{id}` - Synchronous update
  - Thread pool usage: **NO** ❌
  
- `DELETE /api/customers/{id}` - Synchronous delete
  - Thread pool usage: **NO** ❌

## Why You See All Zeros

### Problem: You tested these endpoints
```bash
# ❌ These are SYNCHRONOUS - Don't use thread pool
curl http://localhost:8080/api/products/2
curl http://localhost:8080/api/products/v2/2  # Too fast, completes instantly
```

### Solution: Test these endpoints instead
```bash
# ✅ These are ASYNC - Use thread pool
curl http://localhost:8080/api/products/v2/1 &
curl http://localhost:8080/api/products/v2/2 &
curl http://localhost:8080/api/products/v2/3 &
curl http://localhost:8080/api/customers/async &
curl http://localhost:8080/api/customers/async/1 &

# The '&' runs them in background to create concurrent load
```

## Load Testing for Thread Pool Activity

### Quick Test (10 concurrent requests)
```bash
for i in {1..10}; do
  curl -s "http://localhost:8080/api/customers/async" > /dev/null &
done

# Check metrics immediately
curl http://localhost:8080/api/threadpool/metrics | jq
```

### Medium Test (50 concurrent requests)
```bash
for i in {1..50}; do
  curl -s "http://localhost:8080/api/products/v2/$((i % 10 + 1))" > /dev/null &
  curl -s "http://localhost:8080/api/customers/async" > /dev/null &
done

# Open dashboard to watch
open http://localhost:8080/dashboard/threadpool
```

### Heavy Test (Using the provided script)
```bash
# Run the load test script
./test-threadpool-load.sh

# In another terminal, watch metrics
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq ".activeCount, .poolSize, .queueSize"'
```

### Continuous Load (60 RPS)
```bash
# This will generate ~60 requests per second
while true; do
  for i in {1..10}; do
    curl -s "http://localhost:8080/api/products/v2/$i" > /dev/null &
    curl -s "http://localhost:8080/api/customers/async/$i" > /dev/null &
  done
  sleep 0.3
done
```

## How to Verify Thread Pool Activity

### Step 1: Open Dashboard
```
http://localhost:8080/dashboard/threadpool
```

### Step 2: Run Load Test in Terminal
```bash
# Option A: Use the script
./test-threadpool-load.sh

# Option B: Manual concurrent requests
for i in {1..30}; do
  curl -s "http://localhost:8080/api/customers/async" > /dev/null &
done
```

### Step 3: Watch the Dashboard Update
You should see:
- **Active Threads**: Increasing as requests are processed
- **Pool Size**: Growing from 10 towards 20 (your max)
- **Queue Size**: Tasks waiting if pool is saturated
- **Completed Tasks**: Incrementing as tasks finish

## Expected Values During Load

### Light Load (10-20 RPS)
- Active Threads: 2-5
- Pool Size: 10 (core pool size)
- Queue Size: 0
- Completed Tasks: Incrementing

### Medium Load (40-60 RPS)
- Active Threads: 8-15
- Pool Size: 15-20 (growing to max)
- Queue Size: 0-5
- Completed Tasks: Rapidly incrementing

### Heavy Load (100+ RPS)
- Active Threads: 18-20 (at max)
- Pool Size: 20 (max pool size)
- Queue Size: 10-40 (tasks waiting)
- Completed Tasks: Rapidly incrementing

## Code Reference

### Where @Async is Used

Look for these in your code:

```java
// In ProductServiceAsync.java
@Async("taskExecutor")  // ← Uses thread pool!
public CompletableFuture<ProductResponse> getProductWithMockApiAsync(Long id) {
    // This method runs in the thread pool
}

// In PostmanEchoService.java
@Async("taskExecutor")  // ← Uses thread pool!
public CompletableFuture<String> callMockApiAsync(Long productId) {
    // This method runs in the thread pool
}

@Async("taskExecutor")  // ← Uses thread pool!
public CompletableFuture<List<Customer>> getAllCustomersAsync() {
    // This method runs in the thread pool
}
```

### Thread Pool Configuration

From `ConnectionPoolDemoApplication.java`:
```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);      // Start with 10 threads
    executor.setMaxPoolSize(20);       // Grow to max 20 threads
    executor.setQueueCapacity(50);     // Queue up to 50 tasks
    executor.setThreadNamePrefix("async-");
    executor.initialize();
    return executor;
}
```

## Troubleshooting

### "I'm testing async endpoints but still see zeros"

**Possible causes:**
1. Requests completing too fast (before you refresh dashboard)
2. Not enough concurrent load
3. Testing wrong endpoints

**Solution:**
```bash
# Generate sustained load
for i in {1..100}; do
  curl -s "http://localhost:8080/api/customers/async" > /dev/null &
  sleep 0.05  # Small delay between requests
done

# Check metrics
curl http://localhost:8080/api/threadpool/metrics | jq
```

### "How do I know if it's working?"

**Test:**
```bash
# Terminal 1: Watch metrics
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | jq "{active: .activeCount, pool: .poolSize, queue: .queueSize}"'

# Terminal 2: Generate load
for i in {1..50}; do curl -s "http://localhost:8080/api/customers/async" > /dev/null & done
```

You should see:
- `active` increase from 0 to ~10-20
- `pool` stay at 10 initially, may grow to 20
- `queue` increase if requests overwhelm the pool

## Summary

| Endpoint Pattern | Uses Thread Pool? | Monitor With |
|-----------------|-------------------|--------------|
| `/api/products/v2/*` | ✅ YES | Thread Pool Dashboard |
| `/api/customers/async*` | ✅ YES | Thread Pool Dashboard |
| `/api/products/*` (non-v2) | ❌ NO | HikariCP Dashboard |
| `/api/customers/*` (non-async) | ❌ NO | N/A (external API) |

**Key Point:** Only `@Async` annotated methods use the thread pool. Regular synchronous endpoints use database connections directly.

---

**Quick Start to See Activity:**
```bash
# 1. Open dashboard
open http://localhost:8080/dashboard/threadpool

# 2. Generate load
./test-threadpool-load.sh

# 3. Watch the metrics update in real-time!
```

