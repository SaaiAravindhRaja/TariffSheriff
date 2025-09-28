#!/bin/bash

# Configuration Validation Script for TariffSheriff Authentication System
# This script validates that all required environment variables are set

set -e

echo "🔍 Validating TariffSheriff Configuration..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validation results
ERRORS=0
WARNINGS=0

# Function to check required variable
check_required() {
    local var_name=$1
    local var_value=${!var_name}
    
    if [ -z "$var_value" ]; then
        echo -e "${RED}❌ ERROR: $var_name is not set${NC}"
        ((ERRORS++))
        return 1
    else
        echo -e "${GREEN}✅ $var_name is set${NC}"
        return 0
    fi
}

# Function to check optional variable
check_optional() {
    local var_name=$1
    local default_value=$2
    local var_value=${!var_name}
    
    if [ -z "$var_value" ]; then
        echo -e "${YELLOW}⚠️  WARNING: $var_name is not set (will use default: $default_value)${NC}"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✅ $var_name is set${NC}"
    fi
}

# Function to validate JWT secret strength
validate_jwt_secret() {
    local jwt_secret=$1
    
    if [ ${#jwt_secret} -lt 32 ]; then
        echo -e "${RED}❌ ERROR: JWT_SECRET is too short (minimum 32 characters required)${NC}"
        ((ERRORS++))
        return 1
    fi
    
    if [[ "$jwt_secret" == *"dev"* ]] && [[ "$SPRING_PROFILES_ACTIVE" == "prod" ]]; then
        echo -e "${RED}❌ ERROR: Development JWT secret detected in production environment${NC}"
        ((ERRORS++))
        return 1
    fi
    
    echo -e "${GREEN}✅ JWT_SECRET strength is adequate${NC}"
    return 0
}

# Function to validate database URL format
validate_database_url() {
    local db_url=$1
    
    if [[ ! "$db_url" =~ ^postgresql:// ]]; then
        echo -e "${RED}❌ ERROR: DATABASE_URL must start with 'postgresql://'${NC}"
        ((ERRORS++))
        return 1
    fi
    
    if [[ "$db_url" == *"localhost"* ]] && [[ "$SPRING_PROFILES_ACTIVE" == "prod" ]]; then
        echo -e "${RED}❌ ERROR: Localhost database URL detected in production environment${NC}"
        ((ERRORS++))
        return 1
    fi
    
    echo -e "${GREEN}✅ DATABASE_URL format is valid${NC}"
    return 0
}

# Function to validate Redis URL format
validate_redis_url() {
    local redis_url=$1
    
    if [[ ! "$redis_url" =~ ^redis:// ]]; then
        echo -e "${RED}❌ ERROR: REDIS_URL must start with 'redis://'${NC}"
        ((ERRORS++))
        return 1
    fi
    
    echo -e "${GREEN}✅ REDIS_URL format is valid${NC}"
    return 0
}

# Function to validate email configuration
validate_email_config() {
    local email_host=$1
    local email_port=$2
    local email_from=$3
    
    if [[ "$email_host" == "localhost" ]] && [[ "$SPRING_PROFILES_ACTIVE" == "prod" ]]; then
        echo -e "${RED}❌ ERROR: Localhost email host detected in production environment${NC}"
        ((ERRORS++))
        return 1
    fi
    
    if [[ ! "$email_port" =~ ^[0-9]+$ ]] || [ "$email_port" -lt 1 ] || [ "$email_port" -gt 65535 ]; then
        echo -e "${RED}❌ ERROR: EMAIL_PORT must be a valid port number (1-65535)${NC}"
        ((ERRORS++))
        return 1
    fi
    
    if [[ ! "$email_from" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
        echo -e "${RED}❌ ERROR: EMAIL_FROM_ADDRESS must be a valid email address${NC}"
        ((ERRORS++))
        return 1
    fi
    
    echo -e "${GREEN}✅ Email configuration is valid${NC}"
    return 0
}

echo "📋 Checking required environment variables..."

# Database Configuration
echo -e "\n🗄️  Database Configuration:"
check_required "DATABASE_URL"
check_required "DATABASE_USERNAME"
check_required "DATABASE_PASSWORD"

if [ ! -z "$DATABASE_URL" ]; then
    validate_database_url "$DATABASE_URL"
fi

# JWT Configuration
echo -e "\n🔐 JWT Configuration:"
check_required "JWT_SECRET"
check_optional "JWT_ACCESS_TOKEN_EXPIRATION" "900000"
check_optional "JWT_REFRESH_TOKEN_EXPIRATION" "604800000"

if [ ! -z "$JWT_SECRET" ]; then
    validate_jwt_secret "$JWT_SECRET"
fi

# Redis Configuration
echo -e "\n🔴 Redis Configuration:"
check_required "REDIS_URL"
check_optional "REDIS_PASSWORD" "(empty)"

if [ ! -z "$REDIS_URL" ]; then
    validate_redis_url "$REDIS_URL"
fi

# Email Configuration
echo -e "\n📧 Email Configuration:"
check_required "EMAIL_HOST"
check_required "EMAIL_PORT"
check_required "EMAIL_USERNAME"
check_required "EMAIL_PASSWORD"
check_required "EMAIL_FROM_ADDRESS"
check_optional "EMAIL_FROM_NAME" "TariffSheriff"

if [ ! -z "$EMAIL_HOST" ] && [ ! -z "$EMAIL_PORT" ] && [ ! -z "$EMAIL_FROM_ADDRESS" ]; then
    validate_email_config "$EMAIL_HOST" "$EMAIL_PORT" "$EMAIL_FROM_ADDRESS"
fi

# Application URLs
echo -e "\n🌐 Application URLs:"
check_required "APP_BASE_URL"
check_required "FRONTEND_URL"
check_required "CORS_ALLOWED_ORIGINS"

# Security Configuration
echo -e "\n🛡️  Security Configuration:"
check_optional "RATE_LIMIT_ENABLED" "true"
check_optional "ACCOUNT_LOCKOUT_ENABLED" "true"
check_optional "MAX_LOGIN_ATTEMPTS" "5"
check_optional "LOCKOUT_DURATION" "900000"

# Environment-specific validations
echo -e "\n🏷️  Environment Validation:"
PROFILE=${SPRING_PROFILES_ACTIVE:-"default"}
echo "Current profile: $PROFILE"

if [[ "$PROFILE" == "prod" ]]; then
    echo "🔍 Running production environment checks..."
    
    # Ensure no development values in production
    if [[ "$JWT_SECRET" == *"dev"* ]]; then
        echo -e "${RED}❌ ERROR: Development JWT secret detected in production${NC}"
        ((ERRORS++))
    fi
    
    if [[ "$DATABASE_URL" == *"localhost"* ]]; then
        echo -e "${RED}❌ ERROR: Localhost database URL in production${NC}"
        ((ERRORS++))
    fi
    
    if [[ "$REDIS_URL" == *"localhost"* ]]; then
        echo -e "${RED}❌ ERROR: Localhost Redis URL in production${NC}"
        ((ERRORS++))
    fi
    
    if [[ "$EMAIL_HOST" == "localhost" ]]; then
        echo -e "${RED}❌ ERROR: Localhost email host in production${NC}"
        ((ERRORS++))
    fi
fi

# Summary
echo -e "\n📊 Validation Summary:"
echo -e "Errors: ${RED}$ERRORS${NC}"
echo -e "Warnings: ${YELLOW}$WARNINGS${NC}"

if [ $ERRORS -eq 0 ]; then
    echo -e "\n${GREEN}🎉 Configuration validation passed!${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Please review the warnings above${NC}"
    fi
    exit 0
else
    echo -e "\n${RED}❌ Configuration validation failed!${NC}"
    echo -e "${RED}Please fix the errors above before proceeding${NC}"
    exit 1
fi