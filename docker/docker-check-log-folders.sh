#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

set -e
source compose-utils.sh
if checkFolders "$@" ; then
    echo "------"
    echo "All checks have passed"
else
    CHECK_EXIT_CODE=$?
    echo "------"
    echo "Some checks did not pass - check the output"
    exit $CHECK_EXIT_CODE
fi