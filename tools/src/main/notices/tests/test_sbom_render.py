#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import json
import os
import tempfile
import unittest

from notices.config import Policy
from notices.policy import ResolvedPackage
from notices.render import render_notices
from notices.sbom import load_sboms
from notices.spdx import License


def _policy():
    return Policy(
        own_modules=frozenset({'org.thingsboard.ai/common'}),
        exclude_paths=(),
        same_category_preference={},
        license_category={
            'Apache-2.0': 'notice',
            'MIT': 'notice',
            'EPL-2.0': 'source-link',
            'LGPL-2.1-or-later': 'written-offer',
        },
        license_aliases={},
        license_titles={},
        license_files={'pkg:generic/chromium/chromium@1.0.0?os=linux': 'CREDITS.txt'},
        copyright_garbage=frozenset(),
    )


class SbomAndRenderTests(unittest.TestCase):
    def test_load_sboms_filters_own_and_syft_and_detects_version_drift(self):
        with tempfile.TemporaryDirectory() as tmp:
            sbom = os.path.join(tmp, 'app.cdx.json')
            with open(sbom, 'w', encoding='utf-8') as f:
                json.dump({
                    'metadata': {'component': {'version': '4.4.1-SNAPSHOT'}},
                    'components': [
                        {'group': 'org.thingsboard', 'name': 'dao', 'version': '4.4.1-SNAPSHOT',
                         'purl': 'pkg:maven/org.thingsboard/dao@4.4.1-SNAPSHOT?type=jar'},
                        {'group': 'org.thingsboard.ai', 'name': 'common', 'version': '1.0.0',
                         'purl': 'pkg:maven/org.thingsboard.ai/common@1.0.0?type=jar'},
                        {'name': 'libc6', 'version': '2.39', 'purl': 'pkg:deb/debian/libc6@2.39',
                         'properties': [{'name': 'syft:location:0:path', 'value': '/usr/lib/libc.so'}]},
                        {'name': 'lwm2m-registry', 'type': 'data'},
                        {'group': 'com.example', 'name': 'demo', 'version': '1.2.3',
                         'purl': 'pkg:maven/com.example/demo@1.2.3?type=jar',
                         'hashes': [{'alg': 'SHA-256', 'content': '0' * 64}],
                         'licenses': [{'license': {'id': 'MIT'}}]},
                    ],
                }, f)

            inv = load_sboms([sbom], '4.4.1-SNAPSHOT', _policy(), check_version=True)
            self.assertEqual(list(inv.packages.keys()), ['pkg:maven/com.example/demo@1.2.3'])
            self.assertEqual(len(inv.jars), 1)
            self.assertTrue(any(r.generated == 'lwm2m-registry' for r in inv.license_files))

            # Stale SBOM version fails closed when check_version=True
            with self.assertRaises(SystemExit) as ctx:
                load_sboms([sbom], '4.5.0-SNAPSHOT', _policy(), check_version=True)
            self.assertIn('older build', str(ctx.exception))

            # Version drift on Chromium in policy.toml [license_files] stops cleanly
            drift = os.path.join(tmp, 'drift.cdx.json')
            with open(drift, 'w', encoding='utf-8') as f:
                json.dump({
                    'components': [{'name': 'chromium', 'version': '2.0.0', 'purl': 'pkg:generic/chromium/chromium@2.0.0?os=linux'}],
                }, f)
            with self.assertRaises(SystemExit) as ctx:
                load_sboms([drift], '4.4.1-SNAPSHOT', _policy())
            self.assertIn('policy.toml [license_files]', str(ctx.exception))

    def test_load_sboms_merges_duplicate_purl_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            sbom1 = os.path.join(tmp, 'a.cdx.json')
            sbom2 = os.path.join(tmp, 'b.cdx.json')
            with open(sbom1, 'w', encoding='utf-8') as f:
                json.dump({
                    'components': [{
                        'name': 'demo', 'version': '1.0.0', 'purl': 'pkg:npm/demo@1.0.0',
                        'licenses': [{'license': {'id': 'MIT'}}],
                    }],
                }, f)
            with open(sbom2, 'w', encoding='utf-8') as f:
                json.dump({
                    'components': [{
                        'name': 'demo', 'version': '1.0.0', 'purl': 'pkg:npm/demo@1.0.0',
                        'externalReferences': [{
                            'type': 'distribution',
                            'url': 'https://registry.npmjs.org/demo/-/demo-1.0.0.tgz',
                            'hashes': [{'alg': 'SHA-1', 'content': 'A' * 40}],
                        }],
                    }],
                }, f)
            inv = load_sboms([sbom1, sbom2], '4.4.1-SNAPSHOT', _policy())
            pkg = inv.packages['pkg:npm/demo@1.0.0']
            self.assertEqual(pkg.declared_raw, ('MIT',))
            self.assertEqual(pkg.npm_tarball, {
                'url': 'https://registry.npmjs.org/demo/-/demo-1.0.0.tgz',
                'sha1': 'a' * 40,
            })

    def test_render_notices_includes_ce_source_link_written_offer_and_attached_sections(self):
        with tempfile.TemporaryDirectory() as tmp:
            pkgs = [
                ResolvedPackage('pkg:maven/com.a/a@1.0', 'com.a:a 1.0', (License('MIT'),), ('Copyright 2024 A',), ''),
                ResolvedPackage('pkg:maven/com.b/b@2.0', 'com.b:b 2.0', (License('EPL-2.0'),), ('Copyright 2024 B',), 'https://example/b-sources.jar'),
                ResolvedPackage('pkg:maven/com.c/c@3.0', 'com.c:c 3.0', (License('LGPL-2.1-or-later'),), ('Copyright 2024 C',), ''),
            ]
            custom = {
                'Apache-2.0': 'APACHE TEXT',
                'MIT': 'MIT TEXT',
                'EPL-2.0': 'EPL TEXT',
                'LGPL-2.1-or-later': 'LGPL TEXT',
            }
            text = render_notices(
                resolved_packages=pkgs,
                license_files={'NODE LICENSE': {('node 22.0.0', 'https://nodejs.org/LICENSE')}},
                notice_files={'NOTICE BODY': {('com.a:a 1.0', 'META-INF/NOTICE')}},
                policy=_policy(),
                custom_licenses=custom,
                cache_dir=tmp,
            )
            self.assertIn('ThingsBoard is derived from ThingsBoard Community Edition', text)
            self.assertIn('Source code: https://example/b-sources.jar', text)
            self.assertIn('Written offer for source code', text)
            self.assertIn('  com.c:c 3.0 (LGPL-2.1-or-later)', text)
            self.assertIn('License files of the components', text)
            self.assertIn('NOTICE files of the components', text)
            self.assertIn('    NOTICE BODY', text)


if __name__ == '__main__':
    unittest.main()
