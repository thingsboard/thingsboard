#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

set -euo pipefail

usage() {
    echo "Checks SPDX license headers against CE provenance."
    echo "usage: $0 <ce-ref> [--fix] [--ce-remote <url>] [--output target/...]"
    echo "    ce-ref       CE branch, tag or commit matching this PE branch, e.g. lts-4.2;"
    echo "                 fetched from the CE repository on every run"
    echo "    --fix        restamp files whose expected header the check determined"
    echo "    --ce-remote  CE repository to fetch from (default: https://github.com/thingsboard/thingsboard.git)"
}

if [[ $# -eq 0 || "$1" == "-h" || "$1" == "--help" || "$1" == -* ]]; then
    usage
    [[ $# -gt 0 && ( "$1" == "-h" || "$1" == "--help" ) ]] && exit 0
    exit 2
fi

ce_ref="$1"
shift

cd "$(git rev-parse --show-toplevel)"
exec python3 tools/src/main/python/license-headers/check_license_headers.py check --ce-ref "$ce_ref" "$@"
