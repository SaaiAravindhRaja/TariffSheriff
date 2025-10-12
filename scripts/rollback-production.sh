#!/bin/bash

# Production Rollback Script for Enhanced AI Trade Copilot
# Provides emergency rollback capabilities with health verification

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ROLLBACK_LOG="$PROJECT_ROOT/logs/rollback-$(date +%Y%m%d-%H%M%S).log"
HEALTH_CHECK_TIMEOUT=180
HEALTH_CHECK_INTERVAL=10

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$ROLLBACK_LOG"
}

# Create logs directory
mkdir -p "$PROJECT_ROOT/logs"

# Parse command line arguments
REASON=""
FORCE_ROLLBACK=false
DRY_RUN=false
TARGET_VERSION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--reason)
            REASON="$2"
            shift 2
            ;;
        -t|--target-version)
            TARGET_VERSION="$2"
            shift 2
            ;;
        --force)
            FORCE_ROLLBACK=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -r, --reason REASON      Reason for rollback (required)"
            echo "  -t, --target-version VER Specific version to rollback to"
            echo "  --force                  Force rollback without confirmation"
            echo "  --dry-run                Show what would be rolled back"
            echo "  -h, --help               Show this help message"
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [[ -z "$REASON" ]]; then
    error "Rollback reason is required. Use -r or --reason to specify."
    exit 1
fi

log "Starting rollback process"
log "Reason: $REASON"

# Get current deployment info
get_deployment_info() {
    log "Getting current deployment information..."
    
    CURRENT_LINK="/opt/tariffsheriff/current"
    PREVIOUS_LINK="/opt/tariffsheriff/previous"
    
    if [[ -L "$CURRENT_LINK" ]]; then
        CURRENT_VERSION=$(basename "$(readlink "$CURRENT_LINK")")
        log "Current version: $CURRENT_VERSION"
    else
        error "No current deployment found"
        return 1
    fi
    
    if [[ -L "$PREVIOUS_LINK" ]]; then
        PREVIOUS_VERSION=$(basename "$(readlink "$PREVIOUS_LINK")")
        log "Previous version: $PREVIOUS_VERSION"
    else
        warning "No previous deployment found"
        PREVIOUS_VERSION=""
    fi
    
    # If target version specified, validate it exists
    if [[ -n "$TARGET_VERSION" ]]; then
        if [[ -d "/opt/tariffsheriff/deployments/$TARGET_VERSION" ]]; then
            log "Target version $TARGET_VERSION found"
            ROLLBACK_VERSION="$TARGET_VERSION"
        else
            error "Target version $TARGET_VERSION not found"
            return 1
        fi
    else
        ROLLBACK_VERSION="$PREVIOUS_VERSION"
    fi
    
    if [[ -z "$ROLLBACK_VERSION" ]]; then
        error "No version available for rollback"
        return 1
    fi
    
    log "Will rollback to version: $ROLLBACK_VERSION"
    return 0
}

# Create backup of current state
backup_current_state() {
    log "Creating backup of current state..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would create backup of current state"
        return 0
    fi
    
    BACKUP_DIR="/opt/tariffsheriff/backups/rollback-$(date +%Y%m%d-%H%M%S)"
    sudo mkdir -p "$BACKUP_DIR"
    
    # Backup current configuration
    if [[ -L "/opt/tariffsheriff/current" ]]; then
        sudo cp -r "$(readlink /opt/tariffsheriff/current)" "$BACKUP_DIR/current"
    fi
    
    # Backup database (optional - can be time consuming)
    # sudo pg_dump -h localhost -U tariffsheriff tariffsheriff_prod > "$BACKUP_DIR/database.sql"
    
    success "Backup created at $BACKUP_DIR"
    return 0
}

# Stop services gracefully
stop_services() {
    log "Stopping application services..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would stop services"
        return 0
    fi
    
    # Stop services with timeout
    timeout 30 sudo systemctl stop tariffsheriff-backend || {
        warning "Backend service did not stop gracefully, forcing stop"
        sudo systemctl kill tariffsheriff-backend
    }
    
    timeout 30 sudo systemctl stop tariffsheriff-frontend || {
        warning "Frontend service did not stop gracefully, forcing stop"
        sudo systemctl kill tariffsheriff-frontend
    }
    
    success "Services stopped"
    return 0
}

# Perform rollback
perform_rollback() {
    log "Performing rollback to version $ROLLBACK_VERSION..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would rollback to version $ROLLBACK_VERSION"
        return 0
    fi
    
    CURRENT_LINK="/opt/tariffsheriff/current"
    ROLLBACK_PATH="/opt/tariffsheriff/deployments/$ROLLBACK_VERSION"
    
    # Verify rollback target exists
    if [[ ! -d "$ROLLBACK_PATH" ]]; then
        error "Rollback target $ROLLBACK_PATH does not exist"
        return 1
    fi
    
    # Update symlink
    sudo rm -f "$CURRENT_LINK"
    sudo ln -sf "$ROLLBACK_PATH" "$CURRENT_LINK"
    
    success "Symlink updated to point to $ROLLBACK_VERSION"
    return 0
}

