#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Cleans, filters, and merges copyright statements (including year-range coalescing)."""
import re

# Matches a leading copyright prefix + years + holder so statements for the same holder
# across different years (e.g. "Copyright 2020 Foo" + "Copyright (c) 2021 Foo") coalesce cleanly.
YEAR_PATTERN = re.compile(
    r'^(?P<prefix>(?:Portions\s+)?Copyright(?:\s*\(c\))?\s+)'
    r'(?P<years>\d{4}(?:\s*[-,]\s*\d{4})*)'
    r'(?P<rest>.*)$',
    re.IGNORECASE,
)
SINGLE_YEAR_RE = re.compile(r'^\d{4}$')
YEAR_RANGE_RE = re.compile(r'^(\d{4})\s*-\s*(\d{4})$')
SYMBOL_RE = re.compile(r'\s*\(c\)', re.IGNORECASE)


def normalize_statement(statement):
    """Normalizes whitespace, copyright symbols, and spaces before commas."""
    text = ' '.join(statement.split())
    text = text.replace('©', '(c)').replace('(C)', '(c)')
    text = re.sub(r'\s+,', ',', text)
    return text.strip()


def process_copyrights(statements, garbage):
    """Cleans, drops garbage statements, merges year ranges for identical holders, and sorts."""
    # Sort first so display casing for merged holders is independent of input order.
    cleaned = sorted({
        stmt for raw in statements
        if (stmt := normalize_statement(raw)) and stmt not in garbage
    })

    by_holder = {}  # (base_prefix_lower, rest_lower) -> [display_prefix, display_rest, set_of_years]
    unparsed = set()

    for stmt in cleaned:
        match = YEAR_PATTERN.match(stmt)
        if not match:
            unparsed.add(stmt)
            continue
        prefix, years_text, rest = match.group('prefix'), match.group('years'), match.group('rest')
        years = _parse_years(years_text)
        if not years:
            unparsed.add(stmt)
            continue
        # Strip '(c)' from key so "Copyright 2020 X" and "Copyright (c) 2021 X" coalesce.
        base_prefix = SYMBOL_RE.sub('', prefix).strip().lower()
        key = (base_prefix, rest.strip().lower())
        if key not in by_holder:
            by_holder[key] = [prefix, rest, set()]
        elif '(c)' in prefix and '(c)' not in by_holder[key][0]:
            by_holder[key][0] = prefix
        by_holder[key][2].update(years)

    merged = set(unparsed)
    for prefix, rest, years in by_holder.values():
        merged.add(f'{prefix}{_format_years(years)}{rest}'.strip())

    return sorted(merged)


def _parse_years(text):
    years = set()
    for part in text.split(','):
        part = part.strip()
        if SINGLE_YEAR_RE.match(part):
            years.add(int(part))
            continue
        rng = YEAR_RANGE_RE.match(part)
        if not rng:
            return set()
        lo, hi = int(rng.group(1)), int(rng.group(2))
        if lo <= hi and hi - lo <= 100:
            years.update(range(lo, hi + 1))
        else:
            return set()
    return years


def _format_years(years):
    ordered = sorted(years)
    ranges = []
    start = prev = ordered[0]
    for year in ordered[1:]:
        if year == prev + 1:
            prev = year
        else:
            ranges.append(f'{start}-{prev}' if prev > start else str(start))
            start = prev = year
    ranges.append(f'{start}-{prev}' if prev > start else str(start))
    return ', '.join(ranges)
