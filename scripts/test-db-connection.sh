#!/bin/bash

# Database Connection Test Script
# Tests connectivity to the configured PostgreSQL database

set -e

echo "🗄️  Testing Database Connection..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if required variables are set
if [ -z "$DATABASE_URL" ]; then
    echo -e "${RED}❌ ERROR: DATABASE_URL is not set${NC}"
    exit 1
fi

# Extract connection details from DATABASE_URL
# Format: postgresql://username:password@host:port/database?params
if [[ "$DATABASE_URL" =~ postgresql://([^:]+):([^@]+)@([^:]+):([^/]+)/([^?]+) ]]; then
    DB_USER="${BASH_REMATCH[1]}"
    DB_PASS="${BASH_REMATCH[2]}"
    DB_HOST="${BASH_REMATCH[3]}"
    DB_PORT="${BASH_REMATCH[4]}"
    DB_NAME="${BASH_REMATCH[5]}"
else
    echo -e "${RED}❌ ERROR: Invalid DATABASE_URL format${NC}"
    echo "Expected format: postgresql://username:password@host:port/database"
    exit 1
fi

echo "📋 Connection Details:"
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "Database: $DB_NAME"
echo "User: $DB_USER"

# Test basic connectivity
echo -e "\n🔍 Testing basic connectivity..."
if command -v pg_isready &> /dev/null; then
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME"; then
        echo -e "${GREEN}✅ Database server is accepting connections${NC}"
    else
        echo -e "${RED}❌ Database server is not accepting connections${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  pg_isready not available, skipping basic connectivity test${NC}"
fi

# Test authentication and query execution
echo -e "\n🔐 Testing authentication and query execution..."
if command -v psql &> /dev/null; then
    # Test with a simple query
    QUERY_RESULT=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT 1;" 2>/dev/null | xargs)
    
    if [ "$QUERY_RESULT" = "1" ]; then
        echo -e "${GREEN}✅ Authentication successful and queries working${NC}"
    else
        echo -e "${RED}❌ Authentication failed or query execution failed${NC}"
        exit 1
    fi
    
    # Test database schema
    echo -e "\n📊 Checking database schema..."
    TABLE_COUNT=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null | xargs)
    
    if [ "$TABLE_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✅ Database schema exists ($TABLE_COUNT tables found)${NC}"
        
        # Check for authentication-related tables
        USER_TABLE_EXISTS=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'users';" 2>/dev/null | xargs)
        
        if [ "$USER_TABLE_EXISTS" = "1" ]; then
            echo -e "${GREEN}✅ Users table exists${NC}"
        else
            echo -e "${YELLOW}⚠️  Users table not found (may need migration)${NC}"
        fi
        
        AUDIT_TABLE_EXISTS=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'audit_logs';" 2>/dev/null | xargs)
        
        if [ "$AUDIT_TABLE_EXISTS" = "1" ]; then
            echo -e "${GREEN}✅ Audit logs table exists${NC}"
        else
            echo -e "${YELLOW}⚠️  Audit logs table not found (may need migration)${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  No tables found in database (may need initial migration)${NC}"
    fi
    
    # Check Flyway schema history
    FLYWAY_TABLE_EXISTS=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history';" 2>/dev/null | xargs)
    
    if [ "$FLYWAY_TABLE_EXISTS" = "1" ]; then
        MIGRATION_COUNT=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;" 2>/dev/null | xargs)
        echo -e "${GREEN}✅ Flyway migrations table exists ($MIGRATION_COUNT successful migrations)${NC}"
    else
        echo -e "${YELLOW}⚠️  Flyway migrations table not found${NC}"
    fi
    
else
    echo -e "${YELLOW}⚠️  psql not available, skipping detailed database tests${NC}"
    echo "To install psql:"
    echo "  - macOS: brew install postgresql"
    echo "  - Ubuntu: sudo apt-get install postgresql-client"
    echo "  - CentOS: sudo yum install postgresql"
fi

# Test connection pool settings
echo -e "\n🏊 Connection Pool Information:"
if [ ! -z "$SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE" ]; then
    echo "Maximum pool size: $SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE"
else
    echo "Maximum pool size: default (10)"
fi

# Performance test (optional)
if command -v psql &> /dev/null && [ "$1" = "--performance" ]; then
    echo -e "\n⚡ Running performance test..."
    
    start_time=$(date +%s%N)
    for i in {1..10}; do
        PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT 1;" > /dev/null 2>&1
    done
    end_time=$(date +%s%N)
    
    duration=$(( (end_time - start_time) / 1000000 ))
    avg_duration=$(( duration / 10 ))
    
    echo "Average query time: ${avg_duration}ms"
    
    if [ $avg_duration -lt 100 ]; then
        echo -e "${GREEN}✅ Database performance is good${NC}"
    elif [ $avg_duration -lt 500 ]; then
        echo -e "${YELLOW}⚠️  Database performance is acceptable${NC}"
    else
        echo -e "${RED}❌ Database performance is slow${NC}"
    fi
fi

echo -e "\n${GREEN}🎉 Database connection test completed successfully!${NC}"