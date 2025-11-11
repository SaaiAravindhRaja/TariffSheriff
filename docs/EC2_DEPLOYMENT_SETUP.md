# EC2 Deployment Setup Guide

This guide explains how to set up the EC2 instance and GitHub Secrets for automated deployments.

## Prerequisites

- EC2 instance running with Docker installed
- SSH access to the EC2 instance
- AWS CLI configured on EC2 instance

## 1. EC2 Instance Setup

### Install Docker (if not already installed)

```bash
# Connect to EC2
ssh -i your-key.pem ubuntu@your-ec2-ip

# Update system
sudo apt-get update

# Install Docker
sudo apt-get install -y docker.io docker-compose

# Add user to docker group
sudo usermod -aG docker $USER

# Install AWS CLI
sudo apt-get install -y awscli

# Logout and login again for group changes to take effect
exit
```

### Configure AWS CLI on EC2

```bash
# Connect again
ssh -i your-key.pem ubuntu@your-ec2-ip

# Configure AWS CLI (use IAM role or access keys)
aws configure
# Enter your AWS credentials
# Region: ap-southeast-1
```

### Set up environment variables (for backend)

```bash
# Create environment file
sudo mkdir -p /etc/tariffsheriff
sudo nano /etc/tariffsheriff/backend.env
```

Add these variables:
```env
DATABASE_URL=your-database-url
DATABASE_USERNAME=your-db-username
DATABASE_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
```

## 2. GitHub Secrets Setup

Go to your GitHub repository → Settings → Secrets and variables → Actions

Add these secrets:

### Required Secrets:

1. **EC2_SSH_KEY**
   - Your private SSH key to access EC2
   - Copy the entire content of your `.pem` file
   ```bash
   cat your-key.pem
   ```

2. **EC2_HOST**
   - Your EC2 public IP or DNS
   - Example: `ec2-13-250-123-456.ap-southeast-1.compute.amazonaws.com`
   - Or: `13.250.123.456`

3. **EC2_USER**
   - SSH username (usually `ubuntu` for Ubuntu, `ec2-user` for Amazon Linux)
   - Example: `ubuntu`

4. **AWS_ROLE_ARN** (if not already set)
   - Your GitHub Actions IAM role ARN
   - Example: `arn:aws:iam::229037374885:role/github-actions-role`

### Environment Variables on EC2:

The deployment script needs these environment variables on the EC2 instance:

```bash
# On EC2, add to ~/.bashrc or /etc/environment
export DATABASE_URL="your-database-url"
export DATABASE_USERNAME="your-db-username"
export DATABASE_PASSWORD="your-db-password"
export JWT_SECRET="your-jwt-secret"
```

Or use AWS Secrets Manager (recommended):

```bash
# Install jq for JSON parsing
sudo apt-get install -y jq

# Create a script to fetch secrets
cat > ~/fetch-secrets.sh << 'EOF'
#!/bin/bash
export DATABASE_URL=$(aws secretsmanager get-secret-value --secret-id tariffsheriff/database-url --region ap-southeast-1 --query SecretString --output text | jq -r .DATABASE_URL)
export DATABASE_USERNAME=$(aws secretsmanager get-secret-value --secret-id tariffsheriff/database-username --region ap-southeast-1 --query SecretString --output text | jq -r .DATABASE_USERNAME)
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value --secret-id tariffsheriff/database-password --region ap-southeast-1 --query SecretString --output text | jq -r .DATABASE_PASSWORD)
export JWT_SECRET=$(aws secretsmanager get-secret-value --secret-id tariffsheriff/jwt-secret --region ap-southeast-1 --query SecretString --output text | jq -r .JWT_SECRET)
EOF

chmod +x ~/fetch-secrets.sh
```

## 3. Security Group Configuration

Ensure your EC2 security group allows:

- **Port 22** (SSH) - From GitHub Actions IPs or your IP
- **Port 80** (HTTP) - From 0.0.0.0/0 (public access for frontend)
- **Port 8080** (Backend API) - From 0.0.0.0/0 or your load balancer
- **Port 443** (HTTPS) - If using SSL

## 4. Test Deployment

### Manual Test:

```bash
# On EC2, test pulling from ECR
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com

# Pull and run backend
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-backend:latest
docker run -d --name tariffsheriff-backend -p 8080:8080 \
  -e DATABASE_URL="$DATABASE_URL" \
  -e DATABASE_USERNAME="$DATABASE_USERNAME" \
  -e DATABASE_PASSWORD="$DATABASE_PASSWORD" \
  -e JWT_SECRET="$JWT_SECRET" \
  229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-backend:latest

# Pull and run frontend
docker pull 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-frontend:latest
docker run -d --name tariffsheriff-frontend -p 80:80 \
  229037374885.dkr.ecr.ap-southeast-1.amazonaws.com/tariffsheriff-frontend:latest

# Check status
docker ps
```

### Trigger GitHub Actions:

```bash
# Push to main branch
git push origin main

# Or manually trigger from GitHub Actions UI
```

## 5. Monitoring

### Check container logs:

```bash
# Backend logs
docker logs -f tariffsheriff-backend

# Frontend logs
docker logs -f tariffsheriff-frontend
```

### Check container status:

```bash
docker ps --filter "name=tariffsheriff"
```

### Restart containers:

```bash
docker restart tariffsheriff-backend
docker restart tariffsheriff-frontend
```

## 6. Troubleshooting

### SSH Connection Issues:

```bash
# Test SSH connection
ssh -i ~/.ssh/your-key.pem ubuntu@your-ec2-ip

# Check SSH key permissions
chmod 600 ~/.ssh/your-key.pem
```

### Docker Permission Issues:

```bash
# Add user to docker group
sudo usermod -aG docker $USER
# Logout and login again
```

### ECR Login Issues:

```bash
# Check AWS credentials
aws sts get-caller-identity

# Manually login to ECR
aws ecr get-login-password --region ap-southeast-1 | docker login --username AWS --password-stdin 229037374885.dkr.ecr.ap-southeast-1.amazonaws.com
```

### Container Not Starting:

```bash
# Check logs
docker logs tariffsheriff-backend
docker logs tariffsheriff-frontend

# Check if port is already in use
sudo netstat -tulpn | grep :8080
sudo netstat -tulpn | grep :80
```

## 7. Production Recommendations

1. **Use HTTPS**: Set up SSL/TLS with Let's Encrypt
2. **Use a reverse proxy**: Nginx or Traefik in front of containers
3. **Set up monitoring**: CloudWatch, Datadog, or Prometheus
4. **Enable auto-restart**: Containers already configured with `--restart unless-stopped`
5. **Regular backups**: Database and configuration files
6. **Use IAM roles**: Instead of access keys on EC2
7. **Implement health checks**: Already configured in deployment script

## Need Help?

Contact your DevOps team or check:
- GitHub Actions logs
- EC2 instance logs: `/var/log/cloud-init-output.log`
- Docker logs: `docker logs <container-name>`
