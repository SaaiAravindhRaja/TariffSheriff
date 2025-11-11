#!/bin/bash
# Run this script to add all GitHub secrets at once
# Usage: ./scripts/add-github-secrets.sh

REPO="SaaiAravindhRaja/TariffSheriff"

echo "Adding GitHub Secrets for $REPO..."

# EC2_SSH_KEY
gh secret set EC2_SSH_KEY --body "$(cat .ssh/singapore-nathan-lenovo.pem)" --repo $REPO

# EC2_HOST
gh secret set EC2_HOST --body "13.251.43.134" --repo $REPO

# EC2_USER
gh secret set EC2_USER --body "ubuntu" --repo $REPO

echo "✅ All secrets added!"
echo ""
echo "Verify with: gh secret list --repo $REPO"
