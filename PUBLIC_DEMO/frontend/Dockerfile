FROM node:24-alpine AS builder
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
# These values are supplied only as Docker build arguments from ignored local configuration.
ARG VITE_AMAP_JS_KEY
ARG VITE_AMAP_JS_SECURITY_CODE
ENV VITE_AMAP_JS_KEY=${VITE_AMAP_JS_KEY}
ENV VITE_AMAP_JS_SECURITY_CODE=${VITE_AMAP_JS_SECURITY_CODE}
RUN npm run build

FROM nginx:1.27-alpine AS runtime
RUN apk add --no-cache curl
COPY frontend/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
