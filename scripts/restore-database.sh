#!/bin/bash

# Database Recovery Script for TariffSheriff Authentication System
# Restores database backups with support for encrypted backups

set -e

echo "🔄 Starting TariffSheriff Database Recovery..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKUP_DIR="${BACKUP_DIR:-./backups}"
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY}"
RESTORE_TYPE="${RESTORE_TYPE:-full}"
BACKUP_FILE=""
FORCE_RESTORE="${FORCE_RESTORE:-false}"

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

# Function to show usage
show_usage() {
    echo "TariffSheriff Database Recovery Script"
    echo ""
    echo "Usage: $0 [options] <backup_file>"
    echo ""
    echo "Options:"
    echo "  --help, -h              Show this help message"
    echo "  --list-backups          List available backup files"
    echo "  --type TYPE             Restore type: full, auth, schema (default: full)"
    echo "  --force                 Force restore without confirmation"
    echo "  --decrypt               Decrypt backup file before restore"
    echo "  --verify-only           Only verify backup integrity"
    echo ""
    echo "Environment Variables:"
    echo "  DATABASE_URL            PostgreSQL connection URL (required)"
    echo "  BACKUP_DIR              Backup directory (default: ./backups)"
    echo "  BACKUP_ENCRYPTION_KEY   Encryption key for encrypted backups"
    echo "  FORCE_RESTORE           Skip confirmation prompts (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0 tariffsheriff_backup_20240101_120000_full.sql"
    echo "  $0 --type auth tariffsheriff_backup_20240101_120000_auth.sql"
    echo "  $0 --decrypt tariffsheriff_backup_20240101_120000_full.sql.enc"
    echo ""
}

