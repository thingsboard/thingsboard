#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Loads policy.toml and curations.toml and matches Maven/npm version ranges."""
from dataclasses import dataclass, field
import os
import re
import tomllib

from notices import NOTICES_DIR, stop
from notices.spdx import SpdxSyntaxError, parse as parse_spdx

POLICY_PATH = os.path.join(NOTICES_DIR, 'policy.toml')
CURATIONS_PATH = os.path.join(NOTICES_DIR, 'curations.toml')

_POLICY_KEYS = frozenset({
    'own_modules', 'exclude_paths', 'source_tree_exclude_paths', 'same_category_preference',
    'copyright_garbage', 'licenses', 'license_aliases', 'license_titles', 'license_files',
})
_CURATIONS_TOP_KEYS = frozenset({'curations', 'custom_licenses'})
_CURATION_KEYS = frozenset({
    'id', 'ids', 'comment', 'metadata_only', 'vcs',
    'declared_license', 'declared_license_map', 'concluded_license', 'ignore_findings',
})
_IGNORE_KEYS = frozenset({'path', 'licenses', 'comment'})
_VCS_KEYS = frozenset({'type', 'url', 'revision', 'path'})

CATEGORY_ORDER = {
    'nothing': 0,
    'notice': 1,
    'source-link': 2,
    'written-offer': 3,
    'forbidden': 4,
}
RANGE_RE = re.compile(r'([\[\]])([^,]*),([^,]*)([\[\]])')


@dataclass(frozen=True)
class Policy:
    own_modules: frozenset[str] = frozenset()
    exclude_paths: tuple[str, ...] = ()
    source_tree_exclude_paths: tuple[str, ...] = ()
    same_category_preference: dict[str, int] = field(default_factory=dict)
    license_category: dict[str, str] = field(default_factory=dict)
    license_aliases: dict[str, str] = field(default_factory=dict)
    license_titles: dict[str, str] = field(default_factory=dict)
    license_files: dict[str, str] = field(default_factory=dict)
    copyright_garbage: frozenset[str] = frozenset()

    def category(self, license_id):
        return self.license_category.get(license_id)

    def title(self, license_id):
        return self.license_titles.get(license_id, license_id)


@dataclass(frozen=True)
class IgnoreFinding:
    path: str
    licenses: frozenset[str]
    comment: str = ''


@dataclass(frozen=True)
class Curation:
    raw_id: str
    package_key: str  # purl without @version (e.g. pkg:maven/group/name or pkg:npm/@scope/name)
    version_spec: str  # '' (all versions), exact version, or interval [a,b[
    comment: str = ''
    metadata_only: bool = False
    vcs: dict | None = None
    declared_license: str | None = None
    declared_license_map: dict[str, str] = field(default_factory=dict)
    concluded_license: str | None = None
    ignore_findings: tuple[IgnoreFinding, ...] = ()


@dataclass(frozen=True)
class Curations:
    entries: tuple[Curation, ...] = ()
    custom_licenses: dict[str, str] = field(default_factory=dict)
    _by_package: dict[str, tuple[Curation, ...]] = field(init=False, repr=False, compare=False)

    def __post_init__(self):
        grouped = {}
        for entry in self.entries:
            grouped.setdefault(entry.package_key, []).append(entry)
        object.__setattr__(self, '_by_package', {k: tuple(v) for k, v in grouped.items()})

    def for_package(self, purl_no_version, version=None):
        """All curations for a package, optionally filtered to those covering `version`."""
        matched = self._by_package.get(purl_no_version, ())
        if version is None:
            return matched
        return tuple(c for c in matched if not c.version_spec or covers(c.version_spec, version))

    def unreviewed_concluded(self, purl_no_version, version):
        """Version specs of concluded_license curations when none of them covers `version`."""
        specs = [c.version_spec for c in self._by_package.get(purl_no_version, ()) if c.concluded_license]
        return [] if any(covers(spec, version) for spec in specs) else specs

    def unused(self, packages, used_licenses=None):
        """Returns sorted unused curation raw_ids and unused custom_licenses names."""
        matched = {
            c.raw_id
            for pkg in packages
            for c in self.for_package(pkg.package_key, pkg.version)
        }
        unused_curations = sorted({c.raw_id for c in self.entries} - matched)
        unused_custom = sorted(self.custom_licenses.keys() - used_licenses) if used_licenses is not None else []
        return unused_curations, unused_custom

    def unused_ignores(self, packages, matched_ignores):
        """Returns sorted ignore_findings rules on shipped packages that suppressed zero findings."""
        return sorted({
            f'{c.raw_id} ignore_findings[{ig.path!r}]'
            for pkg in packages
            for c in self.for_package(pkg.package_key, pkg.version)
            for ig in c.ignore_findings
            if (c.raw_id, ig) not in matched_ignores
        })


