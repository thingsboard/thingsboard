#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import contextlib
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import os
import tempfile
import threading
import unittest
from unittest import mock

from notices.cache import ScanCache, scan_cache_key


class _QuietHandler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, format, *args):
        pass

    def _respond(self, status, body=b''):
        self.send_response(status)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)


@contextlib.contextmanager
def _http_server(handler_cls, path=''):
    server = ThreadingHTTPServer(('127.0.0.1', 0), handler_cls)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield f'http://127.0.0.1:{server.server_port}{path}'
    finally:
        server.shutdown()
        server.server_close()


class CacheTests(unittest.TestCase):
    def test_scan_cache_key_deterministic_and_normalizes_vcs_path(self):
        src = {'type': 'artifact', 'url': 'https://example.org/a-1.0-sources.jar', 'sha1': 'a' * 40}
        self.assertEqual(scan_cache_key('32.5.0', src), scan_cache_key('32.5.0', src))
        self.assertNotEqual(scan_cache_key('32.5.0', src), scan_cache_key('32.6.0', src))

        vcs_a = {'type': 'vcs', 'vcs_type': 'Git', 'url': 'https://example.org/repo', 'revision': 'abc', 'path': '/sub/dir/'}
        vcs_b = {'type': 'vcs', 'vcs_type': 'Git', 'url': 'https://example.org/repo', 'revision': 'abc', 'path': 'sub/dir'}
        self.assertEqual(scan_cache_key('32.5.0', vcs_a), scan_cache_key('32.5.0', vcs_b))

    def test_invalid_remote_url_stops(self):
        with self.assertRaises(SystemExit):
            ScanCache(remote_url='ftp://example.org/cache')

    def test_cache_dir_expands_tilde_from_arg_and_env(self):
        expected = os.path.abspath(os.path.expanduser('~/.cache/tb-notices-nightly'))
        self.assertEqual(ScanCache(cache_dir='~/.cache/tb-notices-nightly', remote_url='none').cache_dir, expected)
        with mock.patch.dict(os.environ, {'NOTICES_CACHE_DIR': '~/.cache/tb-notices-nightly'}, clear=False):
            self.assertEqual(ScanCache(remote_url='none').cache_dir, expected)

    def test_parallel_fetch_and_upload_with_retry(self):
        store = {}
        failures = {'count': 1}
        lock = threading.Lock()

        class Handler(_QuietHandler):
            def do_GET(self):
                with lock:
                    if failures['count'] > 0:
                        failures['count'] -= 1
                        self._respond(503)
                        return
                    body = store.get(self.path)
                self._respond(404) if body is None else self._respond(200, body)

            def do_HEAD(self):
                with lock:
                    exists = self.path in store
                self._respond(200 if exists else 404)

            def do_PUT(self):
                length = int(self.headers.get('Content-Length', '0'))
                body = self.rfile.read(length)
                with lock:
                    store[self.path] = body
                self._respond(201)

        with _http_server(Handler, '/ort/v2') as url, tempfile.TemporaryDirectory() as d1, tempfile.TemporaryDirectory() as d2:
            writer = ScanCache(cache_dir=d1, remote_url=url, concurrency=4)
            writer.write_local('key1', {'schema': 1, 'findings': []})
            writer.write_local('key2', {'schema': 1, 'findings': []})
            uploaded, skipped = writer.upload_local()
            self.assertEqual((uploaded, skipped), (2, 0))
            # Second upload skips already-present keys
            uploaded2, skipped2 = writer.upload_local()
            self.assertEqual((uploaded2, skipped2), (0, 2))

            # Cold reader fetches both (recovering from injected 503) and reports missing key3
            reader = ScanCache(cache_dir=d2, remote_url=url, concurrency=4)
            fetched, missing, errors = reader.fetch_missing(['key1', 'key2', 'key3'])
            self.assertEqual(errors, [])
            self.assertEqual(len(fetched), 2)
            self.assertEqual(fetched['key1'], {'schema': 1, 'findings': []})
            self.assertEqual(missing, ['key3'])
            self.assertEqual(reader.read_local('key1'), {'schema': 1, 'findings': []})

    def test_fetch_and_upload_fail_fast_on_probe_and_mid_batch_error(self):
        requested_paths = []
        lock = threading.Lock()
        mode = {'behavior': 'fail_all'}

        class Handler(_QuietHandler):
            def _handle(self, probe_status):
                with lock:
                    requested_paths.append(self.path)
                    behavior = mode['behavior']
                if behavior == 'fail_after_first' and self.path.endswith('/k0.json.gz'):
                    self._respond(probe_status)
                else:
                    self._respond(403)

            def do_GET(self):
                self._handle(404)

            def do_HEAD(self):
                self._handle(200)

        keys = [f'k{i}' for i in range(25)]
        with _http_server(Handler, '/ort') as url, tempfile.TemporaryDirectory() as d:
            cache = ScanCache(cache_dir=d, remote_url=url, concurrency=4)

            # Probe fails -> remaining 24 keys are never requested
            requested_paths.clear()
            fetched, missing, errors = cache.fetch_missing(keys)
            self.assertEqual(fetched, {})
            self.assertEqual(len(errors), 1)
            self.assertEqual(requested_paths, ['/ort/scans/k0.json.gz'])

            # Mid-batch failure -> worker aborts synchronously, bounding total requests to <= 1 + concurrency
            mode['behavior'] = 'fail_after_first'
            requested_paths.clear()
            fetched, missing, errors = cache.fetch_missing(keys)
            self.assertEqual(len(errors), 1)
            self.assertLessEqual(len(requested_paths), 1 + cache.concurrency)

            # Upload probe fails -> remaining keys are never requested
            for key in keys[:15]:
                cache.write_local(key, {'schema': 1, 'findings': []})
            mode['behavior'] = 'fail_all'
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.upload_local()
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertEqual(requested_paths, ['/ort/scans/k0.json.gz'])

            # Upload mid-batch failure -> bounded to <= 1 + concurrency requests
            mode['behavior'] = 'fail_after_first'
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.upload_local()
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertLessEqual(len(requested_paths), 1 + cache.concurrency)


if __name__ == '__main__':
    unittest.main()
