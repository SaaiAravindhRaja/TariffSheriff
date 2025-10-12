#!/bin/bash

# Production Deployment Script for Enhanced AI Trade Copilot
# Implements blue-green deployment with health checks and rollback capabilities

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DEPLOYMENT_LOG="$PROJECT_ROOT/logs/deployment-$(date +%Y%m%d-%H%M%S).log"
HEALTH_CHECK_TIMEOUT=300
HEALTH_CHECK_INTERVAL=10

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$DEPLOYMENT_LOG"
}

# Create logs directory
mkdir -p "$PROJECT_ROOT/logs"

# Parse command line arguments
VERSION=""
ENVIRONMENT="production"
SKIP_TESTS=false
FORCE_DEPLOY=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --force)
            FORCE_DEPLOY=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 -v VERSION [OPTIONS]"
            echo "Options:"
            echo "  -v, --version VERSION    Deployment version (required)"
            echo "  -e, --environment ENV    Target environment (default: production)"
            echo "  --skip-tests            Skip test execution"
            echo "  --force                 Force deployment without confirmation"
            echo "  --dry-run               Show what would be deployed without executing"
            echo "  -h, --help              Show this help message"
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Validate required parameters
if [[ -z "$VERSION" ]]; then
    error "Version is required. Use -v or --version to specify."
    exit 1
fi

log "Starting deployment process for version $VERSION to $ENVIRONMENT environment"

# Pre-deployment checks
pre_deployment_checks() {
    log "Performing pre-deployment checks..."
    
    # Check if required environment variables are set
    required_vars=("DATABASE_URL" "JWT_SECRET" "GEMINI_API_KEY")
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            error "Required environment variable $var is not set"
            return 1
        fi
    done
    
    # Check database connectivity
    log "Checking database connectivity..."
    if ! timeout 10 pg_isready -h "${DATABASE_HOST:-localhost}" -p "${DATABASE_PORT:-5432}" -U "${DATABASE_USERNAME:-tariffsheriff}" > /dev/null 2>&1; then
        error "Database is not accessible"
        return 1
    fi
    
    # Check Redis connectivity
    log "Checking Redis connectivity..."
    if ! timeout 10 redis-cli -h "${REDIS_HOST:-localhost}" -p "${REDIS_PORT:-6379}" ping > /dev/null 2>&1; then
        warning "Redis is not accessible - caching will be disabled"
    fi
    
    # Check disk space
    log "Checking disk space..."
    available_space=$(df / | awk 'NR==2 {print $4}')
    if [[ $available_space -lt 1048576 ]]; then # Less than 1GB
        error "Insufficient disk space (less than 1GB available)"
        return 1
    fi
    
    success "Pre-deployment checks passed"
    return 0
}

# Build application
build_application() {
    log "Building application..."
    
    cd "$PROJECT_ROOT"
    
    # Build backend
    log "Building backend..."
    cd apps/backend
    if [[ "$DRY_RUN" == "false" ]]; then
        ./mvnw clean package -DskipTests=$SKIP_TESTS -Dspring.profiles.active=$ENVIRONMENT
        if [[ $? -ne 0 ]]; then
            error "Backend build failed"
            return 1
        fi
    else
        log "[DRY RUN] Would build backend with: ./mvnw clean package -DskipTests=$SKIP_TESTS"
    fi
    
    # Build frontend
    log "Building frontend..."
    cd ../frontend
    if [[ "$DRY_RUN" == "false" ]]; then
        npm ci
        npm run build
        if [[ $? -ne 0 ]]; then
            error "Frontend build failed"
            return 1
        fi
    else
        log "[DRY RUN] Would build frontend with: npm ci && npm run build"
    fi
    
    cd "$PROJECT_ROOT"
    success "Application build completed"
    return 0
}

# Run tests
run_tests() {
    if [[ "$SKIP_TESTS" == "true" ]]; then
        warning "Skipping tests as requested"
        return 0
    fi
    
    log "Running tests..."
    
    cd "$PROJECT_ROOT"
    
    # Run backend tests
    log "Running backend tests..."
    cd apps/backend
    if [[ "$DRY_RUN" == "false" ]]; then
        ./mvnw test -Dspring.profiles.active=test
        if [[ $? -ne 0 ]]; then
            error "Backend tests failed"
            return 1
        fi
    else
        log "[DRY RUN] Would run backend tests"
    fi
    
    # Run frontend tests
    log "Running frontend tests..."
    cd ../frontend
    if [[ "$DRY_RUN" == "false" ]]; then
        npm test -- --run
        if [[ $? -ne 0 ]]; then
            error "Frontend tests failed"
            return 1
        fi
    else
        log "[DRY RUN] Would run frontend tests"
    fi
    
    cd "$PROJECT_ROOT"
    success "All tests passed"
    return 0
}

