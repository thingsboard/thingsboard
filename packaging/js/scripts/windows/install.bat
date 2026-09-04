@REM
@REM SPDX-FileCopyrightText: Copyright The Thingsboard Authors
@REM SPDX-License-Identifier: Apache-2.0
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Installing ${pkg.name} ...

SET BASE=%~dp0

"%BASE%"${pkg.name}.exe install

@ECHO ${pkg.name} installed successfully!

GOTO END

:END
