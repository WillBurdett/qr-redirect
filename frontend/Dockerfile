# Builds the Vite frontend and serves it with Caddy (which also handles HTTPS + proxying)
FROM node:22-alpine AS build
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /web/dist /srv
COPY Caddyfile /etc/caddy/Caddyfile