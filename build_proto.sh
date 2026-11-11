#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
# SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
#

echo "Building ThingsBoard protobuf-containing packages..."
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=3072" \
mvn clean compile -T4 --also-make --projects='
common/proto,
common/message,
common/agent-api,
common/cluster-api,
common/edge-api,
common/transport/coap,
common/transport/mqtt
'
