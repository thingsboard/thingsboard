@REM
@REM SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
@REM SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
@REM SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Detecting Java version installed.
:CHECK_JAVA
set "jver="
set "jmajor=0"
for /f tokens^=2^ delims^=.+-_^" %%j in ('java -fullversion 2^>^&1 ^| findstr /c:"full version"') do set "jver=%%j"
@ECHO CurrentVersion %jver%

set /a jmajor=jver 2>nul
if %jmajor% LSS 25 GOTO JAVA_NOT_INSTALLED

:JAVA_INSTALLED

@ECHO Java %jmajor% found!
@ECHO Installing thingsboard ...

SET BASE=%~dp0
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\${pkg.name}.jar
SET installDir=%BASE%\data

PUSHD "%BASE%\conf"

java -cp "%jarfile%" -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir="%installDir%"^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config="%BASE%\install\logback.xml"^
                    org.springframework.boot.loader.launch.PropertiesLauncher

if errorlevel 1 (
   @echo ThingsBoard installation failed!
   POPD
   exit /b %errorlevel%
)
POPD

"%BASE%"thingsboard.exe install

@ECHO ThingsBoard installed successfully!

GOTO END

:JAVA_NOT_INSTALLED
@ECHO Java 25 or later is not installed. ThingsBoard requires Java 25 or later.
@ECHO Please go to https://adoptium.net/ and install Java 25. Then retry installation.
PAUSE
GOTO END

:END
