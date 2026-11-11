@REM
@REM SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
@REM SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
@REM SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Upgrading ${pkg.name} ...

SET BASE=%~dp0

:loop
IF NOT "%1"=="" (
    IF "%1"=="--fromVersion" (
        SET fromVersion=%2
    )
    SHIFT
    GOTO :loop
)

SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\${pkg.name}.jar
SET installDir=%BASE%\data

@REM 4.4 folded the twilio rule nodes into the boot jar; the leftover extension jar is an unrelocated
@REM fat jar that shadows core libraries JVM-wide through LOADER_PATH.
DEL /F /Q "%BASE%\extensions\rule-node-twilio-sms*.jar" >nul 2>&1
@REM DEL cannot unlink a jar the running service holds open, and it reports that only through errorlevel.
IF EXIST "%BASE%\extensions\rule-node-twilio-sms*.jar" (
    @ECHO Failed to remove the obsolete twilio extension jar from %BASE%\extensions. Stop the ThingsBoard service and run the upgrade again.
    exit /b 1
)

@REM Set TB_LICENSE_SECRET in the environment this script runs in so the upgrade can verify, before it
@REM changes anything, that this instance does not hold more devices than the license covers. Without it that
@REM check is skipped and the upgrade proceeds.

PUSHD "%BASE%\conf"

java -cp "%jarfile%" -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir="%installDir%"^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=true^
                    -Dinstall.upgrade.from_version=%fromVersion%^
                    -Dlogging.config="%BASE%\install\logback.xml"^
                    org.springframework.boot.loader.launch.PropertiesLauncher

if errorlevel 1 (
   @echo ThingsBoard upgrade failed!
   POPD
   exit /b %errorlevel%
)
POPD

@ECHO ThingsBoard upgraded successfully!

GOTO END

:END
