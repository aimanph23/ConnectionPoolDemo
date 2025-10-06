#!/bin/bash

# Test script for Async V2 endpoint with Thread Pool monitoring
# This script will generate load on the V2 async endpoint to show Thread Pool activity

echo "=========================================="
echo "Async V2 Thread Pool Load Test"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080"
ENDPOINT="/api/products/v2/1"
REQUESTS=1000
CONCURRENCY=50

echo -e "${BLUE}Configuration:${NC}"
echo "  Base URL: $BASE_URL"
echo "  Endpoint: $ENDPOINT"
echo "  Total Requests: $REQUESTS"
echo "  Concurrency: $CONCURRENCY"
echo ""

# Check if server is running
echo -e "${YELLOW}Checking if server is running...${NC}"
if ! curl -s -f "$BASE_URL/api/threadpool/health" > /dev/null; then
    echo -e "${RED}❌ Server is not running on $BASE_URL${NC}"
    echo "Please start the application first:"
    echo "  mvn spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✅ Server is running${NC}"
echo ""

# Check current thread pool status
echo -e "${YELLOW}Current Thread Pool Status (before load test):${NC}"
curl -s "$BASE_URL/api/threadpool/metrics" | jq -r '
  "  Active Threads: \(.activeCount)",
  "  Pool Size: \(.poolSize)",
  "  Queue Size: \(.queueSize)",
  "  Completed Tasks: \(.completedTaskCount)",
  "  Type: \(.type // "N/A")",
  "  Message: \(.message // "N/A")"
'
echo ""

# Show dashboards
echo -e "${BLUE}📊 Dashboards:${NC}"
echo "  Thread Pool: $BASE_URL/dashboard/threadpool"
echo "  Tomcat: $BASE_URL/dashboard/tomcat"
echo "  HikariCP: $BASE_URL/dashboard/hikari"
echo ""

# Ask user to open dashboard
echo -e "${YELLOW}Open the Thread Pool Dashboard in your browser:${NC}"
echo "  $BASE_URL/dashboard/threadpool"
echo ""
read -p "Press Enter when ready to start load test..."
echo ""

# Start load test
echo -e "${GREEN}🚀 Starting load test...${NC}"
echo "This will take approximately $(( ($REQUESTS / $CONCURRENCY) * 5 )) seconds"
echo ""

# Run Apache Bench
ab -n $REQUESTS -c $CONCURRENCY "$BASE_URL$ENDPOINT" 2>&1 | tee /tmp/ab-results.txt

echo ""
echo -e "${GREEN}✅ Load test completed!${NC}"
echo ""

# Show final thread pool status
echo -e "${YELLOW}Final Thread Pool Status (after load test):${NC}"
curl -s "$BASE_URL/api/threadpool/metrics" | jq -r '
  "  Active Threads: \(.activeCount)",
  "  Pool Size: \(.poolSize)",
  "  Queue Size: \(.queueSize)",
  "  Completed Tasks: \(.completedTaskCount)",
  "  Type: \(.type // "N/A")",
  "  Utilization: \(.utilizationPercent // "N/A")%"
'
echo ""

# Show summary
echo -e "${BLUE}📊 Load Test Summary:${NC}"
grep "Requests per second" /tmp/ab-results.txt | head -1
grep "Time per request" /tmp/ab-results.txt | head -2
grep "Failed requests" /tmp/ab-results.txt | head -1
echo ""

# Show configuration reminder
echo -e "${YELLOW}💡 Configuration:${NC}"
echo "  To adjust sleep duration, edit application.properties:"
echo "    product.api.v2.sleep.ms=1000"
echo ""
echo "  To test different endpoints:"
echo "    Sync V1:  $BASE_URL/api/products/1"
echo "    Async V2: $BASE_URL/api/products/v2/1"
echo ""

echo -e "${GREEN}Done! Check the dashboards for detailed metrics.${NC}"
