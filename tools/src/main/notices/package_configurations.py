#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Writes one ORT package configuration per scanned package: the paths of its source that are not part of the shipped
library.

Usage: package_configurations.py <scan-result.json> <output directory>
"""
import json
import os
import re
import shutil
import sys

# Paths of a source archive that the library is not built from, by the reason ORT knows them under
NOT_SHIPPED = {
    'TEST_OF': ['**/test/**', '**/tests/**', '**/__tests__/**', '**/testing/**', '**/spec/**', '**/specs/**',
                '**/fixtures/**', '**/benchmark/**', '**/benchmarks/**'],
    'DOCUMENTATION_OF': ['**/doc/**', '**/docs/**', '**/xdocs/**', '**/README*', '**/CHANGELOG*', '**/CHANGES*',
                         '**/HISTORY*', '**/CONTRIBUTING*'],
    'BUILD_TOOL_OF': ['**/pom.xml', '**/package.json', '**/composer.lock', '**/.github/**', '**/build/preamble.js',
                      '**/Gruntfile*'],
    'OTHER': ['**/*.map'],
    'EXAMPLE_OF': ['**/example/**', '**/examples/**', '**/sample/**', '**/samples/**', '**/demo/**', '**/demos/**'],
}


def main():
    scan_result, out = sys.argv[1], sys.argv[2]
    excludes = [{'pattern': pattern, 'reason': reason, 'comment': 'Not part of the shipped library.'}
                for reason, patterns in NOT_SHIPPED.items() for pattern in patterns]
    shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    written = 0
    for scanned in json.load(open(scan_result))['scanner']['provenances']:
        provenance = scanned.get('package_provenance')
        if not provenance:
            continue
        configuration = {'id': scanned['id']}
        if 'source_artifact' in provenance:
            configuration['source_artifact_url'] = provenance['source_artifact']['url']
        else:
            vcs = provenance['vcs_info']
            configuration['vcs'] = {'type': vcs['type'], 'url': vcs['url'], 'revision': provenance['resolved_revision']}
        configuration['path_excludes'] = excludes
        name = re.sub(r'[^A-Za-z0-9._-]', '_', scanned['id']) + '.json'
        json.dump(configuration, open(os.path.join(out, name), 'w'), indent=1)
        written += 1
    print(f'{written} package configurations written')


if __name__ == '__main__':
    sys.exit(main())
