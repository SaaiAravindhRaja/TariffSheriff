#!/bin/bash
# TariffSheriff AWS Infrastructure Setup
# Sets up complete AWS infrastructure for CD pipeline

set -e

AWS_REGION="ap-southeast-1"

echo "=========================================="
echo "TariffSheriff AWS Infrastructure Setup"
echo "=========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."
if ! command -v aws &> /dev/null; then
    echo "ERROR: AWS CLI not installed"
    exit 1
fi

if ! aws sts get-caller-identity &> /dev/null; then
    echo "ERROR: AWS credentials not configured"
    exit 1
fi

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "AWS Account: ${AWS_ACCOUNT_ID}"
echo "Region: ${AWS_REGION}"
echo ""

# Get GitHub info
read -p "GitHub organization/username: " GITHUB_ORG
read -p "GitHub repository name: " GITHUB_REPO
echo ""

# Confirm
echo "This will create:"
echo "  - IAM OIDC Provider"
echo "  - IAM Roles (github-actions-role, ecsTaskExecutionRole, ecsTaskRole)"
echo "  - ECR Repositories (backend, frontend)"
echo "  - ECS Cluster"
echo "  - Secrets Manager secrets"
echo "  - Task Definitions"
echo "  - CloudWatch Log Groups"
echo ""
read -p "Continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "Cancelled"
    exit 0
fi

echo ""
echo "Starting setup..."
echo ""

# 1. OIDC Provider
echo "[1/8] Creating IAM OIDC Provider..."
if aws iam get-open-id-connect-provider \
    --open-id-connect-provider-arn "arn:aws:iam::${AWS_ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com" \
    &> /dev/null; then
    echo "  Already exists"
else
    aws iam create-open-id-connect-provider \
        --url https://token.actions.githubusercontent.com \
        --client-id-list sts.amazonaws.com \
        --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 \
        &> /dev/null
    echo "  Created"
fi

# 2. GitHub Actions Role
echo "[2/8] Creating GitHub Actions IAM Role..."
cat > /tmp/github-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::${AWS_ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {"token.actions.githubusercontent.com:aud": "sts.amazonaws.com"},
      "StringLike": {"token.actions.githubusercontent.com:sub": "repo:${GITHUB_ORG}/${GITHUB_REPO}:*"}
    }
  }]
}
EOF

if aws iam get-role --role-name github-actions-role &> /dev/null; then
    aws iam update-assume-role-policy \
        --role-name github-actions-role \
        --policy-document file:///tmp/github-trust-policy.json
    echo "  Updated"
else
    aws iam create-role \
        --role-name github-actions-role \
        --assume-role-policy-document file:///tmp/github-trust-policy.json \
        &> /dev/null
    echo "  Created"
fi

cat > /tmp/github-permissions.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["ecr:*", "ecs:*", "iam:PassRole", "logs:*"],
    "Resource": "*"
  }]
}
EOF

aws iam put-role-policy \
    --role-name github-actions-role \
    --policy-name GitHubActionsPolicy \
    --policy-document file:///tmp/github-permissions.json

# 3. ECS Roles
echo "[3/8] Creating ECS Task Execution Role..."
cat > /tmp/ecs-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "ecs-tasks.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF

if ! aws iam get-role --role-name ecsTaskExecutionRole &> /dev/null; then
    aws iam create-role \
        --role-name ecsTaskExecutionRole \
        --assume-role-policy-document file:///tmp/ecs-trust-policy.json \
        &> /dev/null
    
    aws iam attach-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
    
    cat > /tmp/secrets-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["secretsmanager:GetSecretValue"],
    "Resource": ["arn:aws:secretsmanager:${AWS_REGION}:${AWS_ACCOUNT_ID}:secret:tariffsheriff/*"]
  }]
}
EOF
    
    aws iam put-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-name SecretsManagerAccess \
        --policy-document file:///tmp/secrets-policy.json
    
    echo "  Created"
else
    echo "  Already exists"
fi

if ! aws iam get-role --role-name ecsTaskRole &> /dev/null; then
    aws iam create-role \
        --role-name ecsTaskRole \
        --assume-role-policy-document file:///tmp/ecs-trust-policy.json \
        &> /dev/null
    echo "  ecsTaskRole created"
else
    echo "  ecsTaskRole already exists"
fi

# 4. ECR Repositories
echo "[4/8] Creating ECR Repositories..."
for repo in tariffsheriff-backend tariffsheriff-frontend; do
    if aws ecr describe-repositories --repository-names $repo --region $AWS_REGION &> /dev/null; then
        echo "  $repo already exists"
    else
        aws ecr create-repository \
            --repository-name $repo \
            --region $AWS_REGION \
            &> /dev/null
        echo "  $repo created"
    fi
done

# 5. ECS Cluster
echo "[5/8] Creating ECS Cluster..."
if aws ecs describe-clusters --clusters tariffsheriff-cluster --region $AWS_REGION --query 'clusters[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
    echo "  Already exists"
else
    aws ecs create-cluster \
        --cluster-name tariffsheriff-cluster \
        --region $AWS_REGION \
        &> /dev/null
    echo "  Created"
fi

