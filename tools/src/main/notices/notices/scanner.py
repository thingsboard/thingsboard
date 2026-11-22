#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Source resolution, direct ScanCode Docker execution, and NOTICE extraction."""
import concurrent.futures
import hashlib
import io
import json
import os
import re
import shutil
import subprocess
import tarfile
import tempfile
import threading
import urllib.error
import urllib.parse
import zipfile

from notices import BUILD_PROBLEM, HTTP_ERRORS, clean_text, http_get, stop, write_atomically

SCANCODE_VERSION = '32.5.0'
DEFAULT_SCANCODE_IMAGE = 'ghcr.io/oss-review-toolkit/ort-minimal:98.0.0'
SCAN_BATCH_SIZE = 50
MAVEN_REPOSITORIES = [
    'https://repo1.maven.org/maven2',
    'https://repo.thingsboard.io/artifactory/libs-release-public',
]
SHA1_RE = re.compile(r'^[0-9a-f]{40}$')


def scancode_image():
    return os.environ.get('NOTICES_SCANCODE_IMAGE', DEFAULT_SCANCODE_IMAGE)


def maven_path(group, name, version, classifier='', file_type='jar'):
    return f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}{'-' + classifier if classifier else ''}.{file_type}"


def from_maven_repositories(path, timeout=30):
    for repo_url in MAVEN_REPOSITORIES:
        url = f'{repo_url}/{path}'
        try:
            yield url, http_get(url, timeout=timeout)
        except urllib.error.HTTPError as err:
            if err.code != 404:
                raise stop(f'could not download {url}: HTTP {err.code}')
        except HTTP_ERRORS as err:
            raise stop(f'could not download {url}: {err}')


def resolve_maven_sources(packages, curations, cache_dir):
    """Resolves -sources.jar URL + SHA-1 for Maven packages, remembering found ones in maven-sources.json.

    Skips packages that already have a metadata_only or vcs curation so warm/curated builds never
    make unnecessary network requests for packages known to lack a -sources.jar.
    """
    cache_path = os.path.join(cache_dir, 'maven-sources.json')
    known = _read_sources_cache(cache_path)

    def has_source_curation(pkg):
        return any(c.metadata_only or c.vcs for c in curations.for_package(pkg.package_key, pkg.version))

    missing = [pkg for pkg in packages.values() if pkg.kind == 'maven' and pkg.purl not in known and not has_source_curation(pkg)]
    found = {}
    abort = threading.Event()
    try:
        if missing:
            def lookup(pkg):
                if abort.is_set():
                    return pkg.purl, None
                try:
                    path = maven_path(pkg.group, pkg.name, pkg.version, 'sources')
                    for url, content in from_maven_repositories(path + '.sha1', timeout=15):
                        tokens = content.decode('utf-8', 'replace').split()
                        sha1 = tokens[0].lower() if tokens else ''
                        if not SHA1_RE.match(sha1):
                            raise stop(f'{url} is no SHA-1: {sha1!r}. {BUILD_PROBLEM}')
                        return pkg.purl, {'url': url.removesuffix('.sha1'), 'sha1': sha1}
                    return pkg.purl, None
                except BaseException:
                    abort.set()
                    raise

            # Probe the first lookup before fanning out so an unreachable repo fails fast.
            purl0, source0 = lookup(missing[0])
            if source0:
                found[purl0] = source0
            rest = missing[1:]
            if rest:
                pool = concurrent.futures.ThreadPoolExecutor(max_workers=min(16, len(rest)))
                try:
                    futures = [pool.submit(lookup, pkg) for pkg in rest]
                    for future in concurrent.futures.as_completed(futures):
                        purl, source = future.result()
                        if source:
                            found[purl] = source
                except BaseException:
                    abort.set()
                    raise
                finally:
                    pool.shutdown(wait=True, cancel_futures=True)
    finally:
        if found:
            merged = {**known, **_read_sources_cache(cache_path), **found}
            write_atomically(cache_path, json.dumps(merged, indent=1, sort_keys=True).encode('utf-8'))
            known = merged

    return known


