#!/bin/bash

# HikariCP Connection Pool Monitor
# Usage: ./monitor-pool.sh [interval_seconds]

INTERVAL=${1:-2}
ENDPOINT="http://localhost:8080/api/monitoring/hikari/status"

echo "🔍 Monitoring HikariCP Connection Pool"
echo "📍 Endpoint: $ENDPOINT"
echo "⏱️  Refresh: ${INTERVAL}s"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo "⚠️  jq not found. Install it for better formatting:"
    echo "   macOS: brew install jq"
    echo "   Ubuntu: sudo apt-get install jq"
    echo ""
    echo "Continuing without jq..."
    USE_JQ=false
else
    USE_JQ=true
fi

while true; do
    clear
    echo "🔍 HikariCP Connection Pool Monitor"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "⏰ $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    
    RESPONSE=$(curl -s "$ENDPOINT" 2>/dev/null)
    
    if [ $? -ne 0 ] || [ -z "$RESPONSE" ]; then
        echo "❌ Failed to connect to $ENDPOINT"
        echo "   Is the application running?"
        sleep $INTERVAL
        continue
    fi
    
    if [ "$USE_JQ" = true ]; then
        ACTIVE=$(echo "$RESPONSE" | jq -r '.active // "N/A"')
        IDLE=$(echo "$RESPONSE" | jq -r '.idle // "N/A"')
        TOTAL=$(echo "$RESPONSE" | jq -r '.total // "N/A"')
        WAITING=$(echo "$RESPONSE" | jq -r '.waiting // "N/A"')
        MAX=$(echo "$RESPONSE" | jq -r '.max // "N/A"')
        HEALTHY=$(echo "$RESPONSE" | jq -r '.healthy // "N/A"')
        
        # Calculate utilization
        if [ "$MAX" != "N/A" ] && [ "$ACTIVE" != "N/A" ] && [ "$MAX" -gt 0 ]; then
            UTILIZATION=$(awk "BEGIN {printf \"%.1f\", ($ACTIVE / $MAX) * 100}")
        else
            UTILIZATION="N/A"
        fi
        
        # Status indicator
        if [ "$HEALTHY" = "true" ]; then
            STATUS="✅ HEALTHY"
        else
            STATUS="⚠️  WARNING"
        fi
        
        echo "Status: $STATUS"
        echo ""
        echo "📊 Connection Pool Metrics:"
        echo "   Active:     $ACTIVE"
        echo "   Idle:       $IDLE"
        echo "   Total:      $TOTAL"
        echo "   Max:        $MAX"
        echo "   Waiting:    $WAITING"
        echo "   Usage:      ${UTILIZATION}%"
        echo ""
        
        # Visual bar
        if [ "$UTILIZATION" != "N/A" ]; then
            BAR_LENGTH=50
            FILLED=$(awk "BEGIN {printf \"%.0f\", ($UTILIZATION / 100) * $BAR_LENGTH}")
            EMPTY=$((BAR_LENGTH - FILLED))
            
            printf "   ["
            for i in $(seq 1 $FILLED); do printf "█"; done
            for i in $(seq 1 $EMPTY); do printf "░"; done
            printf "] ${UTILIZATION}%%\n"
        fi
        
        echo ""
        
        # Health indicators
        if [ "$WAITING" != "N/A" ] && [ "$WAITING" -gt 0 ]; then
            echo "🚨 ALERT: $WAITING thread(s) waiting for connections!"
        fi
        
        if [ "$UTILIZATION" != "N/A" ]; then
            UTIL_NUM=$(echo "$UTILIZATION" | cut -d'.' -f1)
            if [ "$UTIL_NUM" -ge 80 ]; then
                echo "⚠️  WARNING: High pool utilization (${UTILIZATION}%)"
            fi
        fi
        
        if [ "$IDLE" != "N/A" ] && [ "$IDLE" -eq 0 ]; then
            echo "⚠️  WARNING: No idle connections available!"
        fi
        
    else
        # Fallback without jq
        echo "$RESPONSE"
    fi
    
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Press Ctrl+C to stop"
    
    sleep $INTERVAL
done

