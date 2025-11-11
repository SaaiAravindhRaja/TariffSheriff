#!/usr/bin/env bash
set -euo pipefail
# Adds required GitHub secrets for the CD workflow.
# Usage:
#   REPO=owner/repo KEY_PATH=~/.ssh/your-key.pem EC2_HOST=1.2.3.4 EC2_USER=ubuntu ./scripts/add-github-secrets-oidc.sh
#
# Requires: GitHub CLI (gh) authenticated to the repo owner.

REPO="${REPO:-SaaiAravindhRaja/TariffSheriff}"
KEY_PATH="${KEY_PATH:-$HOME/.ssh/your-key.pem}"
EC2_HOST="${EC2_HOST:-}"
EC2_USER="${EC2_USER:-ubuntu}"

command -v gh >/dev/null 2>&1 || { echo "gh (GitHub CLI) is required. Install from https://cli.github.com/"; exit 1; }

echo "Adding GitHub Secrets for $REPO"
echo ""

read -p "Enter AWS_ROLE_ARN (OIDC role for GitHub Actions): " AWS_ROLE_ARN
gh secret set AWS_ROLE_ARN --body "$AWS_ROLE_ARN" --repo "$REPO"

echo "📡 Adding EC2 connection secrets..."
if [[ ! -f "$KEY_PATH" ]]; then
  echo "SSH key not found at $KEY_PATH (override with KEY_PATH=...)"; exit 1
fi
gh secret set EC2_SSH_KEY --body "$(cat "$KEY_PATH")" --repo "$REPO"
if [[ -z "$EC2_HOST" ]]; then
  read -p "Enter EC2_HOST (public IP/DNS): " EC2_HOST
fi
gh secret set EC2_HOST --body "$EC2_HOST" --repo "$REPO"
gh secret set EC2_USER --body "$EC2_USER" --repo "$REPO"

echo "🔐 Adding application secrets..."
read -p  "Enter DATABASE_URL: " DATABASE_URL
read -p  "Enter DATABASE_USERNAME: " DATABASE_USERNAME
read -sp "Enter DATABASE_PASSWORD: " DATABASE_PASSWORD; echo ""
read -sp "Enter JWT_SECRET: " JWT_SECRET; echo ""
gh secret set DATABASE_URL --body "$DATABASE_URL" --repo "$REPO"
gh secret set DATABASE_USERNAME --body "$DATABASE_USERNAME" --repo "$REPO"
gh secret set DATABASE_PASSWORD --body "$DATABASE_PASSWORD" --repo "$REPO"
gh secret set JWT_SECRET --body "$JWT_SECRET" --repo "$REPO"

echo "🔑 Adding Auth0 (frontend build) secrets..."
read -p  "Enter AUTH0_DOMAIN (e.g. dev-xxx.us.auth0.com): " AUTH0_DOMAIN
read -p  "Enter AUTH0_CLIENT_ID: " AUTH0_CLIENT_ID
read -p  "Enter AUTH0_AUDIENCE (e.g. https://api.tariffsheriff.com): " AUTH0_AUDIENCE
gh secret set AUTH0_DOMAIN --body "$AUTH0_DOMAIN" --repo "$REPO"
gh secret set AUTH0_CLIENT_ID --body "$AUTH0_CLIENT_ID" --repo "$REPO"
gh secret set AUTH0_AUDIENCE --body "$AUTH0_AUDIENCE" --repo "$REPO"

echo ""
echo "✅ All secrets added!"
echo "Verify with: gh secret list --repo $REPO"
echo ""

