#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of which files notice_files.py takes for NOTICE files, and of how it lays their texts out.

Run: python3 -m unittest test_notice_files   (in this directory)
"""
import unittest

import notice_files


class IsNoticeTest(unittest.TestCase):
    def test_names(self):
        for path, expected in [
            ('META-INF/NOTICE', True),
            ('META-INF/NOTICE.txt', True),
            ('META-INF/notice.txt', True),
            ('META-INF/legal/NOTICE.md', True),
            ('META-INF/NOTICE_binary.txt', True),
            ('license/NOTICE.harmony.txt', True),
            ('NOTICE', True),
            ('org/bouncycastle/asn1/x509/NoticeReference.class', False),
            ('org/bouncycastle/asn1/x509/NoticeReference.java', False),
            ('notice/README', False),
            ('META-INF/NOTICE.class', False),
            ('META-INF/LICENSE', False),
        ]:
            self.assertEqual(notice_files.is_notice(path), expected, path)


class LayoutTest(unittest.TestCase):
    def test_a_text_is_given_once_under_all_its_packages(self):
        found = {'Second\n\ntext': {('b:b 1', 'NOTICE')},
                 'First text': {('a:z 1', 'META-INF/NOTICE'), ('a:a 1', 'META-INF/NOTICE.txt')}}
        self.assertEqual(notice_files.layout(found), '\n'.join([
            notice_files.SEPARATOR, 'a:a 1 (META-INF/NOTICE.txt)', 'a:z 1 (META-INF/NOTICE)', '', '    First text', '',
            notice_files.SEPARATOR, 'b:b 1 (NOTICE)', '', '    Second', '', '    text', '']))

    def test_clean_keeps_the_text_and_drops_what_does_not_show(self):
        self.assertEqual(notice_files.clean(b'\xef\xbb\xbf\r\n One  \r\n\r\nTwo\t\r\n\r\n'), ' One\n\nTwo')


if __name__ == '__main__':
    unittest.main()
