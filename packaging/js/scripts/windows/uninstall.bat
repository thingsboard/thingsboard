@REM
@REM SPDX-FileCopyrightText: Copyright The Thingsboard Authors
@REM SPDX-License-Identifier: Apache-2.0
@REM

@ECHO OFF

@ECHO Stopping ${pkg.name} ...
net stop ${pkg.name}

@ECHO Uninstalling ${pkg.name} ...
"%~dp0"${pkg.name}.exe uninstall

@ECHO DONE.