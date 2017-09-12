@ECHO OFF

setlocal ENABLEEXTENSIONS

SET BASE=${project.basedir}\target
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions
SET SQL_DATA_FOLDER=%~dp0\tmp
SET jarfile=%BASE%\thingsboard-${project.version}-boot.jar
SET installDir=%BASE%\data
SET loadDemo=true

SET SQL_DATA_FOLDER=/tmp

java -cp %jarfile% -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir=%installDir%^
                    -Dinstall.load_demo=%loadDemo%^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config=%BASE%\windows\install\logback.xml^
                    org.springframework.boot.loader.PropertiesLauncher

if NOT errorlevel == 0 (
   @echo ThingsBoard DB installation failed!
   POPD
   exit /b %errorlevel%
   )
@echo ThingsBoard DB installed successfully!
