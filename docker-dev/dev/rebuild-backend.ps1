<#
  Awaryjny, pelny rebuild backendu.

  Watcher w kontenerze kompiluje pojedyncze moduly po zapisie pliku .java.
  Ten skrypt przydaje sie, gdy zmiana wykracza poza kod Javy:
    - zmiana w pom.xml lub nowa zaleznosc,
    - zmiana zasobow (application.yml, sql, freemarker),
    - watcher zgubil zmiane.

    powershell -ExecutionPolicy Bypass -File dev\rebuild-backend.ps1
    powershell -ExecutionPolicy Bypass -File dev\rebuild-backend.ps1 dao
    powershell -ExecutionPolicy Bypass -File dev\rebuild-backend.ps1 restart
#>
[CmdletBinding()]
param(
  [string]$Target = 'application'
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$compose = @('compose', '--env-file', '.env.dev', '-f', 'docker-compose.dev.yml')

if ($Target -eq 'restart') {
  Write-Host '==> restart kontenera backendu' -ForegroundColor Cyan
  & docker @($compose + @('restart', 'tb-node-dev'))
  exit $LASTEXITCODE
}

Write-Host "==> rebuild modulu '$Target' w kontenerze" -ForegroundColor Cyan
& docker @($compose + @(
  'exec', 'tb-node-dev', 'bash', '-lc',
  "cd /src && mvn -o -q install -pl $Target -am -DskipTests -Dpkg.skip=true -Dlicense.skip=true"
))
if ($LASTEXITCODE -ne 0) { throw 'Build nie powiodl sie - sprawdz logi wyzej.' }

Write-Host '==> restart aplikacji' -ForegroundColor Cyan
& docker @($compose + @('exec', 'tb-node-dev', 'bash', '-lc', 'touch /tmp/tb-restart-requested'))

Write-Host 'Gotowe. Logi: docker-dev\dev\dev.cmd logs-be' -ForegroundColor Green
