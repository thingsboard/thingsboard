#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Allows running the notices tool directly via `python3 tools/src/main/notices ...`."""
import os
import sys

if sys.version_info < (3, 11):
    ver = f'{sys.version_info.major}.{sys.version_info.minor}'
    msg = (
        f'Third-party notices: Python 3.11 or later is required (found Python {ver}). '
        'See docs/licensing-guide.md#checking-the-notices-locally.'
    )
    if os.environ.get('TEAMCITY_VERSION'):
        print(f"##teamcity[buildProblem description='{msg}']", flush=True)
    sys.exit(msg)

from notices import emit_teamcity_problem
from notices.cli import main

if __name__ == '__main__':
    try:
        sys.exit(main())
    except SystemExit as err:
        if isinstance(err.code, str) and err.code and os.environ.get('TEAMCITY_VERSION'):
            emit_teamcity_problem(err.code)
        raise
