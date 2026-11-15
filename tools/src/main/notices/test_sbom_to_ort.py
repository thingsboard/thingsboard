#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of what sbom_to_ort.py decides about curations by itself, where ORT is not asked.

Run: python3 -m unittest test_sbom_to_ort   (in this directory)
"""
import unittest

import sbom_to_ort


def concluded(identifier):
    return {'id': identifier, 'curations': {'concluded_license': 'MIT'}}


class CoversTest(unittest.TestCase):
    def test_range(self):
        for versions, version, expected in [
            ('[1.0,2.0[', '1.0', True),
            ('[1.0,2.0[', '1.5.38', True),
            ('[1.0,2.0[', '2.0', False),
            ('[1.0,2.0[', '2.0.0', False),
            ('[1.0,2.0[', '0.9', False),
            ('[1.0,2.0]', '2.0', True),
            (']1.0,2.0[', '1.0', False),
            (']1.0,2.0[', '1.0.1', True),
            ('[1.0,[', '7.4', True),
            (']1.0,[', '1.0', False),
            ('],2.0[', '1.9', True),
            ('],2.0[', '2.0', False),
        ]:
            with self.subTest(versions=versions, version=version):
                self.assertEqual(sbom_to_ort.covers(versions, version), expected)

    def test_version_shorter_or_longer_than_the_limits(self):
        for versions, version, expected in [
            ('[2.0,3.0[', '2', True),
            ('[2.0,3.0[', '3', False),
            ('[75.0,76.0[', '76', False),
            ('[2,3[', '2.9.9', True),
            ('[2,3[', '3.0.0', False),
        ]:
            with self.subTest(versions=versions, version=version):
                self.assertEqual(sbom_to_ort.covers(versions, version), expected)

    def test_version_with_a_qualifier_or_leading_zeros(self):
        self.assertTrue(sbom_to_ort.covers('[2.0,3.0[', '2.4.0-b180830.0359'))
        self.assertTrue(sbom_to_ort.covers('[1.4,2.0[', '1.4.01'))
        self.assertTrue(sbom_to_ort.covers('[1.0,2.0[', '1.3.04'))
        self.assertTrue(sbom_to_ort.covers('[4.0,5.0[', '4.1.138.Final'))

    def test_single_version(self):
        self.assertTrue(sbom_to_ort.covers('2.0.1', '2.0.1'))
        self.assertFalse(sbom_to_ort.covers('2.0.1', '2.0.2'))


class UnreviewedTest(unittest.TestCase):
    CURATIONS = [
        concluded('Maven:org.example:lib:[1.0,2.0['),
        concluded('Maven:org.example:lib:[3.0,4.0['),
        concluded('Maven:org.example:library:[7.0,8.0['),
        {'id': 'Maven:org.example:lib', 'curations': {'declared_license_mapping': {'Example License': 'MIT'}}},
        {'id': 'Maven:org.example:named:[1.0,2.0[', 'curations': {'declared_license_mapping': {'Example License': 'MIT'}}},
    ]

    def test_version_within_a_range(self):
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:lib:1.2', self.CURATIONS), [])
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:lib:3.9.1', self.CURATIONS), [])

    def test_version_outside_every_range(self):
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:lib:2.1', self.CURATIONS), ['[1.0,2.0[', '[3.0,4.0['])

    def test_package_whose_name_starts_like_another(self):
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:library:2.1', self.CURATIONS), ['[7.0,8.0['])

    def test_package_without_a_concluded_license(self):
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:other:2.1', self.CURATIONS), [])
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:named:2.1', self.CURATIONS), [])

    def test_npm_package(self):
        curations = [concluded('NPM:@mdi:svg:[7.0,8.0['), concluded('NPM::jstree:[3.0,4.0[')]
        self.assertEqual(sbom_to_ort.unreviewed('NPM:@mdi:svg:7.4.47', curations), [])
        self.assertEqual(sbom_to_ort.unreviewed('NPM::jstree:4.0.0', curations), ['[3.0,4.0['])


class MappedTest(unittest.TestCase):
    CURATIONS = [
        {'id': 'Maven:org.example:lib', 'curations': {'declared_license_mapping': {'Example License': 'MIT'}}},
        {'id': 'Maven:org.group', 'curations': {'declared_license_mapping': {'Group License': 'MIT'}}},
        {'id': 'Maven:org.example:ranged:[1.0,2.0[', 'curations': {'declared_license_mapping': {'Example License': 'MIT'}}},
    ]

    def test_curation_of_the_package(self):
        self.assertTrue(sbom_to_ort.mapped('Maven:org.example:lib:1.0', 'Example License', self.CURATIONS))
        self.assertFalse(sbom_to_ort.mapped('Maven:org.example:lib:1.0', 'Another License', self.CURATIONS))

    def test_curation_of_the_group(self):
        self.assertTrue(sbom_to_ort.mapped('Maven:org.group:any:1.0', 'Group License', self.CURATIONS))
        self.assertFalse(sbom_to_ort.mapped('Maven:org.group.sub:any:1.0', 'Group License', self.CURATIONS))

    def test_curation_with_a_version_does_not_count(self):
        self.assertFalse(sbom_to_ort.mapped('Maven:org.example:ranged:1.5', 'Example License', self.CURATIONS))


if __name__ == '__main__':
    unittest.main()
