#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Renders THIRD-PARTY-NOTICES deterministically and loads cached SPDX / ScanCode license texts."""
import os
import re
import subprocess
import urllib.error
import urllib.request

from notices import stop, write_atomically
from notices.scanner import DEFAULT_SCANCODE_IMAGE, SCANCODE_VERSION

RULE = '=' * 78
SEPARATOR = '-' * 78
CE_LICENSE = 'Apache-2.0'

SPDX_VERSION = '3.27.0'
SPDX_TEXT_URL = f'https://raw.githubusercontent.com/spdx/license-list-data/v{SPDX_VERSION}/text/{{id}}.txt'
SCANCODE_TEXT_URL = (
    f'https://raw.githubusercontent.com/aboutcode-org/scancode-toolkit/v{SCANCODE_VERSION}'
    '/src/licensedcode/data/licenses/{key}.LICENSE'
)


def render_notices(resolved_packages, license_files, notice_files, policy, custom_licenses, cache_dir):
    """Formats the full THIRD-PARTY-NOTICES text, matching ThingsBoard's established layout."""
    # Group packages by license string, always keeping Apache-2.0 for ThingsBoard CE.
    by_license = {CE_LICENSE: []}
    for pkg in resolved_packages:
        for lic in pkg.licenses:
            by_license.setdefault(str(lic), []).append((pkg, lic))

    sections = sorted(
        by_license.items(),
        key=lambda item: (-len(item[1]), item[0]),
    )

    out = [
        'THIRD-PARTY NOTICES',
        '',
        'ThingsBoard contains the third-party software components listed below.',
        'The components are grouped by license. Each section lists the components under',
        'that license with their copyright notices, followed by the license text.',
        '',
        'Licenses in this file:',
        '',
    ]

    for lic_name, entries in sections:
        title = policy.title(lic_name)
        count = len(entries)
        suffix = f' ({count} {"component" if count == 1 else "components"})' if count > 0 else ''
        out.append(f'  {title}{suffix}')
    out.append('')

    offered = []
    for lic_name, entries in sections:
        title = policy.title(lic_name)
        out.extend([RULE, title, RULE, ''])
        if lic_name != CE_LICENSE:
            out.append(f'The following components are licensed under {title}:')
            out.append('')
        else:
            ce_intro = (
                'ThingsBoard is derived from ThingsBoard Community Edition, Copyright The\n'
                'ThingsBoard Authors, which is licensed under Apache-2.0.'
            )
            if entries:
                ce_intro += ' The components below\nare licensed under Apache-2.0 as well:'
            out.append(ce_intro)
            out.append('')

        cat = policy.category(lic_name)
        sample_lic = None
        for pkg, lic in sorted(entries, key=lambda pair: pair[0].coordinates):
            sample_lic = sample_lic or lic
            out.append(f'  {pkg.coordinates}')
            for cpr in pkg.copyrights:
                out.append(f'      {cpr}')
            if cat == 'source-link' and pkg.source_url:
                out.append(f'      Source code: {pkg.source_url}')
            if cat == 'written-offer':
                out.append('      Source code: see "Written offer for source code" at the end of this file')
                offered.append(f'{pkg.coordinates} ({lic_name})')

        text = load_license_text(lic_name, sample_lic, custom_licenses, cache_dir)
        out.append('')
        out.append(text)
        out.append('')

    if license_files:
        out.extend([
            RULE,
            'License files of the components',
            RULE,
            '',
            'Some components come with a license file of their own, which covers them and',
            'the software they contain. Each file below is given as it is, once, under the',
            'components it belongs to and where it was taken from: an address, or a file of',
            'the ThingsBoard source.',
            '',
            format_attached_blocks(license_files),
        ])

    if notice_files:
        out.extend([
            RULE,
            'NOTICE files of the components',
            RULE,
            '',
            'Some components come with a NOTICE file. Each text below is given as it is in',
            'the component, once, under the components that carry it and the name of the',
            'file in each.',
            '',
            format_attached_blocks(notice_files),
        ])

    if offered:
        out.extend([
            RULE,
            'Written offer for source code',
            RULE,
            '',
            'This product contains the software components listed below, each under the',
            'license shown beside it.',
            '',
            'ThingsBoard, Inc. will give any third party a complete machine-readable copy of',
            'the corresponding source code of these components, on a medium customarily used',
            'for software interchange, for a charge no more than our cost of physically',
            'performing the distribution.',
            '',
            'This offer is valid for three years from the date on which we last distributed',
            'this version of the product.',
            '',
            'To request the source code, write to legal@thingsboard.io and state the product',
            'name, the product version and the component.',
            '',
            'Components covered by this offer:',
            '',
        ])
        for line in sorted(set(offered)):
            out.append(f'  {line}')

    return '\n'.join(out).rstrip('\n') + '\n'


