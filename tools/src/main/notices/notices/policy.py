#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""License resolution and fail-closed compliance policy engine."""
from dataclasses import dataclass
from functools import lru_cache
import re

from notices import stop
from notices.config import CATEGORY_ORDER
from notices.copyrights import process_copyrights
from notices.spdx import (
    And,
    License,
    Or,
    SpdxSyntaxError,
    is_no_license,
    leaves,
    or_branch_atoms,
    parse as parse_spdx,
    resolve_or,
)

POLICY_REL = 'tools/src/main/notices/policy.toml'
CURATIONS_REL = 'tools/src/main/notices/curations.toml'

# Root or META-INF license documentation files (never source files!).
ROOT_LICENSE_FILE_RE = re.compile(r'^(?:META-INF/)?(?:LICENSE|LICENCE|COPYING)(?:\.[^/]+)?$', re.IGNORECASE)


@dataclass(frozen=True)
class ResolvedPackage:
    purl: str
    coordinates: str
    licenses: tuple[License, ...]       # effective non-'nothing' licenses
    copyrights: tuple[str, ...]         # cleaned & merged copyright statements
    source_url: str                     # artifact or VCS URL for source-link licenses


def glob_match(path, pattern):
    """Matches a forward-slash relative path against a glob pattern supporting '**/'."""
    return _compile_glob(pattern).match(path) is not None


@lru_cache(maxsize=512)
def _compile_glob(pattern):
    parts = []
    i, n = 0, len(pattern)
    while i < n:
        if pattern[i:i + 3] == '**/':
            parts.append('(?:.*/)?')
            i += 3
        elif pattern[i:i + 3] == '/**':
            parts.append('(?:/.*)?')
            i += 3
        elif pattern[i:i + 2] == '**':
            parts.append('.*')
            i += 2
        elif pattern[i] == '*':
            parts.append('[^/]*')
            i += 1
        elif pattern[i] == '?':
            parts.append('[^/]')
            i += 1
        else:
            parts.append(re.escape(pattern[i]))
            i += 1
    return re.compile('^' + ''.join(parts) + '$')


def is_excluded_path(path, exclude_paths):
    return any(glob_match(path, pattern) for pattern in exclude_paths)


