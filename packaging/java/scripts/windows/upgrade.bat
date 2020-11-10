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

if not defined fromVersion (
    echo "--fromVersion parameter is invalid or unspecified!"
    echo "Usage: upgrade.bat --fromVersion {VERSION}"
    exit /b 1
)

SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\${pkg.name}.jar
SET installDir=%BASE%\data

PUSHD "%BASE%\conf"

java -cp "%jarfile%" -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir="%installDir%"^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=true^
                    -Dinstall.upgrade.from_version=%fromVersion%^
                    -Dlogging.config="%BASE%\install\logback.xml"^
                    org.springframework.boot.loader.PropertiesLauncher

if errorlevel 1 (
   @echo V-Sensor upgrade failed!
   POPD
   exit /b %errorlevel%
)
POPD

@ECHO V-Sensor upgraded successfully!

GOTO END

:END
