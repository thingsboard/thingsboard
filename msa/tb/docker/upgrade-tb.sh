#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

start-db.sh

CONF_FOLDER="${pkg.installFolder}/conf"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

echo "Starting ThingsBoard upgrade ..."

java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                -Dspring.jpa.hibernate.ddl-auto=none \
                -Dinstall.upgrade=true \
                -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                org.springframework.boot.loader.launch.PropertiesLauncher

stop-db.sh