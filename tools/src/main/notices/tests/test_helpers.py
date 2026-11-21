#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import unittest

from notices.license_files import filter_platforms
from notices.lwm2m import identify_license, parse_header
from notices.scanner import is_notice_path


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


if __name__ == '__main__':
    unittest.main()
