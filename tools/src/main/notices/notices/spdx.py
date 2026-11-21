#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Pure-stdlib SPDX license expression parser and OR resolver.

Supports SPDX 2.x/3.x grammar: simple identifiers (including LicenseRef-*), '+' suffix,
WITH exceptions, AND, OR, and parentheses. Operator precedence: WITH > AND > OR.
"""
from dataclasses import dataclass
import re

TOKEN = re.compile(r'\s*(?:(\()|(\))|\b(AND|OR|WITH)\b|([A-Za-z0-9][A-Za-z0-9._+-]*))')


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
    tokens, pos = [], 0
    length = len(text)
    while pos < length:
        if text[pos].isspace():
            pos += 1
            continue
        match = TOKEN.match(text, pos)
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
    if not tokens:
        raise SpdxSyntaxError('empty license expression')
    return tokens


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

    Includes both full `License WITH Exception` and base `License` IDs so that a dual-license
    LICENSE file containing e.g. the GPL-2.0-only text alongside a Classpath-exception-2.0 header
    is recognized as documenting the declared offer.
    """
    atoms = set()

    def walk(n, inside_or):
        if isinstance(n, License):
            if inside_or:
                atoms.add(str(n))
                atoms.add(n.id)
                # A root LICENSE file for GPL-3.0-or-later contains the verbatim GPL-3.0 text (detected as -only).
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
    """Resolves every OR node using branch_key(license_list) -> comparable rank tuple (lower is better).

    Returns (resolved_licenses_set, undecided_expressions_list).
    If an OR node has a tie for best rank among distinct branches, records str(node) in undecided
    instead of guessing.
    """
    undecided = []

    def walk(n):
        if isinstance(n, License):
            return set() if is_no_license(n) else {n}
        if isinstance(n, And):
            merged = set()
            for child in n.terms:
                merged |= walk(child)
            return merged
        # Or: evaluate each branch independently, then pick the strictly best one.
        candidates = []
        for child in n.terms:
            child_licenses = walk(child)
            if not child_licenses:
                continue
            rank = branch_key(child_licenses)
            candidates.append((rank, child_licenses))
        if not candidates:
            return set()
        # Deduplicate identical license sets across branches (e.g. MIT OR MIT)
        unique = {}
        for rank, lics in candidates:
            key = frozenset(lics)
            if key not in unique or rank < unique[key]:
                unique[key] = rank
        ordered = sorted(unique.items(), key=lambda item: item[1])
        best_rank = ordered[0][1]
        tied = [lics for lics, rank in ordered if rank == best_rank]
        if len(tied) > 1:
            undecided.append(str(n))
            # Return union only so downstream checks don't crash; caller stops on undecided first.
            return set().union(*tied)
        return set(ordered[0][0])

    resolved = walk(node)
    return resolved, undecided
