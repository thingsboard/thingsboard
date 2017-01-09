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


until nmap db -p 9042 | grep "9042/tcp open"
do
  echo "Wait for Cassandra..."
  sleep 10
done

if [ "$SKIP_SCHEMA_CREATION" == "false" ]; then
    echo "Creating 'Thingsboard' keyspace..."
    cqlsh db -f /root/schema.cql
    if [ "$?" -eq 0 ]; then
        echo "'Thingsboard' keyspace was successfully created!"
    else
        echo "There were issues while creating 'Thingsboard' keyspace!"
    fi
fi

if [ "$SKIP_SYSTEM_DATA" == "false" ]; then
    echo "Adding system data..."
    cqlsh db -f /root/system-data.cql
    if [ "$?" -eq 0 ]; then
        echo "System data was successfully added!"
    else
        echo "There were issues while adding System data!"
    fi
fi

if [ "$SKIP_DEMO_DATA" == "false" ]; then
    echo "Adding demo data..."
    cqlsh db -f /root/demo-data.cql
    if [ "$?" -eq 0 ]; then
        echo "Demo data was successfully added!"
    else
        echo "There were issues while adding Demo data!"
    fi
fi
