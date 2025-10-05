#!/bin/bash

# Thread Pool Load Test Script
# This script generates load to show thread pool activity

echo "🧵 Thread Pool Load Test"
echo "========================"
echo ""
echo "📊 Open the dashboard to watch live updates:"
echo "   http://localhost:8080/dashboard/threadpool"
echo ""
echo "Press Ctrl+C to stop the test"
echo ""

# Function to make async requests
make_requests() {
    for i in {1..100}; do
        # Async product requests (these use the thread pool!)
        curl -s "http://localhost:8080/api/products/v2/1" > /dev/null &
        curl -s "http://localhost:8080/api/products/v2/2" > /dev/null &
        curl -s "http://localhost:8080/api/products/v2/3" > /dev/null &
        
        # Async customer requests (these also use the thread pool!)
        curl -s "http://localhost:8080/api/customers/async" > /dev/null &
        curl -s "http://localhost:8080/api/customers/async/1" > /dev/null &
        
        # Add small delay to control request rate
        sleep 0.1
    done
}

echo "🚀 Starting load test..."
echo "📈 Generating ~50 requests per second"
echo ""

# Run the load test
make_requests

echo ""
echo "✅ Load test completed!"
echo ""
echo "💡 To see activity in real-time:"
echo "   1. Run: ./test-threadpool-load.sh &"
echo "   2. Open: http://localhost:8080/dashboard/threadpool"
echo "   3. Watch the charts update!"