def resolve_all(packages, sources, scan_records, policy, curations):
    """Validates and resolves every package against policy and curations.

    Fail-closed checks run in deterministic order:
      1. Unreviewed version bumps on concluded_license curations
      2. Unmapped non-SPDX declared license names
      3. Unscanned packages (no source and not metadata_only)
      4. Undecided OR ties
      5. Forbidden licenses (checked AFTER OR resolution)
      6. Unclassified licenses
      7. Unlicensed packages
    """
    unreviewed = []
    unmapped = []
    unscanned = []
    undecided = []
    forbidden = []
    unclassified = []
    unlicensed = []
    resolved = []

    for purl in sorted(packages):
        pkg = packages[purl]
        # 1. Check unreviewed concluded_license curations
        ranges = curations.unreviewed_concluded(pkg.package_key, pkg.version)
        if ranges:
            unreviewed.append((pkg.purl, ranges))

        # 2. Resolve declared licenses (apply curations + global aliases; catch unmapped names)
        declared_node, pkg_unmapped = _resolve_declared(pkg, policy, curations)
        for name in pkg_unmapped:
            unmapped.append((pkg.purl, name))

        # 3. Check source availability
        source = sources.get(purl)
        if source is None:
            unscanned.append(pkg.purl)

    # Stop early on pre-resolution issues (these don't depend on scan findings)
    if unreviewed:
        raise stop(_unreviewed_message(unreviewed))
    if unmapped:
        raise stop(_unmapped_message(unmapped))
    if unscanned:
        raise stop(_unscanned_message(unscanned))

    # Build OR branch ranking function: lower rank tuple is better.
    def branch_rank(lic_set):
        # Worst category among AND-ed licenses in this branch determines branch tier.
        worst_cat = max(
            CATEGORY_ORDER.get(policy.category(str(lic)), -1 if policy.category(str(lic)) is None else 99)
            for lic in lic_set
        )
        # If any license in this branch is unclassified, rank it worse than written-offer (but above forbidden)
        # so known permissive branches win cleanly, while unknown vs unknown still surfaces clearly.
        unknown_penalty = 1 if any(policy.category(str(lic)) is None for lic in lic_set) else 0
        # Within the same category, use same_category_preference; unranked licenses tie at len(prefs).
        pref_ranks = tuple(sorted(
            policy.same_category_preference.get(str(lic), len(policy.same_category_preference))
            for lic in lic_set
        ))
        return (worst_cat if worst_cat >= 0 else 3.5, unknown_penalty, pref_ranks)

    for purl in sorted(packages):
        pkg = packages[purl]
        applying = curations.for_package(pkg.package_key, pkg.version)
        declared_node, _ = _resolve_declared(pkg, policy, curations)
        concluded_expr = next((c.concluded_license for c in applying if c.concluded_license), None)
        ignores = [ig for c in applying for ig in c.ignore_findings]
        record = scan_records.get(purl) or {'findings': []}

        # Collect copyrights & detected licenses from non-excluded paths
        declared_or_atoms = or_branch_atoms(declared_node) if declared_node else set()
        detected_nodes = []
        raw_copyrights = []

        for finding in record.get('findings', []):
            fpath = finding.get('path', '')
            if is_excluded_path(fpath, policy.exclude_paths):
                continue
            raw_copyrights.extend(finding.get('copyrights', ()))
            if concluded_expr:
                continue  # concluded_license sets aside detected licenses (copyrights are still kept!)
            is_root_lic = ROOT_LICENSE_FILE_RE.match(fpath.removeprefix('package/')) is not None
            for raw_expr in finding.get('licenses', ()):
                try:
                    det_node = parse_spdx(raw_expr)
                except SpdxSyntaxError:
                    det_node = License(raw_expr)
                # Strip ignored findings for this path
                filtered = _filter_ignored(det_node, fpath, ignores)
                if filtered is None:
                    continue
                # Dual-license root LICENSE* rule: if declared has OR and all detected leaves in this
                # root LICENSE/COPYING file are branches of that declared OR, don't turn OR into AND.
                if is_root_lic and declared_or_atoms:
                    det_leaves = [l for l in leaves(filtered) if not is_no_license(l)]
                    if det_leaves and all(str(l) in declared_or_atoms or l.id in declared_or_atoms for l in det_leaves):
                        continue
                detected_nodes.append(filtered)

        # Combine effective expression: concluded OR (declared AND detected)
        if concluded_expr:
            effective_node = parse_spdx(concluded_expr)
        else:
            terms = ([declared_node] if declared_node else []) + detected_nodes
            if not terms:
                effective_node = None
            elif len(terms) == 1:
                effective_node = terms[0]
            else:
                effective_node = And(tuple(terms))

        if effective_node is None:
            unlicensed.append(pkg.purl)
            continue

        resolved_lics, pkg_undecided = resolve_or(effective_node, branch_rank)
        for expr_str in pkg_undecided:
            undecided.append((pkg.purl, expr_str))

        effective_list = sorted(resolved_lics, key=str)
        if not effective_list:
            unlicensed.append(pkg.purl)
            continue

        for lic in effective_list:
            cat = policy.category(str(lic))
            if cat is None:
                unclassified.append(f'{lic} ({pkg.purl})')
            elif cat == 'forbidden':
                forbidden.append(f'{lic} ({pkg.purl})')

        listed_lics = tuple(lic for lic in effective_list if policy.category(str(lic)) != 'nothing')
        cleaned_copyrights = tuple(process_copyrights(raw_copyrights, policy.copyright_garbage))
        source = sources.get(purl) or {}
        source_url = source.get('url', '') if source.get('type') in ('artifact', 'vcs') else ''

        resolved.append(ResolvedPackage(
            purl=pkg.purl,
            coordinates=pkg.coordinates,
            licenses=listed_lics,
            copyrights=cleaned_copyrights,
            source_url=source_url,
        ))

    if undecided:
        raise stop(_undecided_message(undecided))
    if forbidden:
        raise stop(_forbidden_message(forbidden))
    if unclassified:
        raise stop(_unclassified_message(unclassified))
    if unlicensed:
        raise stop(_unlicensed_message(unlicensed))

    return resolved


def _resolve_declared(pkg, policy, curations):
    """Returns (AST_or_None, list_of_unmapped_names)."""
    applying = curations.for_package(pkg.package_key, pkg.version)
    override = next((c.declared_license for c in applying if c.declared_license), None)
    if override:
        return parse_spdx(override), []

    pkg_map = {}
    for c in applying:
        pkg_map.update(c.declared_license_map)

    parsed_items = []
    unmapped = []
    for raw in pkg.declared_raw:
        mapped = pkg_map.get(raw)
        if mapped is None and raw in pkg.named_raw:
            mapped = policy.license_aliases.get(raw)
        if mapped is not None:
            parsed_items.append(parse_spdx(mapped))
            continue
        if raw in pkg.named_raw:
            # Some POMs place a valid SPDX expression (e.g. "EPL-2.0 OR BSD-3-Clause") in <name>.
            if any(op in raw for op in (' OR ', ' AND ', ' WITH ')):
                try:
                    expr = parse_spdx(raw)
                    if all(policy.category(str(leaf)) is not None for leaf in leaves(expr)):
                        parsed_items.append(expr)
                        continue
                except SpdxSyntaxError:
                    pass
            unmapped.append(raw)
            continue
        try:
            parsed_items.append(parse_spdx(raw))
        except SpdxSyntaxError:
            # Some SBOM entries put free text in expression; check maps before failing
            fallback = pkg_map.get(raw) or policy.license_aliases.get(raw)
            if fallback:
                parsed_items.append(parse_spdx(fallback))
            else:
                unmapped.append(raw)

    if unmapped or not parsed_items:
        return None, unmapped
    if len(parsed_items) == 1:
        return parsed_items[0], []
    # Maven convention: multiple <license> tags in a POM represent a choice (OR).
    # For npm, multiple entries are AND.
    return (Or(tuple(parsed_items)) if pkg.kind == 'maven' else And(tuple(parsed_items))), []