def _read_sources_cache(path):
    if not os.path.exists(path):
        return {}
    try:
        with open(path, encoding='utf-8') as file:
            data = json.load(file)
    except (OSError, json.JSONDecodeError) as err:
        raise stop(f'corrupt Maven sources cache {path}: {err}. Remove it and re-run.')
    return {k: v for k, v in data.items() if v} if isinstance(data, dict) else {}


def _artifact_source(url, sha1):
    return {'type': 'artifact', 'url': url, 'sha1': sha1}


def _vcs_source(vcs_type, url, revision, path=''):
    return {'type': 'vcs', 'vcs_type': vcs_type, 'url': url, 'revision': revision, 'path': path.strip('/')}


def _make_scan_record(source, findings, notices):
    return {
        'schema': 1,
        'scanner': f'ScanCode {SCANCODE_VERSION}',
        'source': source,
        'findings': findings,
        'notices': sorted(notices, key=lambda n: n['path']),
    }


def resolve_package_source(pkg, curations, maven_sources):
    """Returns a scan source descriptor (artifact, vcs, or metadata_only), or None if unavailable."""
    if pkg.kind == 'declared':
        return {'type': 'metadata_only'}
    applying = curations.for_package(pkg.package_key, pkg.version)
    if any(c.metadata_only for c in applying):
        return {'type': 'metadata_only'}
    for c in applying:
        if c.vcs:
            return _vcs_source(c.vcs.get('type', 'Git'), c.vcs['url'], c.vcs['revision'], c.vcs.get('path', ''))
    if pkg.kind == 'maven':
        src = maven_sources.get(pkg.purl)
        return _artifact_source(src['url'], src['sha1']) if src else None
    if pkg.npm_tarball:
        return _artifact_source(pkg.npm_tarball['url'], pkg.npm_tarball['sha1'])
    if pkg.vcs:
        return _vcs_source(pkg.vcs['type'], pkg.vcs['url'], pkg.vcs['revision'], pkg.vcs.get('path', ''))
    return None


def ensure_docker_available():
    """Verifies that `docker` is installed and its daemon is reachable before scanning uncached packages."""
    try:
        proc = subprocess.run(['docker', 'info'], capture_output=True, text=True, timeout=15)
    except FileNotFoundError:
        raise stop(
            'Docker is required to scan uncached packages, but `docker` was not found in PATH. '
            'Install Docker or run with a warm scan cache. See docs/licensing-guide.md#checking-the-notices-locally.'
        )
    except subprocess.TimeoutExpired:
        raise stop(
            'Docker is required to scan uncached packages, but `docker info` timed out. '
            'Check that the Docker daemon is running and responsive.'
        )
    if proc.returncode != 0:
        detail = (proc.stderr or proc.stdout).strip().splitlines()[:3]
        suffix = ('\n  ' + '\n  '.join(detail)) if detail else ''
        raise stop(
            'Docker is required to scan uncached packages, but the Docker daemon is not reachable '
            f'(start Docker and re-run).{suffix}'
        )


def scan_sources_with_scancode(items, batch_size=SCAN_BATCH_SIZE):
    """Downloads/extracts uncached sources in batches, runs one ScanCode Docker container per batch,
    and yields (key, record) pairs as each batch completes.

    `items` is a sequence of (key, purl, source) tuples with unique keys. Scans every file in each
    source archive without policy-specific --ignore flags so cached scan records stay policy-independent;
    policy.exclude_paths is applied during resolution.
    """
    image = scancode_image()
    items = list(items)
    for start in range(0, len(items), batch_size):
        batch = items[start:start + batch_size]
        with tempfile.TemporaryDirectory(prefix='tb-scancode-') as work:
            src_root = os.path.join(work, 'src')
            os.makedirs(src_root)
            sources_by_key = {}
            notices_by_key = {}
            for key, purl, source in batch:
                print(f'Staging {purl} for ScanCode ...', flush=True)
                pkg_dir = os.path.join(src_root, key)
                os.makedirs(pkg_dir)
                notices_by_key[key] = _fetch_and_extract(source, pkg_dir)
                sources_by_key[key] = source

            print(f'Running ScanCode on {len(batch)} uncached package(s) ...', flush=True)
            out_json = os.path.join(work, 'scancode.json')
            cmd = [
                'docker', 'run', '--rm',
                '-u', f'{os.getuid()}:{os.getgid()}',
                '-v', f'{work}:/work',
                '--entrypoint', 'scancode',
                image,
                '--copyright', '--license', '--info', '--strip-root',
                '--timeout', '300',
                '--processes', str(min(os.cpu_count() or 4, 14)),
                '--json', '/work/scancode.json', '/work/src',
            ]

            try:
                proc = subprocess.run(cmd, capture_output=True, text=True)
            except FileNotFoundError:
                raise stop('docker is required to scan uncached packages, but `docker` was not found in PATH.')
            if proc.returncode != 0 or not os.path.exists(out_json):
                purls = ', '.join(purl for _, purl, _ in batch[:5])
                suffix = f' (+{len(batch) - 5} more)' if len(batch) > 5 else ''
                raise stop(
                    f'ScanCode failed for batch ({purls}{suffix}, exit {proc.returncode}):\n'
                    f'{(proc.stderr or proc.stdout)[-1500:]}'
                )
            with open(out_json, encoding='utf-8') as file:
                raw = json.load(file)
            records = _normalize_scancode_batch(raw, sources_by_key, notices_by_key)
            for key, _, _ in batch:
                yield key, records[key]