# 6. Secrets Manager
echo "[6/8] Creating Secrets Manager secrets..."
create_secret() {
    local name=$1
    local key=$2
    local value=$3
    
    if aws secretsmanager describe-secret --secret-id $name --region $AWS_REGION &> /dev/null; then
        echo "  $name already exists"
    else
        aws secretsmanager create-secret \
            --name $name \
            --secret-string "{\"$key\":\"$value\"}" \
            --region $AWS_REGION \
            &> /dev/null
        echo "  $name created"
    fi
}

create_secret "tariffsheriff/database-url" "DATABASE_URL" "jdbc:postgresql://ep-broad-bar-a14utknk-pooler.ap-southeast-1.aws.neon.tech:5432/tariffsheriff?sslmode=require"
create_secret "tariffsheriff/database-username" "DATABASE_USERNAME" "app_dev"
create_secret "tariffsheriff/database-password" "DATABASE_PASSWORD" "2vKk?uw3mhdF"
create_secret "tariffsheriff/jwt-secret" "JWT_SECRET" "ff12189b52cbc92e16daaba6c45a7abda817faadf8c10b32dc1d73eebf91f8d9ef2eb143514e627a1f427beddf7c1ab3f398652e70e4abb37245879da0f02c8b"

# 7. Task Definitions
echo "[7/8] Registering ECS Task Definitions..."
cd aws-task-definitions

# Update account ID
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/229037374885/${AWS_ACCOUNT_ID}/g" backend-task-definition.json
    sed -i '' "s/229037374885/${AWS_ACCOUNT_ID}/g" frontend-task-definition.json
else
    sed -i "s/229037374885/${AWS_ACCOUNT_ID}/g" backend-task-definition.json
    sed -i "s/229037374885/${AWS_ACCOUNT_ID}/g" frontend-task-definition.json
fi

aws ecs register-task-definition \
    --cli-input-json file://backend-task-definition.json \
    --region $AWS_REGION \
    &> /dev/null
echo "  Backend task definition registered"

aws ecs register-task-definition \
    --cli-input-json file://frontend-task-definition.json \
    --region $AWS_REGION \
    &> /dev/null
echo "  Frontend task definition registered"

cd ..

# 8. CloudWatch Log Groups
echo "[8/8] Creating CloudWatch Log Groups..."
for log_group in "/ecs/tariffsheriff-backend-task" "/ecs/tariffsheriff-frontend-task"; do
    if aws logs describe-log-groups --log-group-name-prefix "$log_group" --region $AWS_REGION --query 'logGroups[0]' 2>/dev/null | grep -q "logGroupName"; then
        echo "  $log_group already exists"
    else
        aws logs create-log-group \
            --log-group-name "$log_group" \
            --region $AWS_REGION
        echo "  $log_group created"
    fi
done

# Cleanup
rm -f /tmp/github-trust-policy.json /tmp/github-permissions.json /tmp/ecs-trust-policy.json /tmp/secrets-policy.json

echo ""
echo "=========================================="
echo "Setup Complete"
echo "=========================================="
echo ""
echo "Next Steps:"
echo ""
echo "1. Create ECS Services (run these commands):"
echo ""
echo "export VPC_ID=\$(aws ec2 describe-vpcs --filters \"Name=isDefault,Values=true\" --query 'Vpcs[0].VpcId' --output text --region ${AWS_REGION})"
echo "export SUBNET_1=\$(aws ec2 describe-subnets --filters \"Name=vpc-id,Values=\$VPC_ID\" --query 'Subnets[0].SubnetId' --output text --region ${AWS_REGION})"
echo "export SUBNET_2=\$(aws ec2 describe-subnets --filters \"Name=vpc-id,Values=\$VPC_ID\" --query 'Subnets[1].SubnetId' --output text --region ${AWS_REGION})"
echo ""
echo "export SG_ID=\$(aws ec2 create-security-group --group-name tariffsheriff-sg --description \"TariffSheriff\" --vpc-id \$VPC_ID --query 'GroupId' --output text --region ${AWS_REGION})"
echo ""
echo "aws ec2 authorize-security-group-ingress --group-id \$SG_ID --protocol tcp --port 8080 --cidr 0.0.0.0/0 --region ${AWS_REGION}"
echo "aws ec2 authorize-security-group-ingress --group-id \$SG_ID --protocol tcp --port 80 --cidr 0.0.0.0/0 --region ${AWS_REGION}"
echo ""
echo "aws ecs create-service --cluster tariffsheriff-cluster --service-name tariffsheriff-backend-service --task-definition tariffsheriff-backend-task --desired-count 1 --launch-type FARGATE --network-configuration \"awsvpcConfiguration={subnets=[\$SUBNET_1,\$SUBNET_2],securityGroups=[\$SG_ID],assignPublicIp=ENABLED}\" --region ${AWS_REGION}"
echo ""
echo "aws ecs create-service --cluster tariffsheriff-cluster --service-name tariffsheriff-frontend-service --task-definition tariffsheriff-frontend-task --desired-count 1 --launch-type FARGATE --network-configuration \"awsvpcConfiguration={subnets=[\$SUBNET_1,\$SUBNET_2],securityGroups=[\$SG_ID],assignPublicIp=ENABLED}\" --region ${AWS_REGION}"
echo ""
echo "2. Verify GitHub Secrets are set:"
echo "   AWS_ROLE_ARN=arn:aws:iam::${AWS_ACCOUNT_ID}:role/github-actions-role"
echo ""
echo "3. Push to main branch to trigger deployment"
echo ""