# Start services
start_services() {
    log "Starting application services..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would start services"
        return 0
    fi
    
    # Start backend service
    sudo systemctl start tariffsheriff-backend
    if [[ $? -ne 0 ]]; then
        error "Failed to start backend service"
        return 1
    fi
    
    # Start frontend service
    sudo systemctl start tariffsheriff-frontend
    if [[ $? -ne 0 ]]; then
        error "Failed to start frontend service"
        return 1
    fi
    
    success "Services started"
    return 0
}

# Verify rollback health
verify_rollback_health() {
    log "Verifying rollback health..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would verify rollback health"
        return 0
    fi
    
    local start_time=$(date +%s)
    local end_time=$((start_time + HEALTH_CHECK_TIMEOUT))
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Check backend health
        if curl -f -s "http://localhost:8080/api/actuator/health" > /dev/null 2>&1; then
            log "Backend health check passed"
            
            # Check frontend availability
            if curl -f -s "http://localhost:3000" > /dev/null 2>&1; then
                log "Frontend health check passed"
                
                # Check basic AI functionality
                if curl -f -s -H "Content-Type: application/json" \
                   -d '{"query":"health check"}' \
                   "http://localhost:8080/api/chat/query" > /dev/null 2>&1; then
                    success "Rollback health verification passed"
                    return 0
                fi
            fi
        fi
        
        log "Health checks in progress... ($(( (end_time - $(date +%s)) / 60 )) minutes remaining)"
        sleep $HEALTH_CHECK_INTERVAL
    done
    
    error "Rollback health verification failed"
    return 1
}

# Send rollback notification
send_notification() {
    log "Sending rollback notification..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would send rollback notification"
        return 0
    fi
    
    # This would typically integrate with your notification system
    # For now, we'll just log the notification
    local message="ROLLBACK COMPLETED: TariffSheriff rolled back from $CURRENT_VERSION to $ROLLBACK_VERSION. Reason: $REASON"
    
    # Log to system log
    logger -t "tariffsheriff-rollback" "$message"
    
    # Could also send to Slack, email, etc.
    # curl -X POST -H 'Content-type: application/json' \
    #   --data "{\"text\":\"$message\"}" \
    #   "$SLACK_WEBHOOK_URL"
    
    success "Rollback notification sent"
    return 0
}

# Emergency rollback (faster, less safe)
emergency_rollback() {
    log "Performing EMERGENCY rollback..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would perform emergency rollback"
        return 0
    fi
    
    # Kill services immediately
    sudo systemctl kill tariffsheriff-backend tariffsheriff-frontend
    
    # Quick rollback
    CURRENT_LINK="/opt/tariffsheriff/current"
    PREVIOUS_LINK="/opt/tariffsheriff/previous"
    
    if [[ -L "$PREVIOUS_LINK" ]]; then
        sudo rm -f "$CURRENT_LINK"
        sudo mv "$PREVIOUS_LINK" "$CURRENT_LINK"
    else
        error "No previous deployment available for emergency rollback"
        return 1
    fi
    
    # Start services
    sudo systemctl start tariffsheriff-backend tariffsheriff-frontend
    
    success "Emergency rollback completed"
    return 0
}

# Main rollback process
main() {
    log "Enhanced AI Trade Copilot Rollback Script"
    log "Reason: $REASON"
    log "Dry Run: $DRY_RUN"
    
    # Check if this is an emergency rollback
    if [[ "$REASON" == *"EMERGENCY"* ]]; then
        warning "Emergency rollback detected - using fast rollback procedure"
        
        if [[ "$FORCE_ROLLBACK" == "false" && "$DRY_RUN" == "false" ]]; then
            echo -n "Are you sure you want to perform an EMERGENCY rollback? (y/N): "
            read -r confirmation
            if [[ "$confirmation" != "y" && "$confirmation" != "Y" ]]; then
                log "Emergency rollback cancelled by user"
                exit 0
            fi
        fi
        
        if ! emergency_rollback; then
            error "Emergency rollback failed"
            exit 1
        fi
        
        send_notification
        success "Emergency rollback completed!"
        exit 0
    fi
    
    # Normal rollback process
    if [[ "$FORCE_ROLLBACK" == "false" && "$DRY_RUN" == "false" ]]; then
        echo -n "Are you sure you want to rollback? This will affect production users. (y/N): "
        read -r confirmation
        if [[ "$confirmation" != "y" && "$confirmation" != "Y" ]]; then
            log "Rollback cancelled by user"
            exit 0
        fi
    fi
    
    # Execute rollback steps
    if ! get_deployment_info; then
        error "Failed to get deployment information"
        exit 1
    fi
    
    if ! backup_current_state; then
        error "Failed to create backup"
        exit 1
    fi
    
    if ! stop_services; then
        error "Failed to stop services"
        exit 1
    fi
    
    if ! perform_rollback; then
        error "Rollback failed"
        exit 1
    fi
    
    if ! start_services; then
        error "Failed to start services after rollback"
        exit 1
    fi
    
    if ! verify_rollback_health; then
        error "Rollback health verification failed"
        exit 1
    fi
    
    send_notification
    
    success "Rollback completed successfully!"
    log "Rollback log saved to: $ROLLBACK_LOG"
}

# Trap signals for cleanup
trap 'error "Rollback interrupted"; exit 1' INT TERM

# Run main function
main "$@"