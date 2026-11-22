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
GUIDE_REL = 'docs/licensing-guide.md'

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
        if pattern[i:i + 4] == '/**/':
            parts.append('(?:/|/.*/)')
            i += 4
        elif pattern[i:i + 3] == '**/':
            parts.append('(?:.*/)?')
            i += 3
        elif pattern[i:i + 3] == '/**':
            parts.append('/.*')
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


def _make_branch_rank(policy):
    default_pref = len(policy.same_category_preference)

    def branch_rank(lic_set):
        # Worst category among AND-ed licenses in this branch determines branch tier.
        # Unclassified licenses rank at 3.5 (worse than written-offer=3, better than forbidden=4)
        # so known permissive branches win cleanly, while unknown vs unknown still surfaces.
        worst_cat = max(
            3.5 if (cat := policy.category(str(lic))) is None else CATEGORY_ORDER[cat]
            for lic in lic_set
        )
        # Within the same category, compare preference worst-first so a branch with a less-preferred
        # license loses; shorter license sets win ties (e.g. MIT beats Apache-2.0 AND MIT).
        pref_ranks = tuple(sorted(
            (policy.same_category_preference.get(str(lic), default_pref) for lic in lic_set),
            reverse=True,
        ))
        return (worst_cat, pref_ranks, len(lic_set))

    return branch_rank


def _evaluate_license_node(node, label, branch_rank, policy, undecided, forbidden, unclassified, unlicensed):
    """Resolves OR choices on `node`, records policy errors, and returns sorted effective licenses."""
    resolved_lics, node_undecided = resolve_or(node, branch_rank)
    for expr_str in node_undecided:
        undecided.append((label, expr_str))
    effective_list = sorted(resolved_lics, key=str)
    if not effective_list:
        unlicensed.append(label)
        return []
    for lic in effective_list:
        cat = policy.category(str(lic))
        if cat is None:
            unclassified.append(f'{lic} ({label})')
        elif cat == 'forbidden':
            forbidden.append(f'{lic} ({label})')
    return effective_list


def _raise_license_errors(undecided, forbidden, unclassified, unlicensed):
    if undecided:
        raise stop(_undecided_message(undecided))
    if forbidden:
        raise stop(_forbidden_message(forbidden))
    if unclassified:
        raise stop(_unclassified_message(unclassified))
    if unlicensed:
        raise stop(_unlicensed_message(unlicensed))


def validate_pre_scan(packages, sources, policy, curations, license_files=()):
    """Runs fast pre-scan fail-closed checks (including license-file components) and returns declared ASTs."""
    unreviewed = []
    unmapped = []
    unscanned = []
    declared_by_purl = {}

    for purl in sorted(packages):
        pkg = packages[purl]
        ranges = curations.unreviewed_concluded(pkg.package_key, pkg.version)
        if ranges:
            unreviewed.append((pkg.purl, pkg.package_key, pkg.version, ranges))

        declared_node, pkg_unmapped = _resolve_declared(pkg, policy, curations)
        declared_by_purl[purl] = declared_node
        for name in pkg_unmapped:
            unmapped.append((pkg.purl, pkg.package_key, name))

        if sources.get(purl) is None:
            unscanned.append(pkg.purl)

    lf_unmapped, lf_undecided, lf_forbidden, lf_unclassified, lf_unlicensed = _check_license_files(license_files, policy)
    unmapped.extend(lf_unmapped)

    if unreviewed:
        raise stop(_unreviewed_message(unreviewed))
    if unmapped:
        raise stop(_unmapped_message(unmapped))
    if unscanned:
        raise stop(_unscanned_message(unscanned))
    _raise_license_errors(lf_undecided, lf_forbidden, lf_unclassified, lf_unlicensed)
    return declared_by_purl


def _check_license_files(license_files, policy):
    """Validates declared licenses of bundled license-file components (Chromium, Node.js, fonts, LwM2M)."""
    branch_rank = _make_branch_rank(policy)
    unmapped, undecided, forbidden, unclassified, unlicensed = [], [], [], [], []
    for ref in sorted(license_files, key=lambda r: r.sort_key):
        if not ref.declared_raw:
            # lwm2m-registry has its own XML-header vs application/pom.xml check in lwm2m.py;
            # other license-file components must declare their licenses in the SBOM/POM.
            if ref.generated != 'lwm2m-registry':
                unlicensed.append(ref.coordinates)
            continue
        parsed_items, ref_unmapped = _parse_declared_items(ref.declared_raw, ref.named_raw, {}, policy, ref.coordinates)
        for name in ref_unmapped:
            unmapped.append((ref.coordinates, ref.coordinates, name))
        if ref_unmapped:
            continue
        node = parsed_items[0] if len(parsed_items) == 1 else And(tuple(parsed_items))
        _evaluate_license_node(node, ref.coordinates, branch_rank, policy, undecided, forbidden, unclassified, unlicensed)
    return unmapped, undecided, forbidden, unclassified, unlicensed


