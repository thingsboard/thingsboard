#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Allows running the notices tool directly via `python3 tools/src/main/notices ...`."""
import sys

from notices.cli import main

if __name__ == '__main__':
    sys.exit(main())
