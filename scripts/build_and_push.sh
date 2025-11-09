#!/usr/bin/env bash
set -euo pipefail

# Builds and pushes backend and/or frontend images to ECR.
# Configuration via env vars (export before running):
#   AWS_REGION                e.g., us-east-1
#   BACKEND_REPO              e.g., tariffsheriff-backend
#   FRONTEND_REPO             e.g., tariffsheriff-frontend
#   BUILD_BACKEND             default: 1 (build); set to 0 to skip
#   BUILD_FRONTEND            default: 1 (build); set to 0 to skip
#   IMAGE_TAG                 default: latest
#
# Frontend Vite build-time vars (optional; only used if set):
#   VITE_API_BASE_URL
#   VITE_AUTH0_DOMAIN
#   VITE_AUTH0_CLIENT_ID
#   VITE_AUTH0_REDIRECT_URI
#   VITE_AUTH0_AUDIENCE
#   VITE_ENABLE_ANALYTICS
#   VITE_ENABLE_ERROR_REPORTING
#
# Example:
#   export AWS_REGION=us-east-1
#   export BACKEND_REPO=tariffsheriff-backend
#   export FRONTEND_REPO=tariffsheriff-frontend
#   export VITE_API_BASE_URL=https://example.com/api
#   export VITE_AUTH0_DOMAIN=your-domain.auth0.com
#   export VITE_AUTH0_CLIENT_ID=abc123
#   export VITE_AUTH0_REDIRECT_URI=https://example.com
#   ./scripts/build_and_push.sh

AWS_REGION="${AWS_REGION:-us-east-1}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
BUILD_BACKEND="${BUILD_BACKEND:-1}"
BUILD_FRONTEND="${BUILD_FRONTEND:-1}"
# Platforms to build for (single or comma-separated list). Default fixes EC2 x86_64.
# Example multi-arch: linux/amd64,linux/arm64
BUILD_PLATFORMS="${BUILD_PLATFORMS:-linux/amd64}"

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI not found. Install AWS CLI v2 and configure credentials."
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Install Docker and try again."
  exit 1
fi

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_DOMAIN="${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

docker_login() {
  aws ecr get-login-password --region "${AWS_REGION}" | \
    docker login --username AWS --password-stdin "${ECR_DOMAIN}"
}

ensure_buildx() {
  if ! docker buildx inspect >/dev/null 2>&1; then
    docker buildx create --use --name tsbuilder >/dev/null
  fi
}

ensure_repo() {
  local repo="$1"
  aws ecr describe-repositories --repository-names "${repo}" --region "${AWS_REGION}" >/dev/null 2>&1 || \
    aws ecr create-repository --repository-name "${repo}" --region "${AWS_REGION}" >/dev/null
}

build_and_push_backend() {
  local repo="${BACKEND_REPO:?BACKEND_REPO not set}"
  local remote="${ECR_DOMAIN}/${repo}:${IMAGE_TAG}"
  echo "==> Building (buildx) backend image for platforms: ${BUILD_PLATFORMS}"
  ensure_buildx
  ensure_repo "${repo}"
  docker buildx build \
    --platform "${BUILD_PLATFORMS}" \
    -t "${remote}" \
    --push \
    apps/backend
}

build_and_push_frontend() {
  local repo="${FRONTEND_REPO:?FRONTEND_REPO not set}"
  local remote="${ECR_DOMAIN}/${repo}:${IMAGE_TAG}"

  # Collect Vite build args if set
  declare -a args
  for key in VITE_API_BASE_URL VITE_AUTH0_DOMAIN VITE_AUTH0_CLIENT_ID VITE_AUTH0_REDIRECT_URI VITE_AUTH0_AUDIENCE VITE_ENABLE_ANALYTICS VITE_ENABLE_ERROR_REPORTING; do
    val="${!key-}"
    if [[ -n "${val}" ]]; then
      args+=(--build-arg "${key}=${val}")
    fi
  done

  echo "==> Building (buildx) frontend image for platforms: ${BUILD_PLATFORMS}"
  ensure_buildx
  ensure_repo "${repo}"
  docker buildx build \
    --platform "${BUILD_PLATFORMS}" \
    -t "${remote}" \
    --push \
    "${args[@]}" \
    apps/frontend
}

echo "==> Logging into ECR: ${ECR_DOMAIN}"
docker_login

if [[ "${BUILD_BACKEND}" == "1" ]]; then
  build_and_push_backend
else
  echo "==> Skipping backend build (BUILD_BACKEND=0)"
fi

if [[ "${BUILD_FRONTEND}" == "1" ]]; then
  build_and_push_frontend
else
  echo "==> Skipping frontend build (BUILD_FRONTEND=0)"
fi

echo "==> Done."


