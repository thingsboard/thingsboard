#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
# SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
#

# --loadDemo was retired: demo data is now loaded from the setup wizard, after the first system administrator
# is created. Matched only to tell an operator following an older runbook why nothing happened; every other
# argument is ignored, exactly as before.
for i in "$@"
do
case $i in
    --loadDemo)
    echo "--loadDemo is no longer supported; demo data is loaded from the setup wizard."
    shift
    ;;
    *)
            # unknown option
    ;;
esac
done

CONF_FOLDER=${pkg.installFolder}/conf
configfile=${pkg.name}.conf
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
installDir=${pkg.installFolder}/data

source "${CONF_FOLDER}/${configfile}"

require_java_25() {
    local javaVersion javaMajor
    javaVersion=$(JAVA_TOOL_OPTIONS= JDK_JAVA_OPTIONS= "$1" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n 1)
    javaMajor=$(echo "$javaVersion" | sed -e 's/^1\.//' -e 's/^\([0-9]*\).*/\1/')
    if [ -z "$javaMajor" ] || [ "$javaMajor" -lt 25 ]; then
        echo "${pkg.name} requires Java 25 or later, but '$1' is ${javaVersion:-not a working Java}." >&2
        echo "Select Java 25 with 'update-alternatives --config java' (deb) or 'alternatives --config java' (rpm), or set JAVA_HOME in $CONF_FOLDER/${pkg.name}.conf." >&2
        exit 78
    fi
}

require_java_25 java

run_user=${pkg.user}

su -s /bin/sh -c "java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dinstall.data_dir=${installDir} \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=false \
                    -Dlogging.config=${pkg.installFolder}/bin/install/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher" "$run_user"

# Captured immediately: by the end of the if/else below $? is the status of the echo that ran, so exiting on
# it would report success even when the installation failed.
installStatus=$?

if [ $installStatus -ne 0 ]; then
    echo "ThingsBoard installation failed!"
else
    echo "ThingsBoard installed successfully!"
fi

exit $installStatus
