#!/bin/bash
#
# Copyright © 2016-2022 The Thingsboard Authors
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

mkdir -p tb-node/log && sudo chown -R 799:799 tb-node/log

mkdir -p tb-transports/coap/log && sudo chown -R 799:799 tb-transports/coap/log

mkdir -p tb-transports/lwm2m/log && sudo chown -R 799:799 tb-transports/lwm2m/log

mkdir -p tb-transports/http/log && sudo chown -R 799:799 tb-transports/http/log

mkdir -p tb-transports/mqtt/log && sudo chown -R 799:799 tb-transports/mqtt/log

mkdir -p tb-transports/snmp/log && sudo chown -R 799:799 tb-transports/snmp/log

mkdir -p tb-vc-executor/log && sudo chown -R 799:799 tb-vc-executor/log

mkdir -p tb-node/postgres && sudo chown -R 999:999 tb-node/postgres

mkdir -p tb-node/cassandra && sudo chown -R 999:999 tb-node/cassandra

source .env
CACHE="${CACHE:-redis}"
case $CACHE in
    redis)
    mkdir -p tb-node/redis-data && sudo chown -R 1001:1001 tb-node/redis-data
    ;;
    redis-cluster)
    mkdir -p tb-node/redis-cluster-data-0 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-0
    mkdir -p tb-node/redis-cluster-data-1 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-1
    mkdir -p tb-node/redis-cluster-data-2 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-2
    mkdir -p tb-node/redis-cluster-data-3 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-3
    mkdir -p tb-node/redis-cluster-data-4 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-4
    mkdir -p tb-node/redis-cluster-data-5 && sudo chown -R 1001:1001 tb-node/redis-cluster-data-5
    ;;
    *)
    echo "Unknown CACHE value specified in the .env file: '${CACHE}'. Should be either 'redis' or 'redis-cluster'." >&2
    exit 1
esac