# Function to list available backups
list_backups() {
    echo "📁 Available backup files in $BACKUP_DIR:"
    echo ""
    
    if [ ! -d "$BACKUP_DIR" ]; then
        echo -e "${YELLOW}⚠️  Backup directory does not exist: $BACKUP_DIR${NC}"
        return 1
    fi
    
    local backup_files=($(find "$BACKUP_DIR" -name "tariffsheriff_backup_*" -type f | sort -r))
    
    if [ ${#backup_files[@]} -eq 0 ]; then
        echo -e "${YELLOW}⚠️  No backup files found${NC}"
        return 1
    fi
    
    printf "%-50s %-10s %-20s\n" "Filename" "Size" "Modified"
    printf "%-50s %-10s %-20s\n" "--------" "----" "--------"
    
    for file in "${backup_files[@]}"; do
        local filename=$(basename "$file")
        local filesize=$(du -h "$file" | cut -f1)
        local modified=$(stat -f "%Sm" -t "%Y-%m-%d %H:%M" "$file" 2>/dev/null || stat -c "%y" "$file" 2>/dev/null | cut -d' ' -f1,2 | cut -d'.' -f1)
        
        printf "%-50s %-10s %-20s\n" "$filename" "$filesize" "$modified"
    done
    
    echo ""
    echo "Use the filename as an argument to restore:"
    echo "  $0 <filename>"
}

# Function to verify backup file
verify_backup() {
    local backup_file="$1"
    
    echo "🔍 Verifying backup file: $(basename "$backup_file")"
    
    if [ ! -f "$backup_file" ]; then
        echo -e "${RED}❌ ERROR: Backup file not found: $backup_file${NC}"
        return 1
    fi
    
    # Check if file is encrypted
    if [[ "$backup_file" == *.enc ]]; then
        if [ -z "$ENCRYPTION_KEY" ]; then
            echo -e "${RED}❌ ERROR: Backup is encrypted but BACKUP_ENCRYPTION_KEY not provided${NC}"
            return 1
        fi
        
        echo "🔒 Verifying encrypted backup..."
        if openssl enc -aes-256-cbc -d -in "$backup_file" -k "$ENCRYPTION_KEY" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ Encrypted backup verification successful${NC}"
        else
            echo -e "${RED}❌ ERROR: Failed to decrypt backup file${NC}"
            return 1
        fi
    else
        # Verify SQL file
        if [[ "$backup_file" == *.sql ]]; then
            if grep -q "PostgreSQL database dump\|INSERT INTO\|CREATE TABLE" "$backup_file" 2>/dev/null; then
                echo -e "${GREEN}✅ SQL backup verification successful${NC}"
            else
                echo -e "${RED}❌ ERROR: Invalid SQL backup file${NC}"
                return 1
            fi
        fi
    fi
    
    return 0
}

# Function to decrypt backup file
decrypt_backup() {
    local encrypted_file="$1"
    local decrypted_file="${encrypted_file%.enc}"
    
    echo "🔓 Decrypting backup file..."
    
    if [ -z "$ENCRYPTION_KEY" ]; then
        echo -e "${RED}❌ ERROR: BACKUP_ENCRYPTION_KEY not provided${NC}"
        return 1
    fi
    
    if openssl enc -aes-256-cbc -d -in "$encrypted_file" -out "$decrypted_file" -k "$ENCRYPTION_KEY"; then
        echo -e "${GREEN}✅ Backup decrypted successfully: $(basename "$decrypted_file")${NC}"
        echo "$decrypted_file"
        return 0
    else
        echo -e "${RED}❌ ERROR: Failed to decrypt backup file${NC}"
        return 1
    fi
}

# Function to create database backup before restore
create_pre_restore_backup() {
    echo "💾 Creating pre-restore backup..."
    
    local pre_restore_backup="$BACKUP_DIR/pre_restore_$(date +%Y%m%d_%H%M%S).sql"
    
    PGPASSWORD="$DB_PASS" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --no-password \
        --format=custom \
        --compress=9 \
        --file="$pre_restore_backup"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Pre-restore backup created: $(basename "$pre_restore_backup")${NC}"
        return 0
    else
        echo -e "${RED}❌ Failed to create pre-restore backup${NC}"
        return 1
    fi
}

# Function to restore full database
restore_full_database() {
    local backup_file="$1"
    
    echo "🔄 Restoring full database from: $(basename "$backup_file")"
    
    # Check if backup is in custom format or plain SQL
    if file "$backup_file" | grep -q "PostgreSQL custom database dump"; then
        # Custom format - use pg_restore
        PGPASSWORD="$DB_PASS" pg_restore \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$DB_USER" \
            -d "$DB_NAME" \
            --verbose \
            --no-password \
            --clean \
            --if-exists \
            --single-transaction \
            "$backup_file"
    else
        # Plain SQL format - use psql
        PGPASSWORD="$DB_PASS" psql \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$DB_USER" \
            -d "$DB_NAME" \
            --single-transaction \
            -f "$backup_file"
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Full database restore completed successfully${NC}"
        return 0
    else
        echo -e "${RED}❌ Full database restore failed${NC}"
        return 1
    fi
}

# Function to restore authentication data only
restore_auth_data() {
    local backup_file="$1"
    
    echo "🔐 Restoring authentication data from: $(basename "$backup_file")"
    
    # Create temporary file with only auth-related data
    local temp_file=$(mktemp)
    
    # Extract only authentication-related INSERT statements
    grep -E "INSERT INTO (users|audit_logs)" "$backup_file" > "$temp_file"
    
    if [ -s "$temp_file" ]; then
        PGPASSWORD="$DB_PASS" psql \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$DB_USER" \
            -d "$DB_NAME" \
            --single-transaction \
            -f "$temp_file"
        
        local result=$?
        rm -f "$temp_file"
        
        if [ $result -eq 0 ]; then
            echo -e "${GREEN}✅ Authentication data restore completed successfully${NC}"
            return 0
        else
            echo -e "${RED}❌ Authentication data restore failed${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ No authentication data found in backup file${NC}"
        rm -f "$temp_file"
        return 1
    fi
}

# Function to restore schema only
restore_schema() {
    local backup_file="$1"
    
    echo "🏗️  Restoring schema from: $(basename "$backup_file")"
    
    # Create temporary file with only schema statements
    local temp_file=$(mktemp)
    
    # Extract schema-related statements (CREATE, ALTER, etc.)
    grep -E "^(CREATE|ALTER|DROP|COMMENT)" "$backup_file" > "$temp_file"
    
    if [ -s "$temp_file" ]; then
        PGPASSWORD="$DB_PASS" psql \
            -h "$DB_HOST" \
            -p "$DB_PORT" \
            -U "$DB_USER" \
            -d "$DB_NAME" \
            --single-transaction \
            -f "$temp_file"
        
        local result=$?
        rm -f "$temp_file"
        
        if [ $result -eq 0 ]; then
            echo -e "${GREEN}✅ Schema restore completed successfully${NC}"
            return 0
        else
            echo -e "${RED}❌ Schema restore failed${NC}"
            return 1
        fi
    else
        echo -e "${RED}❌ No schema data found in backup file${NC}"
        rm -f "$temp_file"
        return 1
    fi
}

# Function to verify database after restore
verify_restore() {
    echo "🔍 Verifying database after restore..."
    
    # Check if essential tables exist
    local tables=("users" "audit_logs" "flyway_schema_history")
    
    for table in "${tables[@]}"; do
        local count=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '$table';" 2>/dev/null | xargs)
        
        if [ "$count" = "1" ]; then
            echo -e "${GREEN}✅ Table exists: $table${NC}"
        else
            echo -e "${RED}❌ Table missing: $table${NC}"
        fi
    done
    
    # Check if admin user exists
    local admin_count=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM users WHERE role = 'ADMIN';" 2>/dev/null | xargs)
    
    if [ "$admin_count" -gt 0 ]; then
        echo -e "${GREEN}✅ Admin user(s) found: $admin_count${NC}"
    else
        echo -e "${YELLOW}⚠️  No admin users found${NC}"
    fi
    
    # Check recent audit logs
    local audit_count=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM audit_logs;" 2>/dev/null | xargs)
    
    echo "Audit log entries: $audit_count"
}

# Function to confirm restore operation
confirm_restore() {
    if [ "$FORCE_RESTORE" = "true" ]; then
        return 0
    fi
    
    echo -e "\n${YELLOW}⚠️  WARNING: This operation will modify the database!${NC}"
    echo "Database: $DB_NAME"
    echo "Host: $DB_HOST"
    echo "Restore Type: $RESTORE_TYPE"
    echo "Backup File: $(basename "$BACKUP_FILE")"
    echo ""
    
    read -p "Are you sure you want to proceed? (yes/no): " -r
    if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
        return 0
    else
        echo "Restore operation cancelled."
        return 1
    fi
}

# Main restore function
main() {
    echo "📋 Restore Configuration:"
    echo "Database: $DB_NAME"
    echo "Host: $DB_HOST"
    echo "Port: $DB_PORT"
    echo "User: $DB_USER"
    echo "Restore Type: $RESTORE_TYPE"
    echo "Backup File: $(basename "$BACKUP_FILE")"
    
    # Verify backup file
    if ! verify_backup "$BACKUP_FILE"; then
        exit 1
    fi
    
    # Decrypt if necessary
    local restore_file="$BACKUP_FILE"
    if [[ "$BACKUP_FILE" == *.enc ]]; then
        restore_file=$(decrypt_backup "$BACKUP_FILE")
        if [ $? -ne 0 ]; then
            exit 1
        fi
    fi
    
    # Confirm restore operation
    if ! confirm_restore; then
        exit 1
    fi
    
    # Create pre-restore backup
    if ! create_pre_restore_backup; then
        echo -e "${YELLOW}⚠️  WARNING: Failed to create pre-restore backup${NC}"
        read -p "Continue without pre-restore backup? (yes/no): " -r
        if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
            exit 1
        fi
    fi
    
    # Perform restore based on type
    case "$RESTORE_TYPE" in
        full)
            restore_full_database "$restore_file"
            ;;
        auth)
            restore_auth_data "$restore_file"
            ;;
        schema)
            restore_schema "$restore_file"
            ;;
        *)
            echo -e "${RED}❌ ERROR: Invalid restore type: $RESTORE_TYPE${NC}"
            exit 1
            ;;
    esac
    
    if [ $? -eq 0 ]; then
        verify_restore
        echo -e "\n${GREEN}🎉 Database restore completed successfully!${NC}"
        
        # Clean up decrypted file if it was created
        if [[ "$BACKUP_FILE" == *.enc ]] && [ -f "$restore_file" ]; then
            rm -f "$restore_file"
            echo "Cleaned up temporary decrypted file"
        fi
    else
        echo -e "\n${RED}❌ Database restore failed!${NC}"
        exit 1
    fi
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --help|-h)
            show_usage
            exit 0
            ;;
        --list-backups)
            list_backups
            exit 0
            ;;
        --type)
            RESTORE_TYPE="$2"
            shift 2
            ;;
        --force)
            FORCE_RESTORE=true
            shift
            ;;
        --decrypt)
            # This flag is handled automatically based on file extension
            shift
            ;;
        --verify-only)
            if [ -z "$2" ]; then
                echo -e "${RED}❌ ERROR: --verify-only requires a backup file${NC}"
                exit 1
            fi
            verify_backup "$2"
            exit $?
            ;;
        -*)
            echo -e "${RED}❌ ERROR: Unknown option: $1${NC}"
            show_usage
            exit 1
            ;;
        *)
            if [ -z "$BACKUP_FILE" ]; then
                # If it's not an absolute path, look in backup directory
                if [[ "$1" != /* ]]; then
                    BACKUP_FILE="$BACKUP_DIR/$1"
                else
                    BACKUP_FILE="$1"
                fi
            else
                echo -e "${RED}❌ ERROR: Multiple backup files specified${NC}"
                exit 1
            fi
            shift
            ;;
    esac
done

# Check if backup file was provided
if [ -z "$BACKUP_FILE" ]; then
    echo -e "${RED}❌ ERROR: No backup file specified${NC}"
    echo ""
    show_usage
    exit 1
fi

# Check prerequisites
if ! command -v pg_restore &> /dev/null || ! command -v psql &> /dev/null; then
    echo -e "${RED}❌ ERROR: PostgreSQL client tools not found${NC}"
    exit 1
fi

# Run main restore process
main