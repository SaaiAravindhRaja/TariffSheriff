#!/bin/bash

# Redis Connection Test Script
# Tests connectivity to the configured Redis instance

set -e

echo "🔴 Testing Redis Connection..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if required variables are set
if [ -z "$REDIS_URL" ]; then
    echo -e "${RED}❌ ERROR: REDIS_URL is not set${NC}"
    exit 1
fi

# Extract connection details from REDIS_URL
# Format: redis://[username:password@]host:port[/database]
if [[ "$REDIS_URL" =~ redis://([^@]*@)?([^:]+):([^/]+)(/[0-9]+)? ]]; then
    REDIS_AUTH="${BASH_REMATCH[1]}"
    REDIS_HOST="${BASH_REMATCH[2]}"
    REDIS_PORT="${BASH_REMATCH[3]}"
    REDIS_DB="${BASH_REMATCH[4]}"
    
    # Remove @ from auth if present
    REDIS_AUTH=${REDIS_AUTH%@}
    
    # Extract username and password if present
    if [[ "$REDIS_AUTH" == *":"* ]]; then
        REDIS_USER="${REDIS_AUTH%:*}"
        REDIS_PASS="${REDIS_AUTH#*:}"
    else
        REDIS_USER=""
        REDIS_PASS="$REDIS_AUTH"
    fi
    
    # Remove leading slash from database number
    REDIS_DB=${REDIS_DB#/}
else
    echo -e "${RED}❌ ERROR: Invalid REDIS_URL format${NC}"
    echo "Expected format: redis://[username:password@]host:port[/database]"
    exit 1
fi

echo "📋 Connection Details:"
echo "Host: $REDIS_HOST"
echo "Port: $REDIS_PORT"
if [ ! -z "$REDIS_DB" ]; then
    echo "Database: $REDIS_DB"
else
    echo "Database: 0 (default)"
fi
if [ ! -z "$REDIS_USER" ]; then
    echo "Username: $REDIS_USER"
fi
if [ ! -z "$REDIS_PASS" ]; then
    echo "Password: [REDACTED]"
fi

# Test basic connectivity
echo -e "\n🔍 Testing basic connectivity..."
if command -v redis-cli &> /dev/null; then
    # Build redis-cli command
    REDIS_CMD="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
    
    if [ ! -z "$REDIS_PASS" ]; then
        REDIS_CMD="$REDIS_CMD -a $REDIS_PASS"
    fi
    
    if [ ! -z "$REDIS_DB" ]; then
        REDIS_CMD="$REDIS_CMD -n $REDIS_DB"
    fi
    
    # Test PING command
    if $REDIS_CMD ping 2>/dev/null | grep -q "PONG"; then
        echo -e "${GREEN}✅ Redis server is responding to PING${NC}"
    else
        echo -e "${RED}❌ Redis server is not responding${NC}"
        exit 1
    fi
    
    # Test authentication (if password is set)
    if [ ! -z "$REDIS_PASS" ]; then
        echo -e "\n🔐 Testing authentication..."
        if $REDIS_CMD auth "$REDIS_PASS" 2>/dev/null | grep -q "OK"; then
            echo -e "${GREEN}✅ Authentication successful${NC}"
        else
            echo -e "${RED}❌ Authentication failed${NC}"
            exit 1
        fi
    fi
    
    # Test basic operations
    echo -e "\n📝 Testing basic operations..."
    
    # Test SET operation
    if $REDIS_CMD set test_key "test_value" 2>/dev/null | grep -q "OK"; then
        echo -e "${GREEN}✅ SET operation successful${NC}"
    else
        echo -e "${RED}❌ SET operation failed${NC}"
        exit 1
    fi
    
    # Test GET operation
    GET_RESULT=$($REDIS_CMD get test_key 2>/dev/null)
    if [ "$GET_RESULT" = "test_value" ]; then
        echo -e "${GREEN}✅ GET operation successful${NC}"
    else
        echo -e "${RED}❌ GET operation failed${NC}"
        exit 1
    fi
    
    # Test DEL operation
    if $REDIS_CMD del test_key 2>/dev/null | grep -q "1"; then
        echo -e "${GREEN}✅ DEL operation successful${NC}"
    else
        echo -e "${RED}❌ DEL operation failed${NC}"
        exit 1
    fi
    
    # Test TTL operations (for token blacklisting)
    echo -e "\n⏰ Testing TTL operations..."
    
    # Set key with TTL
    if $REDIS_CMD setex test_ttl_key 10 "test_ttl_value" 2>/dev/null | grep -q "OK"; then
        echo -e "${GREEN}✅ SETEX operation successful${NC}"
    else
        echo -e "${RED}❌ SETEX operation failed${NC}"
        exit 1
    fi
    
    # Check TTL
    TTL_RESULT=$($REDIS_CMD ttl test_ttl_key 2>/dev/null)
    if [ "$TTL_RESULT" -gt 0 ] && [ "$TTL_RESULT" -le 10 ]; then
        echo -e "${GREEN}✅ TTL operation successful (TTL: ${TTL_RESULT}s)${NC}"
    else
        echo -e "${RED}❌ TTL operation failed${NC}"
        exit 1
    fi
    
    # Clean up
    $REDIS_CMD del test_ttl_key 2>/dev/null
    
    # Test Redis info
    echo -e "\n📊 Redis Server Information:"
    REDIS_VERSION=$($REDIS_CMD info server 2>/dev/null | grep "redis_version" | cut -d: -f2 | tr -d '\r')
    if [ ! -z "$REDIS_VERSION" ]; then
        echo "Redis Version: $REDIS_VERSION"
    fi
    
    REDIS_MODE=$($REDIS_CMD info server 2>/dev/null | grep "redis_mode" | cut -d: -f2 | tr -d '\r')
    if [ ! -z "$REDIS_MODE" ]; then
        echo "Redis Mode: $REDIS_MODE"
    fi
    
    # Check memory usage
    USED_MEMORY=$($REDIS_CMD info memory 2>/dev/null | grep "used_memory_human" | cut -d: -f2 | tr -d '\r')
    if [ ! -z "$USED_MEMORY" ]; then
        echo "Used Memory: $USED_MEMORY"
    fi
    
    MAX_MEMORY=$($REDIS_CMD info memory 2>/dev/null | grep "maxmemory_human" | cut -d: -f2 | tr -d '\r')
    if [ ! -z "$MAX_MEMORY" ] && [ "$MAX_MEMORY" != "0B" ]; then
        echo "Max Memory: $MAX_MEMORY"
    fi
    
    # Check connected clients
    CONNECTED_CLIENTS=$($REDIS_CMD info clients 2>/dev/null | grep "connected_clients" | cut -d: -f2 | tr -d '\r')
    if [ ! -z "$CONNECTED_CLIENTS" ]; then
        echo "Connected Clients: $CONNECTED_CLIENTS"
    fi
    
else
    echo -e "${YELLOW}⚠️  redis-cli not available, using alternative connectivity test${NC}"
    echo "To install redis-cli:"
    echo "  - macOS: brew install redis"
    echo "  - Ubuntu: sudo apt-get install redis-tools"
    echo "  - CentOS: sudo yum install redis"
    
    # Alternative test using nc (netcat)
    if command -v nc &> /dev/null; then
        echo -e "\n🔍 Testing connectivity with netcat..."
        if nc -z "$REDIS_HOST" "$REDIS_PORT" 2>/dev/null; then
            echo -e "${GREEN}✅ Port $REDIS_PORT is open on $REDIS_HOST${NC}"
        else
            echo -e "${RED}❌ Cannot connect to $REDIS_HOST:$REDIS_PORT${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  netcat not available, skipping connectivity test${NC}"
    fi
fi

# Performance test (optional)
if command -v redis-cli &> /dev/null && [ "$1" = "--performance" ]; then
    echo -e "\n⚡ Running performance test..."
    
    # Build redis-cli command
    REDIS_CMD="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
    
    if [ ! -z "$REDIS_PASS" ]; then
        REDIS_CMD="$REDIS_CMD -a $REDIS_PASS"
    fi
    
    if [ ! -z "$REDIS_DB" ]; then
        REDIS_CMD="$REDIS_CMD -n $REDIS_DB"
    fi
    
    start_time=$(date +%s%N)
    for i in {1..100}; do
        $REDIS_CMD ping > /dev/null 2>&1
    done
    end_time=$(date +%s%N)
    
    duration=$(( (end_time - start_time) / 1000000 ))
    avg_duration=$(( duration / 100 ))
    
    echo "Average PING time: ${avg_duration}ms"
    
    if [ $avg_duration -lt 10 ]; then
        echo -e "${GREEN}✅ Redis performance is excellent${NC}"
    elif [ $avg_duration -lt 50 ]; then
        echo -e "${GREEN}✅ Redis performance is good${NC}"
    elif [ $avg_duration -lt 100 ]; then
        echo -e "${YELLOW}⚠️  Redis performance is acceptable${NC}"
    else
        echo -e "${RED}❌ Redis performance is slow${NC}"
    fi
fi

# Test Spring Boot Redis configuration
echo -e "\n🍃 Spring Boot Redis Configuration:"
if [ ! -z "$SPRING_DATA_REDIS_TIMEOUT" ]; then
    echo "Timeout: $SPRING_DATA_REDIS_TIMEOUT"
else
    echo "Timeout: default (2000ms)"
fi

if [ ! -z "$SPRING_DATA_REDIS_JEDIS_POOL_MAX_ACTIVE" ]; then
    echo "Max Active Connections: $SPRING_DATA_REDIS_JEDIS_POOL_MAX_ACTIVE"
else
    echo "Max Active Connections: default (8)"
fi

echo -e "\n${GREEN}🎉 Redis connection test completed successfully!${NC}"