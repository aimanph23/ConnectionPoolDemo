# High RPS (200 req/s) Thread Pool Analysis

## Your Scenario

- **Endpoint**: `http://localhost:8080/api/products/v2/1` ✅ Correct!
- **RPS**: 200 requests per second
- **Sleep**: 2 seconds per request
- **Thread Pool**: 20 max threads, 50 queue capacity

## 🔴 Problem: Thread Pool is TOO SMALL for 200 RPS!

### The Math

```
Requests per second: 200
Sleep time per request: 2 seconds
Concurrent requests at any moment: 200 × 2 = 400 requests

Your thread pool capacity:
  Max threads: 20
  Queue capacity: 50
  Total capacity: 70 concurrent requests

Result: 400 - 70 = 330 requests REJECTED! ❌
```

### What's Happening

```
Time: 0s
├─ 200 requests arrive
├─ 20 threads start processing (busy for 2s)
├─ 50 requests go to queue
└─ 130 requests REJECTED (pool exhausted!)

Time: 1s
├─ Another 200 requests arrive
├─ 20 threads still busy
├─ Queue still full (50)
└─ 200 more requests REJECTED!

Time: 2s
├─ First 20 threads complete
├─ 20 requests from queue start processing
├─ Another 200 requests arrive
├─ 20 go to threads, 30 go to queue
└─ 150 requests REJECTED!
```

## 📊 Expected Metrics at 200 RPS

### What You SHOULD See:

```json
{
  "activeCount": 20,           ← ✅ Maxed out immediately
  "poolSize": 20,              ← ✅ At maximum
  "queueSize": 50,             ← ✅ Queue full
  "completedTaskCount": 1000+, ← ✅ Growing rapidly
  "utilizationPercent": 100    ← ✅ Fully utilized
}
```

### But Also:

- **Many failed requests** (pool exhausted)
- **High error rate** (rejections)
- **Possible timeouts**

## 🎯 Solutions

### Solution 1: Increase Thread Pool Size (Recommended)

Update `ConnectionPoolDemoApplication.java`:

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);      // Increased from 10
    executor.setMaxPoolSize(200);      // Increased from 20
    executor.setQueueCapacity(500);    // Increased from 50
    executor.setThreadNamePrefix("async-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
}
```

**Now you can handle:**
- 200 concurrent threads
- 500 in queue
- Total: 700 concurrent requests ✅

### Solution 2: Reduce Sleep Time

```properties
# application.properties
product.api.v2.sleep.ms=500  # Reduced from 2000
```

**New capacity:**
```
200 RPS × 0.5s = 100 concurrent requests
Your pool: 70 capacity
Result: Still need bigger pool, but closer!
```

### Solution 3: Reduce RPS in Load Test

```bash
# Instead of 200 RPS, use 30 RPS
ab -n 1000 -c 60 -t 30 http://localhost:8080/api/products/v2/1
```

**Calculation:**
```
60 concurrent requests
2 second sleep
Pool capacity: 70 (20 threads + 50 queue)
Result: All requests handled! ✅
```

### Solution 4: Use Virtual Threads (Java 21)

Enable virtual threads in `ConnectionPoolDemoApplication.java`:

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    // Unlimited concurrency!
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

**With virtual threads:**
- Can handle 1000+ concurrent requests
- No pool limits
- No queue needed
- Perfect for 200 RPS!

## 🧪 Proper Test for Current Configuration

### Test 1: Sustainable Load (Will Show Activity)

```bash
# 30 concurrent requests = fits in your pool
ab -n 1000 -c 30 http://localhost:8080/api/products/v2/1
```

**Expected results:**
```
Active Threads: 20
Pool Size: 20
Queue Size: 10
Completed: Growing steadily
Failed: 0
```

### Test 2: Moderate Overload (Will Show Queue)

```bash
# 60 concurrent = fills pool + queue
ab -n 1000 -c 60 http://localhost:8080/api/products/v2/1
```

**Expected results:**
```
Active Threads: 20
Pool Size: 20
Queue Size: 40-50
Completed: Growing
Failed: Some (when queue full)
```

### Test 3: Extreme Load (Will Show Rejections)

```bash
# 200 concurrent = massive overload
ab -n 1000 -c 200 http://localhost:8080/api/products/v2/1
```

**Expected results:**
```
Active Threads: 20
Pool Size: 20
Queue Size: 50 (maxed)
Completed: Growing slowly
Failed: MANY (pool exhausted)
```

## 📈 Real-Time Monitoring for 200 RPS

### Terminal 1: Monitor Metrics
```bash
while true; do
  clear
  echo "=== Thread Pool Metrics @ $(date +%H:%M:%S) ==="
  curl -s http://localhost:8080/api/threadpool/metrics | \
    grep -E '"(activeCount|poolSize|queueSize|completedTaskCount)"' | \
    sed 's/[",]//g'
  echo ""
  echo "Press Ctrl+C to stop"
  sleep 0.5
