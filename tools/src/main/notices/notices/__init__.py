#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""ThingsBoard third-party notices generator."""
import http.client
import os
import tempfile
import urllib.error
import urllib.request

ROOT = os.path.realpath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', '..'))
NOTICES_DIR = os.path.realpath(os.path.join(os.path.dirname(__file__), '..'))
BUILD_PROBLEM = 'This is a problem of the build or of these tools, not something to decide in the files of the notices.'


def stop(text):
    """Consistent fail-closed exit message."""
    return SystemExit(f'Third-party notices: {text}')


def ort_home():
    """Legacy ORT home directory (for one-shot cache migration and warm-machine seeding)."""
    return os.path.expanduser(os.environ.get('NOTICES_ORT_HOME') or '~/.ort')


def write_atomically(path, data):
    """Writes bytes to path in one atomic rename (thread- and multi-process-safe)."""
    parent = os.path.dirname(os.path.abspath(path))
    os.makedirs(parent, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix='.tmp-', dir=parent)
    try:
        with os.fdopen(fd, 'wb') as file:
            file.write(data)
        os.chmod(temporary, 0o644)
        os.replace(temporary, path)
    except BaseException:
        if os.path.exists(temporary):
            os.remove(temporary)
        raise


def http_get(url, timeout=30):
    """Performs an HTTP GET with our standard User-Agent and returns response bytes."""
    req = urllib.request.Request(url, headers={'User-Agent': 'tb-notices'})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return resp.read()


def normalize_text(text):
    """Normalizes line endings and strips trailing line whitespace and outer blank lines."""
    return '\n'.join(line.rstrip() for line in text.replace('\r\n', '\n').replace('\r', '\n').split('\n')).strip('\n')


def clean_text(content):
    """Decodes UTF-8 (falling back to latin-1) and normalizes line endings and whitespace."""
    try:
        text = content.decode('utf-8-sig')
    except UnicodeDecodeError:
        text = content.decode('latin-1')
    return normalize_text(text)


def format_coordinates(group, name, version=''):
    """Formats a component's display coordinates: `group:name version` (or `name version`)."""
    base = f'{group}:{name}' if group else name
    return f'{base} {version}'.rstrip()


HTTP_ERRORS = (urllib.error.URLError, http.client.HTTPException, OSError)

_TEAMCITY_ESCAPE_TABLE = str.maketrans({
    '|': '||',
    "'": "|'",
    '\n': '|n',
    '\r': '|r',
    '\u0085': '|x',
    '\u2028': '|l',
    '\u2029': '|p',
    '[': '|[',
    ']': '|]',
})


def escape_teamcity(text):
    """Escapes a value for a TeamCity service message attribute."""
    return text.translate(_TEAMCITY_ESCAPE_TABLE)


def format_teamcity_problem(message, max_len=300):
    """Builds a concise one-line summary for TeamCity's buildProblem description.

    Preserves the headline and as many immediately following indented items (package/license lines)
    as fit within max_len so the failure reason and offending packages appear directly on the build
    summary and Slack alert.
    """
    lines = message.strip().splitlines()
    if not lines:
        return ''
    headline = lines[0].strip()
    items = []
    for line in lines[1:]:
        if line.startswith('  ') and line.strip():
            items.append(line.strip())
        else:
            break

    prefix = f'{headline} - '
    shown = []
    result = ''
    for idx, item in enumerate(items):
        remaining = len(items) - idx - 1
        suffix = f' (+{remaining} more)' if remaining else ''
        candidate = prefix + ', '.join([*shown, item]) + suffix
        if len(candidate) <= max_len:
            shown.append(item)
            result = candidate
            continue
        if not shown:
            budget = max_len - len(prefix) - len(suffix) - 3
            if budget >= 10:
                result = f'{prefix}{item[:budget].rstrip()}...{suffix}'
        break

    if result:
        return result
    if len(headline) <= max_len:
        return headline
    if max_len <= 3:
        return headline[:max(0, max_len)]
    return headline[:max_len - 3].rstrip() + '...'


def emit_teamcity_problem(message):
    """Emits a TeamCity buildProblem service message for a fail-closed stop."""
    summary = format_teamcity_problem(message)
    if summary:
        print(f"##teamcity[buildProblem description='{escape_teamcity(summary)}']", flush=True)


def emit_teamcity_warning(message):
    """Emits a non-fatal TeamCity warning service message."""
    summary = format_teamcity_problem(message)
    if summary:
        print(f"##teamcity[message text='{escape_teamcity(summary)}' status='WARNING']", flush=True)
