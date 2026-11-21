#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""CLI entry point for ThingsBoard third-party notices.

Subcommands:
  render        Generates THIRD-PARTY-NOTICES from one or more SBOMs (or --sboms-under <dir>)
  cache-upload  Uploads locally cached scan results to Artifactory (CI only)
  migrate-ort   One-shot migration from legacy ~/.ort/scanner into the per-package cache
  ci-should-run Decides whether a PR merge touches any file that affects notices (for TeamCity)
"""
import argparse
import os
import subprocess
import sys
import time

from notices import ROOT, stop, write_atomically
from notices.cache import DEFAULT_CACHE_DIR, ScanCache, scan_cache_key
from notices.config import load_curations, load_policy
from notices.license_files import collect_license_files
from notices.policy import resolve_all
from notices.render import render_notices
from notices.sbom import discover_sboms, load_sboms
from notices.scanner import (
    SCANCODE_VERSION,
    collect_jar_notices,
    migrate_ort_cache,
    resolve_maven_sources,
    resolve_package_source,
    scan_source_with_scancode,
)

# All repository paths whose changes can alter the third-party notices.
CI_WATCHED_PATHS = (
    ':(glob)**/pom.xml',
    ':(glob)**/package.json',
    ':(glob)**/yarn.lock',
    'ui-ngx/angular.json',
    'ui-ngx/src/',
    'application/src/main/data/lwm2m-registry/',
    'application/src/main/sbom/',
    'msa/web-report/chromium.js',
    'msa/web-report/install.js',
    'msa/web-report/chromium-credits/',
    'tools/src/main/javascript/yarn-sbom.js',
    'tools/src/main/notices/',
    'THIRD-PARTY-NOTICES',
)


def main(argv=None):
    parser = argparse.ArgumentParser(prog='notices', description='ThingsBoard third-party notices generator')
    sub = parser.add_subparsers(dest='command', required=True)

    render_p = sub.add_parser('render', help='Generate THIRD-PARTY-NOTICES from CycloneDX SBOMs')
    render_p.add_argument('--project-version', required=True)
    render_p.add_argument('--out', required=True, help='Output path for THIRD-PARTY-NOTICES')
    render_p.add_argument('--sboms-under', help='Discover all target/sbom/*.cdx.json under this directory')
    render_p.add_argument('--cache-dir', default=os.environ.get('NOTICES_CACHE_DIR', DEFAULT_CACHE_DIR))
    render_p.add_argument('--cache-url', default=None, help='Remote scan cache base URL, or "none"')
    render_p.add_argument('--maven-repository', default=os.environ.get('NOTICES_MAVEN_REPOSITORY', os.path.expanduser('~/.m2/repository')))
    render_p.add_argument('--no-scan', action='store_true', help='Fail if any package is missing from cache instead of invoking ScanCode')
    render_p.add_argument('sboms', nargs='*')

    upload_p = sub.add_parser('cache-upload', help='Upload new local scan results to remote cache')
    upload_p.add_argument('--cache-dir', default=os.environ.get('NOTICES_CACHE_DIR', DEFAULT_CACHE_DIR))
    upload_p.add_argument('--cache-url', default=None)

    migrate_p = sub.add_parser('migrate-ort', help='Migrate legacy ~/.ort/scanner cache into per-package cache')
    migrate_p.add_argument('--ort-home', default=os.environ.get('NOTICES_ORT_HOME', os.path.expanduser('~/.ort')))
    migrate_p.add_argument('--cache-dir', default=os.environ.get('NOTICES_CACHE_DIR', DEFAULT_CACHE_DIR))

    ci_p = sub.add_parser('ci-should-run', help='Check if merging HEAD into target branch affects notices')
    ci_p.add_argument('--target-branch', required=True)
    ci_p.add_argument('--repo-url', default='https://github.com/thingsboard/thingsboard.git')
    ci_p.add_argument('--teamcity', action='store_true', help='Emit TeamCity service messages')

    args = parser.parse_args(argv)
    if args.command == 'render':
        return cmd_render(args)
    if args.command == 'cache-upload':
        return cmd_cache_upload(args)
    if args.command == 'migrate-ort':
        return cmd_migrate_ort(args)
    if args.command == 'ci-should-run':
        return cmd_ci_should_run(args)
    return 2


def cmd_render(args):
    t0 = time.monotonic()
    policy = load_policy()
    curations = load_curations()
    scan_cache = ScanCache(cache_dir=args.cache_dir, remote_url=args.cache_url)

    sbom_paths = list(args.sboms)
    if args.sboms_under:
        discovered = discover_sboms(os.path.realpath(args.sboms_under), args.project_version)
        print(f'Third-party notices from {len(discovered)} SBOMs')
        sbom_paths = discovered + sbom_paths
    if not sbom_paths:
        raise stop('no SBOMs given (pass SBOM paths or --sboms-under <directory>)')

    inventory = load_sboms(sbom_paths, args.project_version, policy)
    maven_sources = resolve_maven_sources(inventory.packages, curations, scan_cache.cache_dir)

    # Resolve source descriptors and compute cache keys for every scannable package
    sources = {}
    keys_by_purl = {}
    for purl, pkg in sorted(inventory.packages.items()):
        src = resolve_package_source(pkg, curations, maven_sources)
        sources[purl] = src
        if src and src['type'] != 'metadata_only':
            keys_by_purl[purl] = scan_cache_key(SCANCODE_VERSION, src)

    # Pull missing scan records from remote cache in parallel
    t_cache = time.monotonic()
    fetched, still_missing, cache_errors = scan_cache.fetch_missing(list(keys_by_purl.values()))
    if cache_errors:
        raise stop(
            f'remote scan cache {scan_cache.remote_url} had errors ({"; ".join(cache_errors[:3])}), '
            'so the notices stop instead of rescanning everything. '
            'Check network access or set NOTICES_SCAN_CACHE_URL=none to scan locally.'
        )
    cache_s = time.monotonic() - t_cache

    # Scan only genuinely uncached packages via ScanCode in Docker
    t_scan = time.monotonic()
    missing_set = set(still_missing)
    scanned_now = 0
    if missing_set:
        if args.no_scan:
            missing_purls = [p for p, k in keys_by_purl.items() if k in missing_set]
            raise stop(f'{len(missing_purls)} packages are not in cache (--no-scan):\n  ' + '\n  '.join(missing_purls[:20]))
        seen_keys = set()
        for purl, key in sorted(keys_by_purl.items()):
            if key in missing_set and key not in seen_keys:
                seen_keys.add(key)
                print(f'Scanning {purl} ...', flush=True)
                record = scan_source_with_scancode(sources[purl], policy.exclude_paths, scan_cache.cache_dir)
                scan_cache.write_local(key, record)
                scanned_now += 1
    scan_s = time.monotonic() - t_scan

    # Load all cached scan records into memory
    scan_records = {}
    npm_notices = {}
    for purl, key in keys_by_purl.items():
        record = scan_cache.read_local(key)
        scan_records[purl] = record
        pkg = inventory.packages[purl]
        if pkg.kind == 'npm':
            for item in record.get('notices', []):
                npm_notices.setdefault(item['text'], set()).add((pkg.coordinates, item['path']))

    # Policy evaluation (fail-closed)
    resolved = resolve_all(inventory.packages, sources, scan_records, policy, curations)

    # Collect binary Maven jar NOTICE files + merge with npm NOTICE files
    jar_notices, jar_summary = collect_jar_notices(inventory.jars, args.maven_repository, scan_cache.cache_dir)
    all_notices = {text: set(carriers) for text, carriers in jar_notices.items()}
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
        f'(cache fetched {fetched} in {cache_s:.1f}s, scanned {scanned_now} in {scan_s:.1f}s)\n'
        f'{jar_summary}'
    )
    return 0


def cmd_cache_upload(args):
    cache = ScanCache(cache_dir=args.cache_dir, remote_url=args.cache_url)
    if not cache.remote_url:
        print('Scan cache: upload disabled (NOTICES_SCAN_CACHE_URL=none)')
        return 0
    uploaded, skipped = cache.upload_local()
    print(f'Scan cache: uploaded {uploaded} new scans ({skipped} already present) to {cache.remote_url}')
    return 0


def cmd_migrate_ort(args):
    cache = ScanCache(cache_dir=args.cache_dir, remote_url='none')
    curations = load_curations()
    migrated, skipped = migrate_ort_cache(args.ort_home, cache, curations=curations)
    print(f'Migrated {migrated} ORT scan results into {cache.cache_dir} ({skipped} already present)')
    return 0


def cmd_ci_should_run(args):
    """Checks if merging HEAD into target_branch modifies any notice-affecting file."""
    def emit_check(reason):
        print(f'{reason}: the notices will be checked.')
        if args.teamcity:
            print("##teamcity[setParameter name='notices.check' value='true']")
        return 0

    fetch = subprocess.run(
        ['git', 'fetch', '--quiet', '--no-tags', args.repo_url, f'refs/heads/{args.target_branch}'],
        cwd=ROOT, capture_output=True, text=True,
    )
    if fetch.returncode != 0:
        return emit_check(f'{args.target_branch} could not be fetched')

    merged = subprocess.run(
        ['git', 'merge-tree', '--write-tree', '--no-messages', 'FETCH_HEAD', 'HEAD'],
        cwd=ROOT, capture_output=True, text=True,
    )
    if merged.returncode != 0:
        return emit_check(f'PR does not merge cleanly into {args.target_branch}')
    tree = merged.stdout.strip().splitlines()[0]

    diff = subprocess.run(
        ['git', 'diff', '--no-renames', '--name-only', 'FETCH_HEAD', tree, '--', *CI_WATCHED_PATHS],
        cwd=ROOT, capture_output=True, text=True,
    )
    if diff.returncode != 0:
        return emit_check(f'merge could not be diffed against {args.target_branch}')

    changed = [line for line in diff.stdout.splitlines() if line.strip()]
    if changed:
        print(f'Merging into {args.target_branch} changes:')
        for path in changed:
            print(f'  {path}')
        return emit_check('Dependency/notice inputs changed')

    print(f'Merging into {args.target_branch} changes no file that the notices depend on.')
    if args.teamcity:
        print("##teamcity[buildStatus text='No dependency changes: the notices are not checked']")
    return 0
