@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Detecting Java version installed.
:CHECK_JAVA_64
@ECHO Detecting if it is 64 bit machine
set KEY_NAME="HKEY_LOCAL_MACHINE\Software\Wow6432Node\JavaSoft\Java Runtime Environment"
set VALUE_NAME=CurrentVersion

FOR /F "usebackq skip=2 tokens=1-3" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
    set ValueName=%%A
    set ValueType=%%B
    set ValueValue=%%C
)
@ECHO CurrentVersion %ValueValue%

SET KEY_NAME="%KEY_NAME:~1,-1%\%ValueValue%"
SET VALUE_NAME=JavaHome

if defined ValueName (
    FOR /F "usebackq skip=2 tokens=1,2*" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
        set ValueName2=%%A
        set ValueType2=%%B
        set JRE_PATH2=%%C

        if defined ValueName2 (
            set ValueName = %ValueName2%
            set ValueType = %ValueType2%
            set ValueValue =  %JRE_PATH2%
        )
    )
)

IF NOT "%JRE_PATH2%" == "" GOTO JAVA_INSTALLED

:CHECK_JAVA_32
@ECHO Detecting if it is 32 bit machine
set KEY_NAME="HKEY_LOCAL_MACHINE\Software\JavaSoft\Java Runtime Environment"
set VALUE_NAME=CurrentVersion

FOR /F "usebackq skip=2 tokens=1-3" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
    set ValueName=%%A
    set ValueType=%%B
    set ValueValue=%%C
)
@ECHO CurrentVersion %ValueValue%

SET KEY_NAME="%KEY_NAME:~1,-1%\%ValueValue%"
SET VALUE_NAME=JavaHome

if defined ValueName (
    FOR /F "usebackq skip=2 tokens=1,2*" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
        set ValueName2=%%A
        set ValueType2=%%B
        set JRE_PATH2=%%C

        if defined ValueName2 (
            set ValueName = %ValueName2%
            set ValueType = %ValueType2%
            set ValueValue =  %JRE_PATH2%
        )
    )
)

IF "%JRE_PATH2%" == ""  GOTO JAVA_NOT_INSTALLED

:JAVA_INSTALLED

@ECHO Java 1.8 found!
@ECHO Installing ${pkg.name} ...

SET loadDemo=false

if "%1" == "--loadDemo" (
    SET loadDemo=true
)

SET BASE=%~dp0
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%BASE%\data\sql
SET jarfile=%BASE%\lib\${pkg.name}.jar
SET installDir=%BASE%\data

PUSHD %BASE%\conf

java -cp %jarfile% -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir=%installDir%^
                    -Dinstall.load_demo=%loadDemo%^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config=%BASE%\install\logback.xml^
                    org.springframework.boot.loader.PropertiesLauncher

if errorlevel 1 (
   @echo ThingsBoard installation failed!
   POPD
   exit /b %errorlevel%
)
POPD

%BASE%${pkg.name}.exe install

@ECHO ThingsBoard installed successfully!

GOTO END

:JAVA_NOT_INSTALLED
@ECHO Java 1.8 or above is not installed
@ECHO Please go to https://java.com/ and install Java. Then retry installation.
PAUSE
GOTO END

:END


