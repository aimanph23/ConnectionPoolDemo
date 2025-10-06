# JVM Monitoring Dashboard - CPU & Memory

## 🎯 Overview

Real-time monitoring dashboard for your Java application's CPU and memory usage. Track heap memory, CPU load, thread count, garbage collection, and more!

## 🚀 Quick Start

### Access the Dashboard

```
http://localhost:8080/dashboard/jvm
```

### API Endpoints

```bash
# All metrics
GET http://localhost:8080/api/jvm/metrics

# Memory only
GET http://localhost:8080/api/jvm/memory

# CPU only
GET http://localhost:8080/api/jvm/cpu

# Threads only
GET http://localhost:8080/api/jvm/threads

# Garbage collection
GET http://localhost:8080/api/jvm/gc

# Real-time stream (SSE)
GET http://localhost:8080/api/jvm/stream
```

## 📊 Dashboard Features

### 1. **Heap Memory Monitoring**
- **Current Usage**: Shows used heap memory in MB
- **Progress Bar**: Visual representation of heap usage
- **Percentage**: Heap utilization percentage
- **Max Heap**: Maximum heap size configured

**Color Coding:**
- 🟢 Green: < 70% (Healthy)
- 🟡 Yellow: 70-90% (Warning)
- 🔴 Red: > 90% (Critical)

### 2. **CPU Usage Monitoring**
- **Process CPU**: Your application's CPU usage
- **System CPU**: Overall system CPU load
- **Available Cores**: Number of CPU cores
- **Real-time Chart**: CPU usage over time

### 3. **Thread Monitoring**
- **Current Threads**: Active thread count
- **Peak Threads**: Maximum threads reached
- **Daemon Threads**: Background thread count
- **Thread States**: Breakdown by state (RUNNABLE, WAITING, etc.)

### 4. **Memory Charts**
- **Heap Memory**: Real-time heap usage graph
- **Non-Heap Memory**: Metaspace, code cache, etc.
- **60-second History**: Last 60 data points

### 5. **Detailed Metrics**
- Non-heap memory usage
- Total memory consumption
- Loaded classes count
- System load average
- Garbage collection statistics

### 6. **Runtime Information**
- JVM uptime
- JVM version and vendor
- Start time
- Input arguments

## 🎨 What You'll See

### Dashboard Layout

```
┌─────────────────────────────────────────────────────┐
│  JVM Monitoring Dashboard                            │
│  Real-time CPU & Memory Monitoring                   │
├─────────────────────────────────────────────────────┤
│                                                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────┐│
│  │ Heap     │  │ CPU      │  │ Threads  │  │Uptime││
│  │ 512 MB   │  │ 45.2%    │  │ 42       │  │2h 15m││
│  │ ████▒▒▒▒ │  │ ████▒▒▒▒ │  │ Peak: 58 │  │      ││
│  │ 65%      │  │ 8 cores  │  │ Daemon:12│  │      ││
│  └──────────┘  └──────────┘  └──────────┘  └──────┘│
│                                                       │
│  ┌─────────────────────────────────────────────────┐│
│  │ Memory Usage Over Time                           ││
│  │ [Real-time line chart showing heap/non-heap]    ││
│  └─────────────────────────────────────────────────┘│
│                                                       │
│  ┌─────────────────────────────────────────────────┐│
│  │ CPU Usage Over Time                              ││
│  │ [Real-time line chart showing CPU %]            ││
│  └─────────────────────────────────────────────────┘│
│                                                       │
│  ┌─────────────────────────────────────────────────┐│
│  │ Detailed Metrics                                 ││
│  │ Non-Heap: 128 MB  │  Total: 640 MB              ││
│  │ Classes: 15,234   │  System Load: 2.45          ││
│  │                                                   ││
│  │ Garbage Collection                               ││
│  │ G1 Young: 245 collections (1.2s total)          ││
│  │ G1 Old: 12 collections (0.8s total)             ││
│  └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

## 📈 Metrics Explained

### Heap Memory
```json
{
  "heap": {
    "used": "512.5 MB",
    "max": "1024.0 MB",
    "usagePercent": 50,
    "committed": "768.0 MB"
  }
}
```

- **Used**: Currently allocated objects
- **Max**: Maximum heap size (-Xmx)
- **Committed**: Memory guaranteed by OS
- **Usage %**: used / max * 100

### Non-Heap Memory
```json
{
  "nonHeap": {
    "used": "128.3 MB",
    "committed": "135.0 MB"
  }
}
```

Includes:
- **Metaspace**: Class metadata
- **Code Cache**: JIT compiled code
- **Compressed Class Space**: Compressed class pointers

### CPU Metrics
```json
{
  "cpu": {
    "processCpuLoad": "45.2%",
    "systemCpuLoad": "62.8%",
    "availableProcessors": 8,
    "systemLoadAverage": 2.45
  }
}
```

- **Process CPU**: Your app's CPU usage
- **System CPU**: Overall system usage
- **Load Average**: System load (Unix/Linux)

### Thread Metrics
```json
{
  "threads": {
    "threadCount": 42,
    "peakThreadCount": 58,
    "daemonThreadCount": 12,
    "threadStates": {
      "RUNNABLE": 8,
      "WAITING": 20,
      "TIMED_WAITING": 12,
      "BLOCKED": 2
    }
  }
}
```

## 🧪 Testing & Monitoring

### Test 1: Monitor During Load Test

**Terminal 1: Open Dashboard**
```
open http://localhost:8080/dashboard/jvm
```

**Terminal 2: Generate Load**
```bash
ab -n 10000 -c 100 http://localhost:8080/api/products/v2/1
```

**Watch:**
- Heap memory increase
- CPU spike to 80-100%
- Thread count increase
- GC activity

### Test 2: Monitor API Calls

```bash
# Watch metrics
curl -s http://localhost:8080/api/jvm/metrics | jq

