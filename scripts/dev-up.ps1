<#
.SYNOPSIS
  Lokalne środowisko deweloperskie na Windowsie (odpowiednik scripts/dev-up.sh bez Dockera i WSL).

.DESCRIPTION
  Baza: natywny PostgreSQL 18 jako usługa Windows (postgresql-x64-18, port 5433), zainstalowany przez
    winget install --id PostgreSQL.PostgreSQL.18 -e --override "--mode unattended --superpassword <hasło> --serverport 5433"
  Przy pierwszym uruchomieniu skrypt zakłada rolę i bazę "showroom", jeśli w .env jest POSTGRES_SUPERUSER_PASSWORD.
  Backend: jar w tle (PID w backend/data/dev-backend.pid, log w backend/data/dev-backend.log).
  Frontend (opcjonalnie): next dev w tle (PID w frontend/.next-dev.pid, log w frontend/.next-dev.log).

  Sekrety czyta wyłącznie z nieśledzonego pliku .env w katalogu głównym (wzór: .env.example).

.EXAMPLE
  scripts\dev-up.ps1                       # baza + backend
  scripts\dev-up.ps1 -Build -Seed          # przebuduj jar, uruchom, załaduj dane demonstracyjne
  scripts\dev-up.ps1 -Frontend             # baza + backend + frontend (http://localhost:3000)
  scripts\dev-up.ps1 stop                  # zatrzymaj backend i frontend (usługa bazy działa dalej)
#>
[CmdletBinding()]
param(
    [ValidateSet('start', 'stop')]
    [string]$Action = 'start',
    [switch]$Build,
    [switch]$Seed,
    [switch]$Frontend
)

$ErrorActionPreference = 'Stop'

$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$Data = Join-Path $Root 'backend\data'
$Jar = Join-Path $Root 'backend\target\b2b-showroom-0.1.0-SNAPSHOT.jar'
$BackendPid = Join-Path $Data 'dev-backend.pid'
$BackendLog = Join-Path $Data 'dev-backend.log'
$FrontendDir = Join-Path $Root 'frontend'
$FrontendPid = Join-Path $FrontendDir '.next-dev.pid'
$FrontendLog = Join-Path $FrontendDir '.next-dev.log'
$PgService = 'postgresql-x64-18'
$PgBin = 'C:\Program Files\PostgreSQL\18\bin'
$PgPort = 5433

# Świeżo zainstalowane narzędzia (winget) nie są jeszcze w PATH bieżącej sesji.
$env:Path = [Environment]::GetEnvironmentVariable('Path', 'Machine') + ';' + [Environment]::GetEnvironmentVariable('Path', 'User')

function Get-RunningPid([string]$pidFile) {
    if (-not (Test-Path $pidFile)) { return $null }
    $id = [int](Get-Content $pidFile -TotalCount 1)
    if (Get-Process -Id $id -ErrorAction SilentlyContinue) { return $id }
    Remove-Item $pidFile -Force
    return $null
}

function Stop-Tree([string]$pidFile, [string]$label) {
    $id = Get-RunningPid $pidFile
    if ($id) {
        # /T: razem z procesami potomnymi (npm -> node -> next)
        & taskkill.exe /PID $id /T /F | Out-Null
        Write-Host "$label zatrzymany."
    }
    if (Test-Path $pidFile) { Remove-Item $pidFile -Force }
}

function Test-Health {
    try { return (Invoke-RestMethod 'http://localhost:8080/actuator/health' -TimeoutSec 2).status -eq 'UP' }
    catch { return $false }
}

if ($Action -eq 'stop') {
    Stop-Tree $FrontendPid 'Frontend'
    Stop-Tree $BackendPid 'Backend'
    Write-Host "Usługa bazy ($PgService) działa dalej; zatrzymanie: Stop-Service $PgService (jako administrator)."
    exit 0
}

# --- .env
$envFile = Join-Path $Root '.env'
if (-not (Test-Path $envFile)) { throw 'Brak pliku .env — skopiuj .env.example i uzupełnij sekrety.' }
foreach ($line in Get-Content $envFile -Encoding UTF8) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        Set-Item "env:$($Matches[1])" ($Matches[2].Trim().Trim('"').Trim("'"))
    }
}
if (-not $env:SHOWROOM_JWT_SECRET) { throw 'Ustaw SHOWROOM_JWT_SECRET w .env (openssl rand -base64 48).' }
if (-not $env:SHOWROOM_REFRESH_COOKIE_SECURE) { $env:SHOWROOM_REFRESH_COOKIE_SECURE = 'false' }
if (-not $env:SHOWROOM_RATE_LIMIT_ENABLED) { $env:SHOWROOM_RATE_LIMIT_ENABLED = 'false' }
if (-not $env:STORAGE_LOCAL_ROOT) { $env:STORAGE_LOCAL_ROOT = Join-Path $Data 'storage' }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'showroom-dev' }
New-Item -ItemType Directory -Force $Data | Out-Null

