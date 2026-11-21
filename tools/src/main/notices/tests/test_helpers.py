#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import subprocess
import unittest
from unittest import mock

from notices.license_files import filter_platforms
from notices.lwm2m import identify_license, parse_header
from notices.scanner import ensure_docker_available, is_notice_path


class HelpersTests(unittest.TestCase):
    def test_filter_platforms_keeps_selected_and_rejects_unknown(self):
        text = (
            'Common\n'
            '======== Only in the Linux build ========\n'
            'Linux only\n'
            '======== Only in the Windows build ========\n'
            'Windows only'
        )
        self.assertEqual(filter_platforms(text, {'linux'}, 'CREDITS.txt'),
                         'Common\n======== Only in the Linux build ========\nLinux only')
        self.assertEqual(filter_platforms(text, {'windows'}, 'CREDITS.txt'),
                         'Common\n======== Only in the Windows build ========\nWindows only')
        self.assertEqual(filter_platforms(text, None, 'CREDITS.txt'), text)
        with self.assertRaises(SystemExit):
            filter_platforms(text, {'macos'}, 'CREDITS.txt')

    def test_lwm2m_header_and_identify(self):
        xml = '<?xml version="1.0"?>\n<!--\nLEGAL DISCLAIMER\nCopyright 2020 OMA.\nPermission is hereby granted, free of charge, to any person.\n-->\n<LWM2M/>'
        copyrights, text = parse_header(xml)
        self.assertEqual(copyrights, ['Copyright 2020 OMA.'])
        self.assertEqual(identify_license(text, '1.xml'), 'MIT')

    def test_is_notice_path(self):
        self.assertTrue(is_notice_path('META-INF/NOTICE'))
        self.assertTrue(is_notice_path('META-INF/NOTICE.txt'))
        self.assertTrue(is_notice_path('THIRD_PARTY_NOTICES.md'))
        self.assertFalse(is_notice_path('com/example/NoticeReference.class'))

    def test_ensure_docker_available_reports_missing_and_unreachable_daemon(self):
        with mock.patch('notices.scanner.subprocess.run', side_effect=FileNotFoundError):
            with self.assertRaises(SystemExit) as ctx:
                ensure_docker_available()
            self.assertIn('not found in PATH', str(ctx.exception))

        failed = subprocess.CompletedProcess(['docker', 'info'], 1, '', 'Cannot connect to the Docker daemon')
        with mock.patch('notices.scanner.subprocess.run', return_value=failed):
            with self.assertRaises(SystemExit) as ctx:
                ensure_docker_available()
            self.assertIn('Docker daemon is not reachable', str(ctx.exception))


if __name__ == '__main__':
    unittest.main()
