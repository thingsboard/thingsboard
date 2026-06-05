#!/bin/bash
#
# Copyright © 2016-2026 The Thingsboard Authors
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


echo "Starting '${project.name}' ..."

CONF_FOLDER="${pkg.installFolder}/conf"

configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

cd ${pkg.installFolder}

# This will forward this PID 1 to the node.js and forward SIGTERM for graceful shutdown as well
exec node --no-compilation-cache server.js
