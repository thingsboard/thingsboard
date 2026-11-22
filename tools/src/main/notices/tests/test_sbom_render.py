#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import json
import os
import tempfile
import unittest

from notices.config import Curations, Policy
from notices.policy import ResolvedPackage, resolve_all
from notices.render import render_notices
from notices.sbom import load_sboms
from notices.scanner import resolve_package_source
from notices.spdx import License


def _policy():
    return Policy(
        own_modules=frozenset({'org.thingsboard.ai/common'}),
        exclude_paths=(),
        same_category_preference={},
        license_category={
            'Apache-2.0': 'notice',
            'Bitstream-Vera': 'notice',
            'MIT': 'notice',
            'OFL-1.1': 'notice',
            'EPL-2.0': 'source-link',
            'LGPL-2.1-or-later': 'written-offer',
        },
        license_aliases={},
        license_titles={},
        license_files={
            'pkg:generic/chromium/chromium@1.0.0?os=linux': 'CREDITS.txt',
            'lwm2m-registry': 'generated:lwm2m-registry',
        },
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
                        {'name': 'lwm2m-registry', 'type': 'data',
                         'licenses': [{'expression': 'BSD-3-Clause AND MIT'}]},
                        {'name': 'node', 'version': '22.0.0', 'purl': 'pkg:github/nodejs/node@v22.0.0?os=linux',
                         'licenses': [{'license': {'id': 'MIT'}}],
                         'externalReferences': [{'type': 'license', 'url': 'https://nodejs.org/LICENSE'}]},
                        {'group': 'com.example', 'name': 'demo', 'version': '1.2.3',
                         'purl': 'pkg:maven/com.example/demo@1.2.3?type=jar',
                         'hashes': [{'alg': 'SHA-256', 'content': '0' * 64}],
                         'licenses': [{'license': {'id': 'MIT'}}]},
                    ],
                }, f)

            inv = load_sboms([sbom], '4.4.1-SNAPSHOT', _policy(), check_version=True)
            self.assertEqual(list(inv.packages.keys()), ['pkg:maven/com.example/demo@1.2.3'])
            self.assertEqual(len(inv.jars), 1)
            by_name = {r.name: r for r in inv.license_files}
            self.assertEqual(by_name['lwm2m-registry'].generated, 'lwm2m-registry')
            self.assertEqual(by_name['lwm2m-registry'].declared_raw, ('BSD-3-Clause AND MIT',))
            self.assertEqual(by_name['node'].declared_raw, ('MIT',))

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

    def test_declared_font_components_flow_through_policy_and_render(self):
        with tempfile.TemporaryDirectory() as tmp:
            sbom = os.path.join(tmp, 'report.cdx.json')
            with open(sbom, 'w', encoding='utf-8') as f:
                json.dump({
                    'components': [
                        {
                            'type': 'file', 'name': 'Roboto', 'version': '3.009',
                            'bom-ref': 'font:roboto@3.009', 'purl': 'pkg:generic/roboto@3.009',
                            'copyright': 'Copyright 2011 The Roboto Project Authors',
                            'licenses': [{'license': {'id': 'OFL-1.1'}}],
                        },
                        {
                            'type': 'file', 'name': 'DejaVu Sans Mono', 'version': '2.37',
                            'bom-ref': 'font:dejavu-sans-mono@2.37', 'purl': 'pkg:generic/dejavu-sans-mono@2.37',
                            'copyright': 'Copyright (c) 2003 by Bitstream, Inc.\nDejaVu changes are in public domain',
                            'licenses': [{'license': {'id': 'Bitstream-Vera'}}],
                        },
                    ],
                }, f)
            pol = _policy()
            cur = Curations()
            inv = load_sboms([sbom], '4.4.1-SNAPSHOT', pol)
            self.assertEqual(sorted(inv.packages), ['pkg:generic/dejavu-sans-mono@2.37', 'pkg:generic/roboto@3.009'])
            sources = {p: resolve_package_source(pkg, cur, {}) for p, pkg in inv.packages.items()}
            resolved = resolve_all(inv.packages, sources, {}, pol, cur)
            custom = {'Apache-2.0': 'APACHE', 'OFL-1.1': 'OFL TEXT', 'Bitstream-Vera': 'VERA TEXT'}
            text = render_notices(resolved, {}, {}, pol, custom, tmp)
            self.assertIn('  Roboto 3.009\n      Copyright 2011 The Roboto Project Authors', text)
            self.assertIn('  DejaVu Sans Mono 2.37', text)
            self.assertIn('OFL TEXT', text)
            self.assertIn('VERA TEXT', text)

    def test_load_sboms_merges_npm_sha1_across_sboms_and_fails_if_all_lack_sha1(self):
        with tempfile.TemporaryDirectory() as tmp:
            no_hash = os.path.join(tmp, 'bundled.cdx.json')
            with_hash = os.path.join(tmp, 'ui.cdx.json')
            url = 'https://github.com/thingsboard/echarts/archive/5.5.4-TB.tar.gz'
            with open(no_hash, 'w', encoding='utf-8') as f:
                json.dump({'components': [{
                    'type': 'library', 'name': 'echarts', 'version': '5.5.4-TB',
                    'purl': 'pkg:npm/echarts@5.5.4-TB',
                    'licenses': [{'license': {'id': 'Apache-2.0'}}],
                    'externalReferences': [{'type': 'distribution', 'url': url}],
                }]}, f)
            with open(with_hash, 'w', encoding='utf-8') as f:
                json.dump({'components': [{
                    'type': 'library', 'name': 'echarts', 'version': '5.5.4-TB',
                    'purl': 'pkg:npm/echarts@5.5.4-TB',
                    'licenses': [{'license': {'id': 'Apache-2.0'}}],
                    'externalReferences': [{
                        'type': 'distribution', 'url': url,
                        'hashes': [{'alg': 'SHA-1', 'content': '05955421715b5a4ceb9b9b1b503d8e2504d0501d'}],
                    }],
                }]}, f)
            inv = load_sboms([no_hash, with_hash], '4.4.1-SNAPSHOT', _policy())
            self.assertEqual(inv.packages['pkg:npm/echarts@5.5.4-TB'].npm_tarball['sha1'], '05955421715b5a4ceb9b9b1b503d8e2504d0501d')
            with self.assertRaisesRegex(SystemExit, 'gives no SHA-1'):
                load_sboms([no_hash], '4.4.1-SNAPSHOT', _policy())

            # Component-level SHA-1 fallback (e.g. gateway-management-extension.cdx.json)
            comp_hash = os.path.join(tmp, 'comp-hash.cdx.json')
            with open(comp_hash, 'w', encoding='utf-8') as f:
                json.dump({'components': [{
                    'type': 'library', 'name': 'echarts', 'version': '5.5.4-TB',
                    'purl': 'pkg:npm/echarts@5.5.4-TB',
                    'hashes': [{'alg': 'SHA-1', 'content': '05955421715b5a4ceb9b9b1b503d8e2504d0501d'}],
                    'licenses': [{'license': {'id': 'Apache-2.0'}}],
                    'externalReferences': [{'type': 'distribution', 'url': url}],
                }]}, f)
            inv_comp = load_sboms([comp_hash], '4.4.1-SNAPSHOT', _policy())
            self.assertEqual(inv_comp.packages['pkg:npm/echarts@5.5.4-TB'].npm_tarball['sha1'], '05955421715b5a4ceb9b9b1b503d8e2504d0501d')

            # Declared component with license but no copyright (or copyright without license) is NOT silently dropped
            partial = os.path.join(tmp, 'partial-font.cdx.json')
            with open(partial, 'w', encoding='utf-8') as f:
                json.dump({'components': [{
                    'type': 'file', 'name': 'BadFont', 'version': '1.0',
                    'purl': 'pkg:generic/bad-font@1.0',
                    'licenses': [{'license': {'id': 'GPL-3.0-only'}}],
                }]}, f)
            inv_partial = load_sboms([partial], '4.4.1-SNAPSHOT', _policy())
            self.assertIn('pkg:generic/bad-font@1.0', inv_partial.packages)


if __name__ == '__main__':
    unittest.main()