def resolve_all(packages, sources, scan_records, policy, curations, declared_by_purl=None, license_files=()):
    """Validates and resolves every package against policy and curations.

    Fail-closed checks run in deterministic order:
      1. Unreviewed version bumps on concluded_license curations
      2. Unmapped non-SPDX declared license names (packages & license-file components)
      3. Unscanned packages (no source and not metadata_only)
      4. Undecided OR ties
      5. Forbidden licenses (checked AFTER OR resolution)
      6. Unclassified licenses
      7. Unlicensed packages
    """
    if declared_by_purl is None:
        declared_by_purl = validate_pre_scan(packages, sources, policy, curations, license_files=license_files)

    branch_rank = _make_branch_rank(policy)
    undecided = []
    forbidden = []
    unclassified = []
    unlicensed = []
    resolved = []

    for purl in sorted(packages):
        pkg = packages[purl]
        applying = curations.for_package(pkg.package_key, pkg.version)
        declared_node = declared_by_purl.get(purl)
        concluded_expr = next((c.concluded_license for c in applying if c.concluded_license), None)
        ignores = [ig for c in applying for ig in c.ignore_findings]
        source = sources.get(purl) or {}
        if source.get('type') == 'metadata_only':
            record = scan_records.get(purl) or {'findings': []}
        else:
            record = scan_records.get(purl)
            if record is None:
                raise stop(f'missing scan record for {pkg.purl} (expected scan before policy resolution)')

        declared_or_atoms = or_branch_atoms(declared_node) if declared_node else set()
        detected_nodes, scan_copyrights = _collect_findings(
            record, policy.exclude_paths, ignores, declared_or_atoms, skip_licenses=bool(concluded_expr)
        )
        raw_copyrights = list(pkg.copyrights) + scan_copyrights

        if concluded_expr:
            effective_node = _parse_config_spdx(concluded_expr, f'concluded_license for {pkg.purl}')
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

        effective_list = _evaluate_license_node(
            effective_node, pkg.purl, branch_rank, policy, undecided, forbidden, unclassified, unlicensed
        )
        if not effective_list:
            continue

        listed_lics = tuple(lic for lic in effective_list if policy.category(str(lic)) != 'nothing')
        cleaned_copyrights = tuple(process_copyrights(raw_copyrights, policy.copyright_garbage))
        source_url = source.get('url', '') if source.get('type') in ('artifact', 'vcs') else ''

        resolved.append(ResolvedPackage(
            purl=pkg.purl,
            coordinates=pkg.coordinates,
            licenses=listed_lics,
            copyrights=cleaned_copyrights,
            source_url=source_url,
        ))

    _raise_license_errors(undecided, forbidden, unclassified, unlicensed)

    return resolved


def _collect_findings(record, exclude_paths, ignores, declared_or_atoms, skip_licenses):
    """Collects non-excluded copyrights and deduplicated, filtered detected license ASTs from a scan record."""
    detected_nodes = []
    seen_nodes = set()
    raw_copyrights = []
    for finding in record.get('findings', []):
        fpath = finding.get('path', '')
        if is_excluded_path(fpath, exclude_paths):
            continue
        raw_copyrights.extend(finding.get('copyrights', ()))
        if skip_licenses:
            continue
        is_root_lic = ROOT_LICENSE_FILE_RE.match(fpath.removeprefix('package/')) is not None
        for raw_expr in finding.get('licenses', ()):
            try:
                det_node = parse_spdx(raw_expr)
            except SpdxSyntaxError:
                det_node = License(raw_expr)
            filtered = _filter_ignored(det_node, fpath, ignores)
            if filtered is None:
                continue
            # Dual-license root LICENSE* rule: if declared has OR and all detected leaves in this
            # root LICENSE/COPYING file are branches of that declared OR, don't turn OR into AND.
            if is_root_lic and declared_or_atoms:
                det_leaves = [l for l in leaves(filtered) if not is_no_license(l)]
                if det_leaves and all(str(l) in declared_or_atoms for l in det_leaves):
                    continue
            if filtered not in seen_nodes:
                seen_nodes.add(filtered)
                detected_nodes.append(filtered)
    return detected_nodes, raw_copyrights


