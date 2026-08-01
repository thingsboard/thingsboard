<#
  Dev stack ThingsBoard - jedno wejscie dla Windows 10 (PowerShell).

    powershell -ExecutionPolicy Bypass -File dev\up.ps1
    powershell -ExecutionPolicy Bypass -File dev\up.ps1 -Profile minimal
    powershell -ExecutionPolicy Bypass -File dev\up.ps1 -Profile hybrid -WithJs

  Pierwsze uruchomienie samo zaklada schemat bazy i dane demo.
#>
[CmdletBinding()]
param(
  [ValidateSet('hybrid', 'minimal')]
  [string]$Profile = 'hybrid',
  [switch]$WithJs,
  [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$compose = @('compose', '--env-file', '.env.dev', '-f', 'docker-compose.dev.yml')

function Invoke-Compose {
  param([string[]]$Args)
  & docker @($compose + $Args)
  if ($LASTEXITCODE -ne 0) { throw "docker compose zakonczylo sie bledem ($LASTEXITCODE)" }
}

if ($Profile -eq 'minimal') { $env:DATABASE_TS_TYPE = 'sql' } else { $env:DATABASE_TS_TYPE = 'cassandra' }

Write-Host "==> profil: $Profile (DATABASE_TS_TYPE=$($env:DATABASE_TS_TYPE))" -ForegroundColor Cyan

Write-Host '==> start infrastruktury' -ForegroundColor Cyan
Invoke-Compose @('--profile', $Profile, 'up', '-d', 'postgres', 'kafka', 'valkey')
if ($Profile -eq 'hybrid') {
  Invoke-Compose @('--profile', 'hybrid', 'up', '-d', 'cassandra')
}

if (-not (Test-Path '.installed')) {
  Write-Host '==> jednorazowy install schematu ThingsBoard (+ dane demo)' -ForegroundColor Cyan
  Invoke-Compose @('--profile', 'install', 'run', '--rm', 'tb-install')
  New-Item -ItemType File -Path '.installed' -Force | Out-Null
}

$upArgs = @('--profile', $Profile)
if ($WithJs) { $upArgs += @('--profile', 'js') }
$upArgs += @('up', '-d')
if ($Rebuild) { $upArgs += '--build' }

Write-Host '==> start backendu i frontendu' -ForegroundColor Cyan
Invoke-Compose $upArgs

Write-Host ''
Write-Host 'Gotowe. Pierwszy build Mavena i yarn install trwaja dlugo - sledz logi:' -ForegroundColor Green
Write-Host '  dev\dev.cmd logs'
Write-Host ''
Write-Host '  UI:      http://localhost:4200'
Write-Host '  API:     http://localhost:8080'
Write-Host '  debug:   localhost:5005 (JDWP)'
Write-Host '  login:   tenant@thingsboard.org / tenant'
