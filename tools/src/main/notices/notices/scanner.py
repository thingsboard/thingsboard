#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Source resolution, direct ScanCode Docker execution, NOTICE extraction, and ORT cache migration."""
import concurrent.futures
import hashlib
import io
import json
import lzma
import os
import re
import shutil
import subprocess
import tarfile
import tempfile
import urllib.error
import urllib.parse
import zipfile

from notices import BUILD_PROBLEM, HTTP_ERRORS, clean_text, http_get, ort_home, stop, write_atomically
from notices.cache import scan_cache_key

SCANCODE_VERSION = '32.5.0'
DEFAULT_SCANCODE_IMAGE = 'ghcr.io/oss-review-toolkit/ort:98.0.0'
MAVEN_REPOSITORIES = [
    'https://repo1.maven.org/maven2',
    'https://repo.thingsboard.io/artifactory/libs-release-public',
]
SHA1_RE = re.compile(r'^[0-9a-f]{40}$')


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

    # Seed from legacy ~/.ort/maven-sources.json if present so warm developer machines skip lookups.
    seeded = False
    legacy = os.path.join(ort_home(), 'maven-sources.json')
    if not known and os.path.exists(legacy):
        known = _read_sources_cache(legacy)
        seeded = bool(known)

    def has_source_curation(pkg):
        return any(c.metadata_only or c.vcs for c in curations.for_package(pkg.package_key, pkg.version))

    missing = [pkg for pkg in packages.values() if pkg.kind == 'maven' and pkg.purl not in known and not has_source_curation(pkg)]
    found = {}
    if missing:
        def lookup(pkg):
            path = maven_path(pkg.group, pkg.name, pkg.version, 'sources')
            for url, content in from_maven_repositories(path + '.sha1', timeout=30):
                tokens = content.decode('utf-8', 'replace').split()
                sha1 = tokens[0].lower() if tokens else ''
                if not SHA1_RE.match(sha1):
                    raise stop(f'{url} is no SHA-1: {sha1!r}. {BUILD_PROBLEM}')
                return pkg.purl, {'url': url.removesuffix('.sha1'), 'sha1': sha1}
            return pkg.purl, None

        try:
            with concurrent.futures.ThreadPoolExecutor(16) as pool:
                for purl, source in pool.map(lookup, missing):
                    if source:
                        found[purl] = source
        finally:
            if found or (seeded and not os.path.exists(cache_path)):
                merged = {**known, **_read_sources_cache(cache_path), **found}
                write_atomically(cache_path, json.dumps(merged, indent=1, sort_keys=True).encode('utf-8'))
                known = merged
    elif seeded and not os.path.exists(cache_path):
        write_atomically(cache_path, json.dumps(known, indent=1, sort_keys=True).encode('utf-8'))

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
            'Install Docker or run with a warm scan cache. See docs/licensing-guide.md#regenerating-the-notices.'
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


