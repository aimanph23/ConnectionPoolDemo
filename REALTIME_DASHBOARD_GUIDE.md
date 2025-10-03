# Real-Time HikariCP Monitoring Dashboard

This guide explains the real-time monitoring dashboard for visualizing HikariCP connection pool metrics with live updating charts.

## 🎯 Overview

The real-time dashboard provides instant visual feedback on your connection pool's health with:

- **Live Charts**: Auto-updating every 500ms
- **Multiple Visualizations**: Bar charts, doughnut charts, and time-series graphs
- **Real-Time Alerts**: Warnings when pool is stressed
- **No Manual Refresh**: Server-Sent Events (SSE) push updates automatically

## 🚀 Quick Start

### Access the Dashboard

**Option 1: Open in Browser**
```
http://localhost:8080/dashboard/hikari
```

**Option 2: Using cURL (SSE Stream)**
```bash
curl -N http://localhost:8080/api/monitoring/hikari/stream
```

### What You'll See

The dashboard displays:

1. **Status Cards** - Current connection counts
   - Active connections
   - Idle connections
   - Waiting threads
   - Total connections / Max pool size
   - Pool utilization percentage

2. **Connection Pool Status Chart** - Bar chart showing active, idle, and waiting connections

3. **Pool Utilization Chart** - Doughnut chart showing percentage used vs available

4. **Connection History** - Time-series line graph showing 60 seconds of history

5. **Alert System** - Warnings when:
   - Threads are waiting for connections (🚨 Critical)
   - Pool utilization > 80% (⚠️ Warning)
   - No idle connections available (⚠️ Warning)

## 📊 Dashboard Features

### Real-Time Updates

Updates every **500 milliseconds** using Server-Sent Events (SSE):

```javascript
// How it works (simplified)
const eventSource = new EventSource('/api/monitoring/hikari/stream');

eventSource.addEventListener('metrics', (event) => {
    const metrics = JSON.parse(event.data);
    updateCharts(metrics);
});
```

### Connection Status Indicator

- 🟢 **Connected** - Receiving real-time updates
- 🔴 **Disconnected** - Connection lost (auto-reconnects in 3s)

### Alert System

The dashboard shows alerts based on pool health:

#### 🚨 Critical Alert (Red, Pulsing)
```
Threads waiting for connections! Pool may be exhausted.
```
**Trigger:** `waiting > 0`
**Action:** Increase pool size or optimize queries

#### ⚠️ Warning Alert (Yellow)
```
High pool utilization (85%). Consider increasing pool size.
```
**Trigger:** `utilization >= 80%`
**Action:** Monitor closely, prepare to scale

```
No idle connections available. Pool is at capacity.
```
**Trigger:** `idle == 0 && active > 0`
**Action:** Check for connection leaks

## 🧪 Testing the Dashboard

### Test 1: Normal Load

```bash
# Start application
mvn spring-boot:run

# Open dashboard
open http://localhost:8080/dashboard/hikari

# Make a few requests
curl http://localhost:8080/api/products/1
```

**Expected:** 
- Small spike in active connections
- Returns to idle quickly
- No warnings

### Test 2: Moderate Load (V1 - Blocking)

```bash
# Send 10 concurrent requests
for i in {1..10}; do
  curl http://localhost:8080/api/products/1 &
done

# Watch dashboard
```

**Expected:**
- Active connections: 10 (maxed out)
- Idle connections: 0
- Some waiting threads if pool size = 10
- ⚠️ Warning alerts

### Test 3: High Load (V2 - Non-Blocking)

```bash
# Send 20 concurrent requests to V2
for i in {1..20}; do
  curl http://localhost:8080/api/products/v2/1 &
done

# Watch dashboard
```

**Expected:**
- Active connections: 3-5 (healthy)
- Idle connections: 5-7
- No waiting threads
- ✅ No alerts

### Test 4: Stress Test (Show Pool Exhaustion)

```bash
# Send 30 concurrent requests to V1 with 5s delay
echo "external.delay.api.delay-seconds=5" >> application.properties

for i in {1..30}; do
  curl http://localhost:8080/api/products/1 &
done

# Watch dashboard light up with alerts!
```

**Expected:**
- Active connections: 10 (maxed)
- Waiting threads: 20+
- 🚨 Critical alerts
- Utilization: 100%

## 📈 Understanding the Charts

