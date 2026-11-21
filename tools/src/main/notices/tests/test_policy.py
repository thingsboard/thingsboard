#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

from dataclasses import replace
import unittest

from notices.config import Curation, Curations, IgnoreFinding, Policy
from notices.policy import glob_match, resolve_all
from notices.sbom import LicenseFileRef, PackageComponent
from notices.spdx import License


def _policy():
    return Policy(
        own_modules=frozenset(),
        exclude_paths=('**/test/**', '**/docs/**', '**/*.map'),
        same_category_preference={'Apache-2.0': 0, 'MIT': 1},
        license_category={
            'Apache-2.0': 'notice',
            'MIT': 'notice',
            'BSD-3-Clause': 'notice',
            'EPL-2.0': 'source-link',
            'GPL-2.0-only WITH Classpath-exception-2.0': 'written-offer',
            'GPL-2.0-only': 'forbidden',
            'GPL-3.0-only': 'forbidden',
            'LicenseRef-scancode-public-domain': 'nothing',
        },
        license_aliases={'Bouncy Castle Licence': 'MIT'},
        license_titles={},
        license_files={},
        copyright_garbage=frozenset(),
    )


def _pkg(purl='pkg:maven/com.example/lib@1.0.0', declared=('Apache-2.0',), named=()):
    kind = 'maven' if purl.startswith('pkg:maven/') else 'npm'
    key, _, version = purl.rpartition('@')
    return PackageComponent(
        purl=purl, package_key=key, kind=kind, group='com.example', name='lib', version=version,
        declared_raw=tuple(declared), named_raw=frozenset(named), npm_tarball=None, vcs=None,
    )


