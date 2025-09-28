#!/bin/bash

# Database Backup Script for TariffSheriff Authentication System
# Creates comprehensive backups of authentication data with encryption

set -e

echo "🗄️  Starting TariffSheriff Database Backup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
ENCRYPT_BACKUPS="${ENCRYPT_BACKUPS:-true}"
ENCRYPTION_KEY="${BACKUP_ENCRYPTION_KEY}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_PREFIX="tariffsheriff_backup_${TIMESTAMP}"

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

echo "📋 Backup Configuration:"
echo "Database: $DB_NAME"
echo "Host: $DB_HOST"
echo "Port: $DB_PORT"
echo "User: $DB_USER"
echo "Backup Directory: $BACKUP_DIR"
echo "Encryption: $ENCRYPT_BACKUPS"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Function to create full database backup
create_full_backup() {
    echo -e "\n📦 Creating full database backup..."
    
    local backup_file="$BACKUP_DIR/${BACKUP_PREFIX}_full.sql"
    
    PGPASSWORD="$DB_PASS" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --no-password \
        --format=custom \
        --compress=9 \
        --file="$backup_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Full backup created: $backup_file${NC}"
        
        # Get backup file size
        local file_size=$(du -h "$backup_file" | cut -f1)
        echo "Backup size: $file_size"
        
        return 0
    else
        echo -e "${RED}❌ Full backup failed${NC}"
        return 1
    fi
}

# Function to create authentication data backup
create_auth_backup() {
    echo -e "\n🔐 Creating authentication data backup..."
    
    local backup_file="$BACKUP_DIR/${BACKUP_PREFIX}_auth.sql"
    
    # Backup only authentication-related tables
    PGPASSWORD="$DB_PASS" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --no-password \
        --format=plain \
        --data-only \
        --table=users \
        --table=audit_logs \
        --table=flyway_schema_history \
        --file="$backup_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Authentication backup created: $backup_file${NC}"
        return 0
    else
        echo -e "${RED}❌ Authentication backup failed${NC}"
        return 1
    fi
}

# Function to create schema backup
create_schema_backup() {
    echo -e "\n🏗️  Creating schema backup..."
    
    local backup_file="$BACKUP_DIR/${BACKUP_PREFIX}_schema.sql"
    
    PGPASSWORD="$DB_PASS" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --verbose \
        --no-password \
        --schema-only \
        --file="$backup_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Schema backup created: $backup_file${NC}"
        return 0
    else
        echo -e "${RED}❌ Schema backup failed${NC}"
        return 1
    fi
}

# Function to encrypt backup files
encrypt_backups() {
    if [ "$ENCRYPT_BACKUPS" = "true" ]; then
        if [ -z "$ENCRYPTION_KEY" ]; then
            echo -e "${YELLOW}⚠️  WARNING: BACKUP_ENCRYPTION_KEY not set, skipping encryption${NC}"
            return 0
        fi
        
        echo -e "\n🔒 Encrypting backup files..."
        
        for file in "$BACKUP_DIR"/${BACKUP_PREFIX}*.sql; do
            if [ -f "$file" ]; then
                echo "Encrypting: $(basename "$file")"
                
                # Use OpenSSL for encryption
                openssl enc -aes-256-cbc -salt -in "$file" -out "${file}.enc" -k "$ENCRYPTION_KEY"
                
                if [ $? -eq 0 ]; then
                    # Remove unencrypted file
                    rm "$file"
                    echo -e "${GREEN}✅ Encrypted: ${file}.enc${NC}"
                else
                    echo -e "${RED}❌ Encryption failed for: $file${NC}"
                fi
            fi
        done
    fi
}

# Function to create backup metadata
create_backup_metadata() {
    echo -e "\n📝 Creating backup metadata..."
    
    local metadata_file="$BACKUP_DIR/${BACKUP_PREFIX}_metadata.json"
    
    cat > "$metadata_file" << EOF
{
    "backup_timestamp": "$TIMESTAMP",
    "database_name": "$DB_NAME",
    "database_host": "$DB_HOST",
    "database_port": "$DB_PORT",
    "backup_type": "full",
    "encryption_enabled": $ENCRYPT_BACKUPS,
    "spring_profile": "${SPRING_PROFILES_ACTIVE:-default}",
    "git_commit": "${GITHUB_SHA:-unknown}",
    "backup_files": [
EOF

    # List backup files
    local first=true
    for file in "$BACKUP_DIR"/${BACKUP_PREFIX}*; do
        if [ -f "$file" ] && [[ "$file" != *"metadata.json" ]]; then
            if [ "$first" = true ]; then
                first=false
            else
                echo "," >> "$metadata_file"
            fi
            
            local filename=$(basename "$file")
            local filesize=$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file" 2>/dev/null || echo "unknown")
            
            echo -n "        {\"filename\": \"$filename\", \"size_bytes\": $filesize}" >> "$metadata_file"
        fi
    done
    
    cat >> "$metadata_file" << EOF

    ],
    "created_by": "${USER:-unknown}",
    "backup_script_version": "1.0"
}
EOF

    echo -e "${GREEN}✅ Metadata created: $metadata_file${NC}"
}