def _load_toml(path):
    try:
        with open(path, 'rb') as file:
            return tomllib.load(file)
    except (OSError, tomllib.TOMLDecodeError) as err:
        raise stop(f'could not read {path}: {err}')


def _reject_unknown_keys(mapping, allowed, context, path):
    unknown = sorted(set(mapping) - allowed)
    if unknown:
        raise stop(f'unknown key(s) {", ".join(repr(k) for k in unknown)} in {context} ({path})')


def _validate_spdx(expr, context, path):
    try:
        parse_spdx(expr)
    except SpdxSyntaxError as err:
        raise stop(f'invalid SPDX expression {expr!r} in {context} ({path}): {err}')


def load_policy(path=POLICY_PATH):
    data = _load_toml(path)
    _reject_unknown_keys(data, _POLICY_KEYS, 'top-level policy', path)
    license_category = {}
    for category, ids in data.get('licenses', {}).items():
        if category not in CATEGORY_ORDER:
            raise stop(f'unknown license category {category!r} in {path}')
        for lic in ids:
            if lic in license_category:
                raise stop(f'license {lic!r} is listed under multiple categories in {path}')
            license_category[lic] = category
    license_aliases = dict(data.get('license_aliases', {}))
    for alias, expr in license_aliases.items():
        if not isinstance(expr, str):
            raise stop(f'license_aliases[{alias!r}] in {path} must be a string')
        _validate_spdx(expr, f'license_aliases[{alias!r}]', path)
    license_files = dict(data.get('license_files', {}))
    for key, val in license_files.items():
        if not isinstance(val, str):
            raise stop(f'license_files[{key!r}] in {path} must be a string path (got {type(val).__name__})')
    prefs = {lic: i for i, lic in enumerate(data.get('same_category_preference', []))}
    return Policy(
        own_modules=frozenset(data.get('own_modules', [])),
        exclude_paths=tuple(data.get('exclude_paths', [])),
        source_tree_exclude_paths=tuple(data.get('source_tree_exclude_paths', [])),
        same_category_preference=prefs,
        license_category=license_category,
        license_aliases=license_aliases,
        license_titles=dict(data.get('license_titles', {})),
        license_files=license_files,
        copyright_garbage=frozenset(data.get('copyright_garbage', [])),
    )


def load_curations(path=CURATIONS_PATH):
    data = _load_toml(path)
    _reject_unknown_keys(data, _CURATIONS_TOP_KEYS, 'top-level curations', path)
    entries = []
    for raw in data.get('curations', []):
        if 'id' in raw and 'ids' in raw:
            raise stop(f'curation entry in {path} cannot specify both "id" and "ids"')
        if 'ids' in raw:
            raw_ids = raw['ids']
            if not isinstance(raw_ids, list) or not raw_ids or not all(isinstance(x, str) and x.strip() for x in raw_ids):
                raise stop(f'curation "ids" in {path} must be a non-empty list of non-empty strings')
        elif 'id' in raw:
            if not isinstance(raw['id'], str) or not raw['id'].strip():
                raise stop(f'curation "id" in {path} must be a non-empty string')
            raw_ids = [raw['id']]
        else:
            raise stop(f'curation entry in {path} is missing "id" (or "ids")')
        label = f'curation {raw_ids[0]!r}'
        _reject_unknown_keys(raw, _CURATION_KEYS, label, path)
        vcs = raw.get('vcs')
        if vcs is not None:
            if not isinstance(vcs, dict):
                raise stop(f'vcs in {label} ({path}) must be a table')
            _reject_unknown_keys(vcs, _VCS_KEYS, f'vcs of {label}', path)
            for req in ('url', 'revision'):
                if not isinstance(vcs.get(req), str) or not vcs[req].strip():
                    raise stop(f'vcs in {label} ({path}) is missing non-empty string {req!r}')
        if raw.get('declared_license'):
            _validate_spdx(raw['declared_license'], f'declared_license of {label}', path)
        if raw.get('concluded_license'):
            _validate_spdx(raw['concluded_license'], f'concluded_license of {label}', path)
        for name, expr in raw.get('declared_license_map', {}).items():
            _validate_spdx(expr, f'declared_license_map[{name!r}] of {label}', path)
        try:
            ignores = []
            for item in raw.get('ignore_findings', []):
                _reject_unknown_keys(item, _IGNORE_KEYS, f'ignore_findings of {label}', path)
                ignores.append(IgnoreFinding(
                    path=item['path'],
                    licenses=frozenset(item['licenses']),
                    comment=item.get('comment', ''),
                ))
            ignores = tuple(ignores)
        except KeyError as err:
            raise stop(f'ignore_findings entry in {path} for {raw_ids[0]!r} is missing {err}')

        for raw_id in raw_ids:
            pkg_key, version_spec = split_curation_id(raw_id)
            _validate_version_spec(version_spec, raw_id, path, require_upper=bool(raw.get('concluded_license')))
            if raw.get('concluded_license') and not version_spec:
                raise stop(
                    f'curation {raw_id!r} in {path} sets concluded_license without a version or version range. '
                    'A concluded license must cover only reviewed versions (for example @[1.0,2.0[).'
                )
            entries.append(Curation(
                raw_id=raw_id,
                package_key=pkg_key,
                version_spec=version_spec,
                comment=raw.get('comment', ''),
                metadata_only=bool(raw.get('metadata_only', False)),
                vcs=vcs,
                declared_license=raw.get('declared_license'),
                declared_license_map=dict(raw.get('declared_license_map', {})),
                concluded_license=raw.get('concluded_license'),
                ignore_findings=ignores,
            ))
    custom_licenses = {k: v.strip('\n') for k, v in data.get('custom_licenses', {}).items()}
    return Curations(entries=tuple(entries), custom_licenses=custom_licenses)


