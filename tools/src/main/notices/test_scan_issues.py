#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of how scan_issues.py finds the errors of the scan and leaves out the accepted ones.

Run: python3 -m unittest test_scan_issues   (in this directory)
"""
import os
import unittest

import scan_issues

ARCHIVE = {'source_artifact': {'url': 'https://example.com/echarts.tgz', 'hash': {'value': 'a', 'algorithm': 'SHA-1'}}}
RESULT = {'scanner': {
    'provenances': [
        {'id': 'NPM::echarts:5.5.4', 'package_provenance': ARCHIVE},
        {'id': 'NPM::rxjs:7.99.0', 'package_provenance_resolution_issue': {
            'message': 'IOException: Could not resolve provenance\nResolution of ARTIFACT failed', 'severity': 'ERROR'}},
    ],
    'scan_results': [{'provenance': ARCHIVE, 'summary': {'issues': [
        {'message': "ERROR: Timeout after 300 seconds while scanning file 'echarts-5.5.4-TB/dist/echarts.esm.mjs.map'.",
         'severity': 'ERROR'},
        {'message': 'ERROR: Timeout after 300 seconds while scanning file \'dist/echarts.js\'.', 'severity': 'ERROR'},
        {'message': 'Something to note', 'severity': 'HINT'},
    ]}}],
}}


class ScanIssuesTest(unittest.TestCase):
    def test_issues_of_finding_and_of_scanning_the_source_by_package(self):
        self.assertEqual(sorted(scan_issues.issues_by_package(RESULT)), [
            ('NPM::echarts:5.5.4', "ERROR: Timeout after 300 seconds while scanning file 'dist/echarts.js'."),
            ('NPM::echarts:5.5.4', "ERROR: Timeout after 300 seconds while scanning file "
                                   "'echarts-5.5.4-TB/dist/echarts.esm.mjs.map'."),
            ('NPM::rxjs:7.99.0', 'IOException: Could not resolve provenance\nResolution of ARTIFACT failed'),
        ])

    def test_the_resolutions_of_the_product_accept_the_source_map_of_echarts_only(self):
        patterns = scan_issues.accepted(os.path.join(os.path.dirname(__file__), 'ort-config', 'resolutions.yml'))
        left = scan_issues.unaccepted(scan_issues.issues_by_package(RESULT), patterns)
        self.assertEqual(sorted(package for package, _ in left), ['NPM::echarts:5.5.4', 'NPM::rxjs:7.99.0'])
        self.assertNotIn('echarts.esm.mjs.map', ''.join(text for _, text in left))

    def test_message_indents_the_lines_of_an_error_under_its_package(self):
        text = scan_issues.message([('NPM::rxjs:7.99.0', 'first line\nsecond line')])
        self.assertIn('  NPM::rxjs:7.99.0: first line\n    second line\n', text)
        self.assertIn('tools/src/main/notices/ort-config/resolutions.yml', text)


if __name__ == '__main__':
    unittest.main()
