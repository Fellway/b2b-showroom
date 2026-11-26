# syntax=docker/dockerfile:1
# Obraz frontendu: Next.js w trybie standalone (server.js + śledzone zależności), Node 22.
# Kontekst budowania: katalog frontend/.
#
# Zmienne NEXT_PUBLIC_* i rewrite /api są wpisywane do kodu przy `next build` — zmiana wymaga przebudowania obrazu.

FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json .npmrc ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

FROM node:22-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_DEMO_NOTICE=true
# Adres backendu w sieci docker compose — trafia do rewrite'u /api (używa go optymalizator next/image).
ARG API_INTERNAL_URL=http://backend:8080
ENV NEXT_PUBLIC_DEMO_NOTICE=$NEXT_PUBLIC_DEMO_NOTICE \
    API_INTERNAL_URL=$API_INTERNAL_URL \
    NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    API_INTERNAL_URL=http://backend:8080
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -q --spider http://127.0.0.1:3000/admin/login || exit 1
CMD ["node", "server.js"]
