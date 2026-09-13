#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

CONF_FOLDER=${pkg.installFolder}/conf
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

echo "Starting '${project.name}' ..."

cd ${pkg.installFolder}/bin

exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.monitoring.ThingsboardMonitoringApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dlogging.config=$CONF_FOLDER/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher
