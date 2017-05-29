#!/bin/bash
#
# Copyright © 2016-2017 The Thingsboard Authors
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


dpkg -i /root/thingsboard.deb

reachable=0
while [ $reachable -eq 0 ];
do
  echo "$TB_CASSANDRA_SCHEMA_URL container is still in progress. waiting until it completed..."
  sleep 3
  ping -q -c 1 $TB_CASSANDRA_SCHEMA_URL > /dev/null 2>&1
  if [ "$?" -ne 0 ];
  then
    echo "$TB_CASSANDRA_SCHEMA_URL container completed!"
    reachable=1
  fi
done

# Copying env variables into conf files
printenv | awk -F "=" '{print "export " $1 "='\''" $2 "'\''"}' >> /usr/share/thingsboard/conf/thingsboard.conf

cat /usr/share/thingsboard/conf/thingsboard.conf

echo "Starting 'Thingsboard' service..."
service thingsboard start

# Wait until log file is created
sleep 10
tail -f /var/log/thingsboard/thingsboard.log
