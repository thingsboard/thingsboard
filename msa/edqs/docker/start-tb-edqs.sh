#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

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

echo "Starting '${project.name}' ..."

cd ${pkg.installFolder}/bin

exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.edqs.ThingsboardEdqsApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dlogging.config=${LOGGING_CONFIG} \
                    org.springframework.boot.loader.launch.PropertiesLauncher
