#!/bin/bash

# Database Maintenance Script for TariffSheriff Authentication System
# Performs routine maintenance tasks including cleanup, optimization, and health checks

set -e

echo "🔧 Starting TariffSheriff Database Maintenance..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
AUDIT_LOG_RETENTION_DAYS="${AUDIT_LOG_RETENTION_DAYS:-90}"
EXPIRED_TOKEN_CLEANUP_DAYS="${EXPIRED_TOKEN_CLEANUP_DAYS:-7}"
VACUUM_ANALYZE="${VACUUM_ANALYZE:-true}"
REINDEX_TABLES="${REINDEX_TABLES:-false}"

# Check if required variables are set
if [ -z "$DATABASE_URL" ]; then
    echo -e "${RED}❌ ERROR: DATABASE_URL is not set${NC}"
    exit 1
fi

# Extract connection details from DATABASE_URL
if [[ "$DATABASE_URL" =~ postgresql://([^:]+):([^@]+)@([^:]+):([^/]+)/([^?]+) ]]; then
    DB_USER="${BASH_REMATCH[1]}"
    DB_PASS="${BASH_REMATCH[2]}"
    DB_HOST="${BASH_REMATCH[3]}"
    DB_PORT="${BASH_REMATCH[4]}"
    DB_NAME="${BASH_REMATCH[5]}"
else
    echo -e "${RED}❌ ERROR: Invalid DATABASE_URL format${NC}"
    exit 1
fi

echo "📋 Maintenance Configuration:"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST"
echo "Audit Log Retention: $AUDIT_LOG_RETENTION_DAYS days"
echo "Expired Token Cleanup: $EXPIRED_TOKEN_CLEANUP_DAYS days"
echo "Vacuum/Analyze: $VACUUM_ANALYZE"
echo "Reindex Tables: $REINDEX_TABLES"

# Function to execute SQL query
execute_sql() {
    local query="$1"
    local description="$2"
    
    if [ ! -z "$description" ]; then
        echo "🔍 $description"
    fi
    
    PGPASSWORD="$DB_PASS" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -c "$query"
}

# Function to execute SQL query and return result
execute_sql_result() {
    local query="$1"
    
    PGPASSWORD="$DB_PASS" psql \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        -t -c "$query" | xargs
}

# Function to clean up old audit logs
cleanup_audit_logs() {
    echo -e "\n🧹 Cleaning up old audit logs..."
    
    local cutoff_date=$(date -d "$AUDIT_LOG_RETENTION_DAYS days ago" +"%Y-%m-%d")
    
    # Count records to be deleted
    local count=$(execute_sql_result "SELECT COUNT(*) FROM audit_logs WHERE created_at < '$cutoff_date';")
    
    if [ "$count" -gt 0 ]; then
        echo "Found $count audit log records older than $AUDIT_LOG_RETENTION_DAYS days"
        
        # Delete old audit logs
        execute_sql "DELETE FROM audit_logs WHERE created_at < '$cutoff_date';" "Deleting old audit logs"
        
        echo -e "${GREEN}✅ Deleted $count old audit log records${NC}"
        
        # Log the cleanup action
        execute_sql "INSERT INTO audit_logs (user_id, action, resource_type, details, created_at) VALUES (NULL, 'SYSTEM_EVENT', 'SYSTEM', 'Cleaned up $count old audit log records', CURRENT_TIMESTAMP);"
    else
        echo "No old audit logs to clean up"
    fi
}

# Function to clean up expired tokens
cleanup_expired_tokens() {
    echo -e "\n🔑 Cleaning up expired authentication tokens..."
    
    local cutoff_date=$(date -d "$EXPIRED_TOKEN_CLEANUP_DAYS days ago" +"%Y-%m-%d")
    
    # Clean up expired verification tokens
    local verification_count=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE verification_token_expires < '$cutoff_date' AND verification_token IS NOT NULL;")
    
    if [ "$verification_count" -gt 0 ]; then
        execute_sql "UPDATE users SET verification_token = NULL, verification_token_expires = NULL WHERE verification_token_expires < '$cutoff_date';" "Cleaning expired verification tokens"
        echo -e "${GREEN}✅ Cleaned up $verification_count expired verification tokens${NC}"
    fi
    
    # Clean up expired password reset tokens
    local reset_count=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE password_reset_token_expires < '$cutoff_date' AND password_reset_token IS NOT NULL;")
    
    if [ "$reset_count" -gt 0 ]; then
        execute_sql "UPDATE users SET password_reset_token = NULL, password_reset_token_expires = NULL WHERE password_reset_token_expires < '$cutoff_date';" "Cleaning expired password reset tokens"
        echo -e "${GREEN}✅ Cleaned up $reset_count expired password reset tokens${NC}"
    fi
    
    if [ "$verification_count" -eq 0 ] && [ "$reset_count" -eq 0 ]; then
        echo "No expired tokens to clean up"
    fi
}

# Function to reset expired account lockouts
reset_expired_lockouts() {
    echo -e "\n🔓 Resetting expired account lockouts..."
    
    local lockout_count=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE account_locked_until < CURRENT_TIMESTAMP AND account_locked_until IS NOT NULL;")
    
    if [ "$lockout_count" -gt 0 ]; then
        execute_sql "UPDATE users SET account_locked_until = NULL, failed_login_attempts = 0 WHERE account_locked_until < CURRENT_TIMESTAMP;" "Resetting expired account lockouts"
        echo -e "${GREEN}✅ Reset $lockout_count expired account lockouts${NC}"
        
        # Log the action
        execute_sql "INSERT INTO audit_logs (user_id, action, resource_type, details, created_at) VALUES (NULL, 'SYSTEM_EVENT', 'SYSTEM', 'Reset $lockout_count expired account lockouts', CURRENT_TIMESTAMP);"
    else
        echo "No expired account lockouts to reset"
    fi
}

# Function to update database statistics
update_statistics() {
    if [ "$VACUUM_ANALYZE" = "true" ]; then
        echo -e "\n📊 Updating database statistics..."
        
        # Get list of tables
        local tables=$(execute_sql_result "SELECT string_agg(tablename, ' ') FROM pg_tables WHERE schemaname = 'public';")
        
        for table in $tables; do
            echo "Analyzing table: $table"
            execute_sql "ANALYZE $table;"
        done
        
        echo -e "${GREEN}✅ Database statistics updated${NC}"
    fi
}

# Function to vacuum database
vacuum_database() {
    if [ "$VACUUM_ANALYZE" = "true" ]; then
        echo -e "\n🧽 Vacuuming database..."
        
        # Vacuum all tables
        execute_sql "VACUUM;" "Running VACUUM on all tables"
        
        echo -e "${GREEN}✅ Database vacuum completed${NC}"
    fi
}

# Function to reindex tables
reindex_tables() {
    if [ "$REINDEX_TABLES" = "true" ]; then
        echo -e "\n🔄 Reindexing database tables..."
        
        # Reindex authentication-related tables
        local auth_tables=("users" "audit_logs")
        
        for table in "${auth_tables[@]}"; do
            echo "Reindexing table: $table"
            execute_sql "REINDEX TABLE $table;"
        done
        
        echo -e "${GREEN}✅ Table reindexing completed${NC}"
    fi
}

# Function to check database health
check_database_health() {
    echo -e "\n🏥 Checking database health..."
    
    # Check table sizes
    echo "📏 Table sizes:"
    execute_sql "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
    
    # Check index usage
    echo -e "\n📈 Index usage statistics:"
    execute_sql "SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch FROM pg_stat_user_indexes WHERE schemaname = 'public' ORDER BY idx_tup_read DESC LIMIT 10;"
    
    # Check for unused indexes
    echo -e "\n🔍 Potentially unused indexes:"
    execute_sql "SELECT schemaname, tablename, indexname FROM pg_stat_user_indexes WHERE idx_scan = 0 AND schemaname = 'public';"
    
    # Check authentication-specific metrics
    echo -e "\n👥 User statistics:"
    execute_sql "SELECT status, COUNT(*) as count FROM users GROUP BY status;"
    
    echo -e "\n📊 Recent audit activity (last 7 days):"
    execute_sql "SELECT action, COUNT(*) as count FROM audit_logs WHERE created_at > CURRENT_DATE - INTERVAL '7 days' GROUP BY action ORDER BY count DESC LIMIT 10;"
}

# Function to check for data integrity issues
check_data_integrity() {
    echo -e "\n🔍 Checking data integrity..."
    
    # Check for orphaned audit logs (users that no longer exist)
    local orphaned_audits=$(execute_sql_result "SELECT COUNT(*) FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id WHERE a.user_id IS NOT NULL AND u.id IS NULL;")
    
    if [ "$orphaned_audits" -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Found $orphaned_audits orphaned audit log records${NC}"
    else
        echo -e "${GREEN}✅ No orphaned audit log records found${NC}"
    fi
    
    # Check for users with invalid status
    local invalid_status=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE status NOT IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'LOCKED');")
    
    if [ "$invalid_status" -gt 0 ]; then
        echo -e "${RED}❌ Found $invalid_status users with invalid status${NC}"
    else
        echo -e "${GREEN}✅ All users have valid status${NC}"
    fi
    
    # Check for users with verification tokens but no expiry
    local invalid_tokens=$(execute_sql_result "SELECT COUNT(*) FROM users WHERE (verification_token IS NOT NULL AND verification_token_expires IS NULL) OR (password_reset_token IS NOT NULL AND password_reset_token_expires IS NULL);")
    
    if [ "$invalid_tokens" -gt 0 ]; then
        echo -e "${RED}❌ Found $invalid_tokens users with tokens but no expiry date${NC}"
    else
        echo -e "${GREEN}✅ All tokens have proper expiry dates${NC}"
    fi
}

# Function to optimize database performance
optimize_performance() {
    echo -e "\n⚡ Optimizing database performance..."
    
    # Update table statistics
    update_statistics
    
    # Vacuum database
    vacuum_database
    
    # Reindex if requested
    reindex_tables
    
    echo -e "${GREEN}✅ Performance optimization completed${NC}"
}

# Function to generate maintenance report
generate_report() {
    echo -e "\n📋 Generating maintenance report..."
    
    local report_file="maintenance_report_$(date +%Y%m%d_%H%M%S).txt"
    
    {
        echo "TariffSheriff Database Maintenance Report"
        echo "========================================"
        echo "Date: $(date)"
        echo "Database: $DB_NAME"
        echo "Host: $DB_HOST"
        echo ""
        
        echo "Configuration:"
        echo "- Audit Log Retention: $AUDIT_LOG_RETENTION_DAYS days"
        echo "- Expired Token Cleanup: $EXPIRED_TOKEN_CLEANUP_DAYS days"
        echo "- Vacuum/Analyze: $VACUUM_ANALYZE"
        echo "- Reindex Tables: $REINDEX_TABLES"
        echo ""
        
        echo "Database Statistics:"
        execute_sql_result "SELECT 'Total Users: ' || COUNT(*) FROM users;"
        execute_sql_result "SELECT 'Active Users: ' || COUNT(*) FROM users WHERE status = 'ACTIVE';"
        execute_sql_result "SELECT 'Total Audit Logs: ' || COUNT(*) FROM audit_logs;"
        execute_sql_result "SELECT 'Database Size: ' || pg_size_pretty(pg_database_size('$DB_NAME'));"
        
    } > "$report_file"
    
    echo -e "${GREEN}✅ Maintenance report generated: $report_file${NC}"
}

# Function to show usage
show_usage() {
    echo "TariffSheriff Database Maintenance Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --help, -h                  Show this help message"
    echo "  --cleanup-only              Only perform cleanup operations"
    echo "  --health-check-only         Only perform health checks"
    echo "  --optimize-only             Only perform optimization"
    echo "  --skip-vacuum               Skip vacuum operations"
    echo "  --force-reindex             Force table reindexing"
    echo "  --report-only               Generate report without maintenance"
    echo ""
    echo "Environment Variables:"
    echo "  DATABASE_URL                PostgreSQL connection URL (required)"
    echo "  AUDIT_LOG_RETENTION_DAYS    Days to keep audit logs (default: 90)"
    echo "  EXPIRED_TOKEN_CLEANUP_DAYS  Days before cleaning expired tokens (default: 7)"
    echo "  VACUUM_ANALYZE              Enable vacuum and analyze (default: true)"
    echo "  REINDEX_TABLES              Enable table reindexing (default: false)"
    echo ""
}

# Main maintenance function
main() {
    echo "🚀 Starting maintenance operations..."
    
    # Cleanup operations
    cleanup_audit_logs
    cleanup_expired_tokens
    reset_expired_lockouts
    
    # Performance optimization
    optimize_performance
    
    # Health checks
    check_database_health
    check_data_integrity
    
    # Generate report
    generate_report
    
    echo -e "\n${GREEN}🎉 Database maintenance completed successfully!${NC}"
}

# Parse command line arguments
case "${1:-}" in
    --help|-h)
        show_usage
        exit 0
        ;;
    --cleanup-only)
        cleanup_audit_logs
        cleanup_expired_tokens
        reset_expired_lockouts
        ;;
    --health-check-only)
        check_database_health
        check_data_integrity
        ;;
    --optimize-only)
        optimize_performance
        ;;
    --skip-vacuum)
        VACUUM_ANALYZE=false
        main
        ;;
    --force-reindex)
        REINDEX_TABLES=true
        main
        ;;
    --report-only)
        generate_report
        ;;
    *)
        # Check prerequisites
        if ! command -v psql &> /dev/null; then
            echo -e "${RED}❌ ERROR: psql not found. Please install PostgreSQL client tools.${NC}"
            exit 1
        fi
        
        main
        ;;
esac