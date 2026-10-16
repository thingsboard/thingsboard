#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
run_user=${pkg.user}

CONF_FOLDER="/config"
if [ -d "${CONF_FOLDER}" ]; then
  LOGGING_CONFIG="${CONF_FOLDER}/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
  export LOADER_PATH=${CONF_FOLDER},${LOADER_PATH}
else
  CONF_FOLDER="/usr/share/${pkg.name}/conf"
  LOGGING_CONFIG="/usr/share/${pkg.name}/conf/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
fi


cd ${pkg.installFolder}/bin

if [ "$INSTALL_TB" == "true" ]; then

    if [ "$LOAD_DEMO" == "true" ]; then
        loadDemo=true
    else
        loadDemo=false
    fi

    echo "Starting ThingsBoard installation ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                        -Dinstall.load_demo=${loadDemo} \
                        -Dinstall.upgrade=false \
                        -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                        org.springframework.boot.loader.launch.PropertiesLauncher

elif [ "$UPGRADE_TB" == "true" ]; then

    echo "Starting ThingsBoard upgrade ..."


    fromVersion="${FROM_VERSION// }"

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher

else

    echo "Starting '${project.name}' ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardServerApplication \
                        -Dlogging.config=${LOGGING_CONFIG} \
                        org.springframework.boot.loader.launch.PropertiesLauncher

fi