# Deploy to target environment
deploy_application() {
    log "Deploying application to $ENVIRONMENT environment..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would deploy version $VERSION to $ENVIRONMENT"
        return 0
    fi
    
    # Create deployment directory
    DEPLOY_DIR="/opt/tariffsheriff/deployments/$VERSION"
    sudo mkdir -p "$DEPLOY_DIR"
    
    # Copy application files
    log "Copying application files..."
    sudo cp -r "$PROJECT_ROOT/apps/backend/target/"*.jar "$DEPLOY_DIR/"
    sudo cp -r "$PROJECT_ROOT/apps/frontend/dist" "$DEPLOY_DIR/frontend"
    sudo cp "$PROJECT_ROOT/apps/backend/src/main/resources/application-$ENVIRONMENT.properties" "$DEPLOY_DIR/"
    
    # Update symlink for blue-green deployment
    CURRENT_LINK="/opt/tariffsheriff/current"
    PREVIOUS_LINK="/opt/tariffsheriff/previous"
    
    if [[ -L "$CURRENT_LINK" ]]; then
        sudo rm -f "$PREVIOUS_LINK"
        sudo mv "$CURRENT_LINK" "$PREVIOUS_LINK"
    fi
    
    sudo ln -sf "$DEPLOY_DIR" "$CURRENT_LINK"
    
    # Restart application services
    log "Restarting application services..."
    sudo systemctl restart tariffsheriff-backend
    sudo systemctl restart tariffsheriff-frontend
    
    success "Application deployed successfully"
    return 0
}

# Health checks
perform_health_checks() {
    log "Performing health checks..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would perform health checks"
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
                
                # Check AI services
                if curl -f -s -H "Content-Type: application/json" \
                   -d '{"query":"test"}' \
                   "http://localhost:8080/api/chat/query" > /dev/null 2>&1; then
                    success "All health checks passed"
                    return 0
                fi
            fi
        fi
        
        log "Health checks in progress... ($(( (end_time - $(date +%s)) / 60 )) minutes remaining)"
        sleep $HEALTH_CHECK_INTERVAL
    done
    
    error "Health checks failed - deployment may be unhealthy"
    return 1
}

# Rollback deployment
rollback_deployment() {
    log "Rolling back deployment..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would rollback deployment"
        return 0
    fi
    
    CURRENT_LINK="/opt/tariffsheriff/current"
    PREVIOUS_LINK="/opt/tariffsheriff/previous"
    
    if [[ -L "$PREVIOUS_LINK" ]]; then
        sudo rm -f "$CURRENT_LINK"
        sudo mv "$PREVIOUS_LINK" "$CURRENT_LINK"
        
        # Restart services
        sudo systemctl restart tariffsheriff-backend
        sudo systemctl restart tariffsheriff-frontend
        
        success "Rollback completed"
        return 0
    else
        error "No previous deployment found for rollback"
        return 1
    fi
}

# Cleanup old deployments
cleanup_old_deployments() {
    log "Cleaning up old deployments..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "[DRY RUN] Would cleanup old deployments"
        return 0
    fi
    
    # Keep last 5 deployments
    cd /opt/tariffsheriff/deployments
    ls -t | tail -n +6 | xargs -r sudo rm -rf
    
    success "Cleanup completed"
}

# Main deployment process
main() {
    log "Enhanced AI Trade Copilot Deployment Script"
    log "Version: $VERSION"
    log "Environment: $ENVIRONMENT"
    log "Dry Run: $DRY_RUN"
    
    # Confirmation prompt
    if [[ "$FORCE_DEPLOY" == "false" && "$DRY_RUN" == "false" ]]; then
        echo -n "Are you sure you want to deploy version $VERSION to $ENVIRONMENT? (y/N): "
        read -r confirmation
        if [[ "$confirmation" != "y" && "$confirmation" != "Y" ]]; then
            log "Deployment cancelled by user"
            exit 0
        fi
    fi
    
    # Execute deployment steps
    if ! pre_deployment_checks; then
        error "Pre-deployment checks failed"
        exit 1
    fi
    
    if ! build_application; then
        error "Application build failed"
        exit 1
    fi
    
    if ! run_tests; then
        error "Tests failed"
        exit 1
    fi
    
    if ! deploy_application; then
        error "Deployment failed"
        exit 1
    fi
    
    if ! perform_health_checks; then
        error "Health checks failed - initiating rollback"
        if ! rollback_deployment; then
            error "Rollback failed - manual intervention required"
            exit 1
        fi
        exit 1
    fi
    
    cleanup_old_deployments
    
    success "Deployment completed successfully!"
    log "Deployment log saved to: $DEPLOYMENT_LOG"
}

# Trap signals for cleanup
trap 'error "Deployment interrupted"; exit 1' INT TERM

# Run main function
main "$@"