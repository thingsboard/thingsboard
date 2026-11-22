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
        exclude_paths=('**/*.map', '**/pom.xml'),
        source_tree_exclude_paths=('**/test/**', '**/docs/**'),
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

        # Same forbidden license on a non-matching file MUST still fail closed
        scan_unignored = {
            pkg.purl: {
                'findings': [
                    {'path': 'com/example/ModelCard.java', 'licenses': ['GPL-3.0-only'], 'copyrights': []},
                    {'path': 'com/example/Real.java', 'licenses': ['GPL-3.0-only'], 'copyrights': []},
                ]
            }
        }
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}, scan_unignored, _policy(), curations)
        self.assertIn('GPL-3.0-only', str(ctx.exception))

        # Compound expressions: ignoring one leaf keeps remaining leaves; forbidden sibling still stops
        comp_curations = Curations((
            Curation(
                raw_id=pkg.package_key, package_key=pkg.package_key, version_spec='',
                ignore_findings=(IgnoreFinding(path='src/**', licenses=frozenset({'EPL-2.0', 'MIT'})),),
            ),
        ))
        src = {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}
        scan_or = {pkg.purl: {'findings': [{'path': 'src/A.java', 'licenses': ['BSD-3-Clause OR EPL-2.0'], 'copyrights': []}]}}
        resolved_or = resolve_all({pkg.purl: pkg}, src, scan_or, _policy(), comp_curations)
        self.assertEqual(resolved_or[0].licenses, (License('Apache-2.0'), License('BSD-3-Clause')))

        scan_and_gpl = {pkg.purl: {'findings': [{'path': 'src/A.java', 'licenses': ['MIT AND GPL-3.0-only'], 'copyrights': []}]}}
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, src, scan_and_gpl, _policy(), comp_curations)
        self.assertIn('GPL-3.0-only', str(ctx.exception))

    def test_with_exception_not_silenced_by_base_id_ignore_or_missing_scan_record(self):
        # Ignoring bare GPL-2.0-only must NOT silently ignore GPL-2.0-only WITH Classpath-exception-2.0
        pkg = _pkg(declared=('Apache-2.0',))
        curations = Curations((
            Curation(
                raw_id=pkg.package_key, package_key=pkg.package_key, version_spec='',
                ignore_findings=(IgnoreFinding(path='src/A.java', licenses=frozenset({'GPL-2.0-only'})),),
            ),
        ))
        scan = {
            pkg.purl: {
                'findings': [
                    {'path': 'src/A.java', 'licenses': ['GPL-2.0-only WITH Classpath-exception-2.0'], 'copyrights': []},
                ]
            }
        }
        src = {pkg.purl: {'type': 'artifact', 'url': 'https://example/src.jar', 'sha1': 'a' * 40}}
        resolved = resolve_all({pkg.purl: pkg}, src, scan, _policy(), curations)
        self.assertIn(License('GPL-2.0-only', 'Classpath-exception-2.0'), resolved[0].licenses)

        # Missing scan record for non-metadata_only package must fail closed
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, src, {}, _policy(), Curations())
        self.assertIn('missing scan record', str(ctx.exception))

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
        self.assertNotIn('[[curations]]', str(ctx.exception))

        # Bundled url/path component with no declared license stops
        empty_ref = LicenseFileRef(group='', name='mystery', version='1.0', path='mystery/LICENSE')
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({}, {}, {}, _policy(), Curations(), license_files={empty_ref})
        self.assertIn('no license was found', str(ctx.exception))
        self.assertIn('mystery 1.0', str(ctx.exception))

    def test_multi_declared_or_for_maven_and_cumulative_for_npm(self):
        # Maven: multiple <license> entries are a choice (OR) -> Apache-2.0 wins over GPL-3.0-only
        mvn_pkg = _pkg(purl='pkg:maven/com.example/lib@1.0.0', declared=('Apache-2.0', 'GPL-3.0-only'))
        resolved = resolve_all({mvn_pkg.purl: mvn_pkg}, {mvn_pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertEqual(resolved[0].licenses, (License('Apache-2.0'),))

        # npm: multiple declared entries are cumulative (AND) -> GPL-3.0-only stops as forbidden
        npm_pkg = _pkg(purl='pkg:npm/example-lib@1.0.0', declared=('Apache-2.0', 'GPL-3.0-only'))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({npm_pkg.purl: npm_pkg}, {npm_pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertIn('GPL-3.0-only', str(ctx.exception))

    def test_unscanned_unlicensed_excludes_and_concluded_skips_scan_licenses(self):
        # Package with no source fails closed as unscanned
        pkg = _pkg(declared=('Apache-2.0',))
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, {pkg.purl: None}, {}, _policy(), Curations())
        self.assertIn('cannot be found', str(ctx.exception))

        # Package with no declared license and empty scan fails closed as unlicensed
        empty_pkg = _pkg(declared=())
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({empty_pkg.purl: empty_pkg}, {empty_pkg.purl: {'type': 'metadata_only'}}, {}, _policy(), Curations())
        self.assertIn('no license was found', str(ctx.exception))

        # exclude_paths: npm tarball excludes test/ directory; Maven -sources.jar does NOT exclude Java test package
        npm_pkg = _pkg(purl='pkg:npm/demo@1.0.0', declared=('MIT',))
        npm_src = {npm_pkg.purl: {'type': 'artifact', 'url': 'https://registry.npmjs.org/demo/-/demo-1.0.0.tgz', 'sha1': 'a' * 40}}
        npm_scan = {npm_pkg.purl: {'findings': [{'path': 'test/fixture.js', 'licenses': ['GPL-3.0-only'], 'copyrights': []}]}}
        resolved_npm = resolve_all({npm_pkg.purl: npm_pkg}, npm_src, npm_scan, _policy(), Curations())
        self.assertEqual(resolved_npm[0].licenses, (License('MIT'),))

        mvn_src = {pkg.purl: {'type': 'artifact', 'url': 'https://repo1.maven.org/maven2/com/example/lib/1.0.0/lib-1.0.0-sources.jar', 'sha1': 'a' * 40}}
        mvn_scan_test_pkg = {pkg.purl: {'findings': [{'path': 'org/springframework/test/Helper.java', 'licenses': ['GPL-3.0-only'], 'copyrights': []}]}}
        with self.assertRaises(SystemExit) as ctx:
            resolve_all({pkg.purl: pkg}, mvn_src, mvn_scan_test_pkg, _policy(), Curations())
        self.assertIn('GPL-3.0-only', str(ctx.exception))

        # concluded_license skips scanned licenses (even forbidden ones) while preserving copyrights
        concluded_cur = Curations((
            Curation(raw_id=pkg.purl, package_key=pkg.package_key, version_spec='1.0.0', concluded_license='MIT'),
        ))
        concluded_scan = {pkg.purl: {'findings': [{'path': 'src/A.java', 'licenses': ['GPL-3.0-only'], 'copyrights': ['Copyright 2025 Demo']}]}}
        resolved_conc = resolve_all({pkg.purl: pkg}, mvn_src, concluded_scan, _policy(), concluded_cur)
        self.assertEqual(resolved_conc[0].licenses, (License('MIT'),))
        self.assertEqual(resolved_conc[0].copyrights, ('Copyright 2025 Demo',))


if __name__ == '__main__':
    unittest.main()
