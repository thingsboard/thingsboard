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

if [[ -z "${FROM_VERSION// }" ]]; then
    echo "--fromVersion parameter is invalid or unspecified!"
    echo "Usage: upgrade_dev_db.sh --fromVersion={VERSION}"
    exit 1
else
    fromVersion="${FROM_VERSION// }"
fi

BASE=${project.basedir}/target
CONF_FOLDER=${BASE}/conf
jarfile="${BASE}/thingsboard-${project.version}-boot.jar"
installDir=${BASE}/data
loadDemo=true


export JAVA_OPTS="$JAVA_OPTS -Dplatform=@pkg.platform@"
export LOADER_PATH=${BASE}/conf,${BASE}/extensions
export SQL_DATA_FOLDER=${SQL_DATA_FOLDER:-/tmp}


run_user="$USER"

sudo -u "$run_user" -s /bin/sh -c "java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dinstall.data_dir=${installDir} \
                    -Dinstall.load_demo=${loadDemo} \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=logback.xml \
                    org.springframework.boot.loader.PropertiesLauncher"

if [ $? -ne 0 ]; then
    echo "ThingsBoard DB installation failed!"
else
    echo "ThingsBoard DB installed successfully!"
fi

exit $?
