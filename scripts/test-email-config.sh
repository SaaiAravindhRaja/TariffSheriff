#!/bin/bash

# Email Configuration Test Script
# Tests email service configuration and connectivity

set -e

echo "📧 Testing Email Configuration..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if required variables are set
REQUIRED_VARS=("EMAIL_HOST" "EMAIL_PORT" "EMAIL_USERNAME" "EMAIL_PASSWORD" "EMAIL_FROM_ADDRESS")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${RED}❌ ERROR: Missing required environment variables:${NC}"
    for var in "${MISSING_VARS[@]}"; do
        echo -e "${RED}  - $var${NC}"
    done
    exit 1
fi

echo "📋 Email Configuration:"
echo "Host: $EMAIL_HOST"
echo "Port: $EMAIL_PORT"
echo "Username: $EMAIL_USERNAME"
echo "Password: [REDACTED]"
echo "From Address: $EMAIL_FROM_ADDRESS"
if [ ! -z "$EMAIL_FROM_NAME" ]; then
    echo "From Name: $EMAIL_FROM_NAME"
fi

# Validate email address format
validate_email() {
    local email=$1
    if [[ ! "$email" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
        echo -e "${RED}❌ ERROR: Invalid email address format: $email${NC}"
        return 1
    fi
    return 0
}

# Validate port number
validate_port() {
    local port=$1
    if [[ ! "$port" =~ ^[0-9]+$ ]] || [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
        echo -e "${RED}❌ ERROR: Invalid port number: $port${NC}"
        return 1
    fi
    return 0
}

echo -e "\n🔍 Validating configuration..."

# Validate email address
if validate_email "$EMAIL_FROM_ADDRESS"; then
    echo -e "${GREEN}✅ From email address format is valid${NC}"
fi

# Validate port
if validate_port "$EMAIL_PORT"; then
    echo -e "${GREEN}✅ Port number is valid${NC}"
fi

# Check for common email providers and their expected configurations
echo -e "\n🏢 Email Provider Detection:"
case "$EMAIL_HOST" in
    *gmail.com*)
        echo "Detected: Gmail"
        if [ "$EMAIL_PORT" != "587" ] && [ "$EMAIL_PORT" != "465" ]; then
            echo -e "${YELLOW}⚠️  WARNING: Gmail typically uses port 587 (STARTTLS) or 465 (SSL)${NC}"
        fi
        ;;
    *sendgrid.net*)
        echo "Detected: SendGrid"
        if [ "$EMAIL_PORT" != "587" ] && [ "$EMAIL_PORT" != "465" ] && [ "$EMAIL_PORT" != "25" ]; then
            echo -e "${YELLOW}⚠️  WARNING: SendGrid typically uses port 587, 465, or 25${NC}"
        fi
        if [ "$EMAIL_USERNAME" != "apikey" ]; then
            echo -e "${YELLOW}⚠️  WARNING: SendGrid username should be 'apikey'${NC}"
        fi
        ;;
    *mailgun.org*)
        echo "Detected: Mailgun"
        if [ "$EMAIL_PORT" != "587" ] && [ "$EMAIL_PORT" != "465" ]; then
            echo -e "${YELLOW}⚠️  WARNING: Mailgun typically uses port 587 or 465${NC}"
        fi
        ;;
    *amazonaws.com*)
        echo "Detected: Amazon SES"
        if [ "$EMAIL_PORT" != "587" ] && [ "$EMAIL_PORT" != "465" ] && [ "$EMAIL_PORT" != "25" ]; then
            echo -e "${YELLOW}⚠️  WARNING: Amazon SES typically uses port 587, 465, or 25${NC}"
        fi
        ;;
    localhost)
        echo "Detected: Local SMTP (Development)"
        if [ "$EMAIL_PORT" != "1025" ] && [ "$EMAIL_PORT" != "25" ]; then
            echo -e "${YELLOW}⚠️  WARNING: Local SMTP typically uses port 1025 (MailHog) or 25${NC}"
        fi
        ;;
    *)
        echo "Detected: Custom SMTP Server"
        ;;
esac

# Test SMTP connectivity
echo -e "\n🔍 Testing SMTP connectivity..."
if command -v nc &> /dev/null; then
    if nc -z "$EMAIL_HOST" "$EMAIL_PORT" 2>/dev/null; then
        echo -e "${GREEN}✅ SMTP server is reachable on port $EMAIL_PORT${NC}"
    else
        echo -e "${RED}❌ Cannot connect to SMTP server $EMAIL_HOST:$EMAIL_PORT${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  netcat not available, skipping connectivity test${NC}"
fi

# Test SMTP authentication (if possible)
if command -v openssl &> /dev/null && [ "$EMAIL_HOST" != "localhost" ]; then
    echo -e "\n🔐 Testing SMTP authentication..."
    
    # Create a temporary expect script for SMTP testing
    TEMP_SCRIPT=$(mktemp)
    cat > "$TEMP_SCRIPT" << EOF
