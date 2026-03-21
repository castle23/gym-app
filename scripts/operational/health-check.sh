#!/bin/bash

echo "=== Health Check - Gym Platform API Services ==="
echo ""

# Services configuration
declare -A SERVICES=(
    ["Auth Service"]="http://localhost:8081/actuator/health"
    ["Training Service"]="http://localhost:8082/training/actuator/health"
    ["Tracking Service"]="http://localhost:8083/tracking/actuator/health"
    ["Notification Service"]="http://localhost:8084/notifications/actuator/health"
)

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FAILED=0

for service_name in "${!SERVICES[@]}"; do
    url="${SERVICES[$service_name]}"
    
    echo -n "Checking $service_name... "
    
    response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" = "200" ]; then
        status=$(echo "$body" | jq -r '.status // .UP' 2>/dev/null)
        if [ "$status" = "UP" ] || [ "$status" = "RUNNING" ]; then
            echo -e "${GREEN}✓ UP${NC}"
        else
            echo -e "${YELLOW}⚠ Unknown status: $status${NC}"
            FAILED=$((FAILED + 1))
        fi
    else
        echo -e "${RED}✗ FAILED (HTTP $http_code)${NC}"
        FAILED=$((FAILED + 1))
    fi
done

echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All services are healthy${NC}"
    exit 0
else
    echo -e "${RED}$FAILED service(s) failed health check${NC}"
    exit 1
fi
