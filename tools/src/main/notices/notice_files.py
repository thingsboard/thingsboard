#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Collects the NOTICE files of the third-party packages of an artifact and lays them out for the notices: each
distinct text once, under the packages that carry it.

A Maven package is read from the jar that the artifact contains: the sources jar that ORT scans often has no NOTICE
file, or another one. The jar is taken from the local Maven repository, or else downloaded, and must have the SHA-256
that the SBOM gives. An npm package is read from the license files that ORT kept of its scanned source, which is the
tarball it was installed from.

Usage: notice_files.py --jars <file of sbom_to_ort.py> --scan-result <scan-result.json> --archive <directory>
       --maven-repository <directory> --downloads <directory> --out <file>
"""
import argparse
import hashlib
import io
import json
import lzma
import os
import re
import sys
import zipfile

from sbom_to_ort import MAVEN_REPOSITORIES, from_repositories, maven_path

# NOTICE, NOTICE.txt, notice.md, NOTICE_binary.txt; not NoticeReference.class
NOTICE = re.compile(r'notice([_-][^.]*)?((\.[^.]*)*\.(txt|md))?', re.IGNORECASE)
SEPARATOR = '-' * 78


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jars', required=True, help='the jars of the Maven packages, as sbom_to_ort.py lists them')
    parser.add_argument('--scan-result', required=True, help='the scan result of ORT, which names the source of each package')
    parser.add_argument('--archive', required=True, help='the directory where ORT keeps the license files of scanned sources')
    parser.add_argument('--maven-repository', required=True, help='the local Maven repository')
    parser.add_argument('--downloads', required=True, help='directory that keeps the jars that had to be downloaded')
    parser.add_argument('--out', required=True)
    args = parser.parse_args()

    found = {}  # text -> set of (package, path)
    for jar in json.load(open(args.jars)):
        with zipfile.ZipFile(jar_file(jar, args)) as archive:
            collect(found, coordinates(jar['group'], jar['name'], jar['version']), archive)
    for provenance in json.load(open(args.scan_result))['scanner']['provenances']:
        kind, namespace, name, version = provenance['id'].split(':', 3)
        archive = kept_files(provenance, args.archive) if kind == 'NPM' else None
        if archive:
            collect(found, coordinates(namespace, name, version), archive)

    with open(args.out, 'w') as out:
        out.write(layout(found))
    print(f'{len(found)} NOTICE texts of {len({package for carriers in found.values() for package, _ in carriers})} packages')
    for carriers, _ in ordered(found):
        for package, path in carriers:
            print(f'  {package} ({path})')


def coordinates(namespace, name, version):
    """A package as the notices name it: see coordinates in THIRD-PARTY-NOTICES.ftl."""
    return f"{namespace + ':' if namespace else ''}{name} {version}"


def is_notice(path):
    return bool(NOTICE.fullmatch(path.rsplit('/', 1)[-1]))


def collect(found, package, archive):
    for entry in archive.infolist():
        if is_notice(entry.filename):
            text = clean(archive.read(entry))
            if text:
                found.setdefault(text, set()).add((package, entry.filename))


def clean(content):
    """The text as it is, without what does not show: line ends of another system, spaces at the end of lines, and
    empty lines before and after."""
    try:
        text = content.decode('utf-8-sig')
    except UnicodeDecodeError:
        text = content.decode('latin-1')
    return '\n'.join(line.rstrip() for line in text.replace('\r\n', '\n').replace('\r', '\n').split('\n')).strip('\n')


def ordered(found):
    """The packages and files that carry each text, in order, and the text: ordered by the first package of each."""
    return sorted((sorted(carriers), text) for text, carriers in found.items())


def layout(found):
    """The section as THIRD-PARTY-NOTICES.ftl includes it, under the heading and the sentence that describes it."""
    blocks = []
    for carriers, text in ordered(found):
        # Indented, so that a line of a notice cannot be taken for a line of the notices
        indented = '\n'.join('    ' + line if line else '' for line in text.split('\n'))
        blocks.append('\n'.join([SEPARATOR] + [f'{package} ({path})' for package, path in carriers]) + f'\n\n{indented}\n')
    return '\n'.join(blocks)


def jar_file(jar, args):
    """The jar with the SHA-256 of the SBOM: from the local repository, from an earlier download, or downloaded now."""
    path = maven_path(jar['group'], jar['name'], jar['version'], jar['classifier'], jar['type'])
    downloaded = os.path.join(args.downloads, path)
    for candidate in (os.path.join(args.maven_repository, path), downloaded):
        if os.path.exists(candidate) and sha256(candidate) == jar['sha256']:
            return candidate
    for _, content in from_repositories(path, timeout=120):
        if hashlib.sha256(content).hexdigest() == jar['sha256']:
            os.makedirs(os.path.dirname(downloaded), exist_ok=True)
            temporary = f'{downloaded}.{os.getpid()}'
            open(temporary, 'wb').write(content)
            os.replace(temporary, downloaded)
            return downloaded
    raise SystemExit(f"{path} with the SHA-256 {jar['sha256']} of the SBOM is neither in {args.maven_repository} nor in "
                     + ' or '.join(MAVEN_REPOSITORIES) + ': its NOTICE file cannot be read')


def sha256(path):
    with open(path, 'rb') as file:
        return hashlib.file_digest(file, 'sha256').hexdigest()


def kept_files(provenance, directory):
    """The license files that ORT kept of the scanned source of a package, or nothing for a package without a source.
    ORT 98 keeps them for every scanned source, in a directory named by the SHA-1 of what identifies the source."""
    source = provenance.get('package_provenance') or {}
    if 'source_artifact' in source:
        key = source['source_artifact']['url'] + source['source_artifact']['hash']['value']
    elif 'vcs_info' in source:
        key = source['vcs_info']['type'] + source['vcs_info']['url'] + source['resolved_revision']
    else:
        return None
    path = os.path.join(directory, hashlib.sha1(key.encode()).hexdigest(), 'archive.zip.xz')
    if not os.path.exists(path):
        raise SystemExit(f"{path}, where ORT 98 keeps the license files of {provenance['id']}, does not exist: "
                         'another version of ORT may keep them elsewhere, see kept_files() in notice_files.py')
    return zipfile.ZipFile(io.BytesIO(lzma.open(path).read()))


if __name__ == '__main__':
    sys.exit(main())
