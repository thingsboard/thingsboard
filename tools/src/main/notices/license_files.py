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

A file kept in this repository can cover the builds of a component for several platforms, with sections for one of
them at its end, such as the credits of Chromium: each starts with the line "======== Only in the <platform> build
========" and runs to the next one. The notices leave out the sections of the platforms whose components they do not
have, as the os qualifier of the purl of each gives it, in any case: the notices of the Linux packages have no Windows
section, and those of the whole product have the file once, as it is.

Usage: license_files.py --list <file of sbom_to_ort.py> --downloads <directory> --out <file>
"""
import argparse
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.request

from notice_files import clean, coordinates, layout, ordered
from sbom_to_ort import ROOT, stop, write_atomically

SECTION = re.compile(r'^======== Only in the (.+) build ========$')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--list', required=True,
                        help='the components and the addresses of their license files, as sbom_to_ort.py lists them')
    parser.add_argument('--downloads', required=True, help='directory that keeps the files that were downloaded')
    parser.add_argument('--out', required=True)
    args = parser.parse_args()

    entries = json.load(open(args.list, encoding='utf-8'))
    platforms = platforms_of(entries)
    found = {}  # text -> set of (component, address or path of its file)
    texts = {}  # path of a file kept here -> its text for the platforms of its components
    for entry in entries:
        component = coordinates(entry['group'], entry['name'], entry['version']).rstrip()
        if 'path' in entry:
            where, fix = entry['path'], 'Check the file that tools/src/main/notices/license-files-here.json gives.'
            if where not in texts:
                texts[where] = for_platforms(clean(kept(os.path.join(ROOT, where), component)), platforms[where], where)
            text = texts[where]
        else:
            where, fix = entry['url'], ('Check the address in the external reference of the type license of the '
                                        'component, in the pom or the SBOM that declares it.')
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


def platforms_of(entries):
    """The files kept in this repository, each with the platforms of the components that have it, or None for all."""
    platforms = {}
    for entry in entries:
        if 'path' in entry:
            taken = platforms.get(entry['path'], set())
            platforms[entry['path']] = taken | {entry['platform']} if 'platform' in entry and taken is not None else None
    return platforms


def for_platforms(text, platforms, path):
    """The text without its sections for other platforms than these, or all of it for None. A platform without a section
    in a file that has sections is a mistake that would leave its section out of its notices, without a word."""
    if platforms is None:
        return text
    lines, sections, keep = [], set(), True
    for line in text.split('\n'):
        section = SECTION.match(line)
        if section:
            sections.add(section[1])
            keep = section[1].lower() in platforms
        if keep:
            lines.append(line)
    unknown = sorted(platforms - {section.lower() for section in sections}) if sections else []
    if unknown:
        raise stop(f'{path} has no section for the platform {", ".join(unknown)}, which the os qualifier of the purl of '
                   f'its component gives: its sections are for {", ".join(sorted(sections))}. Fix the qualifier in the pom '
                   'that declares the component, or the sections of the file.')
    return '\n'.join(lines).strip('\n')


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
                   'wrong, fix it in the external reference of the type license of the component, in the pom or the '
                   'SBOM that declares it.')
    write_atomically(path, data)
    return data


if __name__ == '__main__':
    sys.exit(main())
