#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Shares the results of the scans between machines, as one archive on a file server.

A scan of the source of a package takes minutes, and ORT keeps its result in the directory `scanner` of its home, so
that the same source is never scanned twice on one machine. The archive holds the files of that directory.

download   adds the files of the archive that this machine does not have yet. The archive is asked for with the ETag of
           the last download, so one that has not changed is not downloaded again. Without the archive the notices are
           still made: the packages are then scanned on this machine. After a failed download the archive is not asked
           for again for 10 minutes, so that the other modules of the build do not wait for the same failure.
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
import os
import shutil
import sys
import tarfile
import tempfile
import time
import urllib.error
import urllib.request

DIRECTORY = 'scanner'
# Seconds that a connection or a read may take: a download waits that long for a server that does not answer
TIMEOUT = 10
# Seconds after a failed download in which the archive is not asked for again
RETRY_AFTER = 600


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
        (download if args.action == 'download' else upload)(args.home, args.url)


def download(home, url):
    etag_file, failed_file = os.path.join(home, 'scan-cache.etag'), os.path.join(home, 'scan-cache.failed')
    if os.path.exists(failed_file) and time.time() - os.path.getmtime(failed_file) < RETRY_AFTER:
        print(f'Scan cache: {url} could not be read a few minutes ago, so it is not asked for now', file=sys.stderr)
        return
    # The ETag says what was downloaded, which is of no use once the files are gone
    known = read(etag_file) if os.path.isdir(os.path.join(home, DIRECTORY)) else None
    try:
        archive, etag = fetch(url, known)
        if not archive:
            print(f'Scan cache: nothing new in {url}')
            return
        with archive:
            added, stored = unpack(archive, home)
    except (OSError, tarfile.TarError) as error:
        open(failed_file, 'w').close()
        print(f'Scan cache: {url} cannot be read ({error}). The notices are made without it: each package that was '
              'not scanned on this machine before is scanned now, which takes minutes for each.', file=sys.stderr)
        return
    with contextlib.suppress(FileNotFoundError):
        os.remove(failed_file)
    if etag:
        with open(etag_file, 'w') as file:
            file.write(etag)
    print(f'Scan cache: {added} of the {len(stored)} files of {url} are new on this machine')


def upload(home, url):
    try:
        archive, _ = fetch(url, None)
        with archive:
            _, stored = unpack(archive, home)
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise SystemExit(f'Scan cache: {url} cannot be read ({error}), so nothing is uploaded')
        stored = set()  # the first upload
    root = os.path.join(home, DIRECTORY)
    local = sorted(os.path.relpath(os.path.join(directory, name), home)
                   for directory, _, names in os.walk(root) for name in names)
    new = [name for name in local if name not in stored]
    if not new:
        print(f'Scan cache: this machine has nothing that {url} does not')
        return

    with tempfile.TemporaryFile() as packed:
        pack(home, local, packed)
        size = packed.tell()
        packed.seek(0)
        request = urllib.request.Request(url, data=packed, method='PUT', headers={
            'Content-Length': str(size), 'Content-Type': 'application/gzip', 'User-Agent': 'scan-cache', **authorization()})
        try:
            urllib.request.urlopen(request, timeout=60).close()
        except OSError as error:
            if isinstance(error, urllib.error.HTTPError):
                error.close()
            raise SystemExit(f'Scan cache: the upload to {url} failed ({error}). It needs the account that may write '
                             'there, in NOTICES_SCAN_CACHE_USER and NOTICES_SCAN_CACHE_PASSWORD.')
    print(f'Scan cache: uploaded {len(new)} new files to {url}, which now has {len(local)} ({size / 1e6:.1f} MB)')


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
            return None, None
        raise


def unpack(archive, home):
    """Adds the files of the archive that are not in the home of ORT yet. Gives how many those were, and the names of
    all files of the archive."""
    added, stored = 0, set()
    with tarfile.open(fileobj=archive, mode='r:gz') as tar:
        for member in tar:
            parts = member.name.split('/')
            if not member.isfile() or parts[0] != DIRECTORY or any(part in ('', '.', '..') for part in parts):
                raise tarfile.TarError(f'{member.name} is no file of {DIRECTORY}/')
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
    with tarfile.open(fileobj=out, mode='w:gz') as tar:
        for name in names:
            tar.add(os.path.join(home, name), arcname=name)


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
    main()
