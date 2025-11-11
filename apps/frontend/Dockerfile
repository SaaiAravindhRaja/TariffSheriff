# Multi-stage build for React frontend
FROM node:20-bookworm-slim AS builder

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (include devDeps for Vite build)
# Workaround npm@10 CI bug: pin to npm@9 and use classic install
ENV NPM_CONFIG_FUND=false NPM_CONFIG_AUDIT=false
RUN npm i -g npm@9.9.2 && npm --version \
    && npm install

# Build-time envs for Vite
ARG VITE_API_BASE_URL
ARG VITE_AUTH0_DOMAIN
ARG VITE_AUTH0_CLIENT_ID
ARG VITE_AUTH0_REDIRECT_URI
ARG VITE_AUTH0_AUDIENCE
ARG VITE_ENABLE_ANALYTICS
ARG VITE_ENABLE_ERROR_REPORTING
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_AUTH0_DOMAIN=$VITE_AUTH0_DOMAIN \
    VITE_AUTH0_CLIENT_ID=$VITE_AUTH0_CLIENT_ID \
    VITE_AUTH0_REDIRECT_URI=$VITE_AUTH0_REDIRECT_URI \
    VITE_AUTH0_AUDIENCE=$VITE_AUTH0_AUDIENCE \
    VITE_ENABLE_ANALYTICS=$VITE_ENABLE_ANALYTICS \
    VITE_ENABLE_ERROR_REPORTING=$VITE_ENABLE_ERROR_REPORTING

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine AS production

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]