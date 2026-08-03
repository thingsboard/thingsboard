# ThingsBoard dev helper for Windows PowerShell.
#   .\dev.ps1 init | build | up | watch | logs | restart | psql | down | reset-db | reset-all
#   .\dev.ps1 mail-settings | mail-test | mail-dev you@example.com
param(
    [Parameter(Position = 0)]
    [ValidateSet('help', 'init', 'build', 'up', 'watch', 'logs', 'logs-builder', 'restart', 'ps', 'psql', 'shell', 'down', 'reset-db', 'reset-all', 'mail-settings', 'mail-test', 'mail-dev')]
    [string]$Command = 'help',
    [Parameter(Position = 1)]
    [string]$To = ''
)


$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

function Invoke-Compose { docker compose @args }

switch ($Command) {
    'help' {
        Write-Host "init         - create .env from .env.example"
        Write-Host "build        - full Maven build of the monorepo (first run, 20-40 min)"
        Write-Host "up           - start postgres + install/demo data + backend"
        Write-Host "watch        - start the builder in watch mode (own terminal)"
        Write-Host "logs         - follow backend logs"
        Write-Host "logs-builder - follow builder logs"
        Write-Host "restart      - restart the backend container"
        Write-Host "psql         - open psql on the dev database"
        Write-Host "down         - stop everything"
        Write-Host "reset-db     - wipe the database and re-run install + demo data"
        Write-Host "reset-all    - wipe database, build output and the Maven cache"
        Write-Host "mail-settings- push .env Mailtrap settings into the running backend"
        Write-Host "mail-test    - send a test email through Mailtrap"
        Write-Host "mail-dev <to>- call the custom /api/dev/mail/test endpoint"
    }
    'init' {
        if (-not (Test-Path '.env')) { Copy-Item '.env.example' '.env' }
        Write-Host ".env ready - put your Mailtrap password in it"
    }
    'build' { Invoke-Compose run --rm builder full }
    'up' {
        Invoke-Compose up -d postgres
        Invoke-Compose up tb-init
        Invoke-Compose up -d thingsboard
        Write-Host "API:     http://localhost:8080"
        Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
        Write-Host "WS:      ws://localhost:8080/api/ws"
    }
    'watch' { Invoke-Compose up builder }
    'logs' { Invoke-Compose logs -f thingsboard }
    'logs-builder' { Invoke-Compose logs -f builder }
    'restart' { Invoke-Compose restart thingsboard }
    'ps' { Invoke-Compose ps }
    'psql' { Invoke-Compose exec postgres psql -U postgres -d thingsboard }
    'shell' { Invoke-Compose exec thingsboard bash }
    'down' { Invoke-Compose down }
    'reset-db' {
        Invoke-Compose down
        docker volume rm -f tb-dev-postgres-data tb-dev-data
        Write-Host "database wiped - next 'up' reinstalls the schema and demo data"
    }
    'mail-settings' { & "$PSScriptRoot\scripts\test-mail.ps1" settings }
    'mail-test' { & "$PSScriptRoot\scripts\test-mail.ps1" send }
    'mail-dev' { & "$PSScriptRoot\scripts\test-mail.ps1" dev $To }
    'reset-all' {
        Invoke-Compose down
        docker volume rm -f tb-dev-postgres-data tb-dev-data tb-dev-build tb-dev-m2-cache
    }
}
