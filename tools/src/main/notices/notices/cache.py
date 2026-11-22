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
import contextlib
try:
    import fcntl
except ImportError:  # pragma: no cover - Windows fallback
    fcntl = None
import gzip
import hashlib
import http.client
import json
import os
import ssl
import threading
import urllib.parse

from notices import stop, write_atomically

DEFAULT_CACHE_DIR = os.path.expanduser('~/.cache/tb-notices')
DEFAULT_CACHE_URL = 'https://repo.thingsboard.io/artifactory/ort'
REQUEST_TIMEOUT = 15
MAX_RETRIES = 3
CONCURRENCY = 20


class _AbortedError(Exception):
    """Internal sentinel raised when another worker already tripped fail-fast abort."""


def scan_cache_key(scancode_version, source):
    """Deterministic 32-hex-char cache key for a source descriptor and ScanCode version."""
    if source['type'] == 'artifact':
        identity = f"artifact|{source['url']}|{source['sha1']}"
    else:
        identity = f"vcs|{source['vcs_type']}|{source['url']}|{source['revision']}|{source.get('path', '').strip('/')}"
    raw = f'v1|{scancode_version}|{identity}'.encode('utf-8')
    return hashlib.sha256(raw).hexdigest()[:32]


def _validate_scan_record(record, origin):
    if not isinstance(record, dict) or not isinstance(record.get('findings'), list) or not isinstance(record.get('notices', []), list):
        raise ValueError(f'malformed scan record in {origin} (expected JSON object with list "findings")')
    return record


