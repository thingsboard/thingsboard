@echo off
setlocal
rem ---------------------------------------------------------------------------
rem  Skroty do dev stacku ThingsBoard - Windows 10, bez basha.
rem
rem    dev\dev.cmd up               start calego stacku (hybrid)
rem    dev\dev.cmd up-minimal       start bez Cassandry (najszybszy)
rem    dev\dev.cmd rebuild          start z przebudowa obrazow dev
rem    dev\dev.cmd logs             logi backendu i frontendu
rem    dev\dev.cmd logs-be          tylko backend (widac auto-kompilacje)
rem    dev\dev.cmd logs-fe          tylko Angular
rem    dev\dev.cmd restart-backend  pelny rebuild modulu i restart backendu
rem    dev\dev.cmd shell-be         konsola w kontenerze backendu
rem    dev\dev.cmd ps               status uslug
rem    dev\dev.cmd down             stop
rem    dev\dev.cmd reset            stop + skasowanie wolumenow (od zera)
rem ---------------------------------------------------------------------------
cd /d "%~dp0"

set COMPOSE=docker compose --env-file .env.dev -f docker-compose.dev.yml --profile
set CMD=%1
if "%CMD%"=="" set CMD=up

if /i "%CMD%"=="up"              goto up
if /i "%CMD%"=="up-minimal"      goto upmin
if /i "%CMD%"=="rebuild"         goto rebuild
if /i "%CMD%"=="logs"            goto logs
if /i "%CMD%"=="logs-be"         goto logsbe
if /i "%CMD%"=="logs-fe"         goto logsfe
if /i "%CMD%"=="restart-backend" goto restartbe
if /i "%CMD%"=="shell-be"        goto shellbe
if /i "%CMD%"=="ps"              goto ps
if /i "%CMD%"=="down"            goto down
if /i "%CMD%"=="reset"           goto reset

echo Nieznane polecenie: %CMD%
echo Dostepne: up ^| up-minimal ^| rebuild ^| logs ^| logs-be ^| logs-fe ^| restart-backend ^| shell-be ^| ps ^| down ^| reset
exit /b 1

:up
%COMPOSE% hybrid up
exit /b %ERRORLEVEL%

:upmin
%COMPOSE% minimal up
exit /b %ERRORLEVEL%

:rebuild
%COMPOSE% hybrid -Rebuild up
exit /b %ERRORLEVEL%

:logs
%COMPOSE% logs -f tb-node-dev tb-ui-dev
exit /b %ERRORLEVEL%

:logsbe
%COMPOSE% logs -f tb-node-dev
exit /b %ERRORLEVEL%

:logsfe
%COMPOSE% logs -f tb-ui-dev
exit /b %ERRORLEVEL%

:restartbe
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rebuild-backend.ps1" %2
exit /b %ERRORLEVEL%

:shellbe
%COMPOSE% exec tb-node-dev bash
exit /b %ERRORLEVEL%

:ps
%COMPOSE% --profile hybrid ps
exit /b %ERRORLEVEL%

:down
%COMPOSE% --profile hybrid --profile minimal --profile js down
exit /b %ERRORLEVEL%

:reset
echo To skasuje wszystkie dane dev (baza, ~/.m2, node_modules).
choice /c TN /m "Kontynuowac? [T/N]"
if errorlevel 2 exit /b 0
%COMPOSE% --profile hybrid --profile minimal --profile js down -v
if exist .installed del .installed
echo Gotowe. Uruchom: dev\dev.cmd up
exit /b 0
