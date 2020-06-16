#!/bin/bash
#
# Copyright © 2016-2020 The Thingsboard Authors
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

CONF_FOLDER="/config"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
run_user=${pkg.user}

source "${CONF_FOLDER}/${configfile}"

export LOADER_PATH=/config,${LOADER_PATH}

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
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dinstall.upgrade=false \
                        -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                        org.springframework.boot.loader.PropertiesLauncher

elif [ "$UPGRADE_TB" == "true" ]; then

    echo "Starting ThingsBoard upgrade ..."

    if [[ -z "${FROM_VERSION// }" ]]; then
        echo "FROM_VERSION variable is invalid or unspecified!"
        exit 1
    else
        fromVersion="${FROM_VERSION// }"
    fi

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                    org.springframework.boot.loader.PropertiesLauncher

else

    echo "Starting '${project.name}' ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardServerApplication \
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dlogging.config=/config/logback.xml \
                        org.springframework.boot.loader.PropertiesLauncher

fi
