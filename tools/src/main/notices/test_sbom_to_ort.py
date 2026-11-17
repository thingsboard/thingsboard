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

    def test_curation_without_a_version_concludes_every_version(self):
        self.assertEqual(sbom_to_ort.unreviewed('Maven:org.example:any:9.0', [concluded('Maven:org.example:any')]), [])


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


class SourceCuratedTest(unittest.TestCase):
    curations = [
        {'id': 'Maven:com.example:fork:2.8.8TB', 'curations': {'vcs': {'type': 'Git', 'url': 'u', 'revision': 'r'}}},
        {'id': 'Maven:com.adobe.xmp:xmpcore:[6.0,7.0[', 'curations': {'is_metadata_only': True}},
        {'id': 'Maven:com.example:licensed:[1.0,2.0[', 'curations': {'concluded_license': 'MIT'}},
        {'id': 'Maven:com.example:everywhere', 'curations': {'is_metadata_only': True}},
        {'id': 'Maven:org.nosources', 'curations': {'is_metadata_only': True}},
    ]

    def test_curation_that_gives_the_source_or_says_there_is_none(self):
        self.assertTrue(sbom_to_ort.source_curated('Maven:com.example:fork:2.8.8TB', self.curations))
        self.assertTrue(sbom_to_ort.source_curated('Maven:com.adobe.xmp:xmpcore:6.1.11', self.curations))
        self.assertTrue(sbom_to_ort.source_curated('Maven:com.example:everywhere:3.0', self.curations))

    def test_curation_of_the_group(self):
        self.assertTrue(sbom_to_ort.source_curated('Maven:org.nosources:any:1.0', self.curations))
        self.assertFalse(sbom_to_ort.source_curated('Maven:org.nosources.sub:any:1.0', self.curations))

    def test_curation_of_another_version_or_without_a_source_does_not_count(self):
        self.assertFalse(sbom_to_ort.source_curated('Maven:com.example:fork:2.8.9TB', self.curations))
        self.assertFalse(sbom_to_ort.source_curated('Maven:com.adobe.xmp:xmpcore:7.0', self.curations))
        self.assertFalse(sbom_to_ort.source_curated('Maven:com.example:licensed:1.2', self.curations))


class MessagesTest(unittest.TestCase):
    def test_each_message_names_the_packages_and_the_file_to_change(self):
        for text, package in [
            (sbom_to_ort.unreviewed_message([('Maven:com.sun.mail:jakarta.mail:3.0.1', ['[2.0,3.0['])]), 'jakarta.mail:3.0.1'),
            (sbom_to_ort.unnamed_message([('Maven:com.example:widget:1.4.0', 'Apache Software License')]), 'widget:1.4.0'),
            (sbom_to_ort.unscanned_message(['Maven:com.example:widget:1.4.0']), 'widget:1.4.0'),
        ]:
            self.assertIn(package, text)
            self.assertIn(sbom_to_ort.CURATIONS, text)

    def test_the_mapping_of_a_name_is_for_every_version(self):
        text = sbom_to_ort.unnamed_message([('Maven:com.example:widget:1.4.0', 'Apache Software License')])
        self.assertIn('"id": "Maven:com.example:widget",', text)
        self.assertIn('{"Apache Software License": "<identifier>"}', text)


if __name__ == '__main__':
    unittest.main()
