#!/usr/bin/env bash
set -euo pipefail

# Pulls latest images from ECR and (re)starts backend and frontend containers on EC2.
# Run this ON the EC2 instance.
#
# Required env vars:
#   AWS_REGION                    e.g., us-east-1
#   BACKEND_REPO                  e.g., tariffsheriff-backend
#   FRONTEND_REPO                 e.g., tariffsheriff-frontend
# Optional:
#   IMAGE_TAG                     default: latest
#   BACKEND_ENV_FILE              default: /opt/tariffsheriff/backend.env
#   DO_BACKEND                    default: 1
#   DO_FRONTEND                   default: 1
#   DOMAIN                        required to run Caddy (e.g., tariffsheriff.app)
#   CADDY_EMAIL                   optional ACME email
#   NETWORK_NAME                  default: app
#   CADDYFILE_PATH                default: /opt/caddy/Caddyfile
#
# Example:
#   export AWS_REGION=us-east-1
#   export BACKEND_REPO=tariffsheriff-backend
#   export FRONTEND_REPO=tariffsheriff-frontend
#   ./scripts/ec2_run.sh

AWS_REGION="${AWS_REGION:-us-east-1}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-/opt/tariffsheriff/backend.env}"
DO_BACKEND="${DO_BACKEND:-1}"
DO_FRONTEND="${DO_FRONTEND:-1}"
NETWORK_NAME="${NETWORK_NAME:-app}"
CADDYFILE_PATH="${CADDYFILE_PATH:-/opt/caddy/Caddyfile}"
HOST_CERTS_DIR="${HOST_CERTS_DIR:-/opt/caddy/certs}"
TLS_CERT_PATH="${TLS_CERT_PATH:-/etc/caddy/certs/origin.pem}"
TLS_KEY_PATH="${TLS_KEY_PATH:-/etc/caddy/certs/origin.key}"

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI not found. Install AWS CLI v2."
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

pull_image() {
  local repo="$1"
  local tag="$2"
  local uri="${ECR_DOMAIN}/${repo}:${tag}"
  echo "==> Pulling ${uri}"
  docker pull "${uri}"
}

ensure_network() {
  docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1 || docker network create "${NETWORK_NAME}" >/dev/null
}

backend_env_args() {
  # Build -e KEY=VALUE args from exported env vars when no env-file is used.
  # Required: DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET
  local -a args=()
  local required=(DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD JWT_SECRET)
  local optional=(SPRING_PROFILES_ACTIVE SERVER_PORT CORS_ALLOWED_ORIGIN_PATTERNS AUTH0_ISSUER AUTH0_AUDIENCE OPENAI_API_KEY THENEWSAPI_API_KEY)

  local missing=0
  for k in "${required[@]}"; do
    local v="${!k-}"
    if [[ -z "${v}" ]]; then
      echo "Missing required env: $k" >&2
      missing=1
    else
      args+=(-e "$k=$v")
    fi
  done
  if [[ $missing -eq 1 ]]; then
    return 2
  fi
  for k in "${optional[@]}"; do
    local v="${!k-}"
    if [[ -n "${v}" ]]; then
      args+=(-e "$k=$v")
    fi
  done
  printf '%s\0' "${args[@]}"
}

restart_backend() {
  local repo="${BACKEND_REPO:?BACKEND_REPO not set}"
  local uri="${ECR_DOMAIN}/${repo}:${IMAGE_TAG}"

  docker rm -f tariffsheriff-backend >/dev/null 2>&1 || true
  echo "==> Starting backend container"
  # If DOMAIN is set (Caddy in front), don't publish host port 8080
  local -a publish_args=()
  if [[ -z "${DOMAIN:-}" ]]; then
    publish_args=(-p 8080:8080)
  fi
  if [[ -f "${BACKEND_ENV_FILE}" ]]; then
    docker run -d --name tariffsheriff-backend \
      --restart unless-stopped \
      --network "${NETWORK_NAME}" \
      "${publish_args[@]}" \
      --env-file "${BACKEND_ENV_FILE}" \
      "${uri}"
  else
    # Build inline -e args
    mapfile -d '' env_args < <(backend_env_args) || {
      echo "Provide required envs (DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, JWT_SECRET) when not using BACKEND_ENV_FILE." >&2
      exit 1
    }
    docker run -d --name tariffsheriff-backend \
      --restart unless-stopped \
      --network "${NETWORK_NAME}" \
      "${publish_args[@]}" \
      "${env_args[@]}" \
      "${uri}"
  fi
}

