#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import unittest

from notices.spdx import And, License, Or, SpdxSyntaxError, is_no_license, leaves, or_branch_atoms, parse, resolve_or


class SpdxTests(unittest.TestCase):
    def test_parse_simple_and_with_and_precedence(self):
        self.assertEqual(parse('Apache-2.0'), License('Apache-2.0'))
        self.assertEqual(parse('GPL-2.0+'), License('GPL-2.0-or-later'))
        self.assertEqual(parse('OR-1.0'), License('OR-1.0'))
        self.assertEqual(
            parse('EPL-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0'),
            Or((License('EPL-2.0'), License('GPL-2.0-only', 'Classpath-exception-2.0'))),
        )
        # AND binds tighter than OR
        self.assertEqual(
            parse('MIT AND BSD-3-Clause OR GPL-3.0-only'),
            Or((And((License('MIT'), License('BSD-3-Clause'))), License('GPL-3.0-only'))),
        )
        self.assertEqual(
            parse('(MIT OR GPL-3.0-or-later) AND Zlib'),
            And((Or((License('MIT'), License('GPL-3.0-or-later'))), License('Zlib'))),
        )

    def test_parse_rejects_free_text(self):
        with self.assertRaises(SpdxSyntaxError):
            parse('Bouncy Castle Licence')
        with self.assertRaises(SpdxSyntaxError):
            parse('')

    def test_or_branch_atoms_includes_base_license_of_with(self):
        node = parse('(EPL-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0) AND Apache-2.0')
        atoms = or_branch_atoms(node)
        self.assertEqual(atoms, {'EPL-2.0', 'GPL-2.0-only WITH Classpath-exception-2.0', 'GPL-2.0-only'})
        self.assertNotIn('Apache-2.0', atoms)

    def test_resolve_or_picks_best_and_ignores_losing_branch_ties(self):
        ranks = {'MIT': 1, 'Apache-2.0': 1, 'BSD-3-Clause': 2, 'GPL-2.0-only': 4, 'GPL-3.0-only': 4}
        branch_key = lambda lics: max(ranks[str(l)] for l in lics)

        resolved, undecided = resolve_or(parse('BSD-3-Clause OR GPL-2.0-only'), branch_key)
        self.assertEqual(resolved, {License('BSD-3-Clause')})
        self.assertEqual(undecided, [])

        _, undecided = resolve_or(parse('Apache-2.0 OR MIT'), branch_key)
        self.assertEqual(undecided, ['Apache-2.0 OR MIT'])

        # Identical branches do not tie
        resolved, undecided = resolve_or(parse('MIT OR MIT'), branch_key)
        self.assertEqual(resolved, {License('MIT')})
        self.assertEqual(undecided, [])

        # A tie inside a losing branch must not abort an enclosing OR with a clear winner
        resolved, undecided = resolve_or(parse('MIT OR (GPL-2.0-only OR GPL-3.0-only)'), branch_key)
        self.assertEqual(resolved, {License('MIT')})
        self.assertEqual(undecided, [])

        # Nested OR ties of equal rank must fail closed rather than picking an untied sibling
        ranks_with_tie = {**ranks, 'Zlib': 1}
        tie_key = lambda lics: (max(ranks_with_tie[str(l)] for l in lics), len(lics))
        _, undecided = resolve_or(parse('(Apache-2.0 OR MIT) OR Zlib'), tie_key)
        self.assertTrue(undecided)
        _, undecided = resolve_or(parse('((Apache-2.0 OR MIT) AND BSD-3-Clause) OR (Zlib AND BSD-3-Clause)'), tie_key)
        self.assertTrue(undecided)
        # Branch-order invariance when a decided sibling overlaps an undecided And branch's representative
        _, und_a = resolve_or(parse('((Apache-2.0 OR MIT) AND BSD-3-Clause) OR (Apache-2.0 AND BSD-3-Clause)'), tie_key)
        _, und_b = resolve_or(parse('((MIT OR Apache-2.0) AND BSD-3-Clause) OR (Apache-2.0 AND BSD-3-Clause)'), tie_key)
        self.assertTrue(und_a)
        self.assertTrue(und_b)

    def test_leaves_deduplicates(self):
        node = parse('MIT AND (MIT OR Apache-2.0)')
        self.assertEqual(leaves(node), [License('MIT'), License('Apache-2.0')])

    def test_parse_memoizes_valid_expressions(self):
        first = parse('Apache-2.0 OR MIT')
        second = parse('Apache-2.0 OR MIT')
        self.assertIs(first, second)
        with self.assertRaises(SpdxSyntaxError):
            parse('not valid spdx')

    def test_is_no_license_preserves_noassertion_with_real_exception(self):
        self.assertTrue(is_no_license(License('NONE')))
        self.assertTrue(is_no_license(License('NOASSERTION')))
        self.assertTrue(is_no_license(License('NOASSERTION', 'NOASSERTION')))
        self.assertFalse(is_no_license(License('NOASSERTION', 'Classpath-exception-2.0')))
        self.assertFalse(is_no_license(License('NOASSERTION-CUSTOM')))

        branch_key = lambda lics: len(lics)
        resolved, undecided = resolve_or(parse('MIT AND NOASSERTION AND NONE'), branch_key)
        self.assertEqual(resolved, {License('MIT')})
        self.assertEqual(undecided, [])
        resolved_exc, _ = resolve_or(parse('MIT AND NOASSERTION WITH Classpath-exception-2.0'), branch_key)
        self.assertEqual(resolved_exc, {License('MIT'), License('NOASSERTION', 'Classpath-exception-2.0')})


if __name__ == '__main__':
    unittest.main()
