#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Tests of lwm2m_notice.py.

Run: python3 -m unittest test_lwm2m_notice   (in this directory)
"""
import pathlib
import tempfile
import unittest

import lwm2m_notice

BSD_3 = '''Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
3. Neither the name of the copyright holder nor the names of its contributors'''


def model(comment):
    return f'<?xml version="1.0" encoding="utf-8"?>\n<!--{comment}-->\n<LWM2M><Object/></LWM2M>\n'


class HeaderTest(unittest.TestCase):
    def test_the_copyright_notices_and_the_text_of_the_license(self):
        copyrights, text = lwm2m_notice.header(model(f' BSD-3 Clause License\n\nCopyright 2020 uCIFI Alliance\n\n{BSD_3}\n'))
        self.assertEqual(copyrights, ['Copyright 2020 uCIFI Alliance'])
        self.assertEqual(text, f'BSD-3 Clause License\n\n{BSD_3}')

    def test_what_the_registry_tells_before_its_license_is_left_out(self):
        indented = '\n'.join('  ' + line for line in BSD_3.split('\n'))
        comment = ('\nFILE INFORMATION\n\n  OMA Permanent Document\n    File: OMA-SUP-XML_0-V1_2-20201110-A.xml\n\n'
                   f'LEGAL DISCLAIMER\n\n  Copyright 2020 Open Mobile Alliance.\n\n{indented}\n')
        copyrights, text = lwm2m_notice.header(model(comment))
        self.assertEqual(copyrights, ['Copyright 2020 Open Mobile Alliance.'])
        self.assertEqual(text, BSD_3)

    def test_a_comment_that_ends_with_an_exclamation_mark(self):
        _, text = lwm2m_notice.header(model(f'Copyright 2021 Open Mobile Alliance.\n{BSD_3}\n!'))
        self.assertEqual(text, BSD_3)

    def test_the_grant_of_copyright_license_of_apache_is_no_copyright_notice(self):
        copyrights, text = lwm2m_notice.header(model('2. Grant of Copyright License.\nCopyright (c) 2018 GSMA.'))
        self.assertEqual(copyrights, ['Copyright (c) 2018 GSMA.'])
        self.assertEqual(text, '2. Grant of Copyright License.')

    def test_a_model_without_a_header(self):
        self.assertIsNone(lwm2m_notice.header('<?xml version="1.0"?><LWM2M><Object/></LWM2M>'))


class IdentifyTest(unittest.TestCase):
    def test_the_licenses(self):
        self.assertEqual(lwm2m_notice.identify(BSD_3, '1.xml'), 'BSD-3-Clause')
        self.assertEqual(lwm2m_notice.identify(BSD_3.split('\n3.')[0], '1.xml'), 'BSD-2-Clause')
        self.assertEqual(lwm2m_notice.identify('Permission is hereby granted, free of charge, to', '1.xml'), 'MIT')
        self.assertEqual(lwm2m_notice.identify('Apache License\nVersion 2.0, January 2004', '1.xml'), 'Apache-2.0')

    def test_another_license_stops_the_script(self):
        with self.assertRaises(SystemExit) as stopped:
            lwm2m_notice.identify('All rights reserved.', '10244.xml')
        self.assertIn('the header of the LwM2M object model 10244.xml is not a license', str(stopped.exception))


class CheckSbomTest(unittest.TestCase):
    def pom(self, license):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = pathlib.Path(directory.name, 'pom.xml')
        path.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><build><plugins><plugin><configuration>'
                        f'<components><component><name>lwm2m-registry</name>{license}</component></components>'
                        '</configuration></plugin></plugins></build></project>')
        return str(path)

    def test_the_licenses_of_the_models(self):
        lwm2m_notice.check_sbom({'MIT', 'BSD-3-Clause'}, self.pom('<license>BSD-3-Clause AND MIT</license>'))

    def test_a_license_that_the_sbom_does_not_give_stops_the_script(self):
        with self.assertRaises(SystemExit) as stopped:
            lwm2m_notice.check_sbom({'MIT', 'BSD-3-Clause'}, self.pom('<license>BSD-3-Clause</license>'))
        self.assertIn('the headers of the models give BSD-3-Clause AND MIT. Set it to "BSD-3-Clause AND MIT"',
                      str(stopped.exception))


class NoticeTest(unittest.TestCase):
    def test_each_text_once_under_its_copyright_notices_and_their_models(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        for name, holder in [('10.xml', 'B'), ('2.xml', 'A'), ('3.xml', 'A')]:
            pathlib.Path(directory.name, name).write_text(model(f'\nCopyright 2020 {holder}\n{BSD_3}\n'))
        pathlib.Path(directory.name, '4.xml').write_text('<?xml version="1.0"?><LWM2M/>')
        groups, without = lwm2m_notice.read(directory.name)
        self.assertEqual(without, ['4.xml'])
        text = lwm2m_notice.notice(groups)
        self.assertIn('BSD-3-Clause (3 models)\n~~~~~~~~~~~~~~~~~~~~~~~\n\nCopyright 2020 A\n    2.xml, 3.xml\n'
                      f'Copyright 2020 B\n    10.xml\n\n{BSD_3}\n', text)
        self.assertEqual(text.count('Redistribution'), 1)


if __name__ == '__main__':
    unittest.main()
