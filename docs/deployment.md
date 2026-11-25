# Wdrożenie produkcyjne

Decyzja: [D22](decisions.md#d22-wdrożenie-jeden-vps-z-docker-compose-obrazy-z-ghcr-użytkownik-2026-11-24). Jeden serwer
VPS z Docker Compose, obrazy budowane przez GitHub Actions i trzymane w GitHub Container Registry.

```
Internet ──443──▶ Caddy (HTTPS) ──/api/*──▶ backend :8080 (Spring Boot) ──▶ postgres :5432
                               └──/*─────▶ frontend :3000 (Next.js) ──SSR──▶ backend
```

| Plik | Rola |
|---|---|
| `backend/Dockerfile`, `frontend/Dockerfile` | obrazy aplikacji (wielostopniowe, użytkownik bez uprawnień roota, healthcheck) |
| `.github/workflows/ci.yml` | testy przy każdym pushu; na `main` obrazy `ghcr.io/fellway/b2b-showroom-{backend,frontend}` |
| `deploy/compose.yml` | usługi produkcyjne i wolumeny (`showroom_pgdata`, `showroom_storage`, certyfikaty Caddy) |
| `deploy/Caddyfile` | routing jednej domeny, HTTPS, nagłówki bezpieczeństwa |
| `deploy/.env.example` | wzór konfiguracji i sekretów (`deploy/.env` na serwerze, poza gitem) |
| `deploy/backup.sh` | kopia bazy i plików z rotacją |

## Serwer — wymagania

- Linux (np. Ubuntu 24.04), 2 vCPU, 4 GB RAM, ≥ 40 GB dysku (np. Hetzner CX22/CPX21, OVH VPS).
- Docker Engine z wtyczką Compose (`curl -fsSL https://get.docker.com | sh`).
- Zapora: otwarte tylko 22 (SSH), 80 i 443 (TCP) oraz 443/UDP (HTTP/3).
- Domena: rekord A (i AAAA, jeśli jest IPv6) na adres serwera. **Bez domeny** na próbę: `SHOWROOM_DOMAIN=203-0-113-10.sslip.io`
  (adres IP z myślnikami) — Caddy dostanie prawdziwy certyfikat.

## Pierwsze wdrożenie

1. **Obrazy w GHCR.** Po zielonym przebiegu CI na `main` obrazy są w zakładce *Packages* profilu Fellway.
   Repozytorium jest publiczne, więc pakiety też — serwer pobiera je bez logowania. Jeśli repozytorium
   (lub pakiety) staną się prywatne, serwer musi się zalogować tokenem z uprawnieniem `read:packages`
   (GitHub → Settings → Developer settings → Personal access tokens → *Tokens (classic)*):
   ```bash
   echo <token> | docker login ghcr.io -u Fellway --password-stdin
   ```
2. **Pliki wdrożeniowe** na serwer (bez klonowania całego repozytorium wystarczy katalog `deploy/`):
   ```bash
   sudo mkdir -p /opt/showroom && sudo chown $USER /opt/showroom
   scp -r deploy/ <serwer>:/opt/showroom/
   ```
3. **Konfiguracja:**
   ```bash
   cd /opt/showroom/deploy
   cp .env.example .env && chmod 600 .env
   openssl rand -base64 48   # dwa razy: POSTGRES_PASSWORD i SHOWROOM_JWT_SECRET
   nano .env                 # domena, sekrety, pierwszy administrator, dane marki
   ```
4. **Start:**
   ```bash
   docker compose pull
   docker compose up -d
   docker compose ps         # wszystkie usługi "healthy"
   docker compose logs -f caddy   # uzyskanie certyfikatu
   ```
5. **Panel:** `https://<domena>/admin` — zaloguj się kontem z `SHOWROOM_ADMIN_*`, potem usuń hasło z `.env`.
6. **Kopie zapasowe** — wpis w crontab (`crontab -e`):
   ```
   30 3 * * * cd /opt/showroom/deploy && ./backup.sh >> /var/log/showroom-backup.log 2>&1
   ```
   Kopia na tym samym serwerze nie chroni przed jego utratą — synchronizuj `BACKUP_DIR` poza serwer
   (np. `rclone sync` do Hetzner Storage Box lub S3) albo włącz snapshoty u dostawcy.
7. Przejdź [listę kontrolną przed produkcją](roadmap.md#lista-kontrolna-przed-produkcją).

## Aktualizacja i wycofanie

```bash
cd /opt/showroom/deploy
docker compose pull && docker compose up -d     # najnowsze obrazy z main (migracje Flyway uruchamiają się same)
docker image prune -f
```

Wycofanie do konkretnej wersji: `IMAGE_TAG=sha-1a2b3c4` w `.env` (tag z zakładki *Packages* lub skrót commita),
potem `docker compose up -d`. Uwaga: migracje bazy są tylko „w przód” — wycofanie obrazu po migracji, która zmieniła
schemat, wymaga odtworzenia kopii sprzed wdrożenia.

## Podpowiedź kraju w rejestracji (opcjonalnie)

Formularz rejestracji podpowiada kraj firmy i numer kierunkowy (D24). Bez dodatkowej konfiguracji korzysta z języka
przeglądarki. Dokładniej działa z nagłówkiem kraju od proxy: za Cloudflare (proxy włączone) nagłówek `CF-IPCountry`
przychodzi sam; przy samym Caddy można ustawić `X-Country-Code` modułem GeoIP (np. baza MaxMind GeoLite2 — wymaga
darmowego konta i klucza licencyjnego). Nagłówek wpływa tylko na podpowiedź — nie służy do kontroli dostępu.

## Zmienne frontendu wpisywane przy buildzie

`NEXT_PUBLIC_DEMO_NOTICE` (domyślnie `true`) i `NEXT_PUBLIC_PRIVACY_POLICY_URL` ustawiasz w repozytorium:
GitHub → Settings → Secrets and variables → Actions → **Variables**. Zmiana działa po ponownym zbudowaniu obrazu
(nowy push na `main` albo *Actions → CI → Run workflow* na `main`).

## Odtwarzanie z kopii

```bash
cd /opt/showroom/deploy
docker compose stop frontend backend
# baza
docker compose exec -T postgres dropdb -U showroom showroom
docker compose exec -T postgres createdb -U showroom showroom
docker compose exec -T postgres pg_restore -U showroom -d showroom --no-owner < /var/backups/showroom/db-<data>.dump
# pliki
docker run --rm -v showroom_storage:/data -v /var/backups/showroom:/backup alpine:3 \
  sh -c 'rm -rf /data/* && tar -xzf /backup/storage-<data>.tar.gz -C /data'
docker compose start backend frontend
```

Odtwarzanie warto przećwiczyć raz po pierwszym wdrożeniu.

## Diagnostyka

- `docker compose ps` / `docker compose logs -f backend` — stan i logi usług.
- Backend „unhealthy”: najczęściej brak `SHOWROOM_JWT_SECRET` lub błędne `POSTGRES_PASSWORD` (hasło bazy ustala się
  przy **pierwszym** starcie wolumenu — późniejsza zmiana w `.env` wymaga `ALTER USER` w bazie).
- Brak certyfikatu: domena nie wskazuje na serwer albo port 80 jest zamknięty (`docker compose logs caddy`).
- Rate limiting liczy klientów po adresie z `X-Forwarded-For`, który ustawia Caddy — nie wystawiaj backendu
  bezpośrednio na zewnątrz.
