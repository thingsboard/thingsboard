#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""ThingsBoard third-party notices generator."""
import os

ROOT = os.path.realpath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', '..'))
NOTICES_DIR = os.path.realpath(os.path.join(os.path.dirname(__file__), '..'))
BUILD_PROBLEM = 'This is a problem of the build or of these tools, not something to decide in the files of the notices.'


def stop(text):
    """Consistent fail-closed exit message."""
    return SystemExit(f'Third-party notices: {text}')


def write_atomically(path, data):
    """Writes bytes to path in one atomic rename so concurrent builds never observe a half-written file."""
    parent = os.path.dirname(os.path.abspath(path))
    os.makedirs(parent, exist_ok=True)
    temporary = f'{path}.{os.getpid()}.tmp'
    try:
        with open(temporary, 'wb') as file:
            file.write(data)
        os.replace(temporary, path)
    except BaseException:
        if os.path.exists(temporary):
            os.remove(temporary)
        raise