class PolicyTests(unittest.TestCase):
    def test_glob_match_handles_leading_and_trailing_doublestar(self):
        self.assertTrue(glob_match('test/Foo.java', '**/test/**'))
        self.assertTrue(glob_match('src/test/Foo.java', '**/test/**'))
        self.assertFalse(glob_match('test', '**/test/**'))
        self.assertFalse(glob_match('src/test', '**/test/**'))
        self.assertFalse(glob_match('src/main/Foo.java', '**/test/**'))
        self.assertTrue(glob_match('dist/app.js.map', '**/*.map'))
        self.assertTrue(glob_match('app.js.map', '**/*.map'))
        self.assertTrue(glob_match('org/smpp/Connection.java', 'org/smpp/**/*.java'))
        self.assertTrue(glob_match('org/smpp/util/Unprocessed.java', 'org/smpp/**/*.java'))
        self.assertFalse(glob_match('org/smpp_other/Connection.java', 'org/smpp/**/*.java'))

    def test_dual_license_root_file_does_not_force_forbidden_branch(self):
        pkg = _pkg(declared=('EPL-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0',))
        scan = {
            pkg.purl: {
                'findings': [
                    {'path': 'META-INF/LICENSE.md', 'licenses': ['EPL-2.0', 'GPL-2.0-only', 'GPL-2.0-only WITH Classpath-exception-2.0'], 'copyrights': []},
                    {'path': 'src/Main.java', 'licenses': ['EPL-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0'], 'copyrights': ['Copyright 2024 Example']},
                ]
            }
        }
        resolved = resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}, scan, _policy(), Curations())
        self.assertEqual(resolved[0].licenses, (License('EPL-2.0'),))

    def test_dual_license_rule_never_exempts_source_files(self):
        pkg = _pkg(declared=('MIT OR GPL-2.0-only',))
        scan = {
            pkg.purl: {
                'findings': [
                    {'path': 'src/Bad.java', 'licenses': ['GPL-2.0-only'], 'copyrights': []},
                ]
            }
        }
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}, scan, _policy(), Curations())
        self.assertIn('cannot ship', str(ctx.exception))

    def test_or_ranking_prefers_known_over_unknown_and_simpler_branch(self):
        # Known source-link (EPL-2.0) must beat a branch containing an unclassified license (MIT AND Unknown-1.0)
        pkg_unknown = _pkg(declared=('(MIT AND Unknown-1.0) OR EPL-2.0',))
        resolved = resolve_all({pkg_unknown.purl: pkg_unknown}, {pkg_unknown.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertEqual(resolved[0].licenses, (License('EPL-2.0'),))

        # Single MIT must beat (Apache-2.0 AND MIT)
        pkg_conj = _pkg(declared=('(Apache-2.0 AND MIT) OR MIT',))
        resolved = resolve_all({pkg_conj.purl: pkg_conj}, {pkg_conj.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertEqual(resolved[0].licenses, (License('MIT'),))

    def test_file_scoped_ignore_silences_only_targeted_file(self):
        pkg = _pkg(declared=('Apache-2.0',))
        curations = Curations((
            Curation(
                raw_id=pkg.package_key, package_key=pkg.package_key, version_spec='',
                ignore_findings=(IgnoreFinding(path='**/ModelCard.java', licenses=frozenset({'GPL-3.0-only'})),),
            ),
        ))
        scan = {
            pkg.purl: {
                'findings': [
                    {'path': 'com/example/ModelCard.java', 'licenses': ['GPL-3.0-only'], 'copyrights': []},
                    {'path': 'com/example/Real.java', 'licenses': ['Apache-2.0'], 'copyrights': []},
                ]
            }
        }
        resolved = resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}, scan, _policy(), curations)
        self.assertEqual(resolved[0].licenses, (License('Apache-2.0'),))

    def test_unmapped_named_license_stops(self):
        pkg = _pkg(declared=('Mystery License',), named=('Mystery License',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertIn('Mystery License', str(ctx.exception))

    def test_global_license_alias_resolves_named_license(self):
        pkg = _pkg(declared=('Bouncy Castle Licence',), named=('Bouncy Castle Licence',))
        resolved = resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertEqual(resolved[0].licenses, (License('MIT'),))

    def test_unreviewed_concluded_version_stops(self):
        pkg = _pkg(purl='pkg:maven/com.example/lib@2.0.0')
        curations = Curations((
            Curation(raw_id='pkg:maven/com.example/lib@[1.0,2.0[', package_key='pkg:maven/com.example/lib', version_spec='[1.0,2.0[', concluded_license='MIT'),
        ))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), curations)
        self.assertIn('older versions', str(ctx.exception))

    def test_invalid_spdx_in_curation_stops_cleanly(self):
        pkg = _pkg(purl='pkg:maven/com.example/lib@1.0.0')
        curations = Curations((
            Curation(raw_id=pkg.purl, package_key=pkg.package_key, version_spec='1.0.0', concluded_license='MIT OR'),
        ))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), curations)
        self.assertIn('invalid SPDX expression', str(ctx.exception))

    def test_unclassified_license_stops(self):
        pkg = _pkg(declared=('ISC',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertIn('new to the product', str(ctx.exception))

    def test_unranked_same_category_tie_stops(self):
        pol = replace(_policy(), same_category_preference={})
        pkg = _pkg(declared=('Apache-2.0 OR MIT',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'metadata_only'}}, {}, pol, Curations())
        self.assertIn('equally ranked', str(ctx.exception))
        self.assertIn('docs/licensing-guide.md', str(ctx.exception))

    def test_license_file_components_enforce_policy_checks(self):
        # Clean bundled component passes
        ok_ref = LicenseFileRef(group='', name='node', version='22.0.0', url='https://nodejs.org/LICENSE', declared_raw=('MIT',))
        or_ref = LicenseFileRef(group='', name='font', version='1.0', path='fonts/LICENSE', declared_raw=('Apache-2.0 OR GPL-3.0-only',))
        resolve_all({}, {}, {}, _policy(), Curations(), license_files={ok_ref, or_ref})

        # Forbidden license on bundled component hard-stops
        bad_ref = LicenseFileRef(group='', name='badfont', version='1.0', path='fonts/LICENSE', declared_raw=('GPL-3.0-only',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({}, {}, {}, _policy(), Curations(), license_files={bad_ref})
        self.assertIn('cannot ship', str(ctx.exception))
        self.assertIn('badfont 1.0', str(ctx.exception))
        self.assertIn('docs/licensing-guide.md', str(ctx.exception))

        # Unclassified license on bundled component stops
        new_ref = LicenseFileRef(group='', name='roboto', version='1.0', path='fonts/OFL.txt', declared_raw=('OFL-1.1',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({}, {}, {}, _policy(), Curations(), license_files={new_ref})
        self.assertIn('new to the product', str(ctx.exception))
        self.assertIn('OFL-1.1 (roboto 1.0)', str(ctx.exception))

        # Unmapped non-SPDX license name on bundled component stops
        unmapped_ref = LicenseFileRef(
            group='', name='custom', version='1.0', path='custom/LICENSE',
            declared_raw=('Some Custom License',), named_raw=frozenset({'Some Custom License'}),
        )
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({}, {}, {}, _policy(), Curations(), license_files={unmapped_ref})
        self.assertIn('Some Custom License', str(ctx.exception))

        # Bundled url/path component with no declared license stops
        empty_ref = LicenseFileRef(group='', name='mystery', version='1.0', path='mystery/LICENSE')
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({}, {}, {}, _policy(), Curations(), license_files={empty_ref})
        self.assertIn('no license was found', str(ctx.exception))
        self.assertIn('mystery 1.0', str(ctx.exception))


if __name__ == '__main__':
    unittest.main()
