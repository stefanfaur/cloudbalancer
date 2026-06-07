# web-dashboard/Dockerfile
# Multi-stage: Node builder + nginx SPA server
# Build context is the repo root (see docker-compose.master.yml)

# Stage 1: Build the dashboard bundle
FROM node:22-alpine AS builder

WORKDIR /app

# Install dependencies first (layer caching)
COPY web-dashboard/package.json web-dashboard/package-lock.json ./
RUN npm ci

# Copy source code
COPY web-dashboard/src ./src
COPY web-dashboard/public ./public
COPY web-dashboard/tsconfig.json web-dashboard/tsconfig.app.json web-dashboard/tsconfig.node.json web-dashboard/vite.config.ts web-dashboard/index.html ./

# Build with empty API/metrics URLs (relative paths → same-origin)
ARG VITE_API_URL=""
ARG VITE_METRICS_URL=""
RUN VITE_API_URL="$VITE_API_URL" VITE_METRICS_URL="$VITE_METRICS_URL" npm run build

# Stage 2: nginx SPA server
FROM nginx:alpine

# Copy built dashboard
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy master nginx config (SPA routing + proxy blocks)
COPY docker/nginx.master.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
