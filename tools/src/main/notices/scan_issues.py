#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Says what the scan reported when ORT stops on it: the errors of each package, and what to do about them.

ORT stops on an error that ort-config/resolutions.yml does not accept, and its log is long. This reads the errors from
scan-result.json instead, leaves out the accepted ones, and prints them by package. It prints nothing and fails when it
finds none, so that the caller shows the end of the log instead.

Usage: scan_issues.py <scan-result.json> <resolutions.yml>
"""
import json
import re
import sys

SEVERE = ('ERROR', 'WARNING')


def main():
    result, resolutions = sys.argv[1:3]
    issues = unaccepted(issues_by_package(json.load(open(result))), accepted(resolutions))
    if not issues:
        return 1
    print(message(issues))
    return 0


def issues_by_package(result):
    """(package, message) of every severe issue of the scan: of finding the source of a package, and of scanning it."""
    scanner = result.get('scanner') or {}
    found, packages = [], {}
    for provenance in scanner.get('provenances', []):
        for key in ('package_provenance_resolution_issue', 'nested_provenance_resolution_issue'):
            if provenance.get(key):
                found.append((provenance['id'], provenance[key]))
        if provenance.get('package_provenance'):
            packages.setdefault(json.dumps(provenance['package_provenance'], sort_keys=True), []).append(provenance['id'])
    for scan in scanner.get('scan_results', []):
        for issue in scan.get('summary', {}).get('issues', []):
            for package in packages.get(json.dumps(scan['provenance'], sort_keys=True), ['(unknown package)']):
                found.append((package, issue))
    return [(package, issue['message']) for package, issue in found if issue.get('severity', 'ERROR') in SEVERE]


def accepted(path):
    """The patterns of the messages that resolutions.yml accepts. The file is ours, with one "- message:" line per
    resolution, so it is read without a YAML library."""
    patterns = []
    for line in open(path):
        match = re.match(r'\s*- message: "(.*)"\s*$', line)
        if match:
            patterns.append(re.compile(match.group(1).replace('\\"', '"').replace('\\\\', '\\'), re.DOTALL))
    return patterns


def unaccepted(issues, patterns):
    return [(package, text) for package, text in issues if not any(p.fullmatch(text) for p in patterns)]


def message(issues):
    return ('Third-party notices: the scan of these packages reported errors, so their notices may be incomplete.\n'
            + ''.join(f'  {package}: ' + text.strip().replace('\n', '\n    ') + '\n' for package, text in sorted(set(issues)))
            + 'To fix:\n'
            '  - a download error is often passing: run the build again;\n'
            '  - if the error does not matter for the notices (for example a generated file that is also scanned from its\n'
            '    sources), add it to tools/src/main/notices/ort-config/resolutions.yml with the reason.')


if __name__ == '__main__':
    sys.exit(main())