def scan_source_with_scancode(source, exclude_paths):
    """Downloads/extracts a source archive or VCS checkout, runs ScanCode in Docker, and returns a normalized record."""
    image = os.environ.get('NOTICES_SCANCODE_IMAGE', DEFAULT_SCANCODE_IMAGE)
    with tempfile.TemporaryDirectory(prefix='tb-scancode-') as work:
        src_dir = os.path.join(work, 'src')
        os.makedirs(src_dir)
        notices = _fetch_and_extract(source, src_dir)
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
        ]
        for pattern in exclude_paths:
            # ScanCode --ignore uses glob patterns without leading **/ when matching anywhere
            cmd.extend(['--ignore', pattern.removeprefix('**/')])
        cmd.extend(['--json-pp', '/work/scancode.json', '/work/src'])

        try:
            proc = subprocess.run(cmd, capture_output=True, text=True)
        except FileNotFoundError:
            raise stop('docker is required to scan uncached packages, but `docker` was not found in PATH.')
        if proc.returncode != 0 or not os.path.exists(out_json):
            raise stop(
                f"ScanCode failed for {source.get('url')} (exit {proc.returncode}):\n"
                f'{(proc.stderr or proc.stdout)[-1500:]}'
            )
        with open(out_json, encoding='utf-8') as file:
            raw = json.load(file)
        return _normalize_scancode_output(raw, source, notices)


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
        return _extract_tarball(data, url, target_dir)

    # VCS checkout (Git or Subversion)
    vcs_type = source['vcs_type']
    url, revision, subpath = source['url'], source['revision'], source.get('path', '').strip('/')
    if vcs_type == 'Git':
        with tempfile.TemporaryDirectory(prefix='tb-git-') as repo_dir:
            _run_cmd(['git', 'init', '-q'], cwd=repo_dir)
            _run_cmd(['git', 'remote', 'add', 'origin', url], cwd=repo_dir)
            _run_cmd(['git', 'fetch', '--depth', '1', 'origin', revision], cwd=repo_dir)
            _run_cmd(['git', 'checkout', '-q', 'FETCH_HEAD'], cwd=repo_dir)
            real_repo = os.path.realpath(repo_dir)
            copy_from = os.path.realpath(os.path.join(real_repo, subpath)) if subpath else real_repo
            if os.path.commonpath([real_repo, copy_from]) != real_repo or not os.path.isdir(copy_from):
                raise stop(f'VCS subpath {subpath!r} does not exist inside {url}@{revision}')
            shutil.copytree(copy_from, target_dir, dirs_exist_ok=True, symlinks=True, ignore=shutil.ignore_patterns('.git'))
    elif vcs_type == 'Subversion':
        svn_url = f"{url.rstrip('/')}/{revision.strip('/')}" + (f'/{subpath}' if subpath else '')
        _run_cmd(['svn', 'export', '--force', '--non-interactive', svn_url, target_dir])
    else:
        raise stop(f'unsupported VCS type {vcs_type!r} for {url}')
    return _collect_dir_notices(target_dir)


def _extract_tarball(data, url, target_dir):
    real_target = os.path.realpath(target_dir)
    notices = []
    try:
        with tarfile.open(fileobj=io.BytesIO(data), mode='r:*') as tf:
            for member in tf.getmembers():
                if not member.isfile():
                    continue
                raw_name = member.name[2:] if member.name.startswith('./') else member.name
                if raw_name.startswith('/') or '..' in raw_name.split('/'):
                    continue
                # Strip leading package/ directory (standard npm tarball root)
                parts = raw_name.split('/', 1)
                rel = (parts[1] if len(parts) == 2 else parts[0]).lstrip('/')
                if not rel or '..' in rel.split('/'):
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
                if is_notice_path(rel):
                    text = clean_text(raw)
                    if text:
                        notices.append({'path': rel, 'text': text})
    except tarfile.TarError as err:
        raise stop(f'corrupt tar archive {url}: {err}')
    return notices


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


def _normalize_scancode_output(raw, source, notices):
    findings = []
    errors = []
    for entry in raw.get('files', []):
        if entry.get('type') != 'file':
            continue
        path = entry.get('path', '')
        for err in entry.get('scan_errors', []):
            errors.append(f'{path}: {err}')
        expr = entry.get('detected_license_expression_spdx') or ''
        lics = [expr] if expr else []
        cprs = [c['copyright'] for c in entry.get('copyrights', []) if c.get('copyright')]
        if lics or cprs:
            findings.append({'path': path, 'licenses': lics, 'copyrights': cprs})
    if errors:
        raise stop(
            f"ScanCode reported errors for {source.get('url')}:\n  " + '\n  '.join(errors[:20])
        )
    return _make_scan_record(source, findings, notices)


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
    legacy_downloads = os.path.join(ort_home(), 'maven-jars')
    notice_cache_dir = os.path.join(cache_dir, 'jar-notices')

    found = {}
    from_cache = from_m2 = from_dl = downloaded_now = 0
    downloaded_bytes = 0
    downloaded_list = []

    for jar in jars.values():
        cached_path = os.path.join(notice_cache_dir, f'{jar.sha256}.json')
        if os.path.exists(cached_path):
            with open(cached_path, encoding='utf-8') as file:
                entries = json.load(file)
            from_cache += 1
        else:
            jar_path, origin, url = _locate_jar(jar, maven_repo, downloads_dir, legacy_downloads)
            if origin == 'm2':
                from_m2 += 1
            elif origin == 'earlier':
                from_dl += 1
            else:
                downloaded_now += 1
                downloaded_bytes += os.path.getsize(jar_path)
                downloaded_list.append(f'  {jar.coordinates} ({url})')
            entries = _extract_zip_notices(jar_path)
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


