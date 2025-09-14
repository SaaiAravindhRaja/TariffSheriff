# 🚀 Deployment Guide

This document explains how to set up and use the automated deployment system for TariffSheriff.

## 📋 Prerequisites

### Vercel Setup
1. Create a Vercel account at [vercel.com](https://vercel.com)
2. Install Vercel CLI: `npm i -g vercel`
3. Link your project: `vercel link` (run in `apps/frontend/`)
4. Get your project details: `vercel project ls`

### Required GitHub Secrets

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

| Secret Name | Description | How to get it |
|-------------|-------------|---------------|
| `VERCEL_TOKEN` | Vercel authentication token | [Vercel Account Settings](https://vercel.com/account/tokens) |
| `VERCEL_ORG_ID` | Your Vercel organization ID | Run `vercel org ls` or check `.vercel/project.json` |
| `VERCEL_PROJECT_ID` | Your Vercel project ID | Run `vercel project ls` or check `.vercel/project.json` |

## 🔄 Workflows

### 1. Continuous Integration (`ci.yml`)
**Triggers:** Push to main/develop branches, PRs
**Purpose:** Run tests, linting, type checking, and security audits

**What it does:**
- ✅ Type checking with TypeScript
- 🧹 Code linting with ESLint  
- 🧪 Run test suite with coverage
- 🏗️ Build application
- 🔒 Security audit
- 📊 Build size analysis

### 2. Vercel Deployment (`deploy-vercel.yml`)
**Triggers:** 
- Push to `main` or `SaaiAravindhRaja-patch-1` branches
- Pull requests
- Manual dispatch

**What it does:**
- 🏗️ Build the frontend application
- 🚀 Deploy to Vercel (production for main, preview for others)
- 💬 Comment on PRs with preview URLs
- ⚡ Smart caching for faster builds

### 3. Dependency Updates (`dependency-update.yml`)
**Triggers:** Weekly schedule (Mondays 9 AM UTC) or manual
**Purpose:** Keep dependencies up to date automatically

**What it does:**
- 📦 Update npm dependencies
- 🔒 Apply security fixes
- 🧪 Run tests to ensure compatibility
- 📝 Create PR with changes

## 🎯 Deployment Environments

### Production
- **Branch:** `main`
- **URL:** Your custom domain or `your-project.vercel.app`
- **Trigger:** Push to main branch
- **Features:** Full production optimizations

### Preview
- **Branches:** All other branches and PRs
- **URL:** Unique preview URL for each deployment
- **Trigger:** Push to any branch or PR creation/update
- **Features:** Full functionality for testing

## 🛠️ Manual Deployment

### Via GitHub Actions
1. Go to Actions tab in your repository
2. Select "Deploy Frontend to Vercel"
3. Click "Run workflow"
4. Choose environment (preview/production)

### Via Vercel CLI
```bash
# Preview deployment
cd apps/frontend
vercel

# Production deployment
cd apps/frontend
vercel --prod
```

## 🔧 Configuration Files

### `apps/frontend/vercel.json`
- Build and output configuration
- Routing rules and rewrites
- Security headers
- Caching policies

### `.github/workflows/`
- `ci.yml` - Continuous integration
- `deploy-vercel.yml` - Deployment automation
- `dependency-update.yml` - Automated updates

## 🐛 Troubleshooting

### Build Failures
1. Check the Actions tab for detailed logs
2. Ensure all dependencies are properly listed in `package.json`
3. Verify TypeScript compilation passes locally
4. Check for linting errors

### Deployment Issues
1. Verify all Vercel secrets are correctly set
2. Check Vercel dashboard for deployment logs
3. Ensure `vercel.json` configuration is valid
4. Test deployment locally with Vercel CLI

### Missing Preview URLs
1. Ensure `VERCEL_TOKEN` secret is set
2. Check if the PR is from a fork (secrets aren't available)
3. Verify the workflow has proper permissions

## 📈 Monitoring

### Build Performance
- Build times are logged in each workflow run
- Bundle size analysis helps track application growth
- Coverage reports show test effectiveness

### Security
- Weekly dependency audits
- Automated security fix applications
- Security headers configured in Vercel

## 🎉 Best Practices

1. **Always test locally** before pushing
2. **Review preview deployments** before merging PRs
3. **Monitor build times** and optimize if needed
4. **Keep dependencies updated** via automated PRs
5. **Use semantic commit messages** for better tracking

---

Need help? Check the [GitHub Actions documentation](https://docs.github.com/en/actions) or [Vercel deployment docs](https://vercel.com/docs/deployments/overview).