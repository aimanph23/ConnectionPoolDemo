#!/bin/bash

# Guaranteed Thread Pool Activity Test
# This script will DEFINITELY show thread pool activity!

echo "=========================================="
echo "🎯 GUARANTEED Thread Pool Activity Test"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"

# Step 1: Check if server is running
echo -e "${YELLOW}Step 1: Checking if server is running...${NC}"
if ! curl -s -f "$BASE_URL/api/threadpool/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ Server is NOT running!${NC}"
    echo ""
    echo "Please start the application first:"
    echo "  mvn spring-boot:run"
    echo ""
    exit 1
fi
echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Step 2: Check configuration
echo -e "${YELLOW}Step 2: Checking thread pool configuration...${NC}"
METRICS=$(curl -s "$BASE_URL/api/threadpool/metrics")
TYPE=$(echo "$METRICS" | grep -o '"type":"[^"]*"' | cut -d'"' -f4)
MAX_POOL=$(echo "$METRICS" | grep -o '"maximumPoolSize":[0-9]*' | cut -d':' -f2)

echo "  Thread Pool Type: $TYPE"
echo "  Max Pool Size: $MAX_POOL"

if [ "$TYPE" != "ThreadPoolTaskExecutor" ]; then
    echo -e "${RED}⚠️  Warning: Not using ThreadPoolTaskExecutor${NC}"
fi
echo ""

# Step 3: Show initial state
echo -e "${YELLOW}Step 3: Initial thread pool state${NC}"
curl -s "$BASE_URL/api/threadpool/metrics" | \
  grep -E '"(activeCount|poolSize|queueSize|completedTaskCount)"' | \
  sed 's/[",]//g' | sed 's/^/  /'
echo ""

# Step 4: Explain the test
echo -e "${BLUE}Step 4: Test Configuration${NC}"
echo "  Endpoint: /api/products/v2/1 (ASYNC endpoint)"
echo "  Total Requests: 1000"
echo "  Concurrency: 100 (will definitely fill 20 thread pool!)"
echo "  Expected Sleep: 2 seconds per request"
echo "  Expected Duration: ~20 seconds"
echo ""

# Step 5: Open dashboard
echo -e "${YELLOW}Step 5: Open the Thread Pool Dashboard${NC}"
echo "  URL: $BASE_URL/dashboard/threadpool"
echo ""
echo -e "${GREEN}IMPORTANT: Open this URL in your browser NOW!${NC}"
echo "You will see the metrics update in REAL-TIME during the test."
echo ""
read -p "Press Enter when dashboard is open and you're ready..."
echo ""

# Step 6: Start monitoring in background
echo -e "${YELLOW}Step 6: Starting real-time monitoring...${NC}"
echo ""

# Create monitoring script
cat > /tmp/monitor-threadpool.sh << 'EOF'
#!/bin/bash
while true; do
  TIMESTAMP=$(date +"%H:%M:%S")
  METRICS=$(curl -s http://localhost:8080/api/threadpool/metrics 2>/dev/null)
  
  if [ -n "$METRICS" ]; then
    ACTIVE=$(echo "$METRICS" | grep -o '"activeCount":[0-9]*' | cut -d':' -f2)
    POOL=$(echo "$METRICS" | grep -o '"poolSize":[0-9]*' | cut -d':' -f2)
    QUEUE=$(echo "$METRICS" | grep -o '"queueSize":[0-9]*' | cut -d':' -f2)
    COMPLETED=$(echo "$METRICS" | grep -o '"completedTaskCount":[0-9]*' | cut -d':' -f2)
    
    printf "\r[$TIMESTAMP] Active: %2s | Pool: %2s | Queue: %2s | Completed: %4s" \
      "$ACTIVE" "$POOL" "$QUEUE" "$COMPLETED"
  fi
  
  sleep 1
done
EOF

chmod +x /tmp/monitor-threadpool.sh

# Start monitoring in background
/tmp/monitor-threadpool.sh &
MONITOR_PID=$!

# Wait a moment for monitoring to start
sleep 2
echo ""

# Step 7: Run load test
echo -e "${GREEN}🚀 Step 7: Starting load test...${NC}"
echo ""

ab -n 1000 -c 100 "$BASE_URL/api/products/v2/1" > /tmp/ab-results.txt 2>&1

# Stop monitoring
kill $MONITOR_PID 2>/dev/null
echo ""
echo ""

# Step 8: Show final results
echo -e "${GREEN}✅ Load test completed!${NC}"
echo ""

echo -e "${YELLOW}Step 8: Final thread pool state${NC}"
curl -s "$BASE_URL/api/threadpool/metrics" | \
  grep -E '"(activeCount|poolSize|queueSize|completedTaskCount)"' | \
  sed 's/[",]//g' | sed 's/^/  /'
echo ""

# Step 9: Show performance summary
echo -e "${BLUE}Step 9: Performance Summary${NC}"
grep "Requests per second" /tmp/ab-results.txt | sed 's/^/  /'
grep "Time per request" /tmp/ab-results.txt | head -2 | sed 's/^/  /'
grep "Failed requests" /tmp/ab-results.txt | sed 's/^/  /'
echo ""

# Step 10: Verify activity was seen
echo -e "${YELLOW}Step 10: Verification${NC}"
FINAL_COMPLETED=$(curl -s "$BASE_URL/api/threadpool/metrics" | \
  grep -o '"completedTaskCount":[0-9]*' | cut -d':' -f2)

if [ "$FINAL_COMPLETED" -gt 0 ]; then
    echo -e "${GREEN}✅ SUCCESS! Thread pool processed $FINAL_COMPLETED tasks!${NC}"
    echo ""
    echo "You should have seen:"
    echo "  - Active threads increase to 20"
    echo "  - Pool size increase to 20"
    echo "  - Queue size increase to 30+"
    echo "  - Completed task counter growing"
else
    echo -e "${RED}❌ No tasks completed. Something went wrong.${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "  1. Check if you tested the correct endpoint (v2)"
    echo "  2. Check application logs for errors"
    echo "  3. Verify sleep is configured: product.api.v2.sleep.ms=2000"
fi

echo ""
echo -e "${BLUE}📊 Check the dashboards:${NC}"
echo "  Thread Pool: $BASE_URL/dashboard/threadpool"
echo "  Tomcat: $BASE_URL/dashboard/tomcat"
echo "  HikariCP: $BASE_URL/dashboard/hikari"
echo ""

# Cleanup
rm -f /tmp/monitor-threadpool.sh

echo -e "${GREEN}Done!${NC}"