# --- baza
$service = Get-Service $PgService -ErrorAction SilentlyContinue
if (-not $service) {
    throw "Brak usługi $PgService — zainstaluj PostgreSQL 18 (port $PgPort), zob. docs/development.md."
}
if ($service.Status -ne 'Running') {
    try { Start-Service $PgService } catch { throw "Nie udało się uruchomić $PgService — uruchom PowerShell jako administrator albo: Start-Service $PgService" }
}
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    & "$PgBin\pg_isready.exe" -h localhost -p $PgPort -q
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep 1
}
if (-not $ready) { throw "PostgreSQL nie odpowiada na localhost:$PgPort." }

# Pierwsze uruchomienie: rola i baza showroom (idempotentnie, tylko gdy znamy hasło superużytkownika).
$env:PGPASSWORD = $dbPassword
# PS 5.1: stderr programu natywnego przy ErrorActionPreference=Stop przerywa skrypt — sprawdzamy przez cmd.
& cmd.exe /c "`"$PgBin\psql.exe`" -h localhost -p $PgPort -U showroom -d showroom -tAc `"select 1`" >nul 2>nul"
$dbOk = $LASTEXITCODE -eq 0
if (-not $dbOk) {
    if (-not $env:POSTGRES_SUPERUSER_PASSWORD) {
        throw 'Baza showroom niedostępna. Dodaj POSTGRES_SUPERUSER_PASSWORD do .env, żeby skrypt ją założył.'
    }
    $env:PGPASSWORD = $env:POSTGRES_SUPERUSER_PASSWORD
    $pw = $dbPassword.Replace("'", "''")
    $roleExists = & "$PgBin\psql.exe" -h localhost -p $PgPort -U postgres -tAc "select 1 from pg_roles where rolname = 'showroom'"
    if ($roleExists -ne '1') {
        & "$PgBin\psql.exe" -h localhost -p $PgPort -U postgres -qc "create role showroom login password '$pw'"
    }
    $dbExists = & "$PgBin\psql.exe" -h localhost -p $PgPort -U postgres -tAc "select 1 from pg_database where datname = 'showroom'"
    if ($dbExists -ne '1') {
        & "$PgBin\psql.exe" -h localhost -p $PgPort -U postgres -qc 'create database showroom owner showroom'
    }
    Write-Host 'Założono rolę i bazę showroom.'
}
Remove-Item env:PGPASSWORD
Write-Host "Baza: localhost:$PgPort ($PgService)"

# --- backend
$running = Get-RunningPid $BackendPid
if ($running) {
    Write-Host "Backend już działa (PID $running)."
}
else {
    if ($Build -or -not (Test-Path $Jar)) {
        Push-Location (Join-Path $Root 'backend')
        try {
            & .\mvnw.cmd -B -q -DskipTests package
            if ($LASTEXITCODE -ne 0) { throw 'Budowanie backendu nie powiodło się.' }
        }
        finally { Pop-Location }
    }
    $proc = Start-Process java -ArgumentList '-jar', "`"$Jar`"" -WorkingDirectory $Root -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $BackendLog -RedirectStandardError "$BackendLog.err"
    $proc.Id | Set-Content $BackendPid
    $up = $false
    for ($i = 0; $i -lt 90; $i++) {
        if (Test-Health) { $up = $true; break }
        if ($proc.HasExited) { break }
        Start-Sleep 1
    }
    if (-not $up) { throw "Backend nie wstał — zobacz $BackendLog" }
    Write-Host "Backend: http://localhost:8080 (PID $($proc.Id), log: $BackendLog)"
}

# --- dane demonstracyjne
if ($Seed) {
    $env:SHOWROOM_API = 'http://localhost:8080'
    & node (Join-Path $Root 'scripts\seed-demo.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Ładowanie danych demonstracyjnych nie powiodło się.' }
}

# --- frontend
if ($Frontend) {
    $running = Get-RunningPid $FrontendPid
    if ($running) {
        Write-Host "Frontend już działa (PID $running)."
    }
    else {
        # Natywny moduł SWC odrzuca %LOCALAPPDATA%\swc, gdy katalog ma nietypowe ACL — cache trzymamy w projekcie.
        $env:SWC_NATIVE_BINDING_CACHE = Join-Path $FrontendDir '.swc-cache'
        $proc = Start-Process 'npm.cmd' -ArgumentList 'run', 'dev' -WorkingDirectory $FrontendDir -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $FrontendLog -RedirectStandardError "$FrontendLog.err"
        $proc.Id | Set-Content $FrontendPid
        $up = $false
        for ($i = 0; $i -lt 60; $i++) {
            try { Invoke-WebRequest 'http://localhost:3000' -UseBasicParsing -TimeoutSec 30 | Out-Null; $up = $true; break }
            catch { if ($proc.HasExited) { break }; Start-Sleep 1 }
        }
        if (-not $up) { throw "Frontend nie wstał — zobacz $FrontendLog" }
        Write-Host "Frontend: http://localhost:3000 (PID $($proc.Id), log: $FrontendLog)"
    }
}
