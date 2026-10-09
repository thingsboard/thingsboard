#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

set -euo pipefail

usage() {
    echo "Checks SPDX license headers against CE provenance."
    echo "usage: $0 [--fix] [--ce-ref <ref>] [--ce-remote <url>] [--output target/...]"
    echo "    --fix        restamp files whose expected header the check determined"
    echo "                 and remove redundant or orphaned curations"
    echo "    --ce-ref     CE branch, tag or commit that this branch integrates, fetched on every run;"
    echo "                 without it, CE is the history that does not contain the relicensing commit"
    echo "    --ce-remote  CE repository to fetch --ce-ref from (default: https://github.com/thingsboard/thingsboard.git)"
}

if [[ $# -gt 0 && ( "$1" == "-h" || "$1" == "--help" ) ]]; then
    usage
    exit 0
fi

cd "$(git rev-parse --show-toplevel)"
exec python3 tools/src/main/python/license-headers/check_license_headers.py check "$@"
