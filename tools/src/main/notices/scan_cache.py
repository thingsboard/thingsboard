#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Shares the results of the scans between machines, as one archive on a file server.

A scan of the source of a package takes minutes, and ORT keeps its result in the directory `scanner` of its home, so
that the same source is never scanned twice on one machine. The archive holds the files of that directory.

download   adds the files of the archive that this machine does not have yet. The archive is asked for with the ETag of
           the last download, so one that has not changed is not downloaded again. Without the archive the notices are
           still made: the packages are then scanned on this machine.
upload     puts the files of this machine and of the archive into a new archive, when this machine has files that the
           archive does not. Needs the account that may write, in NOTICES_SCAN_CACHE_USER and
           NOTICES_SCAN_CACHE_PASSWORD. Two uploads at the same time are not kept apart: the scans of the earlier one
           can be lost, and are then made again by whoever needs them next.

A file that both have is never replaced: other runs of ORT may be reading it.

Usage: scan_cache.py download|upload --home <home of ORT> --url <address of the archive>
"""
import argparse
import base64
import contextlib
import fcntl
import gzip
import os
import shutil
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request

DIRECTORY = 'scanner'
TIMEOUT = 60


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['download', 'upload'])
    parser.add_argument('--home', required=True, help='the directory where ORT keeps its data')
    parser.add_argument('--url', required=True, help='the address of the archive')
    args = parser.parse_args()

    os.makedirs(args.home, exist_ok=True)
    # The modules of one build make their notices at the same time: the first one downloads, the others find it done
    with open(os.path.join(args.home, 'scan-cache.lock'), 'w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        return download(args.home, args.url) if args.action == 'download' else upload(args.home, args.url)


def download(home, url):
    etag_file = os.path.join(home, 'scan-cache.etag')
    # The ETag says what was downloaded, which is of no use once the files are gone
    known = read(etag_file) if os.path.isdir(os.path.join(home, DIRECTORY)) else None
    try:
        archive, etag = fetch(url, known)
        if not archive:
            print(f'Scan cache: nothing new in {url}')
            return 0
        with archive:
            added, stored = unpack(archive, home, url)
    except (OSError, tarfile.TarError) as error:
        print(f'Scan cache: {url} cannot be read ({error}). The notices are made without it: each package that was '
              'not scanned on this machine before is scanned now, which takes minutes for each.', file=sys.stderr)
        return 0
    if etag:
        with open(etag_file, 'w') as file:
            file.write(etag)
    print(f'Scan cache: {added} of the {len(stored)} files of {url} are new on this machine')
    return 0


def upload(home, url):
    try:
        archive, _ = fetch(url, None)
        with archive:
            _, stored = unpack(archive, home, url)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise SystemExit(f'Scan cache: {url} cannot be read ({error}), so nothing is uploaded')
        stored = set()  # the first upload
    root = os.path.join(home, DIRECTORY)
    local = sorted(os.path.relpath(os.path.join(directory, name), home).replace(os.sep, '/')
                   for directory, _, names in os.walk(root) for name in names)
    new = [name for name in local if name not in stored]
    if not new:
        print(f'Scan cache: this machine has nothing that {url} does not')
        return 0

    with tempfile.TemporaryFile() as packed:
        pack(home, local, packed)
        size = packed.tell()
        packed.seek(0)
        request = urllib.request.Request(url, data=packed, method='PUT', headers={
            'Content-Length': str(size), 'Content-Type': 'application/gzip', 'User-Agent': 'scan-cache', **authorization()})
        try:
            urllib.request.urlopen(request, timeout=TIMEOUT).close()
        except OSError as error:
            if isinstance(error, urllib.error.HTTPError):
                error.close()
            raise SystemExit(f'Scan cache: the upload to {url} failed ({error}). It needs the account that may write '
                             'there, in NOTICES_SCAN_CACHE_USER and NOTICES_SCAN_CACHE_PASSWORD.')
    print(f'Scan cache: uploaded {len(new)} new files to {url}, which now has {len(local)} ({size / 1e6:.1f} MB)')
    return 0


def fetch(url, known_etag):
    """The archive as a file and its ETag, or no file when the archive still has the known ETag."""
    headers = {'User-Agent': 'scan-cache', **({'If-None-Match': known_etag} if known_etag else {})}
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=TIMEOUT) as response:
            archive = tempfile.TemporaryFile()
            shutil.copyfileobj(response, archive)
            archive.seek(0)
            return archive, response.headers.get('ETag')
    except urllib.error.HTTPError as error:
        error.close()
        if error.code == 304:
            return None, known_etag
        raise


def unpack(archive, home, url):
    """Adds the files of the archive that are not in the home of ORT yet. Gives how many those were, and the names of
    all files of the archive."""
    added, stored = 0, set()
    with tarfile.open(fileobj=archive, mode='r:gz') as tar:
        for member in tar:
            parts = member.name.split('/')
            if not member.isfile() or parts[0] != DIRECTORY or any(part in ('', '.', '..') for part in parts):
                raise tarfile.TarError(f'{member.name} in {url} is no file of {DIRECTORY}/')
            stored.add(member.name)
            path = os.path.join(home, *parts)
            if os.path.exists(path):
                continue
            os.makedirs(os.path.dirname(path), exist_ok=True)
            # Written beside its place and then moved there, so that ORT never reads half a file
            temporary = f'{path}.{os.getpid()}'
            try:
                with tar.extractfile(member) as source, open(temporary, 'wb') as target:
                    shutil.copyfileobj(source, target)
                os.replace(temporary, path)
            finally:
                with contextlib.suppress(FileNotFoundError):
                    os.remove(temporary)
            added += 1
    return added, stored


def pack(home, names, out):
    """Writes the files as a tar.gz. The same files give the same archive: no times, no owners."""
    with gzip.GzipFile(fileobj=out, mode='wb', mtime=0) as compressed, tarfile.open(fileobj=compressed, mode='w') as tar:
        for name in names:
            path = os.path.join(home, *name.split('/'))
            member = tarfile.TarInfo(name)
            member.size = os.path.getsize(path)
            with open(path, 'rb') as file:
                tar.addfile(member, file)


def authorization():
    user, password = os.environ.get('NOTICES_SCAN_CACHE_USER'), os.environ.get('NOTICES_SCAN_CACHE_PASSWORD')
    if not user or not password:
        return {}
    return {'Authorization': 'Basic ' + base64.b64encode(f'{user}:{password}'.encode()).decode()}


def read(path):
    if not os.path.exists(path):
        return None
    with open(path) as file:
        return file.read().strip() or None


if __name__ == '__main__':
    sys.exit(main())
