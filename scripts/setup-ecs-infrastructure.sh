#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

AWS_REGION="ap-southeast-1"
CLUSTER_NAME="tariffsheriff-cluster"

echo -e "${YELLOW}🚀 Setting up ECS Infrastructure for TariffSheriff${NC}"
echo ""

# 1. Create ECS Cluster
echo -e "${YELLOW}📦 Creating ECS Cluster...${NC}"
if aws ecs describe-clusters --clusters $CLUSTER_NAME --region $AWS_REGION --query 'clusters[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
    echo -e "${GREEN}✅ Cluster already exists${NC}"
else
    aws ecs create-cluster \
        --cluster-name $CLUSTER_NAME \
        --region $AWS_REGION \
        --capacity-providers FARGATE FARGATE_SPOT \
        --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
    echo -e "${GREEN}✅ Cluster created${NC}"
fi
echo ""

# 2. Register Task Definitions
echo -e "${YELLOW}📝 Registering Task Definitions...${NC}"

echo "  - Backend task definition..."
aws ecs register-task-definition \
    --cli-input-json file://aws-task-definitions/backend-task-definition.json \
    --region $AWS_REGION > /dev/null
echo -e "${GREEN}  ✅ Backend task registered${NC}"

echo "  - Frontend task definition..."
aws ecs register-task-definition \
    --cli-input-json file://aws-task-definitions/frontend-task-definition.json \
    --region $AWS_REGION > /dev/null
echo -e "${GREEN}  ✅ Frontend task registered${NC}"
echo ""

# 3. Get VPC and Subnet information
echo -e "${YELLOW}🔍 Getting VPC and Subnet information...${NC}"
VPC_ID=$(aws ec2 describe-vpcs --region $AWS_REGION --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
SUBNETS=$(aws ec2 describe-subnets --region $AWS_REGION --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')
echo -e "${GREEN}✅ VPC: $VPC_ID${NC}"
echo -e "${GREEN}✅ Subnets: $SUBNETS${NC}"
echo ""

# 4. Create Security Group
echo -e "${YELLOW}🔒 Creating Security Group...${NC}"
SG_NAME="tariffsheriff-ecs-sg"
SG_ID=$(aws ec2 describe-security-groups --region $AWS_REGION --filters "Name=group-name,Values=$SG_NAME" --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null)

if [ "$SG_ID" == "None" ] || [ -z "$SG_ID" ]; then
    SG_ID=$(aws ec2 create-security-group \
        --group-name $SG_NAME \
        --description "Security group for TariffSheriff ECS tasks" \
        --vpc-id $VPC_ID \
        --region $AWS_REGION \
        --query 'GroupId' \
        --output text)
    
    # Allow inbound traffic
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 8080 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    aws ec2 authorize-security-group-ingress \
        --group-id $SG_ID \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region $AWS_REGION
    
    echo -e "${GREEN}✅ Security group created: $SG_ID${NC}"
else
    echo -e "${GREEN}✅ Security group already exists: $SG_ID${NC}"
fi
echo ""

# 5. Create ECS Services
echo -e "${YELLOW}🚀 Creating ECS Services...${NC}"

# Backend Service
echo "  - Creating backend service..."
if aws ecs describe-services --cluster $CLUSTER_NAME --services tariffsheriff-backend-service --region $AWS_REGION --query 'services[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
    echo -e "${GREEN}  ✅ Backend service already exists${NC}"
else
    aws ecs create-service \
        --cluster $CLUSTER_NAME \
        --service-name tariffsheriff-backend-service \
        --task-definition tariffsheriff-backend-task \
        --desired-count 1 \
        --launch-type FARGATE \
        --network-configuration "awsvpcConfiguration={subnets=[$SUBNETS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
        --region $AWS_REGION > /dev/null
    echo -e "${GREEN}  ✅ Backend service created${NC}"
fi

# Frontend Service
echo "  - Creating frontend service..."
if aws ecs describe-services --cluster $CLUSTER_NAME --services tariffsheriff-frontend-service --region $AWS_REGION --query 'services[0].status' --output text 2>/dev/null | grep -q "ACTIVE"; then
    echo -e "${GREEN}  ✅ Frontend service already exists${NC}"
else
    aws ecs create-service \
        --cluster $CLUSTER_NAME \
        --service-name tariffsheriff-frontend-service \
        --task-definition tariffsheriff-frontend-task \
        --desired-count 1 \
        --launch-type FARGATE \
        --network-configuration "awsvpcConfiguration={subnets=[$SUBNETS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
        --region $AWS_REGION > /dev/null
    echo -e "${GREEN}  ✅ Frontend service created${NC}"
fi
echo ""

echo -e "${GREEN}🎉 ECS Infrastructure setup complete!${NC}"
echo ""
echo -e "${YELLOW}📋 Summary:${NC}"
echo "  Cluster: $CLUSTER_NAME"
echo "  Region: $AWS_REGION"
echo "  VPC: $VPC_ID"
echo "  Security Group: $SG_ID"
echo ""
echo -e "${YELLOW}🔗 Next steps:${NC}"
echo "  1. Your CD pipeline will now work!"
echo "  2. Push to main branch to trigger deployment"
echo "  3. Check service status:"
echo "     aws ecs describe-services --cluster $CLUSTER_NAME --services tariffsheriff-backend-service tariffsheriff-frontend-service --region $AWS_REGION"
echo ""
