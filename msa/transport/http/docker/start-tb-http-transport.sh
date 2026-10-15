#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

CONF_FOLDER="/config"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

export LOADER_PATH=/config,${LOADER_PATH}

echo "Starting '${project.name}' ..."

cd ${pkg.installFolder}/bin

exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.http.ThingsboardHttpTransportApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dlogging.config=/config/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher
