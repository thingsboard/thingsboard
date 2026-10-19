#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

echo "Building ThingsBoard protobuf-containing packages..."
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=3072" \
mvn clean compile -T4 --also-make --projects='
common/cluster-api,
common/edge-api,
common/transport/coap,
common/transport/mqtt,
common/transport/transport-api'
