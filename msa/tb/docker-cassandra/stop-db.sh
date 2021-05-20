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

CASSANDRA_PID=$(ps aux | grep '[c]assandra' | awk '{print $2}')

echo "Stopping cassandra (pid ${CASSANDRA_PID})."
kill -SIGTERM ${CASSANDRA_PID}

PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)
echo "Stopping postgres."
${PG_CTL} stop

while [ -e /proc/${CASSANDRA_PID} ]
do
    echo "Waiting for cassandra to stop."
    sleep 2
done
echo "Cassandra was stopped."