### Chart 1: Connection Pool Status (Bar Chart)

Shows current snapshot of connections:
- **Green Bar (Active)**: Connections currently executing queries
- **Blue Bar (Idle)**: Available connections waiting for requests
- **Pink Bar (Waiting)**: Threads waiting for a connection (🚨 bad!)

**Healthy State:**
```
Active:  ████ 3
Idle:    ████████ 7
Waiting:  0
```

**Stressed State:**
```
Active:  ██████████ 10
Idle:     0
Waiting: ██████ 5
```

### Chart 2: Pool Utilization (Doughnut Chart)

Visual percentage of pool capacity in use:
- **Purple**: Used capacity
- **Gray**: Available capacity

**Healthy:** < 80%
**Warning:** 80-95%
**Critical:** > 95%

### Chart 3: Connection History (Line Graph)

Time-series showing last 60 seconds:
- **Green Line**: Active connections over time
- **Blue Line**: Idle connections over time
- **Pink Line**: Waiting threads over time

**Look for:**
- Spikes in active during load
- Active staying high (potential leak)
- Any waiting threads (pool exhaustion)

## 🔧 Configuration

### Change Update Frequency

Edit `MonitoringController.java`:

```java
// Current: 500ms
scheduler.scheduleAtFixedRate(..., 0, 500, TimeUnit.MILLISECONDS);

// Faster: 250ms (more responsive, higher CPU)
scheduler.scheduleAtFixedRate(..., 0, 250, TimeUnit.MILLISECONDS);

// Slower: 1000ms (less load, slower updates)
scheduler.scheduleAtFixedRate(..., 0, 1000, TimeUnit.MILLISECONDS);
```

### Change History Duration

Edit `hikari-dashboard.html`:

```javascript
// Current: 120 points (60 seconds at 500ms)
const MAX_HISTORY_POINTS = 120;

// Longer: 240 points (2 minutes)
const MAX_HISTORY_POINTS = 240;

// Shorter: 60 points (30 seconds)
const MAX_HISTORY_POINTS = 60;
```

## 🎨 Dashboard Customization

### Change Chart Colors

Edit `hikari-dashboard.html`:

```javascript
// Active connections color (currently green)
backgroundColor: 'rgba(67, 233, 123, 0.8)',

// Idle connections color (currently blue)
backgroundColor: 'rgba(79, 172, 254, 0.8)',

// Waiting threads color (currently pink)
backgroundColor: 'rgba(250, 112, 154, 0.8)',
```

### Change Alert Thresholds

Edit `hikari-dashboard.html`:

```javascript
// Current: Warning at 80% utilization
if (metrics.utilization >= 80) {
    // Show warning
}

// Stricter: Warning at 70%
if (metrics.utilization >= 70) {
    // Show warning
}
```

## 🔍 Troubleshooting

### Dashboard Not Loading

**Issue:** Blank page or error

**Solutions:**
1. Check Thymeleaf dependency in `pom.xml`
2. Verify `hikari-dashboard.html` in `src/main/resources/templates/`
3. Check logs for errors

### No Data / Charts Empty

**Issue:** Dashboard loads but shows no data

**Solutions:**
```bash
# 1. Check SSE endpoint
curl -N http://localhost:8080/api/monitoring/hikari/stream

# 2. Check HikariCP is configured
curl http://localhost:8080/api/monitoring/hikari/status

# 3. Check browser console for errors (F12)
```

### Dashboard Shows "Disconnected"

**Issue:** Red connection status

**Solutions:**
1. Check application is running
2. Check firewall/network
3. Check browser console for SSE errors
4. Verify endpoint: `/api/monitoring/hikari/stream`

### High CPU Usage

**Issue:** Dashboard causes high CPU

**Solutions:**
1. Reduce update frequency to 1000ms
2. Limit concurrent dashboard viewers
3. Reduce MAX_HISTORY_POINTS

## 📱 Multiple Viewers

The dashboard supports **multiple simultaneous viewers**:

```java
// Track connected clients
private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

// Each browser tab = 1 emitter
```

**Performance Impact:**
- 1-5 viewers: Negligible
- 10+ viewers: Slight increase in CPU
- 50+ viewers: Consider dedicated monitoring solution

## 🔐 Production Considerations

### Security

**⚠️ Important:** The dashboard has NO authentication by default!