restart_frontend() {
  local repo="${FRONTEND_REPO:?FRONTEND_REPO not set}"
  local uri="${ECR_DOMAIN}/${repo}:${IMAGE_TAG}"

  docker rm -f tariffsheriff-frontend >/dev/null 2>&1 || true
  echo "==> Starting frontend container"
  # If DOMAIN is set (Caddy in front), don't publish host port 80
  local -a publish_args=()
  if [[ -z "${DOMAIN:-}" ]]; then
    publish_args=(-p 80:80)
  fi
  docker run -d --name tariffsheriff-frontend \
    --restart unless-stopped \
    --network "${NETWORK_NAME}" \
    "${publish_args[@]}" \
    "${uri}"
}

write_caddyfile() {
  local domain="${DOMAIN:-}"
  if [[ -z "${domain}" ]]; then
    echo "DOMAIN not set; skipping Caddy deployment." >&2
    return 1
  fi
  sudo mkdir -p "$(dirname "${CADDYFILE_PATH}")"
  sudo mkdir -p "${HOST_CERTS_DIR}"
  sudo tee "${CADDYFILE_PATH}" >/dev/null <<EOF
{
	$( [[ -n "${CADDY_EMAIL:-}" ]] && echo "email ${CADDY_EMAIL}" )
}

${domain} {
	tls ${TLS_CERT_PATH} ${TLS_KEY_PATH}
	encode zstd gzip

	# API and backend routes (match before SPA; preserve /api prefix)
	@api {
		path /api/*
		path /v3/api-docs/*
		path /swagger-ui/*
		path /actuator/health
	}
	handle @api {
		header Cache-Control "no-store"
		reverse_proxy tariffsheriff-backend:8080
	}

	# Everything else -> frontend (SPA)
	handle {
		reverse_proxy tariffsheriff-frontend:80
	}
}

www.${domain} {
	tls ${TLS_CERT_PATH} ${TLS_KEY_PATH}
	redir https://${domain}{uri}
}
EOF
}

restart_caddy() {
  if [[ -z "${DOMAIN:-}" ]]; then
    echo "DOMAIN not set; skipping Caddy run."
    return 0
  fi
  write_caddyfile || return 0
  # Basic sanity checks for cert files on host when using origin certs
  if [[ ! -f "${HOST_CERTS_DIR}/origin.pem" || ! -f "${HOST_CERTS_DIR}/origin.key" ]]; then
    echo "Expected cert files not found in ${HOST_CERTS_DIR} (origin.pem, origin.key)."
    echo "Place your Cloudflare origin cert and key there or override TLS_CERT_PATH/TLS_KEY_PATH appropriately."
  fi
  # Validate Caddyfile before starting
  echo "==> Validating Caddyfile"
  docker run --rm \
    -v "${CADDYFILE_PATH}:/etc/caddy/Caddyfile" \
    -v "${HOST_CERTS_DIR}:/etc/caddy/certs" \
    caddy caddy validate --config /etc/caddy/Caddyfile || {
      echo "Caddyfile validation failed. Fix /opt/caddy/Caddyfile and re-run."
      return 1
    }
  docker rm -f caddy >/dev/null 2>&1 || true
  echo "==> Starting Caddy (HTTPS reverse proxy for ${DOMAIN})"
  docker run -d --name caddy \
    --restart unless-stopped \
    --network "${NETWORK_NAME}" \
    -p 80:80 -p 443:443 \
    -v "${CADDYFILE_PATH}:/etc/caddy/Caddyfile" \
    -v "${HOST_CERTS_DIR}:/etc/caddy/certs" \
    caddy
}

echo "==> Logging into ECR: ${ECR_DOMAIN}"
docker_login

ensure_network

if [[ "${DO_BACKEND}" == "1" ]]; then
  pull_image "${BACKEND_REPO:?}" "${IMAGE_TAG}"
  restart_backend
else
  echo "==> Skipping backend (DO_BACKEND=0)"
fi

if [[ "${DO_FRONTEND}" == "1" ]]; then
  pull_image "${FRONTEND_REPO:?}" "${IMAGE_TAG}"
  restart_frontend
else
  echo "==> Skipping frontend (DO_FRONTEND=0)"
fi

restart_caddy

echo "==> Done. Frontend on :80, Backend on :8080"