# Function to clean up old backups
cleanup_old_backups() {
    echo -e "\n🧹 Cleaning up old backups (older than $BACKUP_RETENTION_DAYS days)..."
    
    find "$BACKUP_DIR" -name "tariffsheriff_backup_*" -type f -mtime +$BACKUP_RETENTION_DAYS -delete
    
    local deleted_count=$(find "$BACKUP_DIR" -name "tariffsheriff_backup_*" -type f -mtime +$BACKUP_RETENTION_DAYS | wc -l)
    
    if [ $deleted_count -gt 0 ]; then
        echo -e "${GREEN}✅ Cleaned up $deleted_count old backup files${NC}"
    else
        echo "No old backups to clean up"
    fi
}

# Function to verify backup integrity
verify_backup_integrity() {
    echo -e "\n🔍 Verifying backup integrity..."
    
    for file in "$BACKUP_DIR"/${BACKUP_PREFIX}*.sql*; do
        if [ -f "$file" ]; then
            local filename=$(basename "$file")
            
            if [[ "$file" == *.enc ]]; then
                # Verify encrypted file can be decrypted
                if [ ! -z "$ENCRYPTION_KEY" ]; then
                    openssl enc -aes-256-cbc -d -in "$file" -k "$ENCRYPTION_KEY" > /dev/null 2>&1
                    if [ $? -eq 0 ]; then
                        echo -e "${GREEN}✅ Encryption integrity verified: $filename${NC}"
                    else
                        echo -e "${RED}❌ Encryption integrity failed: $filename${NC}"
                    fi
                fi
            else
                # Verify SQL file syntax
                if [[ "$file" == *.sql ]]; then
                    # Basic SQL syntax check
                    if grep -q "PostgreSQL database dump" "$file" 2>/dev/null; then
                        echo -e "${GREEN}✅ SQL integrity verified: $filename${NC}"
                    else
                        echo -e "${YELLOW}⚠️  SQL integrity warning: $filename${NC}"
                    fi
                fi
            fi
        fi
    done
}

# Main backup process
main() {
    echo "🚀 Starting backup process..."
    
    # Check prerequisites
    if ! command -v pg_dump &> /dev/null; then
        echo -e "${RED}❌ ERROR: pg_dump not found. Please install PostgreSQL client tools.${NC}"
        exit 1
    fi
    
    if [ "$ENCRYPT_BACKUPS" = "true" ] && ! command -v openssl &> /dev/null; then
        echo -e "${RED}❌ ERROR: openssl not found. Required for backup encryption.${NC}"
        exit 1
    fi
    
    # Create backups
    if create_full_backup && create_auth_backup && create_schema_backup; then
        encrypt_backups
        create_backup_metadata
        verify_backup_integrity
        cleanup_old_backups
        
        echo -e "\n${GREEN}🎉 Backup process completed successfully!${NC}"
        echo "Backup location: $BACKUP_DIR"
        echo "Backup prefix: $BACKUP_PREFIX"
        
        # List created files
        echo -e "\n📁 Created backup files:"
        ls -lh "$BACKUP_DIR"/${BACKUP_PREFIX}*
        
        exit 0
    else
        echo -e "\n${RED}❌ Backup process failed!${NC}"
        exit 1
    fi
}

# Handle script arguments
case "${1:-}" in
    --help|-h)
        echo "TariffSheriff Database Backup Script"
        echo ""
        echo "Usage: $0 [options]"
        echo ""
        echo "Options:"
        echo "  --help, -h          Show this help message"
        echo "  --full-only         Create only full backup"
        echo "  --auth-only         Create only authentication data backup"
        echo "  --schema-only       Create only schema backup"
        echo "  --no-encrypt        Skip encryption even if enabled"
        echo ""
        echo "Environment Variables:"
        echo "  DATABASE_URL        PostgreSQL connection URL (required)"
        echo "  BACKUP_DIR          Backup directory (default: ./backups)"
        echo "  BACKUP_RETENTION_DAYS  Days to keep backups (default: 30)"
        echo "  ENCRYPT_BACKUPS     Enable encryption (default: true)"
        echo "  BACKUP_ENCRYPTION_KEY  Encryption key for backups"
        echo ""
        exit 0
        ;;
    --full-only)
        create_full_backup && encrypt_backups && create_backup_metadata
        ;;
    --auth-only)
        create_auth_backup && encrypt_backups && create_backup_metadata
        ;;
    --schema-only)
        create_schema_backup && encrypt_backups && create_backup_metadata
        ;;
    --no-encrypt)
        ENCRYPT_BACKUPS=false
        main
        ;;
    *)
        main
        ;;
esac