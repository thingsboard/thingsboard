#!/bin/bash
#
# Copyright © 2016-2021 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

start-db.sh

CONF_FOLDER="${pkg.installFolder}/conf"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
firstlaunch=${DATA_FOLDER}/.firstlaunch

source "${CONF_FOLDER}/${configfile}"

if [ ! -f ${firstlaunch} ]; then
    install-tb.sh --loadDemo
    touch ${firstlaunch}
fi

echo "Starting ThingsBoard ..."

java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardServerApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dlogging.config=${CONF_FOLDER}/logback.xml \
                    org.springframework.boot.loader.PropertiesLauncher

stop-db.sh