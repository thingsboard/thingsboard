#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Reads CycloneDX SBOMs and partitions components into Maven/npm packages, jars, and license-file components."""
from dataclasses import dataclass
import json
import os
import re
import urllib.parse

from notices import BUILD_PROBLEM, format_coordinates, stop
from notices.config import normalize_purl_scope, split_curation_id


@dataclass(frozen=True)
class PackageComponent:
    purl: str           # canonical purl without qualifiers/fragment (npm scope normalized to @)
    package_key: str    # purl without @version
    kind: str           # 'maven', 'npm', or 'declared' (self-contained SBOM component with license + copyright)
    group: str
    name: str
    version: str
    declared_raw: tuple[str, ...]   # declared license strings (SPDX or names)
    named_raw: frozenset[str]       # subset of declared_raw that came as non-SPDX names
    npm_tarball: dict | None        # {'url': ..., 'sha1': ...}
    vcs: dict | None                # {'type': ..., 'url': ..., 'revision': ..., 'path': ''}
    copyrights: tuple[str, ...] = ()

    @property
    def coordinates(self):
        return format_coordinates(self.group, self.name, self.version)


@dataclass(frozen=True)
class JarRef:
    purl: str
    group: str
    name: str
    version: str
    classifier: str
    sha256: str

    @property
    def coordinates(self):
        return format_coordinates(self.group, self.name, self.version)


@dataclass(frozen=True)
class LicenseFileRef:
    group: str
    name: str
    version: str
    url: str | None = None
    path: str | None = None
    platform: str | None = None
    generated: str | None = None  # e.g. 'lwm2m-registry'
    declared_raw: tuple[str, ...] = ()
    named_raw: frozenset[str] = frozenset()

    @property
    def coordinates(self):
        return format_coordinates(self.group, self.name, self.version)


@dataclass
class SbomInventory:
    packages: dict[str, PackageComponent]   # canonical purl -> PackageComponent
    jars: dict[str, JarRef]                 # full purl (with qualifiers) -> JarRef
    license_files: set[LicenseFileRef]


def discover_sboms(root_dir):
    """Finds all */target/sbom/*.cdx.json files under root_dir (excluding Syft raw scan_*.cdx.json)."""
    sboms = []
    for current, dirs, files in os.walk(root_dir):
        dirs[:] = sorted(d for d in dirs if d != 'node_modules' and d != '.git')
        parent, leaf = os.path.split(current)
        if leaf == 'sbom' and os.path.basename(parent) == 'target':
            for name in sorted(files):
                if name.endswith('.cdx.json') and not name.startswith('scan_'):
                    sboms.append(os.path.join(current, name))
    if not sboms:
        raise stop(
            f'no SBOMs in the target/sbom directories under {root_dir}. Build with them first: '
            'mvn clean package -DskipTests -Dsbom.skip=false\n'
            'See docs/licensing-guide.md#checking-the-notices-locally.'
        )
    return sboms


def load_sboms(sbom_paths, project_version, policy, check_version=False):
    """Loads one or more CycloneDX SBOM files in a single pass into a unified SbomInventory."""
    absent = [p for p in sbom_paths if not os.path.exists(p)]
    if absent:
        raise stop(
            'these SBOMs do not exist, and the notices are made from them:\n  '
            + '\n  '.join(absent)
            + f'\n{BUILD_PROBLEM} The Maven build makes them with -Dsbom.skip=false.'
        )

    packages, jars, license_files, wrong_version = {}, {}, set(), []
    for sbom_path in sbom_paths:
        try:
            with open(sbom_path, encoding='utf-8') as file:
                bom = json.load(file)
        except (OSError, json.JSONDecodeError) as err:
            raise stop(f'could not read SBOM {sbom_path}: {err}. {BUILD_PROBLEM}')

        if check_version:
            version = bom.get('metadata', {}).get('component', {}).get('version')
            if version != project_version:
                wrong_version.append(f'  {sbom_path} ({version})')
                continue

        for component in bom.get('components', []):
            purl = component.get('purl') or ''
            # Components found only by Syft in a Docker base image ship their own license texts there.
            if any(p.get('name', '').startswith('syft:location:') for p in component.get('properties', [])):
                continue

            if not is_maven(purl) and not is_npm(purl):
                ref = _license_file_ref(component, policy.license_files)
                if ref:
                    license_files.add(ref)
                else:
                    declared = _parse_declared_component(component)
                    if declared:
                        packages[declared.purl] = (
                            _merge_package(packages[declared.purl], declared)
                            if declared.purl in packages else declared
                        )
                continue

            canonical = canonical_purl(purl)
            if is_maven(purl) and _is_own(component, project_version, policy.own_modules):
                continue

            parsed = _parse_package(component, canonical)
            packages[canonical] = _merge_package(packages[canonical], parsed) if canonical in packages else parsed
            if is_jar(purl) and purl not in jars:
                jars[purl] = _parse_jar(component, purl)

    if wrong_version:
        raise stop(
            f'these SBOMs are not of the version {project_version}, so an older build left them. '
            'Build again from clean: mvn clean package -DskipTests -Dsbom.skip=false\n'
            + '\n'.join(wrong_version)
            + '\nSee docs/licensing-guide.md#checking-the-notices-locally.'
        )

    for pkg in packages.values():
        if pkg.npm_tarball and not pkg.npm_tarball.get('sha1'):
            raise stop(f"the SBOM gives no SHA-1 for {pkg.npm_tarball['url']}. {BUILD_PROBLEM}")

    return SbomInventory(packages=packages, jars=jars, license_files=license_files)


