#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import contextlib
import gzip
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
import tempfile
import threading
import unittest
from unittest import mock

from notices.cache import ScanCache, scan_cache_key


def _make_scan_record(idx):
    source = {'type': 'artifact', 'url': f'https://example.org/pkg-{idx}.tgz', 'sha1': f'{idx:040x}'}
    key = scan_cache_key('32.5.0', source)
    record = {'schema': 1, 'scanner': 'ScanCode 32.5.0', 'source': source, 'findings': [], 'notices': []}
    return key, record


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
        self.assertEqual(scan_cache_key('32.5.0', src), '5f33f14f6f9552b34ad973ae2705b5d6')
        self.assertNotEqual(scan_cache_key('32.5.0', src), scan_cache_key('32.6.0', src))

        vcs_a = {'type': 'vcs', 'vcs_type': 'Git', 'url': 'https://example.org/repo', 'revision': 'abc', 'path': '/sub/dir/'}
        vcs_b = {'type': 'vcs', 'vcs_type': 'Git', 'url': 'https://example.org/repo', 'revision': 'abc', 'path': 'sub/dir'}
        self.assertEqual(scan_cache_key('32.5.0', vcs_a), 'c0bb3c8db8d018a8fc130b702f6a30dd')
        self.assertEqual(scan_cache_key('32.5.0', vcs_b), 'c0bb3c8db8d018a8fc130b702f6a30dd')

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

        key1, rec1 = _make_scan_record(1)
        key2, rec2 = _make_scan_record(2)
        key3, _ = _make_scan_record(3)

        with _http_server(Handler, '/ort/v2') as url, tempfile.TemporaryDirectory() as d1, tempfile.TemporaryDirectory() as d2:
            writer = ScanCache(cache_dir=d1, remote_url=url, concurrency=4)
            writer.write_local(key1, rec1)
            writer.write_local(key2, rec2)
            uploaded, skipped = writer.upload_local()
            self.assertEqual((uploaded, skipped), (2, 0))
            # Second upload skips already-present keys
            uploaded2, skipped2 = writer.upload_local()
            self.assertEqual((uploaded2, skipped2), (0, 2))

            # Cold reader fetches both (recovering from injected 503) and reports missing key3
            reader = ScanCache(cache_dir=d2, remote_url=url, concurrency=4)
            fetched, missing = reader.fetch_missing([key1, key2, key3])
            self.assertEqual(len(fetched), 2)
            self.assertEqual(fetched[key1], rec1)
            self.assertEqual(missing, [key3])
            self.assertEqual(reader.read_local(key1), rec1)

            # Records missing required "notices" list or with mismatched key fail closed on read
            bad_key, bad_rec = _make_scan_record(99)
            writer.write_local(bad_key, {'schema': 1, 'scanner': 'ScanCode 32.5.0', 'source': bad_rec['source'], 'findings': []})
            with self.assertRaisesRegex(SystemExit, 'malformed scan record'):
                writer.read_local(bad_key)

            writer.write_local(bad_key, rec1)
            with self.assertRaisesRegex(SystemExit, 'scan cache key mismatch'):
                writer.read_local(bad_key)

            # Upload rejects mismatched records without PUTting them
            with self.assertRaisesRegex(SystemExit, 'refusing to upload invalid scan cache file'):
                writer.upload_local()
            self.assertNotIn(f'/ort/v2/scans/{bad_key}.json.gz', store)
            os.remove(writer.scan_path(bad_key))

            # Upload rejects filenames that are not 32 lowercase hex chars
            writer.write_local('not-a-hex-key', rec1)
            with self.assertRaisesRegex(SystemExit, 'unexpected scan cache filename'):
                writer.upload_local()
            os.remove(writer.scan_path('not-a-hex-key'))

            # Fetch rejects a poisoned remote record whose source does not match the requested key
            store[f'/ort/v2/scans/{key3}.json.gz'] = gzip.compress(json.dumps(rec1).encode('utf-8'))
            with self.assertRaisesRegex(SystemExit, 'scan cache key mismatch'):
                reader.fetch_missing([key3])
            self.assertFalse(reader.has_local(key3))

    def test_fetch_and_upload_fail_fast_on_probe_and_mid_batch_error(self):
        requested_paths = []
        lock = threading.Lock()
        mode = {'behavior': 'fail_all'}

        items = sorted(_make_scan_record(i) for i in range(25))
        keys = [k for k, _ in items]
        first_key = keys[0]

        class Handler(_QuietHandler):
            def _handle(self, probe_status):
                with lock:
                    requested_paths.append(self.path)
                    behavior = mode['behavior']
                if behavior == 'fail_after_first' and self.path.endswith(f'/{first_key}.json.gz'):
                    self._respond(probe_status)
                else:
                    self._respond(403)

            def do_GET(self):
                self._handle(404)

            def do_HEAD(self):
                self._handle(200)

        with _http_server(Handler, '/ort') as url, tempfile.TemporaryDirectory() as d:
            cache = ScanCache(cache_dir=d, remote_url=url, concurrency=4)

            # Probe fails -> remaining 24 keys are never requested
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.fetch_missing(keys)
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertEqual(requested_paths, [f'/ort/scans/{first_key}.json.gz'])

            # Mid-batch failure -> worker aborts synchronously, bounding total requests to <= 1 + concurrency
            mode['behavior'] = 'fail_after_first'
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.fetch_missing(keys)
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertLessEqual(len(requested_paths), 1 + cache.concurrency)

            # Upload probe fails -> remaining keys are never requested
            for key, rec in items[:15]:
                cache.write_local(key, rec)
            mode['behavior'] = 'fail_all'
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.upload_local()
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertEqual(requested_paths, [f'/ort/scans/{first_key}.json.gz'])

            # Upload mid-batch failure -> bounded to <= 1 + concurrency requests
            mode['behavior'] = 'fail_after_first'
            requested_paths.clear()
            with self.assertRaises(SystemExit) as ctx:
                cache.upload_local()
            self.assertIn('HTTP 403', str(ctx.exception))
            self.assertLessEqual(len(requested_paths), 1 + cache.concurrency)

    def test_scan_lock_excludes_concurrent_flock_waiters(self):
        if os.name != 'posix':
            return
        import fcntl

        with tempfile.TemporaryDirectory() as tmp:
            cache = ScanCache(cache_dir=tmp, remote_url='none')
            lock_path = os.path.join(tmp, 'scan.lock')
            with cache.scan_lock():
                with open(lock_path, 'a+', encoding='utf-8') as other:
                    with self.assertRaises(BlockingIOError):
                        fcntl.flock(other.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            # Released after context exits
            with open(lock_path, 'a+', encoding='utf-8') as other:
                fcntl.flock(other.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
                fcntl.flock(other.fileno(), fcntl.LOCK_UN)


if __name__ == '__main__':
    unittest.main()