def format_attached_blocks(found):
    """Formats {text -> set of (coordinates, origin)} deduplicated, ordered by first carrier."""
    blocks = []
    ordered = sorted((sorted(carriers), text) for text, carriers in found.items())
    for carriers, text in ordered:
        indented = '\n'.join('    ' + line if line else '' for line in text.split('\n'))
        header = '\n'.join([SEPARATOR] + [f'{comp} ({where})' for comp, where in carriers])
        blocks.append(f'{header}\n\n{indented}\n')
    return '\n'.join(blocks)


def load_license_text(lic_name, lic_obj, custom_licenses, cache_dir):
    """Loads the license text (and exception text if WITH) from custom_licenses, local cache, or upstream."""
    if lic_name in custom_licenses:
        return custom_licenses[lic_name]
    base_id = lic_obj.id if lic_obj else lic_name
    exc_id = lic_obj.exception if lic_obj else None
    base_text = _load_single_license_text(base_id, custom_licenses, cache_dir)
    if exc_id:
        exc_text = _load_single_license_text(exc_id, custom_licenses, cache_dir)
        return f'{base_text}\n\n{exc_text}'
    return base_text


def _load_single_license_text(identifier, custom_licenses, cache_dir):
    if identifier in custom_licenses:
        return custom_licenses[identifier]
    safe = re.sub(r'[^A-Za-z0-9._-]', '_', identifier)
    cached = os.path.join(cache_dir, 'license-texts', f'{safe}.txt')
    if os.path.exists(cached):
        with open(cached, encoding='utf-8') as file:
            return file.read().strip('\n')

    text = _fetch_upstream_license_text(identifier)
    if text is None:
        text = _extract_from_scancode_image(identifier)
    if text is None:
        raise stop(
            f'license text for {identifier!r} could not be fetched from SPDX/ScanCode. '
            'If it is a custom license, add it under [custom_licenses] in tools/src/main/notices/curations.toml.'
        )
    text = '\n'.join(line.rstrip() for line in text.replace('\r\n', '\n').split('\n')).strip('\n')
    write_atomically(cached, (text + '\n').encode('utf-8'))
    return text


def _fetch_upstream_license_text(identifier):
    if identifier.startswith('LicenseRef-scancode-'):
        key = identifier.removeprefix('LicenseRef-scancode-')
        url = SCANCODE_TEXT_URL.format(key=key)
        raw = _http_get_text(url)
        return _strip_yaml_frontmatter(raw) if raw is not None else None
    url = SPDX_TEXT_URL.format(id=identifier)
    return _http_get_text(url)


def _http_get_text(url):
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'tb-notices'})
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.read().decode('utf-8-sig')
    except (urllib.error.URLError, OSError):
        return None


def _strip_yaml_frontmatter(text):
    """Strips leading `---\\n...\\n---\\n` YAML header from ScanCode .LICENSE files."""
    if text.startswith('---\n'):
        end = text.find('\n---\n', 4)
        if end != -1:
            return text[end + 5:].strip('\n')
    return text.strip('\n')


def _extract_from_scancode_image(identifier):
    """Offline fallback: reads license text from ScanCode's bundled license database inside Docker."""
    image = os.environ.get('NOTICES_SCANCODE_IMAGE', DEFAULT_SCANCODE_IMAGE)
    script = (
        'import licensedcode, os, sys; '
        'd = os.path.join(os.path.dirname(licensedcode.__file__), "data", "licenses"); '
        'target = sys.argv[1]; '
        'key = target.removeprefix("LicenseRef-scancode-"); '
        'p = os.path.join(d, key + ".LICENSE"); '
        'if os.path.exists(p): print(open(p, encoding="utf-8").read()); sys.exit(0)\n'
        'for name in os.listdir(d):\n'
        '  if not name.endswith(".LICENSE"): continue\n'
        '  txt = open(os.path.join(d, name), encoding="utf-8").read()\n'
        '  if f"spdx_license_key: {target}\\n" in txt: print(txt); sys.exit(0)\n'
        'sys.exit(1)'
    )
    proc = subprocess.run(
        ['docker', 'run', '--rm', '--entrypoint', 'python3', image, '-c', script, identifier],
        capture_output=True, text=True,
    )
    if proc.returncode == 0 and proc.stdout:
        return _strip_yaml_frontmatter(proc.stdout)
    return None