Add Spring Security:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests()
            .requestMatchers("/dashboard/**").hasRole("ADMIN")
            .requestMatchers("/api/monitoring/**").hasRole("ADMIN")
            .anyRequest().authenticated();
        return http.build();
    }
}
```

### Rate Limiting

For production, add rate limiting:

```properties
# Limit SSE connections per IP
spring.mvc.async.request-timeout=300000
```

### CORS

If dashboard is on different domain:

```java
@CrossOrigin(origins = "https://your-dashboard-domain.com")
@GetMapping("/hikari/stream")
public SseEmitter streamHikariMetrics() { ... }
```

## 🎯 Use Cases

### Development

```bash
# Monitor while developing
open http://localhost:8080/dashboard/hikari

# Code changes
# Watch connection behavior in real-time
```

### Load Testing

```bash
# Terminal 1: Dashboard
open http://localhost:8080/dashboard/hikari

# Terminal 2: Load test
ab -n 1000 -c 50 http://localhost:8080/api/products/1

# Watch metrics during test
```

### Production Monitoring

**Option 1:** Dashboard in admin panel
```
https://your-app.com/admin/monitoring/hikari
(with authentication)
```

**Option 2:** Export metrics to monitoring system
```java
// Custom exporter for Prometheus, Grafana, etc.
```

## 📊 Example Scenarios

### Scenario 1: Healthy Application

**Dashboard Shows:**
- Active: 2-3 (fluctuating)
- Idle: 7-8 (steady)
- Waiting: 0 (always)
- Utilization: 20-30%
- Status: 🟢 No alerts

**Action:** None needed, system healthy

### Scenario 2: High Load

**Dashboard Shows:**
- Active: 8-10 (sustained)
- Idle: 0-2 (low)
- Waiting: 0-3 (occasional)
- Utilization: 80-100%
- Status: ⚠️ Warning alerts

**Action:** 
- Monitor closely
- Prepare to increase pool size
- Check if load is temporary or sustained

### Scenario 3: Pool Exhaustion

**Dashboard Shows:**
- Active: 10 (maxed, constant)
- Idle: 0 (none available)
- Waiting: 10+ (growing)
- Utilization: 100%
- Status: 🚨 Critical alerts (pulsing)

**Action:**
- Immediate: Increase pool size
- Investigation: Check for connection leaks
- Long-term: Optimize queries, use V2 endpoints

### Scenario 4: Connection Leak

**Dashboard Shows:**
- Active: 7-8 (slowly growing)
- Idle: 2-3 (slowly decreasing)
- Waiting: 0 (not yet)
- Pattern: Active never decreases

**Action:**
- Check for missing transaction closures
- Review @Transactional annotations
- Look for unclosed connections

## 🚀 Advanced Features

### Export Metrics

```javascript
// Add to hikari-dashboard.html
function exportMetrics() {
    const csv = historyData.timestamps.map((time, i) => 
        `${time},${historyData.active[i]},${historyData.idle[i]},${historyData.waiting[i]}`
    ).join('\n');
    
    // Download CSV
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'hikari-metrics.csv';
    a.click();
}
```

### Custom Alerts

```javascript
// Add custom threshold alerts
if (metrics.active === metrics.max) {
    sendSlackNotification('Pool at maximum capacity!');
}
```

### Multiple Pool Monitoring

Extend to monitor multiple databases:

```java
@GetMapping("/hikari/stream/database1")
public SseEmitter streamDatabase1() { ... }

@GetMapping("/hikari/stream/database2")
public SseEmitter streamDatabase2() { ... }
```

## 📚 Related Documentation

- [MONITORING_GUIDE.md](MONITORING_GUIDE.md) - API-based monitoring
- [ASYNC_API_GUIDE.md](ASYNC_API_GUIDE.md) - Non-blocking endpoints
- [README.md](README.md) - Main documentation

## 🎉 Summary

| Feature | Details |
|---------|---------|
| **URL** | http://localhost:8080/dashboard/hikari |
| **Update Frequency** | 500ms (configurable) |
| **Technology** | Server-Sent Events (SSE) + Chart.js |
| **Charts** | Bar, Doughnut, Time-series |
| **Alerts** | Automatic warnings |
| **History** | Last 60 seconds |
| **Concurrent Viewers** | Unlimited |
| **Authentication** | None (add Spring Security) |

**Start monitoring now:** `http://localhost:8080/dashboard/hikari` 🎯

