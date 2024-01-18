#!/bin/bash
#
# Copyright © 2016-2024 The Thingsboard Authors
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

echo "Building ThingsBoard protobuf-containing packages..."
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=3072" \
mvn clean compile -T4 --also-make --projects='
common/cluster-api,
common/edge-api,
common/transport/coap,
common/transport/mqtt,
common/transport/transport-api'
