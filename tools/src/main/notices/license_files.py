#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Puts the license files of the components that are not scanned into the notices, as they are: each text once, under
the components that it belongs to.

These are components that are neither Maven nor npm packages, whose SBOM gives the address of a license file that
their vendor publishes for them, such as the LICENSE of Node.js, which covers Node.js and the libraries it bundles,
or whose license file is kept in this repository, such as the one made from the headers of the LwM2M object models:
see sbom_to_ort.py and license-files-here.json. A file is downloaded once and kept, under the SHA-256 of its address:
an address names one version of the file, such as the LICENSE at the tag of a release.

Usage: license_files.py --list <file of sbom_to_ort.py> --downloads <directory> --out <file>
"""
import argparse
import hashlib
import json
import os
import sys
import urllib.error
import urllib.request

from notice_files import clean, coordinates, layout, ordered
from sbom_to_ort import ROOT, stop, write_atomically


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--list', required=True,
                        help='the components and the addresses of their license files, as sbom_to_ort.py lists them')
    parser.add_argument('--downloads', required=True, help='directory that keeps the files that were downloaded')
    parser.add_argument('--out', required=True)
    args = parser.parse_args()

    found = {}  # text -> set of (component, address or path of its file)
    for entry in json.load(open(args.list, encoding='utf-8')):
        component = coordinates(entry['group'], entry['name'], entry['version']).rstrip()
        if 'path' in entry:
            where, fix = entry['path'], 'Check the file that tools/src/main/notices/license-files-here.json gives.'
            text = clean(kept(os.path.join(ROOT, where), component))
        else:
            where, fix = entry['url'], ('Check the address in the external reference of the type license of the '
                                        'component, in the pom that declares it.')
            text = clean(content(where, component, args.downloads))
        if not text:
            raise stop(f'the license file of {component} at {where} is empty. {fix}')
        found.setdefault(text, set()).add((component, where))

    # ORT reads the file as UTF-8, whatever the locale of the build is
    with open(args.out, 'w', encoding='utf-8') as out:
        out.write(layout(found))
    components = {component for carriers in found.values() for component, _ in carriers}
    print(f'{len(found)} license files of {len(components)} components')
    for carriers, _ in ordered(found):
        for component, where in carriers:
            print(f'  {component} ({where})')


def kept(path, component):
    """The file kept in this repository."""
    try:
        return open(path, 'rb').read()
    except OSError as error:
        raise stop(f'the license file of {component} cannot be read: {error}. Fix its path in '
                   'tools/src/main/notices/license-files-here.json.')


def content(url, component, downloads):
    """The file at the address, from an earlier download, or downloaded now."""
    path = os.path.join(downloads, hashlib.sha256(url.encode()).hexdigest())
    if os.path.exists(path):
        return open(path, 'rb').read()
    try:
        request = urllib.request.Request(url, headers={'User-Agent': 'license-files'})
        with urllib.request.urlopen(request, timeout=120) as response:
            data = response.read()
    except (urllib.error.URLError, OSError) as error:
        raise stop(f'the license file of {component} cannot be downloaded from {url}: {error}. If the address is '
                   'wrong, fix it in the external reference of the type license of the component, in the pom that '
                   'declares it.')
    write_atomically(path, data)
    return data


if __name__ == '__main__':
    sys.exit(main())