def canonical_purl(purl):
    """Strips qualifiers/fragments and normalizes npm %40 scope encoding."""
    return normalize_purl_scope(re.sub(r'[?#].*', '', purl))


def qualifiers(purl):
    return dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(purl).query))


def is_maven(purl):
    return purl.startswith('pkg:maven/')


def is_npm(purl):
    return purl.startswith('pkg:npm/')


def is_jar(purl):
    return is_maven(purl) and qualifiers(purl).get('type', 'jar') == 'jar'


def _is_own(component, project_version, own_modules):
    group, name, version = component.get('group', ''), component.get('name', ''), component.get('version')
    if f'{group}/{name}' in own_modules:
        return True
    return (group == 'org.thingsboard' or group.startswith('org.thingsboard.')) and version == project_version


def _parse_package(component, canonical):
    pkg_key, _ = split_curation_id(canonical)
    kind = 'maven' if is_maven(canonical) else 'npm'
    licenses, names = _declared_licenses(component)
    tarball = _npm_tarball(component) if kind == 'npm' else None
    vcs = None if tarball or kind == 'maven' else _vcs_reference(component)
    return PackageComponent(
        purl=canonical,
        package_key=pkg_key,
        kind=kind,
        group=component.get('group', ''),
        name=component.get('name', ''),
        version=component.get('version', ''),
        declared_raw=tuple(licenses),
        named_raw=frozenset(names),
        npm_tarball=tarball,
        vcs=vcs,
    )


def _merge_package(existing, incoming):
    """Merges duplicate occurrences of the same PURL across SBOMs so no metadata is lost by order."""
    declared = tuple(sorted(set(existing.declared_raw) | set(incoming.declared_raw)))
    named = existing.named_raw | incoming.named_raw
    copyrights = tuple(sorted(set(existing.copyrights) | set(incoming.copyrights)))
    npm_tarball = (
        existing.npm_tarball if (existing.npm_tarball and existing.npm_tarball.get('sha1'))
        else incoming.npm_tarball or existing.npm_tarball
    )
    return PackageComponent(
        purl=existing.purl,
        package_key=existing.package_key,
        kind=existing.kind,
        group=existing.group or incoming.group,
        name=existing.name or incoming.name,
        version=existing.version or incoming.version,
        declared_raw=declared,
        named_raw=named,
        npm_tarball=npm_tarball,
        vcs=existing.vcs or incoming.vcs,
        copyrights=copyrights,
    )


def _parse_declared_component(component):
    """Parses a non-Maven/non-npm component that carries both declared licenses and copyright lines (e.g. bundled fonts)."""
    purl = component.get('purl') or ''
    if re.match(r'^pkg:(?:deb|rpm|generic|github)/thingsboard/', purl):
        return None
    copyright_text = (component.get('copyright') or '').strip()
    licenses, names = _declared_licenses(component)
    if not copyright_text or not licenses:
        return None
    canonical = canonical_purl(purl) if purl else (component.get('bom-ref') or component.get('name', ''))
    if not canonical:
        return None
    pkg_key, _ = split_curation_id(canonical)
    copyrights = tuple(line.strip() for line in copyright_text.replace('\r\n', '\n').replace('\r', '\n').split('\n') if line.strip())
    return PackageComponent(
        purl=canonical,
        package_key=pkg_key,
        kind='declared',
        group=component.get('group', ''),
        name=component.get('name', ''),
        version=component.get('version', ''),
        declared_raw=tuple(licenses),
        named_raw=frozenset(names),
        npm_tarball=None,
        vcs=None,
        copyrights=copyrights,
    )


