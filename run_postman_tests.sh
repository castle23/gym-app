#!/bin/bash

echo "=== Gym Platform API - Postman Collection Testing ==="
echo ""
echo "Starting tests at $(date)"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test configuration
COLLECTION="Gym_Platform_API.postman_collection.json"
ENVIRONMENT="Gym_Platform_API_Testing_Environment.postman_environment.json"
RESULTS_FILE="test_results_$(date +%Y%m%d_%H%M%S).json"
RESULTS_HTML="test_results_$(date +%Y%m%d_%H%M%S).html"

# Verify files exist
if [ ! -f "$COLLECTION" ]; then
    echo -e "${RED}ERROR: Collection file not found: $COLLECTION${NC}"
    exit 1
fi

if [ ! -f "$ENVIRONMENT" ]; then
    echo -e "${YELLOW}WARNING: Environment file not found: $ENVIRONMENT${NC}"
    echo "Using default environment values"
fi

# Check if newman is installed
if ! command -v newman &> /dev/null; then
    echo -e "${RED}ERROR: newman is not installed${NC}"
    echo "Install it with: npm install -g newman"
    exit 1
fi

# Run Postman collection
echo -e "${YELLOW}Running Postman collection tests...${NC}"
echo ""

newman run "$COLLECTION" \
    --environment "$ENVIRONMENT" \
    --reporters cli,json,html \
    --reporter-json-export "$RESULTS_FILE" \
    --reporter-html-export "$RESULTS_HTML" \
    --delay-request 500 \
    --timeout-request 10000 \
    --timeout-script 5000 \
    --bail

TEST_EXIT_CODE=$?

echo ""
echo "=== Test Results ==="
echo ""

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✓ All tests PASSED${NC}"
else
    echo -e "${RED}✗ Some tests FAILED${NC}"
fi

echo ""
echo "Detailed results:"
echo "  - JSON: $RESULTS_FILE"
echo "  - HTML: $RESULTS_HTML"
echo ""
echo "To view HTML report: open $RESULTS_HTML"
echo ""

exit $TEST_EXIT_CODE
