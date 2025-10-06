# Troubleshooting: Thread Pool Not Showing Activity

## Problem

"I don't see active connections increase in my Thread Pool dashboard even with 2 second sleep configured."

## Root Causes & Solutions

### ✅ Solution 1: Start the Application

**Check if running:**
```bash
curl http://localhost:8080/api/threadpool/health
```

**If not running, start it:**
```bash
mvn spring-boot:run
```

**Wait for startup (look for this log):**
```
INFO  - Started ConnectionPoolDemoApplication in X.XXX seconds
INFO  - 📊 TaskExecutor is using Platform Threads (ThreadPoolTaskExecutor)
```

---

### ✅ Solution 2: Test the CORRECT Endpoint

**❌ WRONG Endpoint (won't show in Thread Pool):**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/1
# This uses Tomcat threads, NOT taskExecutor!
```

**✅ CORRECT Endpoint (will show in Thread Pool):**
```bash
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
# This uses taskExecutor threads!
```

---

### ✅ Solution 3: Use Sufficient Concurrency

With your current config:
```properties
product.api.v2.sleep.ms=2000  # 2 second sleep
```

And thread pool:
```java
executor.setCorePoolSize(10);   // 10 initial threads
executor.setMaxPoolSize(20);    // 20 max threads
executor.setQueueCapacity(50);  // 50 queue slots
```

**You need at least 20+ concurrent requests to fill the pool!**

**Test with high concurrency:**
```bash
# This will definitely show activity!
ab -n 2000 -c 100 http://localhost:8080/api/products/v2/1
```

**Calculation:**
- 100 concurrent requests
- 2 second sleep per request
- Pool max: 20 threads
- Result: 20 threads busy, 30 in queue, 50 waiting

---

### ✅ Solution 4: Monitor While Testing

**Terminal 1: Start continuous monitoring**
```bash
while true; do
  echo "=== $(date +%H:%M:%S) ==="
  curl -s http://localhost:8080/api/threadpool/metrics 2>/dev/null | \
    grep -E '"(activeCount|poolSize|queueSize|completedTaskCount)"' | \
    sed 's/[",]//g'
  sleep 1
done
```

**Terminal 2: Generate load**
```bash
ab -n 2000 -c 100 http://localhost:8080/api/products/v2/1
```

**You should see output like:**
```
=== 14:30:15 ===
  activeCount: 20
  poolSize: 20
  queueSize: 35
  completedTaskCount: 145

=== 14:30:16 ===
  activeCount: 20
  poolSize: 20
  queueSize: 28
  completedTaskCount: 152
```

---

### ✅ Solution 5: Increase Sleep for Easier Observation

**Make threads stay busy longer:**
```properties
# application.properties
product.api.v2.sleep.ms=5000  # 5 seconds!
```

**Then test with moderate load:**
```bash
ab -n 500 -c 50 http://localhost:8080/api/products/v2/1
```

**With 5 second sleep:**
- Each thread is busy for ~8 seconds (3s API + 5s sleep)
- Much easier to see activity in dashboard
- 50 concurrent requests will definitely max out 20 thread pool

---

### ✅ Solution 6: Check Application Logs

**Look for these log messages:**

**When application starts:**
```
INFO  - 📊 TaskExecutor is using Platform Threads (ThreadPoolTaskExecutor)
```

**When requests come in:**
```
INFO  - [ASYNC] Starting non-blocking product fetch for ID: 1
INFO  - [ASYNC] Fetching product 1 from database - Thread: async-1
INFO  - [ASYNC-V2] Sleeping for 2000 ms in taskExecutor thread: async-1
INFO  - [ASYNC-V2] Sleep completed in thread: async-1
```

**If you see these logs, threads ARE being used!**

---

### ✅ Solution 7: Use the Dashboard

**Open in browser:**
```
http://localhost:8080/dashboard/threadpool
```

**Then in another terminal:**
```bash
ab -n 2000 -c 100 http://localhost:8080/api/products/v2/1
```

**Watch the dashboard update in real-time!**

---

## Complete Test Procedure

### Step 1: Configure
```properties
# application.properties
product.api.v2.sleep.ms=3000  # 3 seconds for easy observation
```

### Step 2: Start Application
```bash
mvn spring-boot:run
```

### Step 3: Verify It's Running
```bash
curl http://localhost:8080/api/threadpool/health
# Should return: "Thread Pool Monitoring API is running!"
```

### Step 4: Check Initial State
```bash
curl http://localhost:8080/api/threadpool/metrics
```

**Should show:**
```json
{
  "poolName": "TaskExecutor",
  "type": "ThreadPoolTaskExecutor",
  "corePoolSize": 10,
  "maximumPoolSize": 20,
  "activeCount": 0,     ← Should be 0 initially
  "poolSize": 0,        ← Should be 0 initially
  "queueSize": 0,
  ...
}
```

### Step 5: Open Dashboard
```
http://localhost:8080/dashboard/threadpool
```

### Step 6: Generate Load
```bash
ab -n 2000 -c 100 http://localhost:8080/api/products/v2/1
```

### Step 7: Watch Metrics Change

**During load test, you should see:**
```json
{
  "activeCount": 20,      ← ✅ Threads busy!
  "poolSize": 20,         ← ✅ Pool at max!
  "queueSize": 45,        ← ✅ Queue filling!
  "completedTaskCount": 235,  ← ✅ Growing!
  "utilizationPercent": 100   ← ✅ Fully utilized!
}
```

---

## Quick Diagnostic Commands

### Check if app is running
```bash
curl -f http://localhost:8080/api/threadpool/health && echo "✅ Running" || echo "❌ Not running"
```

### Check thread pool type
```bash
curl -s http://localhost:8080/api/threadpool/metrics | grep '"type"'
# Should show: "type": "ThreadPoolTaskExecutor"
```

### Check current activity
```bash
curl -s http://localhost:8080/api/threadpool/metrics | \
  grep -E '"(activeCount|poolSize|queueSize)"'
```

### Test V2 endpoint directly
```bash
time curl http://localhost:8080/api/products/v2/1
# Should take ~5 seconds (3s API + 2s sleep)
```

---

## Common Mistakes

### ❌ Mistake 1: Testing Wrong Endpoint
```bash
# This won't show in Thread Pool dashboard!
ab -n 1000 -c 50 http://localhost:8080/api/products/1
```

### ❌ Mistake 2: Not Enough Concurrency
```bash
# Only 10 concurrent = won't fill 20 thread pool
ab -n 1000 -c 10 http://localhost:8080/api/products/v2/1
```

### ❌ Mistake 3: Sleep Too Short
```properties
# 100ms is too fast to observe
product.api.v2.sleep.ms=100
```

### ❌ Mistake 4: Not Monitoring During Test
```bash
# Generate load but don't watch dashboard
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
# By the time you check, it's done!
```

---

## Recommended Test Configuration

### For Easy Observation:
```properties
# application.properties
product.api.v2.sleep.ms=5000  # 5 seconds - easy to see
```

```bash
# Test command
ab -n 1000 -c 50 http://localhost:8080/api/products/v2/1
```

**Why this works:**
- 50 concurrent requests
- 5 second sleep
- 20 thread max
- Result: 20 threads busy for 5+ seconds, 30 in queue
- Easy to observe in dashboard!

---

## Expected Behavior

### With Proper Configuration:

**Before load test:**
```
Active Threads: 0
Pool Size: 0
Queue Size: 0
```

**During load test (50 concurrent, 5s sleep):**
```
Active Threads: 20      ← All threads busy!
Pool Size: 20           ← Pool at maximum!
Queue Size: 30          ← Requests waiting!
Completed: 50, 75, 100  ← Counter growing!
```

**After load test:**
```
Active Threads: 0       ← Back to idle
Pool Size: 10           ← Back to core size
Queue Size: 0           ← Queue empty
Completed: 1000         ← All done!
```

---

## Still Not Working?

### Check Application Logs
```bash
tail -f logs/application.log | grep -E "(ASYNC|async-|TaskExecutor)"
```

### Verify Thread Pool Bean
```bash
# Should see this on startup:
# INFO  - 📊 TaskExecutor is using Platform Threads (ThreadPoolTaskExecutor)
```

### Test Single Request
```bash
curl -v http://localhost:8080/api/products/v2/1
# Should take ~5 seconds
# Check logs for "async-" thread names
```

### Check for Errors
```bash
curl http://localhost:8080/api/threadpool/metrics
# Should NOT contain "error" or "N/A" for activeCount
```

---

## Summary

**To see Thread Pool activity, you MUST:**

1. ✅ Start the application
2. ✅ Test the **V2 async endpoint** (`/api/products/v2/1`)
3. ✅ Use **high concurrency** (50-100 concurrent requests)
4. ✅ Configure **sufficient sleep** (2-5 seconds)
5. ✅ **Monitor while testing** (not after!)
6. ✅ Use the **Thread Pool Dashboard** (not Tomcat dashboard)

**Quick test to verify:**
```bash
# 1. Start app
mvn spring-boot:run

# 2. In another terminal, test
ab -n 1000 -c 100 http://localhost:8080/api/products/v2/1

# 3. While running, check
curl http://localhost:8080/api/threadpool/metrics

# You WILL see activeCount > 0!
```