def _filter_ignored(node, fpath, ignores):
    """Removes ignored licenses from a detected AST node for `fpath`; returns None if nothing remains."""
    ignored_ids = set()
    for ig in ignores:
        if glob_match(fpath, ig.path):
            ignored_ids.update(ig.licenses)
    if not ignored_ids:
        return node

    def strip(n):
        if isinstance(n, License):
            if str(n) in ignored_ids or n.id in ignored_ids or is_no_license(n):
                return None
            return n
        kept = [c for c in (strip(child) for child in n.terms) if c is not None]
        if not kept:
            return None
        if len(kept) == 1:
            return kept[0]
        return And(tuple(kept)) if isinstance(n, And) else Or(tuple(kept))

    return strip(node)


# ---------------------------------------------------------------------------
# Actionable fail-closed messages
# ---------------------------------------------------------------------------

def _unreviewed_message(packages):
    purl, _ = packages[0]
    pkg_key, _, version = purl.rpartition('@')
    lines = ''.join(f'  {p}   (checked: {", ".join(r)})\n' for p, r in packages)
    return (
        'we set the license of these packages by hand, but only for older versions.\n'
        + lines
        + 'A license can change between versions, so it must not carry over to a version nobody has read.\n'
        f'To fix: read the license of the new version, then in {CURATIONS_REL} either:\n'
        f'  - widen the range of the entry, if unchanged: id = "{pkg_key}@[<from>,<to>[" '
        f'(covering {version}), or\n'
        '  - add a new [[curations]] entry for this version with a comment saying where you checked.'
    )


def _unmapped_message(packages):
    purl, name = packages[0]
    pkg_key = purl.rpartition('@')[0]
    lines = ''.join(f'  {p}: {n!r}\n' for p, n in packages)
    return (
        'these packages name their license in words that are no standard SPDX identifier.\n'
        + lines
        + 'We do not guess what non-standard license names mean.\n'
        f'To fix: find the SPDX identifier (https://spdx.org/licenses), then either:\n'
        f'  - if the name is unambiguous everywhere, add it to [license_aliases] in {POLICY_REL}, or\n'
        f'  - add a [[curations]] entry in {CURATIONS_REL}:\n'
        f'      [[curations]]\n'
        f'      id = "{pkg_key}"\n'
        f'      declared_license_map = {{ "{name}" = "<SPDX-id>" }}\n'
        f'      comment = "<where you checked>"'
    )


def _unscanned_message(purls):
    lines = ''.join(f'  {p}\n' for p in purls)
    return (
        'the source of these packages cannot be found, so their licenses and copyrights cannot be read.\n'
        + lines
        + f'They have no sources jar, npm tarball, or VCS curation. To fix, add to {CURATIONS_REL} one of:\n'
        f'  [[curations]]\n'
        f'  id = "{purls[0]}"\n'
        '  vcs = { type = "Git", url = "<repository>", revision = "<commit>", path = "" }\n'
        '  comment = "<why this commit>"\n'
        '  # or, if no source is published: metadata_only = true'
    )


def _undecided_message(items):
    purl, expr = items[0]
    lines = ''.join(f'  {p}: {e}\n' for p, e in items)
    return (
        'these packages offer a license choice between equally ranked licenses.\n'
        + lines
        + f'To fix: add the preferred license to `same_category_preference` in {POLICY_REL}.'
    )


def _forbidden_message(items):
    lines = ''.join(f'  {item}\n' for item in sorted(set(items)))
    return (
        'these packages are under licenses that ThingsBoard cannot ship.\n'
        + lines
        + 'Strong copyleft would put ThingsBoard itself under that license, and others forbid commercial use.\n'
        + 'Remove or replace the dependency. Only if the scan misread a specific file, add a file-scoped\n'
        f'`ignore_findings` entry to {CURATIONS_REL} with a comment explaining why (CODEOWNERS approval required).'
    )


def _unclassified_message(items):
    lines = ''.join(f'  {item}\n' for item in sorted(set(items)))
    return (
        'these licenses are new to the product, and nobody has said what they require of us.\n'
        + lines
        + f'To fix: classify each license under [licenses] in {POLICY_REL} as one of:\n'
        '  notice         copyright lines and license text (MIT, BSD, Apache)\n'
        '  source-link    as notice, plus source URL (EPL, MPL, CDDL)\n'
        '  written-offer  as notice, plus written source offer (LGPL, GPL+Classpath)\n'
        '  nothing        no obligation (public domain)\n'
        '  forbidden      cannot ship (strong copyleft, non-commercial)'
    )


def _unlicensed_message(purls):
    lines = ''.join(f'  {p}\n' for p in purls)
    return (
        'no license was found for these packages, so they would be left out of the notices silently.\n'
        + lines
        + f'To fix: verify the license from its repository/package page and add a version-ranged\n'
        f'`concluded_license` entry to {CURATIONS_REL} (or raise it before merging if truly unlicensed).'
    )