def _fetch_and_extract(source, target_dir):
    """Fetches source into target_dir and returns extracted NOTICE files (for npm tarballs and VCS checkouts)."""
    if source['type'] == 'artifact':
        url, expected_sha1 = source['url'], source['sha1'].lower()
        try:
            data = http_get(url, timeout=120)
        except HTTP_ERRORS as err:
            raise stop(f'could not download source archive {url}: {err}')
        actual = hashlib.sha1(data).hexdigest().lower()
        if expected_sha1 and actual != expected_sha1:
            raise stop(f'SHA-1 mismatch for {url}: got {actual}, expected {expected_sha1}')
        url_path = urllib.parse.urlsplit(url).path.lower()
        if url_path.endswith(('.jar', '.zip')):
            try:
                with zipfile.ZipFile(io.BytesIO(data)) as zf:
                    zf.extractall(target_dir)
            except zipfile.BadZipFile as err:
                raise stop(f'corrupt zip/jar archive {url}: {err}')
            return []
        _extract_tarball(data, url, target_dir)
        return _collect_dir_notices(target_dir)

    # VCS checkout (Git or Subversion)
    vcs_type = source['vcs_type']
    url, revision, subpath = source['url'], source['revision'], source.get('path', '').strip('/')
    if url.startswith('-') or revision.startswith('-'):
        raise stop(f'invalid VCS reference (leading dash not allowed): {url!r}@{revision!r}')
    if vcs_type == 'Git':
        with tempfile.TemporaryDirectory(prefix='tb-git-') as repo_dir:
            _run_cmd(['git', 'init', '-q'], cwd=repo_dir)
            _run_cmd(['git', 'remote', 'add', '--', 'origin', url], cwd=repo_dir)
            _run_cmd(['git', 'fetch', '--depth', '1', '--', 'origin', revision], cwd=repo_dir)
            _run_cmd(['git', 'checkout', '-q', 'FETCH_HEAD'], cwd=repo_dir)
            real_repo = os.path.realpath(repo_dir)
            copy_from = os.path.realpath(os.path.join(real_repo, subpath)) if subpath else real_repo
            if os.path.commonpath([real_repo, copy_from]) != real_repo or not os.path.isdir(copy_from):
                raise stop(f'VCS subpath {subpath!r} does not exist inside {url}@{revision}')
            shutil.copytree(copy_from, target_dir, dirs_exist_ok=True, symlinks=True, ignore=shutil.ignore_patterns('.git'))
    elif vcs_type == 'Subversion':
        svn_url = f"{url.rstrip('/')}/{revision.strip('/')}" + (f'/{subpath}' if subpath else '')
        _run_cmd(['svn', 'export', '--force', '--non-interactive', '--', svn_url, target_dir])
    else:
        raise stop(f'unsupported VCS type {vcs_type!r} for {url}')
    return _collect_dir_notices(target_dir)