done
```

### Terminal 2: Generate 200 RPS Load
```bash
# Use wrk for precise RPS control
wrk -t 10 -c 200 -d 30s --latency http://localhost:8080/api/products/v2/1

# Or with ab (approximate)
ab -n 6000 -c 200 -t 30 http://localhost:8080/api/products/v2/1
```

### Terminal 3: Monitor Application Logs
```bash
tail -f logs/application.log | grep -E "(ASYNC|async-|rejected)"
```

## 🎯 Recommended Configuration for 200 RPS

### Option A: Increase Pool Size

```java
// ConnectionPoolDemoApplication.java
executor.setCorePoolSize(100);
executor.setMaxPoolSize(400);     // 2× your RPS
executor.setQueueCapacity(1000);
```

```properties
# application.properties
product.api.v2.sleep.ms=2000
```

**Capacity:** 400 threads + 1000 queue = 1400 concurrent ✅

### Option B: Use Virtual Threads (Best!)

```java
// ConnectionPoolDemoApplication.java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}
```

```properties
# application.properties
product.api.v2.sleep.ms=2000
spring.threads.virtual.enabled=true
```

**Capacity:** Unlimited! ✅

### Option C: Reduce Sleep

```properties
# application.properties
product.api.v2.sleep.ms=200  # 200ms instead of 2s
```

**Capacity needed:** 200 RPS × 0.2s = 40 concurrent (fits in pool!)

## 🔍 Why You Might Not See Activity

### Reason 1: Requests Failing Too Fast

If pool is exhausted, requests fail immediately:
```
Request arrives → Pool full → Rejected in 1ms → No thread activity!
```

**Check for errors:**
```bash
curl http://localhost:8080/api/products/v2/1
# Look for 500 errors or timeouts
```

### Reason 2: Application Crashed

200 RPS might have crashed the app:
```bash
# Check if still running
curl http://localhost:8080/api/threadpool/health

# Check logs
tail -50 logs/application.log
```

### Reason 3: Monitoring Too Slow

At 200 RPS, metrics change every 5ms:
```bash
# Use faster polling
watch -n 0.1 'curl -s http://localhost:8080/api/threadpool/metrics | grep activeCount'
```

## ✅ Verification Steps

### Step 1: Check Application is Running
```bash
curl http://localhost:8080/api/threadpool/health
# Should return: "Thread Pool Monitoring API is running!"
```

### Step 2: Check Single Request Works
```bash
time curl http://localhost:8080/api/products/v2/1
# Should take ~5 seconds (3s API + 2s sleep)
```

### Step 3: Check Thread Pool Type
```bash
curl -s http://localhost:8080/api/threadpool/metrics | grep '"type"'
# Should show: "type": "ThreadPoolTaskExecutor"
```

### Step 4: Test with Lower Load First
```bash
# Start with 20 concurrent (definitely works)
ab -n 100 -c 20 http://localhost:8080/api/products/v2/1

# Check metrics
curl -s http://localhost:8080/api/threadpool/metrics | \
  grep -E '"(activeCount|poolSize)"'
```

### Step 5: Gradually Increase Load
```bash
# 20 concurrent → should work
ab -n 200 -c 20 http://localhost:8080/api/products/v2/1

# 40 concurrent → should work
ab -n 400 -c 40 http://localhost:8080/api/products/v2/1

# 60 concurrent → queue fills
ab -n 600 -c 60 http://localhost:8080/api/products/v2/1

# 100 concurrent → rejections start
ab -n 1000 -c 100 http://localhost:8080/api/products/v2/1

# 200 concurrent → many rejections
ab -n 2000 -c 200 http://localhost:8080/api/products/v2/1
```

## 📊 Expected Results at Different Loads

| Concurrent | Active Threads | Queue Size | Failed Requests |
|------------|----------------|------------|-----------------|
| 20 | 20 | 0 | 0 |
| 40 | 20 | 20 | 0 |
| 60 | 20 | 40 | 0 |
| 70 | 20 | 50 | 0 |
| 100 | 20 | 50 | ~30 |
| 200 | 20 | 50 | ~130 |

## 🎯 Quick Fix for Your Situation

**Right now, do this:**

1. **Reduce load to see activity:**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```

2. **Monitor while testing:**
```bash
watch -n 1 'curl -s http://localhost:8080/api/threadpool/metrics | grep -E "activeCount|poolSize|queueSize"'
```

3. **You WILL see:**
```
"activeCount": 20
"poolSize": 20
"queueSize": 30
```

**Then, to handle 200 RPS, increase your pool size!**

## Summary

✅ **You're testing the right endpoint**  
✅ **200 RPS is very high load**  
❌ **Your pool is too small (20 threads)**  
❌ **Need 400 threads for 200 RPS with 2s sleep**  

**Solutions:**
1. Increase pool to 400 threads
2. Use virtual threads (unlimited)
3. Reduce sleep to 200ms
4. Test with lower RPS (30-50) first

**To see activity NOW:**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```
