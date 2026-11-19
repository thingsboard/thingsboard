#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of license_files.py.

Run: python3 -m unittest test_license_files   (in this directory)
"""
import os
import pathlib
import tempfile
import unittest

import license_files


class ContentTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.file = pathlib.Path(self.directory.name, 'LICENSE')
        self.file.write_text('Node.js is licensed for use as follows:\n')
        self.downloads = os.path.join(self.directory.name, 'downloads')

    def test_a_file_is_downloaded_once_and_kept(self):
        url = self.file.as_uri()
        self.assertEqual(license_files.content(url, 'node 22.23.4', self.downloads), b'Node.js is licensed for use as follows:\n')
        self.file.unlink()
        self.assertEqual(license_files.content(url, 'node 22.23.4', self.downloads), b'Node.js is licensed for use as follows:\n')

    def test_a_file_that_cannot_be_downloaded_stops_the_notices(self):
        url = pathlib.Path(self.directory.name, 'missing').as_uri()
        with self.assertRaises(SystemExit) as stopped:
            license_files.content(url, 'node 22.23.4', self.downloads)
        self.assertIn(f'the license file of node 22.23.4 cannot be downloaded from {url}', str(stopped.exception))
