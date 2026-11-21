#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Cleans, filters, and merges copyright statements (including year-range coalescing)."""
import re

# Matches a leading copyright prefix + years + holder so statements for the same holder
# across different years (e.g. "Copyright 2020 Foo" + "Copyright 2021 Foo") coalesce cleanly.
YEAR_PATTERN = re.compile(
    r'^(?P<prefix>(?:Portions\s+)?Copyright(?:\s*\(c\))?\s+)'
    r'(?P<years>\d{4}(?:\s*[-,]\s*\d{4})*)'
    r'(?P<rest>.*)$',
    re.IGNORECASE,
)


def normalize_statement(statement):
    """Normalizes whitespace, copyright symbol, and trailing punctuation."""
    text = ' '.join(statement.split())
    text = text.replace('©', '(c)').replace('(C)', '(c)')
    # Remove redundant space before commas/periods and trailing stray punctuation
    text = re.sub(r'\s+,', ',', text)
    return text.strip()


def process_copyrights(statements, garbage):
    """Cleans, drops garbage statements, merges year ranges for identical holders, and sorts."""
    cleaned = []
    for raw in statements:
        stmt = normalize_statement(raw)
        if not stmt or stmt in garbage:
            continue
        cleaned.append(stmt)

    # Group statements that share the exact same (prefix, holder) so year ranges merge.
    by_holder = {}  # (prefix_lower, rest_lower) -> (display_prefix, display_rest, set_of_years)
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
        key = (prefix.strip().lower(), rest.strip().lower())
        if key not in by_holder:
            by_holder[key] = [prefix, rest, set()]
        else:
            # Prefer canonical casing like "Copyright (c)" when multiple forms exist.
            if '(c)' in prefix and '(c)' not in by_holder[key][0]:
                by_holder[key][0] = prefix
        by_holder[key][2].update(years)

    merged = set(unparsed)
    for prefix, rest, years in by_holder.values():
        merged.add(f'{prefix}{_format_years(years)}{rest}'.strip())

    # Remove statements that are pure prefix-less/year-less duplicates when a dated statement exists
    return sorted(merged)


def _parse_years(text):
    years = set()
    for part in text.split(','):
        part = part.strip()
        if '-' in part:
            lo, _, hi = part.partition('-')
            lo_i, hi_i = int(lo.strip()), int(hi.strip())
            if lo_i <= hi_i and hi_i - lo_i <= 100:
                years.update(range(lo_i, hi_i + 1))
            else:
                return set()
        else:
            years.add(int(part))
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