NUMERIC_BOUND_RE = re.compile(r'^\d+(?:\.\d+)*$')


def _validate_version_spec(version_spec, raw_id, path, require_upper=False):
    if not version_spec:
        return
    if version_spec[0] in '[]' or version_spec[-1] in '[]':
        limits = RANGE_RE.fullmatch(version_spec)
        if not limits:
            raise stop(f'invalid version range {version_spec!r} in {raw_id!r} ({path}); expected e.g. [1.0,2.0[')
        _, lowest, highest, _ = limits.groups()
        if not lowest and not highest:
            raise stop(f'empty version range {version_spec!r} in {raw_id!r} ({path})')
        if require_upper and not highest:
            raise stop(f'unbounded version range {version_spec!r} in {raw_id!r} ({path}); concluded_license requires an upper bound')
        if (lowest and not NUMERIC_BOUND_RE.fullmatch(lowest)) or (highest and not NUMERIC_BOUND_RE.fullmatch(highest)):
            raise stop(f'non-numeric bound in version range {version_spec!r} in {raw_id!r} ({path}); expected dot-separated digits e.g. 1.0')


def normalize_purl_scope(purl):
    """Normalizes %40 in scoped npm PURLs to @ for consistent matching."""
    return purl.replace('pkg:npm/%40', 'pkg:npm/@')


def split_curation_id(raw_id):
    """Splits a PURL (without qualifiers) into (package_without_version, version_spec).

    Examples:
      'pkg:maven/org.jfree/jfreechart'               -> ('pkg:maven/org.jfree/jfreechart', '')
      'pkg:maven/com.sun.winsw/winsw@2.0.1'          -> ('pkg:maven/com.sun.winsw/winsw', '2.0.1')
      'pkg:npm/@mdi/svg@[7.0,8.0['                   -> ('pkg:npm/@mdi/svg', '[7.0,8.0[')
      'pkg:npm/%40mdi/svg@[7.0,8.0['                 -> ('pkg:npm/@mdi/svg', '[7.0,8.0[')
    """
    normalized = normalize_purl_scope(raw_id)
    if normalized.startswith('pkg:npm/@'):
        slash = normalized.find('/', len('pkg:npm/@'))
        if slash < 0:
            raise stop(f'malformed scoped npm PURL {raw_id!r} (expected pkg:npm/@scope/name)')
        pkg_part, _, version = normalized[slash:].partition('@')
        return normalized[:slash] + pkg_part, version
    pkg, _, version = normalized.partition('@')
    return pkg, version


def covers(versions, version):
    """Whether `version` matches an exact version or an interval like [1.0,2.0[."""
    limits = RANGE_RE.fullmatch(versions)
    if not limits:
        return versions == version
    opening, lowest, highest, closing = limits.groups()
    num = _numbers(version)
    if not num:
        return False
    low_n = _numbers(lowest) if lowest else []
    high_n = _numbers(highest) if highest else []
    if (lowest and not low_n) or (highest and not high_n):
        return False
    width = max(len(num), len(low_n), len(high_n))
    number = num + [0] * (width - len(num))
    low = low_n + [0] * (width - len(low_n))
    high = high_n + [0] * (width - len(high_n))
    return ((not lowest or number > low or (number == low and opening == '['))
            and (not highest or number < high or (number == high and closing == ']')))


def _numbers(version):
    return [int(n) for n in re.match(r'[0-9.]*', version).group().split('.') if n]
