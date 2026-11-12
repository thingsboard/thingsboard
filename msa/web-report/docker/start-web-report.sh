#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

echo "Starting '${project.name}' ..."

CONF_FOLDER="${pkg.installFolder}/conf"

configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

cd ${pkg.installFolder}

export CHROME_EXECUTABLE=$(node -e "console.log(require('playwright-chromium').chromium.executablePath());")

# This will forward this PID 1 to the node.js and forward SIGTERM for graceful shutdown as well
exec node server.js
