#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Pure-stdlib SPDX license expression parser and OR resolver.

Supports SPDX 2.x/3.x grammar: simple identifiers (including LicenseRef-*), '+' suffix,
WITH exceptions, AND, OR, and parentheses. Operator precedence: WITH > AND > OR.
"""
from dataclasses import dataclass
import functools
import re

TOKEN = re.compile(r'\s*(?:(\()|(\))|(AND|OR|WITH)(?![A-Za-z0-9._+-])|([A-Za-z0-9][A-Za-z0-9._+-]*))')


class SpdxSyntaxError(ValueError):
    """Raised when a string is not a valid SPDX license expression."""


@dataclass(frozen=True)
class License:
    id: str
    exception: str | None = None

    def __str__(self):
        return f'{self.id} WITH {self.exception}' if self.exception else self.id


# Legacy SPDX 1.x/2.x identifiers normalized to canonical SPDX 3.x expressions.
LEGACY_LICENSES = {
    'GPL-1.0+': License('GPL-1.0-or-later'),
    'GPL-2.0+': License('GPL-2.0-or-later'),
    'GPL-3.0+': License('GPL-3.0-or-later'),
    'LGPL-2.0+': License('LGPL-2.0-or-later'),
    'LGPL-2.1+': License('LGPL-2.1-or-later'),
    'LGPL-3.0+': License('LGPL-3.0-or-later'),
    'AGPL-3.0+': License('AGPL-3.0-or-later'),
    'GFDL-1.1+': License('GFDL-1.1-or-later'),
    'GFDL-1.2+': License('GFDL-1.2-or-later'),
    'GFDL-1.3+': License('GFDL-1.3-or-later'),
    'GPL-2.0-with-classpath-exception': License('GPL-2.0-only', 'Classpath-exception-2.0'),
    'GPL-2.0-only-with-classpath-exception': License('GPL-2.0-only', 'Classpath-exception-2.0'),
}


@dataclass(frozen=True)
class And:
    terms: tuple

    def __str__(self):
        return ' AND '.join(f'({t})' if isinstance(t, Or) else str(t) for t in self.terms)


@dataclass(frozen=True)
class Or:
    terms: tuple

    def __str__(self):
        return ' OR '.join(str(t) for t in self.terms)


def normalize_license(identifier, exception=None):
    """Canonicalizes legacy SPDX identifiers (e.g. GPL-2.0+, GPL-2.0-with-classpath-exception)."""
    if exception is None and identifier in LEGACY_LICENSES:
        return LEGACY_LICENSES[identifier]
    base = LEGACY_LICENSES[identifier].id if identifier in LEGACY_LICENSES else identifier
    return License(base, exception)


def tokenize(text):
    stripped = text.strip()
    if not stripped:
        raise SpdxSyntaxError('empty license expression')
    tokens, pos, length = [], 0, len(stripped)
    while pos < length:
        match = TOKEN.match(stripped, pos)
        if not match:
            raise SpdxSyntaxError(f'unexpected character at {pos} in {text!r}')
        lparen, rparen, op, ident = match.groups()
        if lparen:
            tokens.append(('(', '('))
        elif rparen:
            tokens.append((')', ')'))
        elif op:
            tokens.append((op, op))
        else:
            tokens.append(('ID', ident))
        pos = match.end()
    return tokens


@functools.lru_cache(maxsize=None)
def parse(text):
    """Parses an SPDX expression into an AST of License / And / Or nodes."""
    tokens = tokenize(text)
    index = 0

    def peek():
        return tokens[index] if index < len(tokens) else (None, None)

    def consume(expected=None):
        nonlocal index
        kind, value = peek()
        if expected and kind != expected:
            raise SpdxSyntaxError(f'expected {expected}, got {value!r} in {text!r}')
        index += 1
        return value

    def parse_or():
        terms = [parse_and()]
        while peek()[0] == 'OR':
            consume('OR')
            terms.append(parse_and())
        return terms[0] if len(terms) == 1 else Or(tuple(terms))

    def parse_and():
        terms = [parse_atom()]
        while peek()[0] == 'AND':
            consume('AND')
            terms.append(parse_atom())
        return terms[0] if len(terms) == 1 else And(tuple(terms))

    def parse_atom():
        kind, _ = peek()
        if kind == '(':
            consume('(')
            node = parse_or()
            consume(')')
            return node
        if kind != 'ID':
            raise SpdxSyntaxError(f'expected license identifier in {text!r}')
        lic = consume('ID')
        if peek()[0] == 'WITH':
            consume('WITH')
            exc = consume('ID')
            return normalize_license(lic, exc)
        return normalize_license(lic)

    result = parse_or()
    if index != len(tokens):
        raise SpdxSyntaxError(f'trailing tokens in {text!r}: {tokens[index:]}')
    return result


def is_no_license(lic):
    """NONE and NOASSERTION carry no license grant."""
    return lic.id == 'NONE' or lic.id.startswith('NOASSERTION')


def leaves(node):
    """All License leaves in an expression AST, preserving order and deduplicating."""
    found = []
    seen = set()

    def walk(n):
        if isinstance(n, License):
            if n not in seen:
                seen.add(n)
                found.append(n)
        else:
            for child in n.terms:
                walk(child)

    walk(node)
    return found


def or_branch_atoms(node):
    """Simple license strings offered as choices inside any OR node of the AST.

    Includes both full `License WITH Exception`, base `License` IDs, and `-only` equivalents
    of `-or-later` licenses so a root LICENSE file containing the verbatim license text is
    recognized as documenting the declared dual-license offer.
    """
    atoms = set()

    def walk(n, inside_or):
        if isinstance(n, License):
            if inside_or:
                atoms.add(str(n))
                atoms.add(n.id)
                if n.id.endswith('-or-later'):
                    atoms.add(n.id.removesuffix('-or-later') + '-only')
        elif isinstance(n, Or):
            for child in n.terms:
                walk(child, True)
        else:
            for child in n.terms:
                walk(child, inside_or)

    walk(node, False)
    return atoms


def resolve_or(node, branch_key):
    """Resolves every OR node using branch_key(license_set) -> comparable rank tuple (lower is better).

    Returns (resolved_licenses_set, undecided_expressions_list). Only propagates undecided ties from
    winning branches, so an irrelevant tie inside a losing branch (e.g. Apache-2.0 OR (GPL-2.0 OR GPL-3.0))
    does not falsely abort resolution.
    """
    def walk(n):
        if isinstance(n, License):
            return (set() if is_no_license(n) else {n}), []
        if isinstance(n, And):
            merged, undecided = set(), []
            for child in n.terms:
                child_lics, child_undecided = walk(child)
                merged |= child_lics
                undecided.extend(child_undecided)
            return merged, undecided

        # Or: flatten nested Or nodes so equally ranked ties cannot be skewed by inflated union sizes.
        def iter_branches(or_node):
            for child in or_node.terms:
                if isinstance(child, Or):
                    yield from iter_branches(child)
                else:
                    yield child

        # Keep decided branches and undecided branches distinct so a decided sibling cannot erase an
        # undecided tie on an arbitrary representative winner.
        unique = {}  # (frozenset(licenses), has_undecided) -> list of undecided from that branch
        for child in iter_branches(n):
            child_lics, child_undecided = walk(child)
            if not child_lics:
                continue
            key = (frozenset(child_lics), bool(child_undecided))
            if key not in unique:
                unique[key] = child_undecided
        if not unique:
            return set(), []
        ranked = sorted(
            ((branch_key(lics), sorted(str(l) for l in lics), lics, und) for (lics, _), und in unique.items()),
            key=lambda item: (item[0], item[1]),
        )
        best_rank = ranked[0][0]
        winners = [(lics, und) for rank, _, lics, und in ranked if rank == best_rank]
        if len(winners) > 1:
            # Return one deterministic representative winner (keeping best_rank intact for any enclosing Or) plus the tie.
            return set(winners[0][0]), [str(n)]
        return set(winners[0][0]), winners[0][1]

    return walk(node)