class ScanCache:
    """Local + remote content-addressed scan cache."""

    def __init__(self, cache_dir=None, remote_url=None, concurrency=CONCURRENCY):
        raw_dir = cache_dir or os.environ.get('NOTICES_CACHE_DIR') or DEFAULT_CACHE_DIR
        self.cache_dir = os.path.abspath(os.path.expanduser(raw_dir))
        raw_url = remote_url if remote_url is not None else os.environ.get('NOTICES_SCAN_CACHE_URL', DEFAULT_CACHE_URL)
        self.remote_url = None if not raw_url or raw_url == 'none' else raw_url.rstrip('/')
        self._parsed_remote = None
        self._ssl_context = None
        if self.remote_url:
            parsed = urllib.parse.urlsplit(self.remote_url)
            if parsed.scheme not in ('http', 'https') or not parsed.hostname:
                raise stop(f'invalid remote scan cache URL {self.remote_url!r} (expected http(s)://host/path or "none")')
            self._parsed_remote = parsed
            if parsed.scheme == 'https':
                self._ssl_context = ssl.create_default_context()
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
        try:
            with gzip.open(path, 'rt', encoding='utf-8') as file:
                record = json.load(file)
            return _validate_scan_record(record, path)
        except (OSError, gzip.BadGzipFile, json.JSONDecodeError, ValueError) as err:
            raise stop(f'corrupt local scan cache file {path}: {err}. Remove it and re-run.')

    def write_local(self, key, record):
        payload = gzip.compress(json.dumps(record, separators=(',', ':'), sort_keys=True).encode('utf-8'))
        write_atomically(self.scan_path(key), payload)

    @contextlib.contextmanager
    def scan_lock(self):
        """Cross-process exclusive lock serializing local ScanCode runs under parallel builds (e.g. mvn -T)."""
        os.makedirs(self.cache_dir, exist_ok=True)
        lock_path = os.path.join(self.cache_dir, 'scan.lock')
        with open(lock_path, 'a+', encoding='utf-8') as lock_file:
            if fcntl is not None:
                fcntl.flock(lock_file.fileno(), fcntl.LOCK_EX)
            try:
                yield
            finally:
                if fcntl is not None:
                    fcntl.flock(lock_file.fileno(), fcntl.LOCK_UN)

    def fetch_missing(self, keys):
        """Fetches any keys absent from local disk from remote_url in parallel over keep-alive connections.

        Probes the first missing key before fanning out and aborts fail-closed on the first error so an
        unreachable remote cache stops quickly instead of timing out across every package.

        Returns (fetched_records_by_key, still_missing_keys).
        """
        missing = [k for k in dict.fromkeys(keys) if not self.has_local(k)]
        if not missing or not self.remote_url:
            return {}, missing

        fetched = {}
        still_missing = []
        auth_headers = _authorization_headers()

        def fetch_one(key, abort):
            rel_path = f'/scans/{key}.json.gz'
            status, body = self._request_with_retry('GET', rel_path, headers=auth_headers, abort=abort)
            if status == 200:
                # Validate gzip/JSON schema before committing locally so a corrupt download never poisons disk.
                record = _validate_scan_record(json.loads(gzip.decompress(body)), f'{self.remote_url}{rel_path}')
                write_atomically(self.scan_path(key), body)
                return ('hit', key, record)
            if status == 404:
                return ('miss', key, None)
            raise RuntimeError(f'{key}: HTTP {status}')

        def handle_outcome(outcome):
            kind, key, detail = outcome
            if kind == 'hit':
                fetched[key] = detail
            else:
                still_missing.append(key)

        try:
            self._run_fail_fast(missing, fetch_one, handle_outcome)
        except Exception as err:
            raise stop(
                f'remote scan cache {self.remote_url} error ({err}), '
                'so the notices stop instead of rescanning everything. '
                'Check network access or set NOTICES_SCAN_CACHE_URL=none to scan locally.'
            )

        return fetched, sorted(still_missing)

    def upload_local(self):
        """Uploads locally cached scan files that do not yet exist on remote_url.

        Uses HEAD to skip already-uploaded keys and PUT for new ones (content-addressed, so race-free).
        Probes the first key before fanning out and aborts remaining work on the first error.
        Returns (uploaded_count, skipped_count).
        """
        if not self.remote_url:
            return 0, 0
        scans_dir = os.path.join(self.cache_dir, 'scans')
        if not os.path.isdir(scans_dir):
            return 0, 0
        keys = sorted(name.removesuffix('.json.gz') for name in os.listdir(scans_dir) if name.endswith('.json.gz'))
        if not keys:
            return 0, 0

        auth_headers = _authorization_headers()
        uploaded = skipped = 0

        def upload_one(key, abort):
            rel_path = f'/scans/{key}.json.gz'
            status, _ = self._request_with_retry('HEAD', rel_path, headers=auth_headers, abort=abort)
            if status == 200:
                return 'skipped'
            if status not in (404, 405):
                raise stop(f'HEAD {self.remote_url}{rel_path} failed: HTTP {status}')
            with open(self.scan_path(key), 'rb') as file:
                body = file.read()
            headers = {
                'Content-Type': 'application/gzip',
                'Content-Length': str(len(body)),
                **auth_headers,
            }
            put_status, _ = self._request_with_retry('PUT', rel_path, body=body, headers=headers, abort=abort)
            if put_status not in (200, 201, 204):
                hint = (
                    ' Set NOTICES_SCAN_CACHE_USER and NOTICES_SCAN_CACHE_PASSWORD.'
                    if put_status in (401, 403) else ''
                )
                raise stop(f'PUT {self.remote_url}{rel_path} failed: HTTP {put_status}.{hint}')
            return 'uploaded'

        def record_result(result):
            nonlocal uploaded, skipped
            if result == 'uploaded':
                uploaded += 1
            else:
                skipped += 1

        try:
            self._run_fail_fast(keys, upload_one, record_result)
        except RuntimeError as err:
            raise stop(str(err))

        return uploaded, skipped

    def _run_fail_fast(self, keys, worker, on_result):
        """Probes keys[0] synchronously, then fans out keys[1:] with fail-fast abort on first error."""
        abort = threading.Event()

        def run_one(key):
            if abort.is_set():
                raise _AbortedError()
            try:
                return worker(key, abort)
            except BaseException:
                # Set abort synchronously before the worker returns so it cannot dequeue another key.
                abort.set()
                raise

        try:
            on_result(run_one(keys[0]))
            rest = keys[1:]
            if not rest:
                return
            pool = concurrent.futures.ThreadPoolExecutor(max_workers=min(self.concurrency, len(rest)))
            try:
                futures = [pool.submit(run_one, key) for key in rest]
                for future in concurrent.futures.as_completed(futures):
                    try:
                        on_result(future.result())
                    except _AbortedError:
                        continue
            except BaseException:
                abort.set()
                raise
            finally:
                pool.shutdown(wait=True, cancel_futures=True)
        finally:
            self._close_connections()

    def _get_connection(self):
        conn = getattr(self._thread_local, 'conn', None)
        if conn is None:
            parsed = self._parsed_remote
            if parsed.scheme == 'https':
                conn = http.client.HTTPSConnection(
                    parsed.hostname, parsed.port or 443, timeout=REQUEST_TIMEOUT, context=self._ssl_context
                )
            else:
                conn = http.client.HTTPConnection(parsed.hostname, parsed.port or 80, timeout=REQUEST_TIMEOUT)
            self._thread_local.conn = conn
            with self._conn_lock:
                self._connections.append(conn)
        return conn

    def _reset_connection(self):
        conn = getattr(self._thread_local, 'conn', None)
        if conn is not None:
            with contextlib.suppress(Exception):
                conn.close()
            with self._conn_lock:
                with contextlib.suppress(ValueError):
                    self._connections.remove(conn)
            self._thread_local.conn = None

    def _close_connections(self):
        with self._conn_lock:
            conns, self._connections = self._connections, []
        for conn in conns:
            with contextlib.suppress(Exception):
                conn.close()
        self._thread_local = threading.local()

    def _request_with_retry(self, method, rel_path, abort, body=None, headers=None):
        req_headers = {'User-Agent': 'tb-notices-cache', 'Connection': 'keep-alive', **(headers or {})}
        full_path = f'{self._parsed_remote.path}{rel_path}'
        last_error = None
        for attempt in range(MAX_RETRIES):
            if abort.is_set():
                raise _AbortedError()
            try:
                conn = self._get_connection()
                conn.request(method, full_path, body=body, headers=req_headers)
                resp = conn.getresponse()
                data = resp.read()
                if resp.will_close or resp.status >= 500:
                    self._reset_connection()
                if resp.status >= 500 and attempt < MAX_RETRIES - 1:
                    _wait_backoff(attempt, abort)
                    continue
                return resp.status, data
            except (http.client.HTTPException, OSError) as err:
                last_error = err
                self._reset_connection()
                if attempt < MAX_RETRIES - 1:
                    _wait_backoff(attempt, abort)
        raise RuntimeError(f'{method} {self.remote_url}{rel_path} failed after {MAX_RETRIES} attempts: {last_error}')


def _wait_backoff(attempt, abort):
    if abort.wait(0.5 * (2 ** attempt)):
        raise _AbortedError()


def _authorization_headers():
    user = os.environ.get('NOTICES_SCAN_CACHE_USER')
    password = os.environ.get('NOTICES_SCAN_CACHE_PASSWORD')
    if not user or not password:
        return {}
    token = base64.b64encode(f'{user}:{password}'.encode('utf-8')).decode('ascii')
    return {'Authorization': f'Basic {token}'}
