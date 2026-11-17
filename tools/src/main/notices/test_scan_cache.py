#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of scan_cache.py, against a file server that runs in the test.

Run: python3 -m unittest test_scan_cache   (in this directory)
"""
import base64
import contextlib
import hashlib
import io
import os
import tarfile
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import scan_cache


class Server(ThreadingHTTPServer):
    """Keeps one archive. Answers 304 to its ETag, and takes a new archive from the account writer:secret."""
    archive = None
    requests = None

    def __init__(self):
        super().__init__(('127.0.0.1', 0), Handler)
        self.requests = []
        self.url = f'http://127.0.0.1:{self.server_address[1]}/cache.tar.gz'
        threading.Thread(target=self.serve_forever, daemon=True).start()

    def etag(self):
        return '"' + hashlib.sha1(self.archive).hexdigest() + '"'


class Handler(BaseHTTPRequestHandler):
    def answer(self, code, body=b'', headers=()):
        self.server.requests.append(f'{self.command} {code}')
        self.send_response(code)
        for name, value in headers:
            self.send_header(name, value)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.server.archive is None:
            self.answer(404)
        elif self.headers.get('If-None-Match') == self.server.etag():
            self.answer(304)
        else:
            self.answer(200, self.server.archive, [('ETag', self.server.etag())])

    def do_PUT(self):
        body = self.rfile.read(int(self.headers['Content-Length']))
        if self.headers.get('Authorization') != 'Basic ' + base64.b64encode(b'writer:secret').decode():
            return self.answer(401)
        self.server.archive = body
        self.answer(201)

    def log_message(self, *args):
        pass


def archive_of(files, symlink=None):
    out = io.BytesIO()
    with tarfile.open(fileobj=out, mode='w:gz') as tar:
        for name, content in files.items():
            member = tarfile.TarInfo(name)
            member.size = len(content)
            tar.addfile(member, io.BytesIO(content))
        if symlink:
            member = tarfile.TarInfo(symlink)
            member.type, member.linkname = tarfile.SYMTYPE, '/etc/passwd'
            tar.addfile(member)
    return out.getvalue()


def files_of(archive):
    with tarfile.open(fileobj=io.BytesIO(archive), mode='r:gz') as tar:
        return {member.name: tar.extractfile(member).read() for member in tar}


class ScanCacheTest(unittest.TestCase):
    def setUp(self):
        self.server = Server()
        self.addCleanup(self.server.server_close)
        self.addCleanup(self.server.shutdown)
        self.home = self.enterContext(tempfile.TemporaryDirectory())
        self.enterContext(contextlib.redirect_stdout(io.StringIO()))
        self.errors = self.enterContext(contextlib.redirect_stderr(io.StringIO()))
        for name in ('NOTICES_SCAN_CACHE_USER', 'NOTICES_SCAN_CACHE_PASSWORD'):
            self.addCleanup(os.environ.pop, name, None)

    def write(self, name, content):
        path = os.path.join(self.home, name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'wb') as file:
            file.write(content)

    def read(self, name):
        with open(os.path.join(self.home, name), 'rb') as file:
            return file.read()

    def as_writer(self):
        os.environ.update(NOTICES_SCAN_CACHE_USER='writer', NOTICES_SCAN_CACHE_PASSWORD='secret')

    def test_download_adds_the_files_and_is_not_repeated_for_the_same_archive(self):
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'a', 'scanner/archive/1/archive.zip.xz': b'1'})
        self.assertEqual(scan_cache.download(self.home, self.server.url), 0)
        self.assertEqual(self.read('scanner/results/a/scan-results.yml'), b'a')
        self.assertEqual(self.read('scanner/archive/1/archive.zip.xz'), b'1')

        scan_cache.download(self.home, self.server.url)
        self.assertEqual(self.server.requests, ['GET 200', 'GET 304'])

    def test_download_gets_a_changed_archive(self):
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'a'})
        scan_cache.download(self.home, self.server.url)
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'a', 'scanner/results/b/scan-results.yml': b'b'})
        scan_cache.download(self.home, self.server.url)
        self.assertEqual(self.read('scanner/results/b/scan-results.yml'), b'b')

    def test_download_again_when_the_files_are_gone(self):
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'a'})
        scan_cache.download(self.home, self.server.url)
        os.remove(os.path.join(self.home, 'scanner/results/a/scan-results.yml'))
        os.removedirs(os.path.join(self.home, 'scanner/results/a'))
        scan_cache.download(self.home, self.server.url)
        self.assertEqual(self.read('scanner/results/a/scan-results.yml'), b'a')

    def test_download_keeps_a_file_that_is_there(self):
        self.write('scanner/results/a/scan-results.yml', b'mine')
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'theirs'})
        scan_cache.download(self.home, self.server.url)
        self.assertEqual(self.read('scanner/results/a/scan-results.yml'), b'mine')

    def test_no_archive_and_no_server_do_not_fail(self):
        self.assertEqual(scan_cache.download(self.home, self.server.url), 0)
        self.assertIn('404', self.errors.getvalue())
        self.assertEqual(scan_cache.download(self.home, 'http://127.0.0.1:1/cache.tar.gz'), 0)
        self.assertIn('takes minutes', self.errors.getvalue())

    def test_download_takes_nothing_from_an_archive_with_other_things_than_scans(self):
        for archive in (archive_of({'scanner/../../outside': b'x'}), archive_of({'elsewhere/file': b'x'}),
                        archive_of({}, symlink='scanner/link'), b'no archive'):
            self.server.archive = archive
            self.assertEqual(scan_cache.download(self.home, self.server.url), 0)
            self.assertIn('cannot be read', self.errors.getvalue())
            self.assertFalse(os.path.exists(os.path.join(self.home, 'scan-cache.etag')))
        self.assertEqual(sorted(os.listdir(self.home)), [])

    def test_upload_adds_the_new_files_to_those_of_the_archive(self):
        self.server.archive = archive_of({'scanner/results/a/scan-results.yml': b'a'})
        self.write('scanner/results/b/scan-results.yml', b'b')
        self.as_writer()
        self.assertEqual(scan_cache.upload(self.home, self.server.url), 0)
        self.assertEqual(files_of(self.server.archive),
                         {'scanner/results/a/scan-results.yml': b'a', 'scanner/results/b/scan-results.yml': b'b'})
        # Only the directory of the scans is shared
        self.write('maven-sources.json', b'{}')
        scan_cache.upload(self.home, self.server.url)
        self.assertEqual(self.server.requests, ['GET 200', 'PUT 201', 'GET 200'])

    def test_first_upload(self):
        self.write('scanner/results/a/scan-results.yml', b'a')
        self.as_writer()
        scan_cache.upload(self.home, self.server.url)
        self.assertEqual(files_of(self.server.archive), {'scanner/results/a/scan-results.yml': b'a'})

    def test_same_files_give_the_same_archive(self):
        self.write('scanner/results/a/scan-results.yml', b'a')
        first, second = io.BytesIO(), io.BytesIO()
        scan_cache.pack(self.home, ['scanner/results/a/scan-results.yml'], first)
        os.utime(os.path.join(self.home, 'scanner/results/a/scan-results.yml'), (0, 0))
        scan_cache.pack(self.home, ['scanner/results/a/scan-results.yml'], second)
        self.assertEqual(first.getvalue(), second.getvalue())

    def test_upload_without_the_account_fails_and_says_what_it_needs(self):
        self.write('scanner/results/a/scan-results.yml', b'a')
        with self.assertRaises(SystemExit) as failure:
            scan_cache.upload(self.home, self.server.url)
        self.assertIn('NOTICES_SCAN_CACHE_USER', str(failure.exception))
        self.assertIsNone(self.server.archive)


if __name__ == '__main__':
    unittest.main()
