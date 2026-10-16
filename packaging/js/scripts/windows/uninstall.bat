@REM
@REM SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
@REM SPDX-License-Identifier: Apache-2.0
@REM

@ECHO OFF

@ECHO Stopping ${pkg.name} ...
net stop ${pkg.name}

@ECHO Uninstalling ${pkg.name} ...
"%~dp0"${pkg.name}.exe uninstall
SET UNINSTALL_ERROR=%ERRORLEVEL%
IF NOT "%UNINSTALL_ERROR%"=="0" GOTO UNINSTALL_FAILED

@ECHO DONE.
GOTO :EOF

:UNINSTALL_FAILED
@ECHO ERROR: Failed to uninstall ${pkg.name} service (exit code %UNINSTALL_ERROR%).
EXIT /B %UNINSTALL_ERROR%