def _parse_config_spdx(expr, context):
    try:
        return parse_spdx(expr)
    except SpdxSyntaxError as err:
        raise stop(f'invalid SPDX expression {expr!r} in {context}: {err}')


def _parse_declared_items(declared_raw, named_raw, pkg_map, policy, context_id):
    parsed_items = []
    unmapped = []
    for raw in declared_raw:
        mapped = pkg_map.get(raw)
        if mapped is None and raw in named_raw:
            mapped = policy.license_aliases.get(raw)
        if mapped is not None:
            parsed_items.append(_parse_config_spdx(mapped, f'license mapping for {context_id} ({raw!r})'))
            continue
        if raw in named_raw:
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
            # Some SBOM entries put free text in expression; check global aliases before failing.
            fallback = policy.license_aliases.get(raw)
            if fallback:
                parsed_items.append(_parse_config_spdx(fallback, f'license_aliases for {raw!r}'))
            else:
                unmapped.append(raw)
    return parsed_items, unmapped


def _resolve_declared(pkg, policy, curations):
    """Returns (AST_or_None, list_of_unmapped_names)."""
    applying = curations.for_package(pkg.package_key, pkg.version)
    override = next((c.declared_license for c in applying if c.declared_license), None)
    if override:
        return _parse_config_spdx(override, f'declared_license for {pkg.purl}'), []

    pkg_map = {}
    for c in applying:
        pkg_map.update(c.declared_license_map)

    parsed_items, unmapped = _parse_declared_items(pkg.declared_raw, pkg.named_raw, pkg_map, policy, pkg.purl)
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
            if str(n) in ignored_ids or is_no_license(n):
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
    _, pkg_key, version, _ = packages[0]
    lines = ''.join(f'  {p}   (checked: {", ".join(r)})\n' for p, _, _, r in packages)
    return (
        'we set the license of these packages by hand, but only for older versions.\n'
        + lines
        + 'A license can change between versions, so it must not carry over to a version nobody has read.\n'
        f'To fix: read the license of the new version, then in {CURATIONS_REL} either:\n'
        f'  - widen the range of the entry, if unchanged: id = "{pkg_key}@[<from>,<to>[" '
        f'(covering {version}), or\n'
        '  - add a new [[curations]] entry for this version with a comment saying where you checked.\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
    )


def _unmapped_message(packages):
    _, pkg_key, name = packages[0]
    lines = ''.join(f'  {p}: {n!r}\n' for p, _, n in packages)
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
        f'      comment = "<where you checked>"\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
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
        '  # or, if no source is published: metadata_only = true\n'
        f'See {GUIDE_REL}#when-the-generator-stops.'
    )


def _undecided_message(items):
    lines = ''.join(f'  {p}: {e}\n' for p, e in sorted(set(items)))
    return (
        'these packages offer a license choice between equally ranked licenses.\n'
        + lines
        + f'To fix: add the preferred license to `same_category_preference` in {POLICY_REL}.\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
    )


def _forbidden_message(items):
    lines = ''.join(f'  {item}\n' for item in sorted(set(items)))
    return (
        'these packages are under licenses that ThingsBoard cannot ship.\n'
        + lines
        + 'Strong copyleft would put ThingsBoard itself under that license, and others forbid commercial use.\n'
        + 'Remove or replace the dependency. Only if the scan misread a specific file, add a file-scoped\n'
        f'`ignore_findings` entry to {CURATIONS_REL} with a comment explaining why (CODEOWNERS approval required).\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
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
        '  forbidden      cannot ship (strong copyleft, non-commercial)\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
    )


def _unlicensed_message(purls):
    lines = ''.join(f'  {p}\n' for p in purls)
    return (
        'no license was found for these packages, so they would be left out of the notices silently.\n'
        + lines
        + f'To fix: verify the license from its repository/package page and add a version-ranged\n'
        f'`concluded_license` entry to {CURATIONS_REL} (or declare <license> in pom.xml for bundled components).\n'
        f'See {GUIDE_REL}#rules-for-license-decisions.'
    )
