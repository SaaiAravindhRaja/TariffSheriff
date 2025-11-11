# Fix EC2 Deployment - Use AWS Systems Manager (SSM)

## Problem
GitHub Actions cannot SSH to your EC2 because the security group blocks GitHub's IPs.

## Solution
Use AWS Systems Manager (SSM) instead of SSH - more secure and works with GitHub Actions.

## Steps to Fix

### 1. Get Your EC2 Instance ID

```bash
# On your local machine or EC2
aws ec2 describe-instances \
  --region ap-southeast-1 \
  --filters "Name=ip-address,Values=13.251.43.134" \
  --query 'Reservations[*].Instances[*].InstanceId' \
  --output text
```

Or find it in AWS Console → EC2 → Instances (looks like `i-0123456789abcdef0`)

### 2. Attach IAM Role to EC2 (if not already attached)

Your EC2 needs an IAM role with SSM permissions.

**Option A: Via AWS Console**
1. Go to EC2 → Instances
2. Select your instance
3. Actions → Security → Modify IAM role
4. If no role exists, create one with these policies:
   - `AmazonSSMManagedInstanceCore` (AWS managed policy)
   - `AmazonEC2ContainerRegistryReadOnly` (for ECR access)
5. Attach the role

**Option B: Via CLI**
```bash
# Create IAM role
aws iam create-role \
  --role-name EC2-SSM-Role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ec2.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

# Attach policies
aws iam attach-role-policy \
  --role-name EC2-SSM-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore

aws iam attach-role-policy \
  --role-name EC2-SSM-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly

# Create instance profile
aws iam create-instance-profile --instance-profile-name EC2-SSM-Profile
aws iam add-role-to-instance-profile --instance-profile-name EC2-SSM-Profile --role-name EC2-SSM-Role

# Attach to EC2 (replace with your instance ID)
aws ec2 associate-iam-instance-profile \
  --instance-id i-YOUR-INSTANCE-ID \
  --iam-instance-profile Name=EC2-SSM-Profile \
  --region ap-southeast-1
```

### 3. Install SSM Agent on EC2 (if not installed)

SSH into your EC2 (from your local machine, not GitHub Actions):

```bash
ssh -i .ssh/singapore-nathan-lenovo.pem ubuntu@13.251.43.134

# Install SSM agent
sudo snap install amazon-ssm-agent --classic
sudo snap start amazon-ssm-agent

# Check status
sudo snap services amazon-ssm-agent
```

### 4. Verify SSM Connection

Wait 2-3 minutes, then check if instance appears in SSM:

```bash
aws ssm describe-instance-information \
  --region ap-southeast-1 \
  --query 'InstanceInformationList[*].[InstanceId,PingStatus]' \
  --output table
```

You should see your instance with `PingStatus: Online`

### 5. Add EC2_INSTANCE_ID to GitHub Secrets

```bash
# Get your instance ID
INSTANCE_ID=$(aws ec2 describe-instances \
  --region ap-southeast-1 \
  --filters "Name=ip-address,Values=13.251.43.134" \
  --query 'Reservations[*].Instances[*].InstanceId' \
  --output text)

# Add to GitHub Secrets
gh secret set EC2_INSTANCE_ID --body "$INSTANCE_ID" --repo SaaiAravindhRaja/TariffSheriff
```

Or manually:
1. Go to GitHub → Repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `EC2_INSTANCE_ID`
4. Value: Your instance ID (e.g., `i-0123456789abcdef0`)
5. Click "Add secret"

### 6. Test the Deployment

Push to main branch:

```bash
git push origin main
```

The deployment will now use SSM instead of SSH!

## Benefits of SSM over SSH

✅ No need to open port 22 to the internet
✅ Works with GitHub Actions IPs
✅ Better security (IAM-based authentication)
✅ Session logging and auditing
✅ No SSH key management

## Troubleshooting

### Instance not showing in SSM?
- Wait 2-3 minutes after attaching IAM role
- Check IAM role has `AmazonSSMManagedInstanceCore` policy
- Verify SSM agent is running: `sudo snap services amazon-ssm-agent`
- Check instance has internet access (for SSM endpoint)

### Deployment still failing?
- Verify `EC2_INSTANCE_ID` secret is set correctly
- Check GitHub Actions logs for specific error
- Test SSM manually: `aws ssm send-command --instance-ids i-YOUR-ID --document-name "AWS-RunShellScript" --parameters 'commands=["echo hello"]' --region ap-southeast-1`

## Alternative: Open SSH to GitHub IPs (Not Recommended)

If you really want to use SSH, you need to allow GitHub Actions IPs in your security group:

```bash
# Get GitHub Actions IP ranges
curl https://api.github.com/meta | jq -r '.actions[]' > github-ips.txt

# Add each IP to security group (tedious and changes frequently)
# NOT RECOMMENDED - use SSM instead!
```
