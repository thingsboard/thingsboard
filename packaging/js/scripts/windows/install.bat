@REM
@REM SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
@REM SPDX-License-Identifier: Apache-2.0
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Installing ${pkg.name} ...

SET BASE=%~dp0

IF NOT "${pkg.name}"=="tb-web-report" GOTO INSTALL_SERVICE
SET "CHROME_PATH=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
IF NOT EXIST "%CHROME_PATH%" ECHO WARNING: Google Chrome not found at "%CHROME_PATH%". Install Google Chrome for all users, or set CHROME_EXECUTABLE in ${pkg.name}.xml.

:INSTALL_SERVICE
"%BASE%"${pkg.name}.exe install
SET INSTALL_ERROR=%ERRORLEVEL%
IF NOT "%INSTALL_ERROR%"=="0" GOTO INSTALL_FAILED

@ECHO ${pkg.name} installed successfully!

GOTO END

:INSTALL_FAILED
@ECHO ERROR: Failed to install ${pkg.name} service (exit code %INSTALL_ERROR%).
EXIT /B %INSTALL_ERROR%

:END
