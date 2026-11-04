#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

CONF_FOLDER="/config"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

export LOADER_PATH=/config,${LOADER_PATH}

echo "Starting '${project.name}' ..."

cd ${pkg.installFolder}/bin

exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.integration.ThingsboardIntegrationExecutorApplication \
                    -Dlogging.config=${CONF_FOLDER}/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher
