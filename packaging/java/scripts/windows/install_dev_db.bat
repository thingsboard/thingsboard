@REM
@REM SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
@REM SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
@REM SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

SET BASE=${project.basedir}\target
SET LOADER_PATH=%BASE%\conf,%BASE%\extensions

SET jarfile=%BASE%\${pkg.name}-${project.version}-boot.jar
SET installDir=%BASE%\data

IF "%SQL_DATA_FOLDER%" == "" (	
	SET SQL_DATA_FOLDER=/tmp
)

java -cp %jarfile% -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication^
                    -Dinstall.data_dir=%installDir%^
                    -Dspring.jpa.hibernate.ddl-auto=none^
                    -Dinstall.upgrade=false^
                    -Dlogging.config=%BASE%\windows\install\logback.xml^
                    org.springframework.boot.loader.launch.PropertiesLauncher

if errorlevel 1 (
   @echo ThingsBoard DB installation failed!
   POPD
   exit /b %errorlevel%
   )
@echo ThingsBoard DB installed successfully!
