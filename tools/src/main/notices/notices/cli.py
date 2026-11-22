#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""CLI entry point for ThingsBoard third-party notices.

Subcommands:
  render        Generates THIRD-PARTY-NOTICES from one or more SBOMs (or --sboms-under <dir>)
  cache-upload  Uploads locally cached scan results to Artifactory (CI only)
  ci-should-run Decides whether a PR merge touches any file that affects notices (for TeamCity)
"""
import argparse
import os
import subprocess
import time

from notices import ROOT, emit_teamcity_warning, stop, write_atomically
from notices.cache import ScanCache, scan_cache_key
from notices.config import load_curations, load_policy
from notices.license_files import collect_license_files
from notices.policy import CURATIONS_REL, resolve_all, validate_pre_scan
from notices.render import CE_LICENSE, render_notices
from notices.sbom import discover_sboms, load_sboms
from notices.scanner import (
    SCANCODE_VERSION,
    collect_jar_notices,
    ensure_docker_available,
    resolve_maven_sources,
    resolve_package_source,
    scan_sources_with_scancode,
)

# Paths that only affect ui-ngx's SBOM/notices (can run UI-only check).
CI_UI_ONLY_PREFIXES = (
    'ui-ngx/src/',
    'ui-ngx/esbuild/',
    'ui-ngx/patches/',
)
CI_UI_ONLY_FILES = {
    'ui-ngx/angular.json',
    'ui-ngx/package.json',
    'ui-ngx/tsconfig.json',
    'ui-ngx/yarn.lock',
}

# Paths that only affect Java services (can skip UI/Node modules).
CI_JAVA_ONLY_PREFIXES = (
    'common/report/src/main/resources/fonts/',
    'application/src/main/data/lwm2m-registry/',
    'application/src/main/data/resources/js_extensions/',
    'application/src/main/sbom/',
)

# Paths that always require a full check (JS microservices and notices/SBOM tooling).
CI_FULL_ONLY_PATHS = (
    ':(glob)msa/web-report/**/*.ts',
    ':(glob)msa/web-report/**/*.js',
    'msa/web-report/tsconfig.json',
    'msa/web-report/chromium-credits/',
    'tools/src/main/javascript/yarn-sbom.js',
    'tools/src/main/notices/',
)

# All repository paths whose changes can alter the third-party notices.
# Multi-mode globs (pom.xml, package.json, yarn.lock) are classified per-path in classify_ci_changes().
CI_WATCHED_PATHS = (
    ':(glob)**/pom.xml',
    ':(glob)**/package.json',
    ':(glob)**/yarn.lock',
    *CI_FULL_ONLY_PATHS,
    *CI_UI_ONLY_PREFIXES,
    *sorted(CI_UI_ONLY_FILES),
    *CI_JAVA_ONLY_PREFIXES,
)

# POMs that affect UI/JS modules or global build settings and therefore require a full check.
CI_FULL_POM_FILES = {
    'pom.xml',
    'msa/pom.xml',
    'ui-ngx/pom.xml',
    'msa/web-ui/pom.xml',
    'msa/js-executor/pom.xml',
    'msa/web-report/pom.xml',
}


def classify_ci_changes(changed_paths):
    """Classifies changed watched paths into 'none', 'ui', 'java', or 'full'."""
    if not changed_paths:
        return 'none'
    modes = set()
    for path in changed_paths:
        if path in CI_UI_ONLY_FILES or path.startswith(CI_UI_ONLY_PREFIXES):
            modes.add('ui')
        elif (
            path.startswith(CI_JAVA_ONLY_PREFIXES)
            or ((path == 'pom.xml' or path.endswith('/pom.xml')) and path not in CI_FULL_POM_FILES)
        ):
            modes.add('java')
        else:
            return 'full'
    return next(iter(modes)) if len(modes) == 1 else 'full'


def main(argv=None):
    parser = argparse.ArgumentParser(prog='notices', description='ThingsBoard third-party notices generator')
    sub = parser.add_subparsers(dest='command', required=True)

    render_p = sub.add_parser('render', help='Generate THIRD-PARTY-NOTICES from CycloneDX SBOMs')
    render_p.add_argument('--project-version', required=True)
    render_p.add_argument('--out', required=True, help='Output path for THIRD-PARTY-NOTICES')
    render_p.add_argument('--sboms-under', help='Discover all target/sbom/*.cdx.json under this directory')
    render_p.add_argument('sboms', nargs='*')
    render_p.set_defaults(func=cmd_render)

    upload_p = sub.add_parser('cache-upload', help='Upload new local scan results to remote cache')
    upload_p.set_defaults(func=cmd_cache_upload)

    ci_p = sub.add_parser('ci-should-run', help='Check if merging HEAD into target branch affects notices')
    ci_p.add_argument('--target-branch', required=True)
    ci_p.set_defaults(func=cmd_ci_should_run)

    args = parser.parse_args(argv)
    return args.func(args)


def _parse_env_bool(name):
    raw = os.environ.get(name, '').strip().lower()
    if raw in ('', '0', 'false', 'no'):
        return False
    if raw in ('1', 'true', 'yes'):
        return True
    raise stop(f'invalid {name} value {os.environ.get(name)!r} (expected true/false, yes/no, or 1/0)')


def cmd_render(args):
    t0 = time.monotonic()
    fail_on_unused = _parse_env_bool('NOTICES_FAIL_ON_UNUSED_CURATIONS')
    policy = load_policy()
    curations = load_curations()
    scan_cache = ScanCache()

    sbom_paths = list(args.sboms)
    if args.sboms_under:
        discovered = discover_sboms(os.path.realpath(args.sboms_under))
        print(f'Third-party notices from {len(discovered)} SBOMs')
        sbom_paths = discovered + sbom_paths
    if not sbom_paths:
        raise stop('no SBOMs given (pass SBOM paths or --sboms-under <directory>)')

    inventory = load_sboms(sbom_paths, args.project_version, policy, check_version=bool(args.sboms_under))
    maven_sources = resolve_maven_sources(inventory.packages, curations, scan_cache.cache_dir)

    # Resolve source descriptors and fail fast on pre-scan policy issues before hitting remote cache/Docker.
    sources = {purl: resolve_package_source(pkg, curations, maven_sources) for purl, pkg in sorted(inventory.packages.items())}
    declared_by_purl = validate_pre_scan(
        inventory.packages, sources, policy, curations, license_files=inventory.license_files
    )
    keys_by_purl = {
        purl: scan_cache_key(SCANCODE_VERSION, src)
        for purl, src in sources.items()
        if src and src['type'] != 'metadata_only'
    }

    # Pull missing scan records from remote cache in parallel
    t_cache = time.monotonic()
    fetched_records, still_missing = scan_cache.fetch_missing(list(keys_by_purl.values()))
    cache_s = time.monotonic() - t_cache

    # Scan only genuinely uncached packages via ScanCode in Docker (serialized across parallel builds)
    t_scan = time.monotonic()
    records_by_key = dict(fetched_records)
    missing_set = set(still_missing)
    scanned_now = 0
    if missing_set:
        with scan_cache.scan_lock():
            to_scan = []
            seen_keys = set()
            for purl, key in sorted(keys_by_purl.items()):
                if key not in missing_set or key in records_by_key or key in seen_keys:
                    continue
                seen_keys.add(key)
                if scan_cache.has_local(key):
                    continue
                to_scan.append((key, purl, sources[purl]))
            if to_scan:
                ensure_docker_available()
                for key, record in scan_sources_with_scancode(to_scan):
                    scan_cache.write_local(key, record)
                    records_by_key[key] = record
                    scanned_now += 1
    scan_s = time.monotonic() - t_scan

    # Map scan records by PURL (reusing in-memory records and reading each local key at most once)
    scan_records = {}
    npm_notices = {}
    for purl, key in keys_by_purl.items():
        if key not in records_by_key:
            local = scan_cache.read_local(key)
            if local is None:
                raise stop(f'cached scan file {scan_cache.scan_path(key)} for {purl} disappeared during run; re-run.')
            records_by_key[key] = local
        record = records_by_key[key]
        scan_records[purl] = record
        pkg = inventory.packages[purl]
        if pkg.kind == 'npm':
            for item in record.get('notices', []):
                npm_notices.setdefault(item['text'], set()).add((pkg.coordinates, item['path']))

    # Policy evaluation (fail-closed)
    matched_ignores = set()
    resolved = resolve_all(
        inventory.packages, sources, scan_records, policy, curations,
        declared_by_purl=declared_by_purl, license_files=inventory.license_files,
        matched_ignores=matched_ignores,
    )
    check_unused = bool(args.sboms_under) and not _parse_env_bool('NOTICES_SKIP_UNUSED_CURATIONS')
    unused_body = (
        _unused_curations_message(curations, inventory.packages.values(), resolved, matched_ignores=matched_ignores)
        if check_unused else None
    )
    if unused_body and fail_on_unused:
        raise stop(unused_body)

    # Collect binary Maven jar NOTICE files + merge with npm NOTICE files
    maven_repo = os.path.abspath(os.path.expanduser(os.environ.get('NOTICES_MAVEN_REPOSITORY') or '~/.m2/repository'))
    all_notices, jar_summary = collect_jar_notices(inventory.jars, maven_repo, scan_cache.cache_dir)
    for text, carriers in npm_notices.items():
        all_notices.setdefault(text, set()).update(carriers)

    # Collect bundled license files (Node.js URL, Chromium CREDITS.txt, on-the-fly LwM2M registry)
    bundled_license_files = collect_license_files(inventory.license_files, scan_cache.cache_dir)

    # Render deterministically
    rendered = render_notices(
        resolved_packages=resolved,
        license_files=bundled_license_files,
        notice_files=all_notices,
        policy=policy,
        custom_licenses=curations.custom_licenses,
        cache_dir=scan_cache.cache_dir,
    )

    write_atomically(args.out, rendered.encode('utf-8'))

    total_s = time.monotonic() - t0
    lines = rendered.count('\n')
    print(
        f'Third-party notices: {len(inventory.packages)} packages, {lines} lines: {args.out}\n'
        f'Third-party notices: took {total_s:.1f}s '
        f'(cache fetched {len(fetched_records)} in {cache_s:.1f}s, scanned {scanned_now} in {scan_s:.1f}s)\n'
        f'{jar_summary}'
    )
    if unused_body:
        _emit_unused_curations_warning(unused_body)
    return 0


def _unused_curations_message(curations, packages, resolved, matched_ignores=None):
    used_licenses = {CE_LICENSE}
    for pkg in resolved:
        for lic in pkg.licenses:
            full = str(lic)
            if full in curations.custom_licenses:
                used_licenses.add(full)
            else:
                used_licenses.add(lic.id)
                if lic.exception:
                    used_licenses.add(lic.exception)
    unused_curations, unused_custom = curations.unused(packages, used_licenses=used_licenses)
    unused_ignores = curations.unused_ignores(packages, matched_ignores) if matched_ignores is not None else []
    unused = unused_curations + unused_ignores + [f'[custom_licenses] {name}' for name in unused_custom]
    if not unused:
        return None
    lines = ''.join(f'  {item}\n' for item in unused)
    noun = 'entry' if len(unused) == 1 else 'entries'
    return (
        f'{len(unused)} {noun} in {CURATIONS_REL} matched no shipped package or scan finding.\n'
        + lines
        + f'Remove obsolete entries from {CURATIONS_REL}.'
    )


def _emit_unused_curations_warning(body):
    full = f'Third-party notices: warning: {body}'
    print(full, flush=True)
    if os.environ.get('TEAMCITY_VERSION'):
        emit_teamcity_warning(full)


def cmd_cache_upload(args):
    cache = ScanCache()
    if not cache.remote_url:
        print('Scan cache: upload disabled (NOTICES_SCAN_CACHE_URL=none)')
        return 0
    uploaded, skipped = cache.upload_local()
    print(f'Scan cache: uploaded {uploaded} new scans ({skipped} already present) to {cache.remote_url}')
    return 0


def cmd_ci_should_run(args):
    """Classifies whether merging HEAD into target_branch needs 'none', 'ui', 'java', or 'full' notice checks."""
    on_teamcity = bool(os.environ.get('TEAMCITY_VERSION'))
    repo_url = os.environ.get('NOTICES_CI_REPO_URL') or 'https://github.com/thingsboard/thingsboard.git'

    def emit_mode(mode, reason):
        print(f'{reason}: notices check mode = {mode}.')
        if on_teamcity:
            print(f"##teamcity[setParameter name='notices.check' value='{mode}']")
            if mode == 'java':
                print("##teamcity[setParameter name='env.NOTICES_SKIP_UNUSED_CURATIONS' value='true']")
            elif mode == 'none':
                print("##teamcity[buildStatus text='No dependency changes: the notices are not checked']")
        return 0

    fetch = subprocess.run(
        ['git', 'fetch', '--quiet', '--no-tags', repo_url, f'refs/heads/{args.target_branch}'],
        cwd=ROOT, capture_output=True, text=True,
    )
    if fetch.returncode != 0:
        return emit_mode('full', f'{args.target_branch} could not be fetched')

    merged = subprocess.run(
        ['git', 'merge-tree', '--write-tree', '--no-messages', 'FETCH_HEAD', 'HEAD'],
        cwd=ROOT, capture_output=True, text=True,
    )
    if merged.returncode != 0:
        return emit_mode('full', f'PR does not merge cleanly into {args.target_branch}')
    tree = merged.stdout.strip().splitlines()[0]

    diff = subprocess.run(
        ['git', 'diff', '--no-renames', '--name-only', 'FETCH_HEAD', tree, '--', *CI_WATCHED_PATHS],
        cwd=ROOT, capture_output=True, text=True,
    )
    if diff.returncode != 0:
        return emit_mode('full', f'merge could not be diffed against {args.target_branch}')

    changed = [line for line in diff.stdout.splitlines() if line.strip()]
    mode = classify_ci_changes(changed)
    if changed:
        print(f'Merging into {args.target_branch} changes:')
        for path in changed:
            print(f'  {path}')
        return emit_mode(mode, 'Dependency/notice inputs changed')

    return emit_mode('none', f'Merging into {args.target_branch} changes no file that the notices depend on')