def _extract_tarball(data, url, target_dir):
    real_target = os.path.realpath(target_dir)
    try:
        with tarfile.open(fileobj=io.BytesIO(data), mode='r:*') as tf:
            entries = []
            for member in tf.getmembers():
                if not member.isfile():
                    continue
                raw_name = member.name
                while raw_name.startswith('./'):
                    raw_name = raw_name[2:]
                if not raw_name or raw_name.startswith('/') or '..' in raw_name.split('/'):
                    continue
                entries.append((member, raw_name))

            # Strip a single wrapper directory (e.g. npm's package/) only when every member shares it.
            top_dirs = {name.split('/', 1)[0] for _, name in entries if '/' in name}
            strip_root = bool(entries) and len(top_dirs) == 1 and all('/' in name for _, name in entries)

            for member, raw_name in entries:
                rel = raw_name.split('/', 1)[1].lstrip('/') if strip_root else raw_name
                if not rel:
                    continue
                dest = os.path.realpath(os.path.join(real_target, rel))
                if os.path.commonpath([real_target, dest]) != real_target:
                    continue
                extracted = tf.extractfile(member)
                if extracted is None:
                    continue
                with extracted:
                    raw = extracted.read()
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                with open(dest, 'wb') as out:
                    out.write(raw)
    except tarfile.TarError as err:
        raise stop(f'corrupt tar archive {url}: {err}')


def _collect_dir_notices(directory):
    notices = []
    for root, dirs, files in os.walk(directory):
        dirs.sort()
        for name in sorted(files):
            full = os.path.join(root, name)
            if os.path.islink(full):
                continue
            rel = os.path.relpath(full, directory).replace(os.sep, '/')
            if is_notice_path(rel):
                with open(full, 'rb') as file:
                    text = clean_text(file.read())
                if text:
                    notices.append({'path': rel, 'text': text})
    return notices


def _run_cmd(cmd, cwd=None):
    try:
        proc = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    except FileNotFoundError:
        raise stop(f'required command {cmd[0]!r} was not found in PATH.')
    if proc.returncode != 0:
        raise stop(f"command {' '.join(cmd)} failed:\n{(proc.stderr or proc.stdout)[-1000:]}")


def _normalize_scancode_batch(raw, sources_by_key, notices_by_key):
    headers = raw.get('headers')
    if not isinstance(headers, list) or not headers or not isinstance(headers[0], dict):
        raise stop(f'ScanCode output from {scancode_image()} is missing required headers metadata.')
    actual_version = headers[0].get('tool_version')
    if actual_version != SCANCODE_VERSION:
        raise stop(
            f'ScanCode version mismatch: image {scancode_image()} ran ScanCode {actual_version!r}, '
            f'but cache keys expect {SCANCODE_VERSION}. Update SCANCODE_VERSION or NOTICES_SCANCODE_IMAGE.'
        )
    errors = []
    for header in headers:
        if isinstance(header, dict):
            for err in header.get('errors', []):
                errors.append(f'header: {err}')
    findings_by_key = {key: [] for key in sources_by_key}
    for entry in raw.get('files', []):
        full_path = entry.get('path', '')
        key, sep, rel_path = full_path.partition('/')
        for err in entry.get('scan_errors', []):
            origin = sources_by_key[key].get('url', key) if key in sources_by_key else full_path
            errors.append(f'{origin} ({rel_path or full_path}): {err}')
        if entry.get('type') != 'file':
            continue
        if not sep or key not in findings_by_key:
            errors.append(f'unexpected ScanCode path {full_path!r} (expected "<key>/<rel_path>")')
            continue
        expr = entry.get('detected_license_expression_spdx') or ''
        lics = [expr] if expr else []
        cprs = [c['copyright'] for c in entry.get('copyrights', []) if c.get('copyright')]
        if lics or cprs:
            findings_by_key[key].append({'path': rel_path, 'licenses': lics, 'copyrights': cprs})
    if errors:
        raise stop('ScanCode reported errors:\n  ' + '\n  '.join(errors[:20]))
    return {
        key: _make_scan_record(source, findings_by_key[key], notices_by_key.get(key, []))
        for key, source in sources_by_key.items()
    }


# ---------------------------------------------------------------------------
# Binary Maven jar NOTICE extraction (with per-jar SHA-256 cache)
# ---------------------------------------------------------------------------