# Watch memory
watch -n 1 'curl -s http://localhost:8080/api/jvm/memory | jq ".heap.usagePercent"'

# Watch CPU
watch -n 1 'curl -s http://localhost:8080/api/jvm/cpu | jq ".processCpuLoad"'
```

### Test 3: Real-time Stream

```bash
# Connect to SSE stream
curl -N http://localhost:8080/api/jvm/stream
```

## 🔍 Troubleshooting

### High Heap Usage (> 90%)

**Symptoms:**
- Heap usage consistently above 90%
- Frequent garbage collections
- Slow response times

**Solutions:**
```bash
# Increase heap size
java -Xmx2048m -jar app.jar

# Or in Maven
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2048m"
```

### High CPU Usage (> 80%)

**Symptoms:**
- CPU consistently above 80%
- Slow response times
- High thread count

**Check:**
```bash
# Get thread dump
jstack <pid> > thread-dump.txt

# Check for CPU-intensive threads
top -H -p <pid>
```

**Solutions:**
- Reduce concurrent requests
- Optimize code (check thread dump)
- Scale horizontally

### Memory Leak Detection

**Monitor over time:**
```bash
# Watch heap usage trend
while true; do
  curl -s http://localhost:8080/api/jvm/memory | \
    jq -r '"\(.heap.used) / \(.heap.max) = \(.heap.usagePercent)%"'
  sleep 5
done
```

**If heap keeps growing:**
1. Take heap dump: `jmap -dump:live,format=b,file=heap.bin <pid>`
2. Analyze with Eclipse MAT or VisualVM
3. Look for retained objects

### Garbage Collection Issues

**Check GC metrics:**
```bash
curl -s http://localhost:8080/api/jvm/gc | jq
```

**If GC time is high:**
- Consider different GC algorithm (G1GC, ZGC)
- Tune GC parameters
- Increase heap size

## 📊 JVM Configuration

### Recommended Settings for Production

```bash
java \
  -Xms1024m \                    # Initial heap
  -Xmx2048m \                    # Max heap
  -XX:+UseG1GC \                 # G1 garbage collector
  -XX:MaxGCPauseMillis=200 \     # Max GC pause
  -XX:+HeapDumpOnOutOfMemoryError \  # Dump on OOM
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -jar app.jar
```

### For High Load (200 RPS)

```bash
java \
  -Xms2048m \
  -Xmx4096m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -jar app.jar
```

### For Development

```bash
# Current default
mvn spring-boot:run

# With custom heap
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx1024m"
```

## 🎯 Monitoring Best Practices

### 1. Set Alerts

Monitor these thresholds:
- **Heap > 85%**: Warning
- **Heap > 95%**: Critical
- **CPU > 80%**: Warning
- **GC time > 5%**: Warning

### 2. Regular Monitoring

Check dashboard:
- During deployments
- During load tests
- After code changes
- When performance issues reported

### 3. Baseline Metrics

Record normal values:
```
Normal Load (10 RPS):
- Heap: 30-40%
- CPU: 10-20%
- Threads: 30-40

High Load (100 RPS):
- Heap: 60-70%
- CPU: 60-80%
- Threads: 80-100
```

### 4. Correlate with Other Metrics

Compare with:
- Thread Pool Dashboard (async threads)
- HikariCP Dashboard (DB connections)
- Tomcat Dashboard (HTTP threads)
- Application logs

## 🔗 Integration with Other Dashboards

### Complete Monitoring Setup

```
1. JVM Dashboard        → CPU & Memory
2. Thread Pool Dashboard → Async operations
3. HikariCP Dashboard   → Database connections
4. Tomcat Dashboard     → HTTP request threads
```

**Access all:**
```
http://localhost:8080/home
```

## 📝 API Examples

### Get All Metrics
```bash
curl http://localhost:8080/api/jvm/metrics | jq
```

### Monitor Specific Metric
```bash
# Heap usage percentage
curl -s http://localhost:8080/api/jvm/memory | \
  jq '.heap.usagePercent'

# CPU load
curl -s http://localhost:8080/api/jvm/cpu | \
  jq '.processCpuLoad'

# Thread count
curl -s http://localhost:8080/api/jvm/threads | \
  jq '.threadCount'
```

### Continuous Monitoring
```bash
# Monitor heap every second
watch -n 1 'curl -s http://localhost:8080/api/jvm/memory | jq ".heap"'

# Monitor CPU
watch -n 1 'curl -s http://localhost:8080/api/jvm/cpu | jq'
```

## 🎉 Summary

✅ **Real-time CPU monitoring**  
✅ **Real-time Memory monitoring**  
✅ **Interactive charts**  
✅ **Thread metrics**  
✅ **Garbage collection stats**  
✅ **Server-Sent Events stream**  
✅ **REST API access**  
✅ **Color-coded alerts**  

**Dashboard URL:**
```
http://localhost:8080/dashboard/jvm
```

**Now you can monitor your Java application's performance in real-time!** 🚀
