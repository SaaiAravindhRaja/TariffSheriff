#!/bin/bash
set -e

echo "🚀 Setting up EC2 instance for TariffSheriff deployment..."
echo ""

# Update system
echo "📦 Updating system packages..."
sudo apt-get update -qq

# Install Docker if not installed
if ! command -v docker &> /dev/null; then
    echo "🐳 Installing Docker..."
    sudo apt-get install -y docker.io docker-compose
    sudo systemctl start docker
    sudo systemctl enable docker
    echo "✅ Docker installed"
else
    echo "✅ Docker already installed"
fi

# Add ubuntu user to docker group
echo "👤 Adding user to docker group..."
sudo usermod -aG docker ubuntu

# Install AWS CLI if not installed
if ! command -v aws &> /dev/null; then
    echo "☁️  Installing AWS CLI..."
    sudo apt-get install -y awscli
    echo "✅ AWS CLI installed"
else
    echo "✅ AWS CLI already installed"
fi

# Install jq for JSON parsing
if ! command -v jq &> /dev/null; then
    echo "📝 Installing jq..."
    sudo apt-get install -y jq
    echo "✅ jq installed"
else
    echo "✅ jq already installed"
fi

# Configure AWS CLI (using IAM role if available)
echo "🔑 Checking AWS credentials..."
if aws sts get-caller-identity &> /dev/null; then
    echo "✅ AWS credentials configured"
else
    echo "⚠️  AWS credentials not configured"
    echo "Please run: aws configure"
    echo "Or attach an IAM role to this EC2 instance"
fi

# Create directory for environment files
echo "📁 Creating configuration directories..."
sudo mkdir -p /etc/tariffsheriff
sudo chown ubuntu:ubuntu /etc/tariffsheriff

# Create environment file template (secrets will be provided by GitHub Actions)
cat > /etc/tariffsheriff/backend.env.template << 'EOF'
# Database Configuration
DATABASE_URL=<your-database-url>
DATABASE_USERNAME=<your-database-username>
DATABASE_PASSWORD=<your-database-password>

# JWT Configuration
JWT_SECRET=<your-jwt-secret>

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
EOF

echo "✅ Environment template created at /etc/tariffsheriff/backend.env.template"
echo "⚠️  Note: Actual secrets will be provided by GitHub Actions during deployment"

# Test ECR login
echo "🔐 Testing ECR login..."
if aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com 2>/dev/null; then
    echo "✅ ECR login successful"
else
    echo "⚠️  ECR login failed - check AWS credentials"
fi

# Pull latest images
echo "📥 Pulling latest Docker images..."
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-backend:latest || echo "⚠️  Backend image not found (will be built by CI/CD)"
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-frontend:latest || echo "⚠️  Frontend image not found (will be built by CI/CD)"

# Create deployment script (for manual deployments only)
cat > ~/deploy-tariffsheriff.sh << 'DEPLOY_SCRIPT'
#!/bin/bash
set -e

# Note: This script is for manual deployments only
# GitHub Actions will handle automated deployments with secrets

# Check if environment file exists
if [ ! -f /etc/tariffsheriff/backend.env ]; then
  echo "❌ Environment file not found!"
  echo "Please create /etc/tariffsheriff/backend.env with your secrets"
  echo "See /etc/tariffsheriff/backend.env.template for reference"
  exit 1
fi

# Load environment variables
source /etc/tariffsheriff/backend.env

# Login to ECR
echo "🔐 Logging into ECR..."
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com

# Deploy backend
echo "🔄 Deploying backend..."
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-backend:latest
docker stop tariffsheriff-backend 2>/dev/null || true
docker rm tariffsheriff-backend 2>/dev/null || true
docker run -d \
  --name tariffsheriff-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  --env-file /etc/tariffsheriff/backend.env \
  229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-backend:latest

echo "✅ Backend deployed"

# Deploy frontend
echo "🔄 Deploying frontend..."
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-frontend:latest
docker stop tariffsheriff-frontend 2>/dev/null || true
docker rm tariffsheriff-frontend 2>/dev/null || true
docker run -d \
  --name tariffsheriff-frontend \
  --restart unless-stopped \
  -p 80:80 \
  229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-frontend:latest

echo "✅ Frontend deployed"

# Clean up old images
echo "🧹 Cleaning up old images..."
docker image prune -af --filter "until=24h" || true

echo "✅ Deployment complete!"
docker ps --filter "name=tariffsheriff"
DEPLOY_SCRIPT

chmod +x ~/deploy-tariffsheriff.sh
echo "✅ Deployment script created at ~/deploy-tariffsheriff.sh"

echo ""
echo "=========================================="
echo "✅ EC2 Setup Complete!"
echo "=========================================="
echo ""
echo "📋 Summary:"
echo "  - Docker installed and configured"
echo "  - AWS CLI installed"
echo "  - Environment file: /etc/tariffsheriff/backend.env"
echo "  - Deployment script: ~/deploy-tariffsheriff.sh"
echo ""
echo "🔧 Next Steps:"
echo "  1. Logout and login again for docker group to take effect"
echo "  2. Test deployment: ~/deploy-tariffsheriff.sh"
echo "  3. Check containers: docker ps"
echo "  4. View logs: docker logs tariffsheriff-backend"
echo ""
echo "⚠️  IMPORTANT: You may need to logout and login again!"
echo "   Run: exit"
echo "   Then reconnect via SSH"
echo ""