def is_notice_path(path):
    """Whether a path is a NOTICE file: contains 'notice' before its first dot and has '', .txt, or .md extension."""
    name, _, extensions = path.rsplit('/', 1)[-1].lower().partition('.')
    return 'notice' in name and extensions.rsplit('.', 1)[-1] in ('', 'txt', 'md')


def _extract_zip_notices(zip_source):
    notices = []
    with zipfile.ZipFile(zip_source) as archive:
        for info in archive.infolist():
            if not info.is_dir() and is_notice_path(info.filename):
                text = clean_text(archive.read(info))
                if text:
                    notices.append({'path': info.filename, 'text': text})
    return sorted(notices, key=lambda n: n['path'])


def collect_jar_notices(jars, maven_repo, cache_dir):
    """Collects NOTICE files from shipped Maven binary jars (verified by SBOM SHA-256).

    Results are cached per jar SHA-256 under <cache_dir>/jar-notices/<sha256>.json so repeated
    module renders never re-hash or re-open the same jars.
    Returns (found_map {text -> set of (coordinates, path)}, stats_summary_line).
    """
    downloads_dir = os.path.join(cache_dir, 'maven-jars')
    notice_cache_dir = os.path.join(cache_dir, 'jar-notices')

    found = {}
    from_cache = from_m2 = from_dl = downloaded_now = 0
    downloaded_bytes = 0
    downloaded_list = []

    for jar in jars.values():
        cached_path = os.path.join(notice_cache_dir, f'{jar.sha256}.json')
        if os.path.exists(cached_path):
            try:
                with open(cached_path, encoding='utf-8') as file:
                    entries = json.load(file)
                if not isinstance(entries, list):
                    raise ValueError('expected JSON list')
            except (OSError, json.JSONDecodeError, ValueError) as err:
                raise stop(f'corrupt jar-notices cache file {cached_path}: {err}. Remove it and re-run.')
            from_cache += 1
        else:
            jar_path, origin, url = _locate_jar(jar, maven_repo, downloads_dir)
            if origin == 'm2':
                from_m2 += 1
            elif origin == 'earlier':
                from_dl += 1
            else:
                downloaded_now += 1
                downloaded_bytes += os.path.getsize(jar_path)
                downloaded_list.append(f'  {jar.coordinates} ({url})')
            try:
                entries = _extract_zip_notices(jar_path)
            except (OSError, zipfile.BadZipFile) as err:
                raise stop(f'could not read NOTICE files from {jar_path}: {err}. Remove it and re-run.')
            write_atomically(cached_path, json.dumps(entries).encode('utf-8'))

        for item in entries:
            found.setdefault(item['text'], set()).add((jar.coordinates, item['path']))

    summary = (
        f'Jars: {len(jars)} in all; {from_cache} from notice cache, {from_m2} from local Maven repo '
        f'({maven_repo}), {from_dl} downloaded earlier, {downloaded_now} downloaded now '
        f'({downloaded_bytes / 1_000_000:.0f} MB)'
    )
    if downloaded_list:
        summary += ':\n' + '\n'.join(downloaded_list)
    return found, summary


def _locate_jar(jar, maven_repo, downloads_dir):
    rel = maven_path(jar.group, jar.name, jar.version, jar.classifier)
    for origin, candidate in (
        ('m2', os.path.join(maven_repo, rel)),
        ('earlier', os.path.join(downloads_dir, rel)),
    ):
        if os.path.exists(candidate) and _sha256_file(candidate) == jar.sha256:
            return candidate, origin, None
    dest = os.path.join(downloads_dir, rel)
    for url, content in from_maven_repositories(rel, timeout=120):
        if hashlib.sha256(content).hexdigest() == jar.sha256:
            write_atomically(dest, content)
            return dest, 'now', url
    raise stop(
        f"{rel} with the SHA-256 {jar.sha256} of the SBOM is neither in {maven_repo} nor in "
        + ' or '.join(MAVEN_REPOSITORIES)
        + f', so its NOTICE file cannot be read. {BUILD_PROBLEM}'
    )


def _sha256_file(path):
    with open(path, 'rb') as file:
        return hashlib.file_digest(file, 'sha256').hexdigest()
