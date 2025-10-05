# Thread Usage Explained: Sync vs Async Endpoints

## The Question: Does `/api/products/{id}` use threads?

**Answer**: YES, but NOT the TaskExecutor thread pool!

## Two Different Approaches

### 🔴 Synchronous: `/api/products/{id}` (v1)

```
Request Flow:
┌─────────────────────────────────────────────────────────────┐
│  1. Client sends HTTP request                                │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Tomcat assigns a REQUEST THREAD                          │
│     (From Tomcat's thread pool, not TaskExecutor!)           │
│     Thread Name: "http-nio-8080-exec-X"                      │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Gets DB CONNECTION from HikariCP                         │
│     (Blocks if no connection available)                      │
│     ✅ Shows in HikariCP Dashboard                           │
│     ❌ Does NOT show in Thread Pool Dashboard                │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Query database: SELECT * FROM product WHERE id = ?       │
│     (Still holding DB connection)                            │
│     (Request thread is BLOCKED waiting)                      │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Call external API: mockApiService.callMockApi(id)        │
│     (Still holding DB connection!)                           │
│     (Request thread is BLOCKED waiting for API response)     │
│     ⚠️  PROBLEM: DB connection held for 3+ seconds!          │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  6. Return DB connection to pool                             │
│     (Finally releases the connection)                        │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  7. Return HTTP response to client                           │
│     (Request thread goes back to Tomcat pool)                │
└─────────────────────────────────────────────────────────────┘

Threads Used:
- ✅ Tomcat Request Thread (http-nio-8080-exec-X)
- ✅ Database Connection (from HikariCP)
- ❌ TaskExecutor Thread Pool (NOT USED)
```

### 🟢 Asynchronous: `/api/products/v2/{id}` (v2)

```
Request Flow:
┌─────────────────────────────────────────────────────────────┐
│  1. Client sends HTTP request                                │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Tomcat assigns a REQUEST THREAD                          │
│     Thread: "http-nio-8080-exec-X"                           │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Hands off to TaskExecutor ASYNC THREAD                   │
│     ✅ Shows in Thread Pool Dashboard!                       │
│     Thread: "async-1" (from TaskExecutor pool)               │
│     (Request thread is NOW FREE to handle other requests)    │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Async thread gets DB CONNECTION from HikariCP            │
│     ✅ Shows in HikariCP Dashboard                           │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Query database: SELECT * FROM product WHERE id = ?       │
│     (Quick query, then IMMEDIATELY releases connection)      │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  6. Release DB connection to pool                            │
│     ✅ Connection is FREE for other requests!                │
│     (Async thread continues without holding connection)      │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  7. Call external API: callMockApiAsync(id)                  │
│     (Async thread waits, but NO DB connection held)          │
│     ✅ Thread Pool Dashboard shows this!                     │
└────────────────────┬────────────────────────────────────────┘
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  8. Return HTTP response to client                           │
│     (Async thread goes back to TaskExecutor pool)            │
└─────────────────────────────────────────────────────────────┘

Threads Used:
- ✅ Tomcat Request Thread (briefly)
- ✅ TaskExecutor Async Thread (async-X)
- ✅ Database Connection (from HikariCP, held briefly)
```

## Code Comparison

### Synchronous Version (v1)

```java
// In ProductController.java
@GetMapping("/{id}")  // ❌ No @Async
public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
    log.info("Received request to get product with ID: {}", id);
    
    // This runs on the Tomcat request thread
    ProductResponse response = productService.getProductById(id);
    
    // Still on the same thread, DB connection still held
    String mockApiResponse = mockApiService.callMockApi(id);
    
    // Thread is blocked here for 3+ seconds while API responds
    // DB connection is HELD the entire time!
    
    return ResponseEntity.ok(response);
}
```

**What happens:**
1. Tomcat thread handles everything
2. Gets DB connection
3. Queries database
4. Calls external API (3+ second delay)
5. **Problem**: DB connection held for 3+ seconds!

### Asynchronous Version (v2)