def _parse_jar(component, purl):
    sha256 = [h.get('content', '').lower() for h in component.get('hashes', []) if h.get('alg') == 'SHA-256' and h.get('content')]
    if not sha256:
        raise stop(f'the SBOM gives no SHA-256 for {purl}. {BUILD_PROBLEM}')
    try:
        return JarRef(
            purl=purl,
            group=component['group'],
            name=component['name'],
            version=component['version'],
            classifier=qualifiers(purl).get('classifier', ''),
            sha256=sha256[0],
        )
    except KeyError as err:
        raise stop(f'SBOM component {purl} is missing required field {err}. {BUILD_PROBLEM}')


def _license_file_ref(component, repo_license_files):
    comp_name = component.get('name', '')
    name = component.get('purl') or comp_name
    group = component.get('group', '')
    version = component.get('version', '')
    licenses, names = _declared_licenses(component)
    declared_raw = tuple(licenses)
    named_raw = frozenset(names)

    urls = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'license' and r.get('url')]
    if len(urls) > 1:
        raise stop(
            f"the SBOM gives several license files for {name}: {', '.join(urls)}. "
            'Give the one that covers the component and what it contains.'
        )

    # Match repo_license_files by normalized PURL+qualifiers so reordered qualifiers still match.
    matched_key = _match_license_file_key(name, repo_license_files)
    if urls and matched_key:
        raise stop(
            f'the SBOM gives a license file for {name}, {urls[0]}, and so does policy.toml [license_files], '
            f'{repo_license_files[matched_key]}. Keep one of them.'
        )
    if urls:
        return LicenseFileRef(
            group=group, name=comp_name, version=version, url=urls[0],
            declared_raw=declared_raw, named_raw=named_raw,
        )
    if matched_key:
        target = repo_license_files[matched_key]
        if target.startswith('generated:'):
            return LicenseFileRef(
                group=group, name=comp_name, version=version, generated=target.removeprefix('generated:'),
                declared_raw=declared_raw, named_raw=named_raw,
            )
        platform = qualifiers(name).get('os')
        return LicenseFileRef(
            group=group, name=comp_name, version=version, path=target, platform=platform,
            declared_raw=declared_raw, named_raw=named_raw,
        )
    if name.startswith('pkg:'):
        comp_pkg_key, _ = split_curation_id(canonical_purl(name))
        other = [
            key for key in repo_license_files
            if key.startswith('pkg:') and split_curation_id(canonical_purl(key))[0] == comp_pkg_key
        ]
        if other:
            raise stop(
                f'policy.toml [license_files] has the license file of {other[0]}, {repo_license_files[other[0]]}, '
                f'and the SBOM has {name}. Update the file for this version/platform and then update policy.toml.'
            )
    return None


def _match_license_file_key(name, repo_license_files):
    if name in repo_license_files:
        return name
    if not name.startswith('pkg:'):
        return None
    target = (canonical_purl(name), tuple(sorted(qualifiers(name).items())))
    for key in repo_license_files:
        if key.startswith('pkg:') and (canonical_purl(key), tuple(sorted(qualifiers(key).items()))) == target:
            return key
    return None


def _npm_tarball(component):
    for reference in component.get('externalReferences', []):
        if reference.get('type') == 'distribution' and reference.get('url'):
            sha1 = [h.get('content', '').lower() for h in reference.get('hashes', []) if h.get('alg') == 'SHA-1' and h.get('content')]
            return {
                'url': reference['url'].replace('https://registry.yarnpkg.com/', 'https://registry.npmjs.org/'),
                'sha1': sha1[0] if sha1 else '',
            }
    return None


def _vcs_reference(component):
    references = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'vcs' and r.get('url')]
    pinned = [url for url in references if re.search(r'#[0-9a-f]{40}$', url)]
    if not pinned:
        return None
    raw_url, _, revision = pinned[0].partition('#')
    is_svn = raw_url.startswith('scm:svn:') or raw_url.startswith('svn://') or raw_url.startswith('svn+ssh://')
    url = re.sub(r'^scm:(git|svn):', '', re.sub(r'^git\+', '', raw_url))
    if not url or not revision:
        return None
    return {'type': 'Subversion' if is_svn else 'Git', 'url': url, 'revision': revision, 'path': ''}


def _declared_licenses(component):
    licenses, names = set(), set()
    for entry in component.get('licenses', []):
        if 'expression' in entry and entry['expression']:
            licenses.add(entry['expression'])
        else:
            lic = entry.get('license', {})
            value = lic.get('id') or lic.get('name')
            if value:
                licenses.add(value)
                if not lic.get('id'):
                    names.add(value)
    return sorted(licenses), names
