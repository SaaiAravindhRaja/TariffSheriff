#!/bin/bash

# TariffSheriff Deployment Helper Script
# Usage: ./scripts/deploy.sh [preview|production]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default to preview if no argument provided
ENVIRONMENT=${1:-preview}

echo -e "${BLUE}🚀 TariffSheriff Deployment Script${NC}"
echo -e "${BLUE}Environment: ${ENVIRONMENT}${NC}"
echo ""

# Check if we're in the right directory
if [ ! -f "apps/frontend/package.json" ]; then
    echo -e "${RED}❌ Error: Please run this script from the project root${NC}"
    exit 1
fi

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    echo -e "${YELLOW}⚠️  Vercel CLI not found. Installing...${NC}"
    npm install -g vercel@latest
fi

# Navigate to frontend directory
cd apps/frontend

echo -e "${BLUE}📦 Installing dependencies...${NC}"
npm ci

echo -e "${BLUE}🧪 Running tests...${NC}"
npm run test -- --run

echo -e "${BLUE}🧹 Running linter...${NC}"
npm run lint

echo -e "${BLUE}🏗️  Building application...${NC}"
npm run build

echo -e "${BLUE}📊 Build analysis:${NC}"
echo "Build size: $(du -sh dist/)"
echo "Files created: $(find dist/ -type f | wc -l)"

echo ""
echo -e "${BLUE}🚀 Deploying to Vercel...${NC}"

if [ "$ENVIRONMENT" = "production" ]; then
    echo -e "${YELLOW}⚠️  Deploying to PRODUCTION${NC}"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        vercel --prod --confirm
    else
        echo -e "${RED}❌ Deployment cancelled${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Deploying to PREVIEW${NC}"
    vercel --confirm
fi

echo ""
echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo -e "${BLUE}📱 Check your Vercel dashboard for the deployment URL${NC}"