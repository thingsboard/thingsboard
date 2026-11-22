#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import contextlib
import io
import json
import os
import tempfile
import unittest
from unittest import mock

from notices.cli import _emit_unused_curations_warning, _parse_env_bool, _unused_curations_message, main
from notices.config import Policy, covers, load_curations, load_policy, split_curation_id
from notices.copyrights import process_copyrights
from notices.policy import ResolvedPackage
from notices.sbom import PackageComponent
from notices.spdx import License


class ConfigAndCopyrightTests(unittest.TestCase):
    def test_split_curation_id(self):
        self.assertEqual(split_curation_id('pkg:maven/org.jfree/jfreechart'), ('pkg:maven/org.jfree/jfreechart', ''))
        self.assertEqual(split_curation_id('pkg:maven/com.sun.winsw/winsw@2.0.1'), ('pkg:maven/com.sun.winsw/winsw', '2.0.1'))
        self.assertEqual(split_curation_id('pkg:npm/@mdi/svg@[7.0,8.0['), ('pkg:npm/@mdi/svg', '[7.0,8.0['))
        self.assertEqual(split_curation_id('pkg:npm/%40mdi/svg@[7.0,8.0['), ('pkg:npm/@mdi/svg', '[7.0,8.0['))

    def test_version_range_covers(self):
        self.assertTrue(covers('2.0.1', '2.0.1'))
        self.assertFalse(covers('2.0.1', '2.0.2'))
        self.assertTrue(covers('[1.0,2.0[', '1.0'))
        self.assertTrue(covers('[1.0,2.0[', '1.9.9'))
        self.assertFalse(covers('[1.0,2.0[', '2.0.0'))
        self.assertTrue(covers('[2.0,3.0[', '2.4.0-b180830.0359'))
        # Non-numeric versions fail closed rather than matching an arbitrary numeric range
        self.assertFalse(covers('[1.0,2.0[', 'snapshot'))

    def test_real_policy_and_curations_load_cleanly(self):
        policy = load_policy()
        curations = load_curations()
        self.assertEqual(policy.category('Apache-2.0'), 'notice')
        self.assertEqual(policy.category('GPL-3.0-only'), 'forbidden')
        self.assertIn('LicenseRef-thingsboard-beerware-googlemutant', curations.custom_licenses)
        self.assertGreaterEqual(len(policy.copyright_garbage), 20)
        self.assertNotIn('copyright_garbage', policy.license_files)

    def test_config_rejects_unknown_keys_and_invalid_curations(self):
        with tempfile.TemporaryDirectory() as tmp:
            bad_policy = os.path.join(tmp, 'policy.toml')
            with open(bad_policy, 'w', encoding='utf-8') as f:
                f.write('unknown_key = true\n')
            with self.assertRaises(SystemExit) as ctx:
                load_policy(bad_policy)
            self.assertIn('unknown_key', str(ctx.exception))

            bad_cur = os.path.join(tmp, 'curations.toml')
            for snippet, needle in (
                ('[[curations]]\nid = "pkg:maven/a/b@1.0"\nmetadata-only = true\n', 'metadata-only'),
                ('[[curations]]\nid = "pkg:maven/a/b@[1.0,]"\nconcluded_license = "MIT"\n', 'upper bound'),
                ('[[curations]]\nid = "pkg:maven/a/b@1.0"\nvcs = { url = "https://example.org" }\n', 'revision'),
                ('[[curations]]\nid = "pkg:maven/a/b@1.0"\ndeclared_license = "MIT OR"\n', 'invalid SPDX'),
            ):
                with open(bad_cur, 'w', encoding='utf-8') as f:
                    f.write(snippet)
                with self.assertRaises(SystemExit) as ctx:
                    load_curations(bad_cur)
                self.assertIn(needle, str(ctx.exception))

    def test_curations_supports_multi_id_entries(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, 'curations.toml')
            with open(path, 'w', encoding='utf-8') as f:
                f.write(
                    '[[curations]]\n'
                    'ids = ["pkg:maven/com.example/a@1.0", "pkg:maven/com.example/b@1.0"]\n'
                    'metadata_only = true\n'
                )
            curations = load_curations(path)
            self.assertEqual(len(curations.for_package('pkg:maven/com.example/a', '1.0')), 1)
            self.assertEqual(len(curations.for_package('pkg:maven/com.example/b', '1.0')), 1)

    def test_copyright_year_merging_chained_hyphens_and_garbage(self):
        result = process_copyrights(
            [
                'Copyright (C) 2020 Example Corp.',
                'Copyright 2021 Example Corp.',
                'Copyright © 2022-2023 Example Corp.',
                'Copyright (c) 1989, 1991 Free Software Foundation, Inc.',
                'Copyright 2010-2015-2020 Weird Range Inc.',
            ],
            garbage=frozenset({'Copyright (c) 1989, 1991 Free Software Foundation, Inc.'}),
        )
        self.assertEqual(
            result,
            [
                'Copyright (c) 2020-2023 Example Corp.',
                'Copyright 2010-2015-2020 Weird Range Inc.',
            ],
        )

    def test_unused_curations_warns_by_default_and_stops_when_strict(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, 'curations.toml')
            with open(path, 'w', encoding='utf-8') as f:
                f.write(
                    '[custom_licenses]\n'
                    '"LicenseRef-used" = "USED TEXT"\n'
                    '"LicenseRef-stale" = "STALE TEXT"\n'
                    '[[curations]]\n'
                    'ids = ["pkg:maven/com.example/used@1.0", "pkg:maven/com.example/stale@1.0"]\n'
                    'metadata_only = true\n'
                )
            curations = load_curations(path)
            pkg = PackageComponent(
                purl='pkg:maven/com.example/used@1.0',
                package_key='pkg:maven/com.example/used',
                kind='maven', group='com.example', name='used', version='1.0',
                declared_raw=('MIT',), named_raw=frozenset(), npm_tarball=None, vcs=None,
            )
            resolved = [ResolvedPackage(
                purl=pkg.purl, coordinates='com.example:used 1.0',
                licenses=(License('LicenseRef-used'),), copyrights=(), source_url='',
            )]

            unused_cur, unused_custom = curations.unused([pkg], used_licenses={'LicenseRef-used'})
            self.assertEqual(unused_cur, ['pkg:maven/com.example/stale@1.0'])
            self.assertEqual(unused_custom, ['LicenseRef-stale'])

            # Default mode warns without failing (and emits TeamCity WARNING when TEAMCITY_VERSION is set)
            body = _unused_curations_message(curations, [pkg], resolved)
            self.assertIsNotNone(body)
            buf = io.StringIO()
            with mock.patch.dict(os.environ, {'TEAMCITY_VERSION': '2024.12.3'}, clear=False):
                with contextlib.redirect_stdout(buf):
                    _emit_unused_curations_warning(body)
            out = buf.getvalue()
            self.assertIn('Third-party notices: warning: 2 entries in tools/src/main/notices/curations.toml', out)
            self.assertIn('pkg:maven/com.example/stale@1.0', out)
            self.assertIn('[custom_licenses] LicenseRef-stale', out)
            self.assertIn("##teamcity[message text='Third-party notices: warning:", out)
            self.assertIn("status='WARNING']", out)

            # Invalid env var fails closed instead of silently disabling strict mode
            with mock.patch.dict(os.environ, {'NOTICES_FAIL_ON_UNUSED_CURATIONS': 'true'}, clear=False):
                self.assertTrue(_parse_env_bool('NOTICES_FAIL_ON_UNUSED_CURATIONS'))
            with mock.patch.dict(os.environ, {'NOTICES_FAIL_ON_UNUSED_CURATIONS': 'false'}, clear=False):
                self.assertFalse(_parse_env_bool('NOTICES_FAIL_ON_UNUSED_CURATIONS'))
            with mock.patch.dict(os.environ, {'NOTICES_FAIL_ON_UNUSED_CURATIONS': 'ture'}, clear=False):
                with self.assertRaises(SystemExit) as ctx:
                    _parse_env_bool('NOTICES_FAIL_ON_UNUSED_CURATIONS')
                self.assertIn('invalid NOTICES_FAIL_ON_UNUSED_CURATIONS', str(ctx.exception))

    def test_render_cli_unused_curations_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            sbom_dir = os.path.join(tmp, 'mod', 'target', 'sbom')
            os.makedirs(sbom_dir)
            sbom_path = os.path.join(sbom_dir, 'app.cdx.json')
            with open(sbom_path, 'w', encoding='utf-8') as f:
                json.dump({
                    'metadata': {'component': {'version': '1.0.0'}},
                    'components': [{
                        'type': 'library', 'name': 'demo-used', 'version': '1.0.0',
                        'purl': 'pkg:npm/demo-used@1.0.0',
                        'licenses': [{'license': {'id': 'LicenseRef-used'}}],
                    }],
                }, f)

            curations_path = os.path.join(tmp, 'curations.toml')
            with open(curations_path, 'w', encoding='utf-8') as f:
                f.write(
                    '[custom_licenses]\n'
                    '"Apache-2.0" = "APACHE"\n'
                    '"LicenseRef-used" = "USED TEXT"\n'
                    '"LicenseRef-stale" = "STALE TEXT"\n'
                    '[[curations]]\n'
                    'ids = ["pkg:npm/demo-used@1.0.0", "pkg:npm/demo-stale@1.0.0"]\n'
                    'metadata_only = true\n'
                )
            curations = load_curations(curations_path)
            policy = Policy(license_category={'Apache-2.0': 'notice', 'LicenseRef-used': 'notice'})
            base_env = {'NOTICES_CACHE_DIR': os.path.join(tmp, 'cache'), 'NOTICES_SCAN_CACHE_URL': 'none'}

            with mock.patch('notices.cli.load_policy', return_value=policy), \
                 mock.patch('notices.cli.load_curations', return_value=curations):
                # 1. --sboms-under default: warns, writes --out, returns 0
                out_warn = os.path.join(tmp, 'warn.txt')
                buf = io.StringIO()
                env_warn = {**base_env, 'TEAMCITY_VERSION': '2024.12.3', 'NOTICES_FAIL_ON_UNUSED_CURATIONS': ''}
                with mock.patch.dict(os.environ, env_warn, clear=False), contextlib.redirect_stdout(buf):
                    rc = main(['render', '--project-version', '1.0.0', '--out', out_warn, '--sboms-under', tmp])
                self.assertEqual(rc, 0)
                self.assertTrue(os.path.exists(out_warn))
                self.assertIn('pkg:npm/demo-stale@1.0.0', buf.getvalue())
                self.assertIn("status='WARNING']", buf.getvalue())

                # 2. --sboms-under + strict env var: stops before writing --out
                out_env_strict = os.path.join(tmp, 'env-strict.txt')
                with mock.patch.dict(os.environ, {**base_env, 'NOTICES_FAIL_ON_UNUSED_CURATIONS': 'true'}, clear=False):
                    with self.assertRaises(SystemExit) as ctx:
                        main(['render', '--project-version', '1.0.0', '--out', out_env_strict, '--sboms-under', tmp])
                self.assertIn('[custom_licenses] LicenseRef-stale', str(ctx.exception))
                self.assertFalse(os.path.exists(out_env_strict))

                # 3. Single-SBOM run (no --sboms-under): skips unused check even with strict env var
                out_single = os.path.join(tmp, 'single.txt')
                buf_single = io.StringIO()
                with mock.patch.dict(os.environ, {**base_env, 'NOTICES_FAIL_ON_UNUSED_CURATIONS': 'true'}, clear=False), \
                     contextlib.redirect_stdout(buf_single):
                    rc = main(['render', '--project-version', '1.0.0', '--out', out_single, sbom_path])
                self.assertEqual(rc, 0)
                self.assertTrue(os.path.exists(out_single))
                self.assertNotIn('matched no shipped package', buf_single.getvalue())


if __name__ == '__main__':
    unittest.main()
