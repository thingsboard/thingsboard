#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of which files notice_files.py takes for NOTICE files, and of how it lays their texts out.

Run: python3 -m unittest test_notice_files   (in this directory)
"""
import os
import tempfile
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
            ('META-INF/FastDoubleParser-NOTICE', True),
            ('META-INF/THIRD_PARTY_NOTICES', True),
            ('thirdpartynotice.txt', True),
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


class JarsTest(unittest.TestCase):
    def test_counts_the_jars_that_were_there_and_lists_the_downloaded_ones(self):
        with tempfile.TemporaryDirectory() as downloads:
            def jar(name, size):
                path = os.path.join(downloads, name)
                open(path, 'wb').write(b'x' * size)
                return path
            sources = [('a:a 1', '/m2/a.jar', None), ('b:b 1', jar('b.jar', 20), None),
                       ('d:d 1', jar('d.jar', 2_000_000), 'https://repo/d.jar'),
                       ('c:c 1', jar('c.jar', 1_000_000), 'https://repo/c.jar')]
            summary = notice_files.jars(sources, '/m2', downloads)
        self.assertEqual(summary.replace(downloads, '/downloads'), '\n'.join([
            'Jars: 4 in all; 1 from the local Maven repository (/m2), 1 downloaded earlier (/downloads), '
            '2 downloaded now (3 MB):',
            '  c:c 1 (https://repo/c.jar)',
            '  d:d 1 (https://repo/d.jar)']))

    def test_says_when_nothing_was_downloaded(self):
        self.assertEqual(notice_files.jars([('a:a 1', '/m2/a.jar', None)], '/m2', '/downloads'),
                         'Jars: 1 in all; 1 from the local Maven repository (/m2), 0 downloaded earlier (/downloads), '
                         '0 downloaded now (0 MB)')


if __name__ == '__main__':
    unittest.main()