#!/usr/bin/expect -f
set timeout 10
spawn openssl s_client -connect $EMAIL_HOST:$EMAIL_PORT -starttls smtp -quiet
expect "250" { send "AUTH LOGIN\r" }
expect "334" { 
    send "[exec echo -n $EMAIL_USERNAME | base64]\r"
    expect "334" {
        send "[exec echo -n $EMAIL_PASSWORD | base64]\r"
        expect {
            "235" { 
                puts "Authentication successful"
                send "QUIT\r"
                exit 0
            }
            "535" {
                puts "Authentication failed"
                send "QUIT\r"
                exit 1
            }
            timeout {
                puts "Authentication timeout"
                exit 1
            }
        }
    }
}
expect eof
EOF
    
    if command -v expect &> /dev/null; then
        chmod +x "$TEMP_SCRIPT"
        if "$TEMP_SCRIPT" 2>/dev/null | grep -q "Authentication successful"; then
            echo -e "${GREEN}✅ SMTP authentication successful${NC}"
        else
            echo -e "${YELLOW}⚠️  SMTP authentication test inconclusive${NC}"
        fi
        rm -f "$TEMP_SCRIPT"
    else
        echo -e "${YELLOW}⚠️  expect not available, skipping authentication test${NC}"
        rm -f "$TEMP_SCRIPT"
    fi
else
    echo -e "${YELLOW}⚠️  openssl not available or localhost detected, skipping authentication test${NC}"
fi

# Test email template validation
echo -e "\n📝 Email Template Validation:"

# Check if Spring Boot is available to test email templates
if [ -f "apps/backend/pom.xml" ]; then
    echo "Spring Boot project detected"
    
    # Check if email templates exist (if using template engine)
    if [ -d "apps/backend/src/main/resources/templates/email" ]; then
        TEMPLATE_COUNT=$(find apps/backend/src/main/resources/templates/email -name "*.html" | wc -l)
        echo "Email templates found: $TEMPLATE_COUNT"
    else
        echo "No email template directory found (using inline templates)"
    fi
    
    # Check email service configuration in application properties
    if grep -q "spring.mail" apps/backend/src/main/resources/application*.properties; then
        echo -e "${GREEN}✅ Spring Mail configuration found in application properties${NC}"
    else
        echo -e "${YELLOW}⚠️  No Spring Mail configuration found in application properties${NC}"
    fi
fi

# Environment-specific validations
echo -e "\n🏷️  Environment-Specific Validation:"
PROFILE=${SPRING_PROFILES_ACTIVE:-"default"}
echo "Current profile: $PROFILE"

if [[ "$PROFILE" == "prod" ]]; then
    echo "🔍 Running production environment checks..."
    
    if [[ "$EMAIL_HOST" == "localhost" ]]; then
        echo -e "${RED}❌ ERROR: Localhost email host in production environment${NC}"
        exit 1
    fi
    
    if [[ "$EMAIL_PORT" == "1025" ]]; then
        echo -e "${RED}❌ ERROR: Development email port (1025) in production environment${NC}"
        exit 1
    fi
    
    # Check for secure email configuration
    if [[ "$EMAIL_HOST" != *"ssl"* ]] && [[ "$EMAIL_HOST" != *"tls"* ]] && [ "$EMAIL_PORT" != "465" ] && [ "$EMAIL_PORT" != "587" ]; then
        echo -e "${YELLOW}⚠️  WARNING: Email configuration may not be using secure connection${NC}"
    fi
fi

# Rate limiting and quota information
echo -e "\n📊 Email Service Information:"
case "$EMAIL_HOST" in
    *sendgrid.net*)
        echo "SendGrid free tier: 100 emails/day"
        echo "SendGrid paid tiers: Various limits based on plan"
        ;;
    *mailgun.org*)
        echo "Mailgun free tier: 5,000 emails/month for 3 months"
        echo "Mailgun paid tiers: Various limits based on plan"
        ;;
    *amazonaws.com*)
        echo "Amazon SES: 200 emails/day (free tier), then pay-per-use"
        ;;
    *gmail.com*)
        echo "Gmail: 500 emails/day limit"
        ;;
    localhost)
        echo "Local SMTP: No limits (development only)"
        ;;
esac

# Security recommendations
echo -e "\n🛡️  Security Recommendations:"
echo "1. Use environment variables for email credentials"
echo "2. Enable STARTTLS or SSL/TLS encryption"
echo "3. Use application-specific passwords for Gmail"
echo "4. Regularly rotate email service API keys"
echo "5. Monitor email sending quotas and limits"
echo "6. Implement email rate limiting in application"

echo -e "\n${GREEN}🎉 Email configuration test completed!${NC}"

# Optional: Send test email
if [ "$1" = "--send-test" ]; then
    echo -e "\n📤 Sending test email..."
    
    if [ -z "$2" ]; then
        echo -e "${RED}❌ ERROR: Test email recipient not specified${NC}"
        echo "Usage: $0 --send-test recipient@example.com"
        exit 1
    fi
    
    TEST_RECIPIENT="$2"
    
    if ! validate_email "$TEST_RECIPIENT"; then
        exit 1
    fi
    
    # This would require the Spring Boot application to be running
    # or a separate email sending utility
    echo "Test email functionality requires the application to be running"
    echo "You can test email sending through the application's health check endpoint"
    echo "or by triggering a registration/password reset flow"
fi