#!/bin/bash
# Run this script to add all GitHub secrets at once
# Usage: ./scripts/add-github-secrets.sh

REPO="SaaiAravindhRaja/TariffSheriff"

echo "Adding GitHub Secrets for $REPO..."
echo ""

# EC2 Connection Secrets
echo "📡 Adding EC2 connection secrets..."
gh secret set EC2_SSH_KEY --body "$(cat .ssh/singapore-nathan-lenovo.pem)" --repo $REPO
gh secret set EC2_HOST --body "13.251.43.134" --repo $REPO
gh secret set EC2_USER --body "ubuntu" --repo $REPO

# Application Secrets
echo "🔐 Adding application secrets..."
echo "⚠️  You need to provide these values:"
echo ""

# Database secrets
read -p "Enter DATABASE_URL: " DATABASE_URL
gh secret set DATABASE_URL --body "$DATABASE_URL" --repo $REPO

read -p "Enter DATABASE_USERNAME: " DATABASE_USERNAME
gh secret set DATABASE_USERNAME --body "$DATABASE_USERNAME" --repo $REPO

read -sp "Enter DATABASE_PASSWORD: " DATABASE_PASSWORD
echo ""
gh secret set DATABASE_PASSWORD --body "$DATABASE_PASSWORD" --repo $REPO

read -sp "Enter JWT_SECRET: " JWT_SECRET
echo ""
gh secret set JWT_SECRET --body "$JWT_SECRET" --repo $REPO

echo ""
echo "✅ All secrets added!"
echo ""
echo "Verify with: gh secret list --repo $REPO"
