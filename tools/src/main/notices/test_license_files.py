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


class KeptTest(unittest.TestCase):
    def test_a_file_of_this_repository(self):
        with tempfile.NamedTemporaryFile() as file:
            file.write(b'LwM2M object models')
            file.flush()
            self.assertEqual(license_files.kept(file.name, 'lwm2m-registry'), b'LwM2M object models')

    def test_a_missing_file_stops_the_notices(self):
        with self.assertRaises(SystemExit) as stopped:
            license_files.kept('/nonexistent/NOTICE.txt', 'lwm2m-registry')
        self.assertIn('the license file of lwm2m-registry cannot be read', str(stopped.exception))


CREDITS = """Chromium: the third-party software in it.

========
zlib

======== Only in the Linux build ========

========
wayland

======== Only in the Windows build ========

========
Multimedia Redirection API"""


class PlatformsTest(unittest.TestCase):
    def test_the_platforms_of_the_components_that_have_a_file(self):
        entries = [{'name': 'chromium', 'path': 'CREDITS.txt', 'platform': 'Linux'},
                   {'name': 'chromium', 'path': 'CREDITS.txt', 'platform': 'Windows'},
                   {'name': 'lwm2m-registry', 'path': 'NOTICE.txt'},
                   {'name': 'node', 'url': 'https://example.com/LICENSE'}]
        self.assertEqual(license_files.platforms_of(entries), {'CREDITS.txt': {'Linux', 'Windows'}, 'NOTICE.txt': None})

    def test_a_component_for_every_platform_takes_the_whole_file(self):
        entries = [{'name': 'a', 'path': 'CREDITS.txt', 'platform': 'Linux'}, {'name': 'b', 'path': 'CREDITS.txt'}]
        self.assertEqual(license_files.platforms_of(entries), {'CREDITS.txt': None})
        self.assertEqual(license_files.platforms_of(list(reversed(entries))), {'CREDITS.txt': None})

    def test_the_sections_of_other_platforms_are_left_out(self):
        linux = license_files.for_platforms(CREDITS, {'Linux'}, 'CREDITS.txt')
        self.assertIn('wayland', linux)
        self.assertNotIn('Multimedia', linux)
        self.assertTrue(linux.endswith('wayland'))
        windows = license_files.for_platforms(CREDITS, {'Windows'}, 'CREDITS.txt')
        self.assertNotIn('wayland', windows)
        self.assertIn('zlib\n\n======== Only in the Windows build ========\n\n========\nMultimedia', windows)

    def test_the_file_of_every_platform_is_the_whole_file(self):
        self.assertEqual(license_files.for_platforms(CREDITS, {'Linux', 'Windows'}, 'CREDITS.txt'), CREDITS)
        self.assertEqual(license_files.for_platforms(CREDITS, None, 'CREDITS.txt'), CREDITS)

    def test_a_platform_without_a_section_stops_the_notices(self):
        with self.assertRaises(SystemExit) as stopped:
            license_files.for_platforms(CREDITS, {'linux'}, 'CREDITS.txt')
        self.assertIn('CREDITS.txt has no section for the platform linux, which license-files-here.json gives: its '
                      'sections are for Linux, Windows.', str(stopped.exception))

    def test_a_file_without_sections_is_for_every_platform(self):
        self.assertEqual(license_files.for_platforms('zlib', {'Linux'}, 'NOTICE.txt'), 'zlib')
