#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Renders THIRD-PARTY-NOTICES deterministically and loads cached SPDX / ScanCode license texts."""
import os
import re
import subprocess
import textwrap

from notices import HTTP_ERRORS, http_get, normalize_text, stop, write_atomically
from notices.scanner import SCANCODE_VERSION, scancode_image

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
            by_license.setdefault(str(lic), []).append(pkg)

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

    for lic_name, pkgs in sections:
        title = policy.title(lic_name)
        count = len(pkgs)
        suffix = f' ({count} {"component" if count == 1 else "components"})' if count > 0 else ''
        out.append(f'  {title}{suffix}')
    out.append('')

    offered = []
    for lic_name, pkgs in sections:
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
            if pkgs:
                ce_intro += ' The components below\nare licensed under Apache-2.0 as well:'
            out.append(ce_intro)
            out.append('')

        cat = policy.category(lic_name)
        for pkg in sorted(pkgs, key=lambda p: p.coordinates):
            out.append(f'  {pkg.coordinates}')
            for cpr in pkg.copyrights:
                out.append(f'      {cpr}')
            if cat == 'source-link' and pkg.source_url:
                out.append(f'      Source code: {pkg.source_url}')
            if cat == 'written-offer':
                out.append('      Source code: see "Written offer for source code" at the end of this file')
                offered.append(f'{pkg.coordinates} ({lic_name})')

        text = load_license_text(lic_name, custom_licenses, cache_dir)
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
        indented = textwrap.indent(text, '    ')
        header = '\n'.join([SEPARATOR] + [f'{comp} ({where})' for comp, where in carriers])
        blocks.append(f'{header}\n\n{indented}\n')
    return '\n'.join(blocks)


def load_license_text(lic_name, custom_licenses, cache_dir):
    """Loads the license text (and exception text if WITH) from custom_licenses, local cache, or upstream."""
    if lic_name in custom_licenses:
        return custom_licenses[lic_name]
    base_id, _, exc_id = lic_name.partition(' WITH ')
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
    text = normalize_text(text)
    write_atomically(cached, (text + '\n').encode('utf-8'))
    return text


def _fetch_upstream_license_text(identifier):
    if identifier.startswith('LicenseRef-scancode-'):
        key = identifier.removeprefix('LicenseRef-scancode-')
        raw = _http_get_text(SCANCODE_TEXT_URL.format(key=key))
        return _strip_yaml_frontmatter(raw) if raw is not None else None
    return _http_get_text(SPDX_TEXT_URL.format(id=identifier))


def _http_get_text(url):
    try:
        return http_get(url, timeout=30).decode('utf-8-sig')
    except HTTP_ERRORS:
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
    script = '\n'.join([
        'import licensedcode, os, sys',
        'd = os.path.join(os.path.dirname(licensedcode.__file__), "data", "licenses")',
        'target = sys.argv[1]',
        'key = target.removeprefix("LicenseRef-scancode-")',
        'p = os.path.join(d, key + ".LICENSE")',
        'if os.path.exists(p):',
        '    with open(p, encoding="utf-8") as f:',
        '        print(f.read())',
        '    sys.exit(0)',
        'for name in os.listdir(d):',
        '    if not name.endswith(".LICENSE"):',
        '        continue',
        '    with open(os.path.join(d, name), encoding="utf-8") as f:',
        '        txt = f.read()',
        '    if f"spdx_license_key: {target}\\n" in txt:',
        '        print(txt)',
        '        sys.exit(0)',
        'sys.exit(1)',
    ])
    try:
        proc = subprocess.run(
            ['docker', 'run', '--rm', '--entrypoint', 'python3', scancode_image(), '-c', script, identifier],
            capture_output=True, text=True, timeout=60,
        )
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return None
    if proc.returncode == 0 and proc.stdout:
        return _strip_yaml_frontmatter(proc.stdout)
    return None
