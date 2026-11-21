#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Allows running the notices tool directly via `python3 tools/src/main/notices ...`."""
import sys

if sys.version_info < (3, 11):
    ver = f'{sys.version_info.major}.{sys.version_info.minor}'
    sys.exit(
        f'Third-party notices: Python 3.11 or later is required (found Python {ver}). '
        'See docs/licensing-guide.md#checking-the-notices-locally.'
    )

from notices.cli import main

if __name__ == '__main__':
    sys.exit(main())
