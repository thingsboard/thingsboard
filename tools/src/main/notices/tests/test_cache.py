#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import tempfile
import threading
import unittest

from notices.cache import ScanCache, scan_cache_key


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

    def test_parallel_fetch_and_upload_with_retry(self):
        store = {}
        failures = {'count': 1}
        lock = threading.Lock()

        class Handler(BaseHTTPRequestHandler):
            protocol_version = 'HTTP/1.1'

            def log_message(self, format, *args):
                pass

            def do_GET(self):
                with lock:
                    if failures['count'] > 0:
                        failures['count'] -= 1
                        self.send_response(503)
                        self.send_header('Content-Length', '0')
                        self.end_headers()
                        return
                    body = store.get(self.path)
                if body is None:
                    self.send_response(404)
                    self.send_header('Content-Length', '0')
                    self.end_headers()
                else:
                    self.send_response(200)
                    self.send_header('Content-Length', str(len(body)))
                    self.end_headers()
                    self.wfile.write(body)

            def do_HEAD(self):
                with lock:
                    exists = self.path in store
                self.send_response(200 if exists else 404)
                self.send_header('Content-Length', '0')
                self.end_headers()

            def do_PUT(self):
                length = int(self.headers.get('Content-Length', '0'))
                body = self.rfile.read(length)
                with lock:
                    store[self.path] = body
                self.send_response(201)
                self.send_header('Content-Length', '0')
                self.end_headers()

        server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            url = f'http://127.0.0.1:{server.server_port}/ort/v2'
            with tempfile.TemporaryDirectory() as d1, tempfile.TemporaryDirectory() as d2:
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
        finally:
            server.shutdown()
            server.server_close()


if __name__ == '__main__':
    unittest.main()