def _locate_jar(jar, maven_repo, downloads_dir, legacy_downloads):
    rel = maven_path(jar.group, jar.name, jar.version, jar.classifier)
    for origin, candidate in (
        ('m2', os.path.join(maven_repo, rel)),
        ('earlier', os.path.join(downloads_dir, rel)),
        ('earlier', os.path.join(legacy_downloads, rel)),
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


# ---------------------------------------------------------------------------
# One-shot migration from legacy ~/.ort/scanner into our per-package ScanCache
# ---------------------------------------------------------------------------

def migrate_ort_cache(legacy_ort_home, scan_cache, curations=None):
    """Converts ORT's scanner/results YAML files + scanner/archive NOTICE files into ScanCache format.

    Avoids multi-hour rescanning when moving off ORT. Also slices repo-root VCS scans for any
    curated VCS subpath (e.g. springdoc-swagger-ui, netty/all, xml-commons/java/external).
    """
    results_root = os.path.join(legacy_ort_home, 'scanner', 'results')
    archive_root = os.path.join(legacy_ort_home, 'scanner', 'archive')
    if not os.path.isdir(results_root):
        raise stop(f'no ORT scan results directory found at {results_root}')

    curated_vcs = [c.vcs for c in (curations.entries if curations else ()) if c.vcs]

    migrated = skipped = 0
    for root, _, files in os.walk(results_root):
        for name in files:
            if name != 'scan-results.yml':
                continue
            path = os.path.join(root, name)
            records = _parse_ort_scan_results_yml(path)
            for source, findings, archive_key, declared_rev in records:
                targets = [(source, findings)]
                if source['type'] == 'vcs' and not source['path']:
                    # Also index repo-root scans under declared revision (e.g. SVN tag) and curated subpaths.
                    revs = {source['revision']} | ({declared_rev} if declared_rev else set())
                    for vcs in curated_vcs:
                        if vcs.get('url') == source['url'] and vcs.get('revision') in revs:
                            subpath = vcs.get('path', '').strip('/')
                            sliced = _slice_findings(findings, subpath) if subpath else findings
                            targets.append((
                                _vcs_source(vcs.get('type', source['vcs_type']), vcs['url'], vcs['revision'], subpath),
                                sliced,
                            ))
                notices = None
                for target_source, target_findings in targets:
                    key = scan_cache_key(SCANCODE_VERSION, target_source)
                    if scan_cache.has_local(key):
                        skipped += 1
                        continue
                    if notices is None:
                        notices = _read_ort_archive_notices(archive_root, archive_key)
                    scan_cache.write_local(key, _make_scan_record(target_source, target_findings, notices))
                    migrated += 1
    return migrated, skipped


def _slice_findings(findings, subpath):
    prefix = subpath + '/'
    sliced = []
    for f in findings:
        fpath = f['path']
        if fpath.startswith(prefix):
            sliced.append({**f, 'path': fpath[len(prefix):]})
        elif fpath == subpath:
            sliced.append(f)
    return sliced


def _read_ort_archive_notices(archive_root, archive_key):
    if not archive_key:
        return []
    path = os.path.join(archive_root, hashlib.sha1(archive_key.encode('utf-8')).hexdigest(), 'archive.zip.xz')
    if not os.path.exists(path):
        return []
    with lzma.open(path) as compressed:
        return _extract_zip_notices(io.BytesIO(compressed.read()))


def _parse_ort_scan_results_yml(path):
    """Parses ORT's flat, quoted scan-results.yml using stdlib only."""
    results = []
    for entry in _parse_ort_yml_stdlib(path):
        prov = entry.get('provenance') or {}
        declared_rev = ''
        if 'source_artifact' in prov:
            sa = prov['source_artifact']
            source = _artifact_source(sa['url'], sa['hash']['value'])
            archive_key = sa['url'] + sa['hash']['value']
        elif 'vcs_info' in prov:
            vcs = prov['vcs_info']
            declared_rev = vcs.get('revision', '')
            rev = prov.get('resolved_revision') or declared_rev
            source = _vcs_source(vcs['type'], vcs['url'], rev, vcs.get('path', ''))
            archive_key = vcs['type'] + vcs['url'] + rev
        else:
            continue

        summary = entry.get('summary') or {}
        by_path = {}
        for lic in summary.get('licenses') or []:
            fpath = (lic.get('location') or {}).get('path', '')
            by_path.setdefault(fpath, {'licenses': [], 'copyrights': []})['licenses'].append(lic['license'])
        for cpr in summary.get('copyrights') or []:
            fpath = (cpr.get('location') or {}).get('path', '')
            by_path.setdefault(fpath, {'licenses': [], 'copyrights': []})['copyrights'].append(cpr['statement'])
        findings = [
            {'path': fpath, 'licenses': data['licenses'], 'copyrights': data['copyrights']}
            for fpath, data in sorted(by_path.items())
        ]
        results.append((source, findings, archive_key, declared_rev))
    return results


def _parse_ort_yml_stdlib(path):
    """Minimal stdlib parser for ORT's flat, quoted scan-results.yml format."""
    def unquote(val):
        val = val.strip()
        if len(val) >= 2 and val[0] == '"' and val[-1] == '"':
            return json.loads(val)
        return val

    entries, cur, section, item = [], None, None, None
    sub = None
    with open(path, encoding='utf-8') as file:
        for raw in file:
            line = raw.rstrip('\n')
            if not line or line == '---':
                continue
            if line.startswith('- provenance:'):
                cur = {'provenance': {}, 'summary': {'licenses': [], 'copyrights': []}}
                entries.append(cur)
                section = 'provenance'
                sub = None
                continue
            if cur is None:
                continue
            if line.startswith('  summary:'):
                section = 'summary'
                sub = None
            elif line.startswith('  scanner:'):
                section = 'scanner'
            elif section == 'provenance':
                if line.startswith('    source_artifact:'):
                    cur['provenance']['source_artifact'] = {'hash': {}}
                    sub = 'sa'
                elif line.startswith('    vcs_info:'):
                    cur['provenance']['vcs_info'] = {}
                    sub = 'vcs'
                elif line.startswith('    resolved_revision:'):
                    cur['provenance']['resolved_revision'] = unquote(line.split(':', 1)[1])
                elif sub == 'sa' and line.startswith('      url:'):
                    cur['provenance']['source_artifact']['url'] = unquote(line.split(':', 1)[1])
                elif sub == 'sa' and line.startswith('        value:'):
                    cur['provenance']['source_artifact']['hash']['value'] = unquote(line.split(':', 1)[1])
                elif sub == 'vcs' and line.startswith('      '):
                    k, _, v = line.strip().partition(':')
                    cur['provenance']['vcs_info'][k] = unquote(v)
            elif section == 'summary':
                if line == '    licenses:':
                    sub = 'licenses'
                elif line == '    copyrights:':
                    sub = 'copyrights'
                elif line.startswith('    ') and not line.startswith('    - ') and not line.startswith('      '):
                    sub = None
                elif sub == 'licenses' and line.startswith('    - license:'):
                    item = {'license': unquote(line.split(':', 1)[1]), 'location': {}}
                    cur['summary']['licenses'].append(item)
                elif sub == 'copyrights' and line.startswith('    - statement:'):
                    item = {'statement': unquote(line.split(':', 1)[1]), 'location': {}}
                    cur['summary']['copyrights'].append(item)
                elif item is not None and line.startswith('        path:'):
                    item['location']['path'] = unquote(line.split(':', 1)[1])
    return entries
