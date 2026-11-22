#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Collects license files for non-Maven/non-npm bundled components (Node.js, Chromium, LwM2M)."""
import hashlib
import os
import re

from notices import HTTP_ERRORS, ROOT, clean_text, http_get, stop, write_atomically
from notices.lwm2m import generate_lwm2m_notice

SECTION_RE = re.compile(r'^======== Only in the (.+) build ========$')


def collect_license_files(refs, cache_dir, root=ROOT):
    """Returns {text -> set of (component_coordinates, origin)} for all bundled license-file components."""
    downloads_dir = os.path.join(cache_dir, 'license-files')
    platforms = _platforms_by_path(refs)
    found = {}
    cached_repo_texts = {}

    for ref in sorted(refs, key=lambda r: r.sort_key):
        if ref.generated is not None:
            if ref.generated != 'lwm2m-registry':
                raise stop(
                    f'unknown generated license file {ref.generated!r} for {ref.coordinates}. '
                    'Check policy.toml [license_files].'
                )
            text, origin = generate_lwm2m_notice(root=root)
        elif ref.path:
            origin = ref.path
            if origin not in cached_repo_texts:
                raw = _read_repo_file(os.path.join(root, origin), ref.coordinates)
                cached_repo_texts[origin] = filter_platforms(clean_text(raw), platforms[origin], origin)
            text = cached_repo_texts[origin]
            if not text:
                raise stop(f'the license file of {ref.coordinates} at {origin} is empty. Check policy.toml [license_files].')
        elif ref.url:
            origin = ref.url
            text = clean_text(_download_url(origin, ref.coordinates, downloads_dir))
            if not text:
                raise stop(
                    f'the license file of {ref.coordinates} at {origin} is empty. '
                    'Check the external reference of type license in the pom that declares it.'
                )
        else:
            raise stop(f'license file reference for {ref.coordinates} has no generated source, path, or URL.')
        found.setdefault(text, set()).add((ref.coordinates, origin))

    return found


def _platforms_by_path(refs):
    platforms = {}
    for ref in refs:
        if ref.path:
            taken = platforms.get(ref.path, set())
            platforms[ref.path] = (
                taken | {ref.platform.lower()} if ref.platform and taken is not None else None
            )
    return platforms


def filter_platforms(text, platforms, path):
    """Keeps common text plus sections for `platforms`, or the full file when `platforms` is None."""
    if platforms is None:
        return text
    lines, sections, keep = [], set(), True
    for line in text.split('\n'):
        match = SECTION_RE.match(line)
        if match:
            sections.add(match[1])
            keep = match[1].lower() in platforms
        if keep:
            lines.append(line)
    unknown = sorted(platforms - {s.lower() for s in sections}) if sections else []
    if unknown:
        raise stop(
            f'{path} has no section for the platform {", ".join(unknown)}, which the os qualifier of the purl '
            f'of its component gives: its sections are for {", ".join(sorted(sections))}. '
            'Fix the qualifier in the pom that declares the component, or the sections of the file.'
        )
    return '\n'.join(lines).strip('\n')


def _read_repo_file(full_path, component):
    try:
        with open(full_path, 'rb') as file:
            return file.read()
    except OSError as err:
        raise stop(
            f'the license file of {component} cannot be read: {err}. '
            'Fix its path in tools/src/main/notices/policy.toml [license_files].'
        )


def _download_url(url, component, downloads_dir):
    digest = hashlib.sha256(url.encode('utf-8')).hexdigest()
    candidate = os.path.join(downloads_dir, digest)
    if os.path.exists(candidate):
        with open(candidate, 'rb') as file:
            return file.read()
    try:
        data = http_get(url, timeout=120)
    except HTTP_ERRORS as err:
        raise stop(
            f'the license file of {component} cannot be downloaded from {url}: {err}. '
            'Check the external reference of type license in the pom that declares it.'
        )
    write_atomically(os.path.join(downloads_dir, digest), data)
    return data
