#!/usr/bin/env bash
# Lokalne środowisko deweloperskie: PostgreSQL w kontenerze + backend (jar) w tle, opcjonalnie dane demonstracyjne.
#
#   scripts/dev-up.sh            uruchom bazę i backend
#   scripts/dev-up.sh --build    przebuduj jar przed startem
#   scripts/dev-up.sh --seed     po starcie załaduj dane demonstracyjne (scripts/seed-demo.mjs)
#   scripts/dev-up.sh stop       zatrzymaj backend i kontener bazy
#
# Sekrety czyta wyłącznie z nieśledzonego pliku .env w katalogu głównym (wzór: .env.example).
# Baza startuje na domyślnej sieci "bridge" (bez docker compose), żeby nie tworzyć nowych sieci Dockera.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA="$ROOT/backend/data"
JAR="$ROOT/backend/target/b2b-showroom-0.1.0-SNAPSHOT.jar"
PID_FILE="$DATA/dev-backend.pid"
LOG_FILE="$DATA/dev-backend.log"
DB_CONTAINER=showroom-dev-db

stop() {
  if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    kill "$(cat "$PID_FILE")" && echo "Backend zatrzymany."
  fi
  rm -f "$PID_FILE"
  docker stop "$DB_CONTAINER" >/dev/null 2>&1 && echo "Baza zatrzymana." || true
}

if [[ "${1:-}" == "stop" ]]; then
  stop
  exit 0
fi

BUILD=false
SEED=false
for arg in "$@"; do
  case "$arg" in
    --build) BUILD=true ;;
    --seed) SEED=true ;;
    *) echo "Nieznany argument: $arg" >&2; exit 2 ;;
  esac
done

if [[ ! -f "$ROOT/.env" ]]; then
  echo "Brak pliku .env — skopiuj .env.example i uzupełnij sekrety." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
. "$ROOT/.env"
set +a
: "${SHOWROOM_JWT_SECRET:?Ustaw SHOWROOM_JWT_SECRET w .env (openssl rand -base64 48)}"
export SHOWROOM_REFRESH_COOKIE_SECURE="${SHOWROOM_REFRESH_COOKIE_SECURE:-false}"
export SHOWROOM_RATE_LIMIT_ENABLED="${SHOWROOM_RATE_LIMIT_ENABLED:-false}"
export STORAGE_LOCAL_ROOT="${STORAGE_LOCAL_ROOT:-$DATA/storage}"
mkdir -p "$DATA"

# --- baza
if docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  :
elif docker ps -a --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  docker start "$DB_CONTAINER" >/dev/null
else
  docker run -d --name "$DB_CONTAINER" --network bridge -p 5433:5432 \
    -v showroom-dev-pgdata:/var/lib/postgresql \
    -e POSTGRES_DB=showroom -e POSTGRES_USER=showroom -e POSTGRES_PASSWORD="${DB_PASSWORD:-showroom-dev}" \
    postgres:18.4-alpine >/dev/null
fi
for _ in $(seq 1 30); do
  docker exec "$DB_CONTAINER" pg_isready -U showroom -d showroom >/dev/null 2>&1 && break
  sleep 1
done
echo "Baza: localhost:5433 ($DB_CONTAINER)"

# --- backend
if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "Backend już działa (PID $(cat "$PID_FILE"))."
else
  if $BUILD || [[ ! -f "$JAR" ]]; then
    (cd "$ROOT/backend" && ./mvnw -B -q -DskipTests package)
  fi
  nohup java -jar "$JAR" >"$LOG_FILE" 2>&1 </dev/null &
  echo $! >"$PID_FILE"
  for _ in $(seq 1 90); do
    curl -sf http://localhost:8080/actuator/health >/dev/null && break
    sleep 1
  done
  if ! curl -sf http://localhost:8080/actuator/health >/dev/null; then
    echo "Backend nie wstał — zobacz $LOG_FILE" >&2
    exit 1
  fi
  echo "Backend: http://localhost:8080 (PID $(cat "$PID_FILE"), log: $LOG_FILE)"
fi

if $SEED; then
  SHOWROOM_API=http://localhost:8080 node "$ROOT/scripts/seed-demo.mjs"
fi
