#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Content-addressed per-package scan cache (local disk + parallel HTTP keep-alive Artifactory).

Each scanned source is stored as a single immutable gzipped JSON file:
  Local:  <cache_dir>/scans/<key>.json.gz
  Remote: <base_url>/scans/<key>.json.gz

Immutable keys mean concurrent CI uploads never overwrite each other's scans.
Persistent HTTP keep-alive connections across worker threads make cold downloads fast
(seconds for ~800 packages), while warm runs make zero network requests.
"""
import base64
import concurrent.futures
import gzip
import hashlib
import http.client
import json
import os
import ssl
import threading
import time
import urllib.parse

from notices import write_atomically

DEFAULT_CACHE_DIR = os.path.expanduser('~/.cache/tb-notices')
DEFAULT_CACHE_URL = 'https://repo.thingsboard.io/artifactory/ort/v2'
REQUEST_TIMEOUT = 30
MAX_RETRIES = 3
CONCURRENCY = 20


def scan_cache_key(scancode_version, source):
    """Deterministic 32-hex-char cache key for a source descriptor and ScanCode version."""
    if source['type'] == 'artifact':
        identity = f"artifact|{source['url']}|{source['sha1']}"
    else:
        identity = f"vcs|{source['vcs_type']}|{source['url']}|{source['revision']}|{source.get('path', '')}"
    raw = f'v1|{scancode_version}|{identity}'.encode('utf-8')
    return hashlib.sha256(raw).hexdigest()[:32]


class ScanCache:
    """Local + remote content-addressed scan cache."""

    def __init__(self, cache_dir=None, remote_url=None, concurrency=CONCURRENCY):
        self.cache_dir = os.path.abspath(cache_dir or os.environ.get('NOTICES_CACHE_DIR') or DEFAULT_CACHE_DIR)
        raw_url = remote_url if remote_url is not None else os.environ.get('NOTICES_SCAN_CACHE_URL', DEFAULT_CACHE_URL)
        self.remote_url = None if not raw_url or raw_url == 'none' else raw_url.rstrip('/')
        self.concurrency = concurrency
        self._thread_local = threading.local()
        self._connections = []
        self._conn_lock = threading.Lock()

    def scan_path(self, key):
        return os.path.join(self.cache_dir, 'scans', f'{key}.json.gz')

    def has_local(self, key):
        return os.path.exists(self.scan_path(key))

    def read_local(self, key):
        path = self.scan_path(key)
        if not os.path.exists(path):
            return None
        with gzip.open(path, 'rt', encoding='utf-8') as file:
            return json.load(file)

    def write_local(self, key, record):
        payload = gzip.compress(json.dumps(record, separators=(',', ':'), sort_keys=True).encode('utf-8'))
        write_atomically(self.scan_path(key), payload)

    def fetch_missing(self, keys):
        """Fetches any keys absent from local disk from remote_url in parallel over keep-alive connections.

        Returns (fetched_count, still_missing_keys, errors).
        """
        missing = [k for k in dict.fromkeys(keys) if not self.has_local(k)]
        if not missing or not self.remote_url:
            return 0, missing, []

        fetched = 0
        still_missing = []
        errors = []
        parsed = urllib.parse.urlsplit(self.remote_url)

        def fetch_one(key):
            rel_path = f'{parsed.path}/scans/{key}.json.gz'
            status, body = self._request_with_retry(parsed, 'GET', rel_path)
            if status == 200:
                # Validate gzip/JSON before committing locally so a corrupt download never poisons disk.
                json.loads(gzip.decompress(body))
                write_atomically(self.scan_path(key), body)
                return ('hit', key, None)
            if status == 404:
                return ('miss', key, None)
            return ('error', key, f'HTTP {status}')

        try:
            with concurrent.futures.ThreadPoolExecutor(max_workers=min(self.concurrency, len(missing))) as pool:
                futures = [pool.submit(fetch_one, key) for key in missing]
                for future in concurrent.futures.as_completed(futures):
                    try:
                        kind, key, detail = future.result()
                    except Exception as err:
                        errors.append(str(err))
                        continue
                    if kind == 'hit':
                        fetched += 1
                    elif kind == 'miss':
                        still_missing.append(key)
                    else:
                        errors.append(f'{key}: {detail}')
        finally:
            self._close_connections()

        return fetched, sorted(still_missing), errors

    def upload_local(self, keys=None):
        """Uploads locally cached scan files that do not yet exist on remote_url.

        Uses HEAD to skip already-uploaded keys and PUT for new ones (content-addressed, so race-free).
        Returns (uploaded_count, skipped_count).
        """
        if not self.remote_url:
            return 0, 0
        scans_dir = os.path.join(self.cache_dir, 'scans')
        if not os.path.isdir(scans_dir):
            return 0, 0
        if keys is None:
            keys = sorted(name.removesuffix('.json.gz') for name in os.listdir(scans_dir) if name.endswith('.json.gz'))
        else:
            keys = sorted({k for k in keys if self.has_local(k)})
        if not keys:
            return 0, 0

        auth_headers = _authorization_headers()
        parsed = urllib.parse.urlsplit(self.remote_url)
        uploaded, skipped = 0, 0

        def upload_one(key):
            rel_path = f'{parsed.path}/scans/{key}.json.gz'
            status, _ = self._request_with_retry(parsed, 'HEAD', rel_path, headers=auth_headers)
            if status == 200:
                return 'skipped'
            if status not in (404, 405):
                raise RuntimeError(f'HEAD {self.remote_url}/scans/{key}.json.gz failed: HTTP {status}')
            with open(self.scan_path(key), 'rb') as file:
                body = file.read()
            headers = {
                'Content-Type': 'application/gzip',
                'Content-Length': str(len(body)),
                **auth_headers,
            }
            put_status, _ = self._request_with_retry(parsed, 'PUT', rel_path, body=body, headers=headers)
            if put_status not in (200, 201, 204):
                hint = (
                    ' Set NOTICES_SCAN_CACHE_USER and NOTICES_SCAN_CACHE_PASSWORD.'
                    if put_status in (401, 403) else ''
                )
                raise RuntimeError(f'PUT {self.remote_url}/scans/{key}.json.gz failed: HTTP {put_status}.{hint}')
            return 'uploaded'

        try:
            with concurrent.futures.ThreadPoolExecutor(max_workers=min(self.concurrency, len(keys))) as pool:
                for result in pool.map(upload_one, keys):
                    if result == 'uploaded':
                        uploaded += 1
                    else:
                        skipped += 1
        finally:
            self._close_connections()

        return uploaded, skipped

    def _get_connection(self, parsed):
        conn = getattr(self._thread_local, 'conn', None)
        if conn is None:
            if parsed.scheme == 'https':
                conn = http.client.HTTPSConnection(parsed.hostname, parsed.port or 443, timeout=REQUEST_TIMEOUT,
                                                   context=ssl.create_default_context())
            else:
                conn = http.client.HTTPConnection(parsed.hostname, parsed.port or 80, timeout=REQUEST_TIMEOUT)
            self._thread_local.conn = conn
            with self._conn_lock:
                self._connections.append(conn)
        return conn

    def _reset_connection(self):
        conn = getattr(self._thread_local, 'conn', None)
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass
            self._thread_local.conn = None

    def _close_connections(self):
        with self._conn_lock:
            conns, self._connections = self._connections, []
        for conn in conns:
            try:
                conn.close()
            except Exception:
                pass
        self._thread_local = threading.local()

    def _request_with_retry(self, parsed, method, path, body=None, headers=None):
        req_headers = {'User-Agent': 'tb-notices-cache', 'Connection': 'keep-alive', **(headers or {})}
        last_error = None
        for attempt in range(MAX_RETRIES):
            try:
                conn = self._get_connection(parsed)
                conn.request(method, path, body=body, headers=req_headers)
                resp = conn.getresponse()
                data = resp.read()
                if resp.status >= 500 and attempt < MAX_RETRIES - 1:
                    time.sleep(0.5 * (2 ** attempt))
                    continue
                return resp.status, data
            except (http.client.HTTPException, OSError) as err:
                last_error = err
                self._reset_connection()
                if attempt < MAX_RETRIES - 1:
                    time.sleep(0.5 * (2 ** attempt))
        raise RuntimeError(f'{method} {self.remote_url}{path} failed after {MAX_RETRIES} attempts: {last_error}')


def _authorization_headers():
    user = os.environ.get('NOTICES_SCAN_CACHE_USER')
    password = os.environ.get('NOTICES_SCAN_CACHE_PASSWORD')
    if not user or not password:
        return {}
    token = base64.b64encode(f'{user}:{password}'.encode('utf-8')).decode('ascii')
    return {'Authorization': f'Basic {token}'}
