@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Detecting Java version installed.
:CHECK_JAVA
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "jver=%%j%%k"
@ECHO CurrentVersion %jver%

if %jver% NEQ 18 GOTO JAVA_NOT_INSTALLED

:JAVA_INSTALLED

@ECHO Java 1.8 found!
@ECHO Installing thingsboard ...

SET loadDemo=false

if "%1" == "--loadDemo" (
    SET loadDemo=true
)

SET BASE=%~dp0
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\thingsboard.jar
SET installDir=%BASE%\data

PUSHD "%BASE%\conf"

java -cp "%jarfile%" -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir="%installDir%"^
                    -Dinstall.load_demo=%loadDemo%^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config="%BASE%\install\logback.xml"^
                    org.springframework.boot.loader.PropertiesLauncher

if errorlevel 1 (
   @echo V-Sensor installation failed!
   POPD
   exit /b %errorlevel%
)
POPD

"%BASE%"thingsboard.exe install

@ECHO V-Sensor installed successfully!

GOTO END

:JAVA_NOT_INSTALLED
@ECHO Java 1.8 is not installed. Only Java 1.8 is supported
@ECHO Please go to https://adoptopenjdk.net/index.html and install Java 1.8. Then retry installation.
PAUSE
GOTO END

:END
