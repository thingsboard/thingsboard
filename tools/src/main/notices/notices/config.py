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

POLICY_PATH = os.path.join(NOTICES_DIR, 'policy.toml')
CURATIONS_PATH = os.path.join(NOTICES_DIR, 'curations.toml')

CATEGORY_ORDER = {
    'nothing': 0,
    'notice': 1,
    'source-link': 2,
    'written-offer': 3,
    'forbidden': 4,
}


@dataclass(frozen=True)
class Policy:
    own_modules: frozenset[str]
    exclude_paths: tuple[str, ...]
    same_category_preference: dict[str, int]
    license_category: dict[str, str]
    license_aliases: dict[str, str]
    license_titles: dict[str, str]
    license_files: dict[str, str]
    copyright_garbage: frozenset[str]

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
    package_key: str  # purl without @version (e.g. pkg:maven/group/name or pkg:npm/name)
    version_spec: str  # '' (all versions), exact version, or Maven range [a,b[
    comment: str = ''
    metadata_only: bool = False
    vcs: dict | None = None
    source_artifact: dict | None = None
    declared_license: str | None = None
    declared_license_map: dict[str, str] = field(default_factory=dict)
    concluded_license: str | None = None
    ignore_findings: tuple[IgnoreFinding, ...] = ()


@dataclass(frozen=True)
class Curations:
    entries: tuple[Curation, ...]
    custom_licenses: dict[str, str]

    def for_package(self, purl_no_version, version=None):
        """All curations for a package, optionally filtered to those covering `version`."""
        matched = [c for c in self.entries if c.package_key == purl_no_version]
        if version is None:
            return matched
        return [c for c in matched if not c.version_spec or covers(c.version_spec, version)]

    def unreviewed_concluded(self, purl_no_version, version):
        """Version specs of concluded_license curations when none of them covers `version`."""
        all_concluded = [c.version_spec for c in self.for_package(purl_no_version) if c.concluded_license]
        if not all_concluded:
            return []
        covering = [c for c in self.for_package(purl_no_version, version) if c.concluded_license]
        return [] if covering else all_concluded


def load_policy(path=POLICY_PATH):
    with open(path, 'rb') as file:
        data = tomllib.load(file)
    license_category = {}
    for category, ids in data.get('licenses', {}).items():
        if category not in CATEGORY_ORDER:
            raise stop(f'unknown license category {category!r} in {path}')
        for lic in ids:
            if lic in license_category:
                raise stop(f'license {lic!r} is listed under multiple categories in {path}')
            license_category[lic] = category
    prefs = {lic: i for i, lic in enumerate(data.get('same_category_preference', []))}
    return Policy(
        own_modules=frozenset(data.get('own_modules', [])),
        exclude_paths=tuple(data.get('exclude_paths', [])),
        same_category_preference=prefs,
        license_category=license_category,
        license_aliases=dict(data.get('license_aliases', {})),
        license_titles=dict(data.get('license_titles', {})),
        license_files=dict(data.get('license_files', {})),
        copyright_garbage=frozenset(data.get('copyright_garbage', [])),
    )


def load_curations(path=CURATIONS_PATH):
    with open(path, 'rb') as file:
        data = tomllib.load(file)
    entries = []
    for raw in data.get('curations', []):
        raw_id = raw['id']
        pkg_key, version_spec = split_curation_id(raw_id)
        if raw.get('concluded_license') and not version_spec:
            raise stop(
                f'curation {raw_id!r} in {path} sets concluded_license without a version or version range. '
                'A concluded license must cover only reviewed versions (for example @[1.0,2.0[).'
            )
        ignores = tuple(
            IgnoreFinding(
                path=item['path'],
                licenses=frozenset(item['licenses']),
                comment=item.get('comment', ''),
            )
            for item in raw.get('ignore_findings', [])
        )
        entries.append(Curation(
            raw_id=raw_id,
            package_key=pkg_key,
            version_spec=version_spec,
            comment=raw.get('comment', ''),
            metadata_only=bool(raw.get('metadata_only', False)),
            vcs=raw.get('vcs'),
            source_artifact=raw.get('source_artifact'),
            declared_license=raw.get('declared_license'),
            declared_license_map=dict(raw.get('declared_license_map', {})),
            concluded_license=raw.get('concluded_license'),
            ignore_findings=ignores,
        ))
    custom_licenses = {k: v.strip('\n') for k, v in data.get('custom_licenses', {}).items()}
    return Curations(entries=tuple(entries), custom_licenses=custom_licenses)


def split_curation_id(raw_id):
    """Splits a PURL curation id into (package_without_version, version_spec).

    Examples:
      'pkg:maven/org.jfree/jfreechart'               -> ('pkg:maven/org.jfree/jfreechart', '')
      'pkg:maven/com.sun.winsw/winsw@2.0.1'          -> ('pkg:maven/com.sun.winsw/winsw', '2.0.1')
      'pkg:npm/@mdi/svg@[7.0,8.0['                   -> ('pkg:npm/@mdi/svg', '[7.0,8.0[')
      'pkg:npm/%40mdi/svg@[7.0,8.0['                 -> ('pkg:npm/@mdi/svg', '[7.0,8.0[')
    """
    # Normalize %40 in npm scoped PURLs to @ for consistent matching, then split on the version '@'.
    normalized = raw_id.replace('pkg:npm/%40', 'pkg:npm/@')
    if normalized.startswith('pkg:npm/@'):
        # First '@' is the npm scope; second '@' (if any) starts the version.
        scope_end = normalized.index('/', len('pkg:npm/@'))
        pkg_part, _, version = normalized[scope_end:].partition('@')
        return normalized[:scope_end] + pkg_part, version
    pkg, _, version = normalized.partition('@')
    return pkg, version


def covers(versions, version):
    """Whether `version` matches an exact version or a Maven-style interval like [1.0,2.0[."""
    limits = re.fullmatch(r'([\[\]])([^,]*),([^,]*)([\[\]])', versions)
    if not limits:
        return versions == version
    opening, lowest, highest, closing = limits.groups()
    parts = [_numbers(text) for text in (version, lowest, highest)]
    width = max(map(len, parts))
    number, low, high = (part + [0] * (width - len(part)) for part in parts)
    return ((not lowest or number > low or (number == low and opening == '['))
            and (not highest or number < high or (number == high and closing == ']')))


def _numbers(version):
    return [int(n) for n in re.match(r'[0-9.]*', version).group().split('.') if n]
