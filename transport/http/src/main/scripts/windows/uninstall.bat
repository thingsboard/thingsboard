@ECHO OFF

@ECHO Stopping ${pkg.name} ...
net stop ${pkg.name}

@ECHO Uninstalling ${pkg.name} ...
"%~dp0"${pkg.name}.exe uninstall

@ECHO DONE.