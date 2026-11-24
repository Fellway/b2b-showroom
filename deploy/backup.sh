#!/usr/bin/env bash
# Kopia zapasowa produkcji: zrzut bazy (pg_dump, format custom) + archiwum plików (zdjęcia, PDF-y zestawień).
# Uruchamiaj z katalogu deploy/ na serwerze, np. z crona codziennie o 3:30:
#
#   30 3 * * * cd /opt/showroom/deploy && ./backup.sh >> /var/log/showroom-backup.log 2>&1
#
# Kopie lądują w BACKUP_DIR (deploy/.env) i są usuwane po BACKUP_RETENTION_DAYS dniach.
# Kopia na tym samym serwerze nie chroni przed jego utratą — synchronizuj BACKUP_DIR poza serwer
# (np. rclone do Storage Box / S3). Odtwarzanie: docs/deployment.md.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
set -a
# shellcheck disable=SC1091
. ./.env
set +a

BACKUP_DIR="${BACKUP_DIR:-/var/backups/showroom}"
RETENTION="${BACKUP_RETENTION_DAYS:-14}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

# Baza: spójny zrzut bez zatrzymywania aplikacji
docker compose exec -T postgres pg_dump -U showroom -d showroom --format=custom --no-owner \
  > "$BACKUP_DIR/db-$STAMP.dump.partial"
mv "$BACKUP_DIR/db-$STAMP.dump.partial" "$BACKUP_DIR/db-$STAMP.dump"

# Pliki: wolumen backendu tylko do odczytu
docker run --rm -v showroom_storage:/data:ro -v "$BACKUP_DIR":/backup alpine:3 \
  tar -czf "/backup/storage-$STAMP.tar.gz.partial" -C /data .
mv "$BACKUP_DIR/storage-$STAMP.tar.gz.partial" "$BACKUP_DIR/storage-$STAMP.tar.gz"

chmod 600 "$BACKUP_DIR"/*-"$STAMP".*
find "$BACKUP_DIR" -maxdepth 1 -type f \( -name 'db-*.dump' -o -name 'storage-*.tar.gz' \) -mtime +"$RETENTION" -delete

echo "$(date -u +%FT%TZ) kopia OK: db-$STAMP.dump, storage-$STAMP.tar.gz"
