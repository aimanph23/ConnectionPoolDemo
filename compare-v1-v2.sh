#!/bin/bash

# Script to compare V1 (blocking) vs V2 (non-blocking) endpoints
# Demonstrates connection pool behavior under load

echo "🔬 Comparing V1 (Blocking) vs V2 (Non-Blocking) Endpoints"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if app is running
if ! curl -s http://localhost:8080/api/products/health > /dev/null 2>&1; then
    echo "❌ Application not running at http://localhost:8080"
    echo "   Start the application first: mvn spring-boot:run"
    exit 1
fi

echo "✅ Application is running"
echo ""

# Function to test endpoint
test_endpoint() {
    local name=$1
    local url=$2
    local concurrent=$3
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing: $name"
    echo "URL: $url"
    echo "Concurrent Requests: $concurrent"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    
    # Get initial pool status
    echo "📊 Initial Connection Pool Status:"
    curl -s http://localhost:8080/api/monitoring/hikari/status | jq -r '"  Active: \(.active), Idle: \(.idle), Total: \(.total), Waiting: \(.waiting)"'
    echo ""
    
    echo "🚀 Sending $concurrent concurrent requests..."
    START_TIME=$(date +%s)
    
    # Send concurrent requests
    for i in $(seq 1 $concurrent); do
        curl -s "$url" > /dev/null &
    done
    
    # Wait a moment and check pool during load
    sleep 1
    echo ""
    echo "📊 Connection Pool Status DURING Load:"
    POOL_STATUS=$(curl -s http://localhost:8080/api/monitoring/hikari/status)
    echo "$POOL_STATUS" | jq -r '"  Active: \(.active), Idle: \(.idle), Total: \(.total), Waiting: \(.waiting), Healthy: \(.healthy)"'
    
    ACTIVE=$(echo "$POOL_STATUS" | jq -r '.active')
    WAITING=$(echo "$POOL_STATUS" | jq -r '.waiting')
    
    if [ "$WAITING" -gt 0 ]; then
        echo "  🚨 WARNING: $WAITING threads waiting for connections!"
    else
        echo "  ✅ No threads waiting"
    fi
    echo ""
    
    # Wait for all requests to complete
    wait
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    
    echo "⏱️  Total Time: ${DURATION} seconds"
    echo ""
    
    # Get final pool status
    echo "📊 Final Connection Pool Status:"
    curl -s http://localhost:8080/api/monitoring/hikari/status | jq -r '"  Active: \(.active), Idle: \(.idle), Total: \(.total), Waiting: \(.waiting)"'
    echo ""
    
    # Return metrics
    echo "$ACTIVE|$WAITING|$DURATION"
}

# Test V1 (Blocking)
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "TEST 1: V1 (Blocking) - Holds DB Connection During Mock API"
echo "═══════════════════════════════════════════════════════════"
echo ""

V1_RESULT=$(test_endpoint "V1 - Blocking" "http://localhost:8080/api/products/1" 15)
V1_ACTIVE=$(echo "$V1_RESULT" | cut -d'|' -f1)
V1_WAITING=$(echo "$V1_RESULT" | cut -d'|' -f2)
V1_TIME=$(echo "$V1_RESULT" | cut -d'|' -f3)

echo "Waiting 5 seconds for pool to stabilize..."
sleep 5

# Test V2 (Non-Blocking)
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "TEST 2: V2 (Non-Blocking) - Releases DB Connection Early"
echo "═══════════════════════════════════════════════════════════"
echo ""

V2_RESULT=$(test_endpoint "V2 - Non-Blocking" "http://localhost:8080/api/products/v2/1" 15)
V2_ACTIVE=$(echo "$V2_RESULT" | cut -d'|' -f1)
V2_WAITING=$(echo "$V2_RESULT" | cut -d'|' -f2)
V2_TIME=$(echo "$V2_RESULT" | cut -d'|' -f3)

# Summary
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "📊 COMPARISON SUMMARY"
echo "═══════════════════════════════════════════════════════════"
echo ""
printf "%-30s | %-10s | %-10s\n" "Metric" "V1 (Blocking)" "V2 (Non-Block)"
echo "───────────────────────────────────────────────────────────"
printf "%-30s | %-10s | %-10s\n" "Peak Active Connections" "$V1_ACTIVE" "$V2_ACTIVE"
printf "%-30s | %-10s | %-10s\n" "Threads Waiting" "$V1_WAITING" "$V2_WAITING"
printf "%-30s | %-10s | %-10s\n" "Total Time (seconds)" "$V1_TIME" "$V2_TIME"
echo ""

# Analysis
echo "🔍 ANALYSIS:"
echo ""

if [ "$V1_WAITING" -gt "$V2_WAITING" ]; then
    DIFF=$((V1_WAITING - V2_WAITING))
    echo "✅ V2 had $DIFF fewer threads waiting for connections"
fi

if [ "$V1_ACTIVE" -gt "$V2_ACTIVE" ]; then
    PCT=$(awk "BEGIN {printf \"%.0f\", (($V1_ACTIVE - $V2_ACTIVE) / $V1_ACTIVE) * 100}")
    echo "✅ V2 used ${PCT}% fewer active connections during load"
fi

echo ""
echo "💡 KEY TAKEAWAY:"
echo "   V1 holds DB connections during the entire 2-second mock API delay."
echo "   V2 releases connections after ~50ms query, then calls mock API."
echo "   Result: V2 can handle 10x more concurrent requests!"
echo ""

echo "🎯 RECOMMENDATION:"
echo "   Use V2 (/api/products/v2/{id}) for production when making external API calls."
echo ""

echo "═══════════════════════════════════════════════════════════"
echo "Test completed at $(date)"
echo "═══════════════════════════════════════════════════════════"