```java
// In ProductController.java
@GetMapping("/v2/{id}")
public CompletableFuture<ResponseEntity<ProductResponse>> getProductByIdV2(@PathVariable Long id) {
    log.info("[V2] Received non-blocking request to get product with ID: {}", id);
    
    // This IMMEDIATELY returns a CompletableFuture
    // Tomcat thread is freed up!
    return productServiceAsync.getProductWithMockApiAsync(id)
            .thenApply(response -> ResponseEntity.ok(response));
}

// In ProductServiceAsync.java
@Async("taskExecutor")  // ✅ Uses TaskExecutor thread pool!
public CompletableFuture<ProductResponse> getProductWithMockApiAsync(Long id) {
    // This runs on an async thread (async-1, async-2, etc.)
    
    // Get DB connection, query, IMMEDIATELY release
    CompletableFuture<Product> productFuture = getProductByIdAsync(id);
    
    // Now DB connection is free!
    // Async thread continues...
    
    CompletableFuture<String> mockApiFuture = productFuture.thenCompose(product -> {
        // Call external API without holding DB connection
        return postmanEchoService.callMockApiAsync(id);
    });
    
    return mockApiFuture.thenCombine(productFuture, (mockResponse, product) -> {
        // Combine results and return
    });
}
```

**What happens:**
1. Tomcat thread starts request, returns immediately
2. TaskExecutor async thread takes over
3. Gets DB connection, queries, **releases immediately**
4. Calls external API (DB connection is now free!)
5. **Benefit**: DB connection held for < 50ms instead of 3+ seconds!

## Thread Pool Comparison

### Tomcat Thread Pool (Always Used)
- **Purpose**: Handle HTTP requests
- **Configuration**: Default ~200 threads
- **Monitored**: No (not in your dashboards)
- **Thread Names**: `http-nio-8080-exec-1`, `exec-2`, etc.
- **Used By**: ALL endpoints (sync and async)

### TaskExecutor Thread Pool (Your Custom Pool)
- **Purpose**: Handle async tasks
- **Configuration**: 
  - Core: 10 threads
  - Max: 20 threads
  - Queue: 50 tasks
- **Monitored**: ✅ YES! `/dashboard/threadpool`
- **Thread Names**: `async-1`, `async-2`, etc.
- **Used By**: ONLY `@Async` methods

### HikariCP Connection Pool
- **Purpose**: Database connections
- **Configuration**:
  - Min: 5 connections
  - Max: 10 connections
- **Monitored**: ✅ YES! `/dashboard/hikari`
- **Used By**: ALL database queries (sync and async)

## Why You Saw Zeros

```bash
# What you tested:
curl http://localhost:8080/api/products/2  # ❌ Sync version

# What happened:
# ✅ Uses: Tomcat thread (not monitored)
# ✅ Uses: HikariCP connection (shows in HikariCP dashboard)
# ❌ Does NOT use: TaskExecutor thread pool (so Thread Pool Dashboard shows 0)
```

## To See Thread Pool Activity

```bash
# Test these endpoints instead:
curl http://localhost:8080/api/products/v2/1  # ✅ Uses async thread pool
curl http://localhost:8080/api/customers/async  # ✅ Uses async thread pool

# Generate load:
for i in {1..20}; do
  curl -s "http://localhost:8080/api/products/v2/$i" > /dev/null &
  curl -s "http://localhost:8080/api/customers/async" > /dev/null &
done

# Check Thread Pool Dashboard:
# http://localhost:8080/dashboard/threadpool
# You should now see:
# - Active Threads: 10-20
# - Pool Size: 15-20
# - Queue Size: 0-10
# - Completed Tasks: Incrementing
```

## Summary Table

| Endpoint | Tomcat Thread | TaskExecutor Thread | HikariCP Connection | Thread Pool Dashboard |
|----------|---------------|---------------------|---------------------|----------------------|
| `/api/products/{id}` | ✅ YES | ❌ NO | ✅ YES (held long) | ❌ Shows 0 |
| `/api/products/v2/{id}` | ✅ YES (brief) | ✅ YES | ✅ YES (held briefly) | ✅ Shows activity |
| `/api/customers/async` | ✅ YES (brief) | ✅ YES | ❌ NO | ✅ Shows activity |
| `/api/customers` | ✅ YES | ❌ NO | ❌ NO | ❌ Shows 0 |

## Key Takeaway

**ALL endpoints use threads!**

But only `@Async` methods use the **TaskExecutor thread pool** that you're monitoring.

- `/api/products/{id}` = Uses Tomcat thread + DB connection (sync)
- `/api/products/v2/{id}` = Uses Tomcat thread + TaskExecutor thread + DB connection (async)

The Thread Pool Dashboard ONLY monitors TaskExecutor, not Tomcat threads!

---

**To see Thread Pool activity**: Test endpoints with `/v2/` or `/async` in the path! 🚀

