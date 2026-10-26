#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
# SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
#

# Set TB_LICENSE_SECRET in the configuration file sourced below so the upgrade can verify, before it
# changes anything, that this instance does not hold more devices than the license covers. Without it that
# check is skipped and the upgrade proceeds.

for i in "$@"
do
case $i in
    --fromVersion=*)
    FROM_VERSION="${i#*=}"
    shift
    ;;
    *)
            # unknown option
    ;;
esac
done

fromVersion="${FROM_VERSION// }"

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

# 4.4 folded the twilio rule nodes into the boot jar; the leftover extension jar is an unrelocated
# fat jar that shadows core libraries JVM-wide through LOADER_PATH. Repeated here for installs that
# are upgraded without the deb/rpm control scripts.
rm -f ${pkg.installFolder}/extensions/rule-node-twilio-sms*.jar

su -s /bin/sh -c "java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dinstall.data_dir=${installDir} \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=${pkg.installFolder}/bin/install/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher" "$run_user"

# Captured immediately: by the end of the if/else below $? is the status of the echo that ran, so exiting on
# it would report success even when the upgrade failed - including a deliberate refusal by the pre-upgrade
# license capacity check, which any wrapper driving this script has to be able to see.
upgradeStatus=$?

if [ $upgradeStatus -ne 0 ]; then
    echo "ThingsBoard upgrade failed!"
else
    echo "ThingsBoard upgraded successfully!"
fi

exit $upgradeStatus
