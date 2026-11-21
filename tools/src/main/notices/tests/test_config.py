#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import os
import tempfile
import unittest

from notices.config import covers, load_curations, load_policy, split_curation_id
from notices.copyrights import process_copyrights


class ConfigAndCopyrightTests(unittest.TestCase):
    def test_split_curation_id(self):
        self.assertEqual(split_curation_id('pkg:maven/org.jfree/jfreechart'), ('pkg:maven/org.jfree/jfreechart', ''))
        self.assertEqual(split_curation_id('pkg:maven/com.sun.winsw/winsw@2.0.1'), ('pkg:maven/com.sun.winsw/winsw', '2.0.1'))
        self.assertEqual(split_curation_id('pkg:npm/@mdi/svg@[7.0,8.0['), ('pkg:npm/@mdi/svg', '[7.0,8.0['))
        self.assertEqual(split_curation_id('pkg:npm/%40mdi/svg@[7.0,8.0['), ('pkg:npm/@mdi/svg', '[7.0,8.0['))

    def test_version_range_covers(self):
        self.assertTrue(covers('2.0.1', '2.0.1'))
        self.assertFalse(covers('2.0.1', '2.0.2'))
        self.assertTrue(covers('[1.0,2.0[', '1.0'))
        self.assertTrue(covers('[1.0,2.0[', '1.9.9'))
        self.assertFalse(covers('[1.0,2.0[', '2.0.0'))
        self.assertTrue(covers('[2.0,3.0[', '2.4.0-b180830.0359'))
        # Non-numeric versions fail closed rather than matching an arbitrary numeric range
        self.assertFalse(covers('[1.0,2.0[', 'snapshot'))

    def test_real_policy_and_curations_load_cleanly(self):
        policy = load_policy()
        curations = load_curations()
        self.assertEqual(policy.category('Apache-2.0'), 'notice')
        self.assertEqual(policy.category('GPL-3.0-only'), 'forbidden')
        self.assertIn('LicenseRef-thingsboard-beerware-googlemutant', curations.custom_licenses)

    def test_curations_supports_multi_id_entries(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, 'curations.toml')
            with open(path, 'w', encoding='utf-8') as f:
                f.write(
                    '[[curations]]\n'
                    'ids = ["pkg:maven/com.example/a@1.0", "pkg:maven/com.example/b@1.0"]\n'
                    'metadata_only = true\n'
                )
            curations = load_curations(path)
            self.assertEqual(len(curations.for_package('pkg:maven/com.example/a', '1.0')), 1)
            self.assertEqual(len(curations.for_package('pkg:maven/com.example/b', '1.0')), 1)

    def test_copyright_year_merging_chained_hyphens_and_garbage(self):
        result = process_copyrights(
            [
                'Copyright (C) 2020 Example Corp.',
                'Copyright 2021 Example Corp.',
                'Copyright © 2022-2023 Example Corp.',
                'Copyright (c) 1989, 1991 Free Software Foundation, Inc.',
                'Copyright 2010-2015-2020 Weird Range Inc.',
            ],
            garbage=frozenset({'Copyright (c) 1989, 1991 Free Software Foundation, Inc.'}),
        )
        self.assertEqual(
            result,
            [
                'Copyright (c) 2020-2023 Example Corp.',
                'Copyright 2010-2015-2020 Weird Range Inc.',
            ],
        )


if __name__ == '__main__':
    unittest.main()
