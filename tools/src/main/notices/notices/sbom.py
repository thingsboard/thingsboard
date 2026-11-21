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

from notices import BUILD_PROBLEM, stop
from notices.config import split_curation_id


@dataclass(frozen=True)
class PackageComponent:
    purl: str           # canonical purl without qualifiers/fragment (npm scope normalized to @)
    package_key: str    # purl without @version
    kind: str           # 'maven' or 'npm'
    group: str
    name: str
    version: str
    declared_raw: tuple[str, ...]   # declared license strings (SPDX or names)
    named_raw: frozenset[str]       # subset of declared_raw that came as non-SPDX names
    npm_tarball: dict | None        # {'url': ..., 'sha1': ...}
    vcs: dict | None                # {'type': ..., 'url': ..., 'revision': ..., 'path': ''}

    @property
    def coordinates(self):
        return f'{self.group}:{self.name} {self.version}' if self.group else f'{self.name} {self.version}'


@dataclass(frozen=True)
class JarRef:
    purl: str
    group: str
    name: str
    version: str
    classifier: str
    file_type: str
    sha256: str

    @property
    def coordinates(self):
        return f'{self.group}:{self.name} {self.version}' if self.group else f'{self.name} {self.version}'


@dataclass(frozen=True)
class LicenseFileRef:
    group: str
    name: str
    version: str
    url: str | None = None
    path: str | None = None
    platform: str | None = None
    generated: str | None = None  # e.g. 'lwm2m-registry'

    @property
    def coordinates(self):
        base = f'{self.group}:{self.name}' if self.group else self.name
        return f'{base} {self.version}'.rstrip()


@dataclass
class SbomInventory:
    packages: dict[str, PackageComponent]   # canonical purl -> PackageComponent
    jars: dict[str, JarRef]                 # full purl (with qualifiers) -> JarRef
    license_files: set[LicenseFileRef]
    skipped: dict[str, set[str]]


def discover_sboms(root_dir, expected_version):
    """Finds all target/sbom/*.cdx.json files under root_dir and verifies their project version."""
    sboms = []
    for current, dirs, files in os.walk(root_dir):
        dirs[:] = sorted(d for d in dirs if d != 'node_modules' and d != '.git')
        if current.endswith(os.path.join('target', 'sbom')):
            for name in sorted(files):
                if name.endswith('.cdx.json') and not name.startswith('scan_'):
                    sboms.append(os.path.join(current, name))
    if not sboms:
        raise stop(
            f'no SBOMs in the target/sbom directories under {root_dir}. Build with them first: '
            'mvn clean package -DskipTests -Dsbom.skip=false'
        )
    wrong = []
    for path in sboms:
        with open(path, encoding='utf-8') as file:
            version = json.load(file).get('metadata', {}).get('component', {}).get('version')
        if version != expected_version:
            wrong.append(f'  {path} ({version})')
    if wrong:
        raise stop(
            f'these SBOMs are not of the version {expected_version}, so an older build left them. '
            'Build again from clean: mvn clean package -DskipTests -Dsbom.skip=false\n' + '\n'.join(wrong)
        )
    return sboms


def load_sboms(sbom_paths, project_version, policy):
    """Loads one or more CycloneDX SBOM files into a unified SbomInventory."""
    absent = [p for p in sbom_paths if not os.path.exists(p)]
    if absent:
        raise stop(
            'these SBOMs do not exist, and the notices are made from them:\n  '
            + '\n  '.join(absent)
            + f'\n{BUILD_PROBLEM} The Maven build makes them with -Dsbom.skip=false.'
        )

    packages, jars, license_files, skipped = {}, {}, set(), {}
    for sbom_path in sbom_paths:
        with open(sbom_path, encoding='utf-8') as file:
            bom = json.load(file)
        for component in bom.get('components', []):
            purl = component.get('purl') or ''
            # Components found only by Syft in a Docker base image ship their own license texts there.
            if any(p.get('name', '').startswith('syft:location:') for p in component.get('properties', [])):
                skipped.setdefault('part of the base image', set()).add(purl or component.get('name', ''))
                continue

            if not is_maven(purl) and not is_npm(purl):
                ref = _license_file_ref(component, policy.license_files)
                if ref:
                    license_files.add(ref)
                else:
                    skipped.setdefault('not a Maven or npm package', set()).add(purl or component.get('name', ''))
                continue

            canonical = canonical_purl(purl)
            if is_maven(purl) and _is_own(component, project_version, policy.own_modules):
                skipped.setdefault('our own module', set()).add(canonical)
                continue

            if canonical not in packages:
                packages[canonical] = _parse_package(component, canonical)
            if is_jar(purl) and purl not in jars:
                jars[purl] = _parse_jar(component, purl)

    return SbomInventory(packages=packages, jars=jars, license_files=license_files, skipped=skipped)


def canonical_purl(purl):
    """Strips qualifiers/fragments and normalizes npm %40 scope encoding."""
    base = re.sub(r'[?#].*', '', purl)
    return base.replace('pkg:npm/%40', 'pkg:npm/@')


def without_version(purl):
    return re.sub(r'@[^?#]*', '', purl)


def qualifiers(purl):
    return dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(purl).query))


def is_maven(purl):
    return purl.startswith('pkg:maven/')


def is_npm(purl):
    return purl.startswith('pkg:npm/')


def file_type(purl):
    return qualifiers(purl).get('type', 'jar')


def is_jar(purl):
    return is_maven(purl) and file_type(purl) == 'jar'


def _is_own(component, project_version, own_modules):
    group, name, version = component.get('group', ''), component['name'], component.get('version')
    if f'{group}/{name}' in own_modules:
        return True
    return (group == 'org.thingsboard' or group.startswith('org.thingsboard.')) and version == project_version


def _parse_package(component, canonical):
    pkg_key, _ = split_curation_id(canonical)
    kind = 'maven' if is_maven(canonical) else 'npm'
    licenses, names = _declared_licenses(component, kind)
    tarball = _npm_tarball(component) if kind == 'npm' else None
    vcs = None if tarball or kind == 'maven' else _vcs_reference(component)
    return PackageComponent(
        purl=canonical,
        package_key=pkg_key,
        kind=kind,
        group=component.get('group', ''),
        name=component['name'],
        version=component.get('version', ''),
        declared_raw=tuple(licenses),
        named_raw=frozenset(names),
        npm_tarball=tarball,
        vcs=vcs,
    )


def _parse_jar(component, purl):
    sha256 = [h['content'] for h in component.get('hashes', []) if h.get('alg') == 'SHA-256']
    if not sha256:
        raise stop(f'the SBOM gives no SHA-256 for {purl}. {BUILD_PROBLEM}')
    return JarRef(
        purl=purl,
        group=component['group'],
        name=component['name'],
        version=component['version'],
        classifier=qualifiers(purl).get('classifier', ''),
        file_type=file_type(purl),
        sha256=sha256[0],
    )


def _license_file_ref(component, repo_license_files):
    name = component.get('purl') or component['name']
    group = component.get('group', '')
    comp_name = component['name']
    version = component.get('version', '')

    if comp_name == 'lwm2m-registry' and not component.get('purl'):
        return LicenseFileRef(group=group, name=comp_name, version=version, generated='lwm2m-registry')

    urls = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'license']
    if len(urls) > 1:
        raise stop(
            f"the SBOM gives several license files for {name}: {', '.join(urls)}. "
            'Give the one that covers the component and what it contains.'
        )
    if urls and name in repo_license_files:
        raise stop(
            f'the SBOM gives a license file for {name}, {urls[0]}, and so does policy.toml [license_files], '
            f'{repo_license_files[name]}. Keep one of them.'
        )
    if urls:
        return LicenseFileRef(group=group, name=comp_name, version=version, url=urls[0])
    if name in repo_license_files:
        platform = qualifiers(name).get('os')
        return LicenseFileRef(
            group=group, name=comp_name, version=version, path=repo_license_files[name], platform=platform
        )
    other = [key for key in repo_license_files if key.startswith('pkg:') and without_version(key) == without_version(name)]
    if other:
        raise stop(
            f'policy.toml [license_files] has the license file of {other[0]}, {repo_license_files[other[0]]}, '
            f'and the SBOM has {name}. Update the file for this version and then update policy.toml.'
        )
    return None


def _npm_tarball(component):
    for reference in component.get('externalReferences', []):
        if reference.get('type') == 'distribution':
            sha1 = [h['content'] for h in reference.get('hashes', []) if h.get('alg') == 'SHA-1']
            if not sha1:
                raise stop(f"the SBOM gives no SHA-1 for {reference['url']}. {BUILD_PROBLEM}")
            return {
                'url': reference['url'].replace('https://registry.yarnpkg.com/', 'https://registry.npmjs.org/'),
                'sha1': sha1[0],
            }
    return None


def _vcs_reference(component):
    references = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'vcs']
    pinned = [url for url in references if re.search(r'#[0-9a-f]{40}$', url)]
    url, _, revision = (pinned or references or [''])[0].partition('#')
    url = re.sub(r'^scm:(git|svn):', '', re.sub(r'^git\+', '', url))
    if not url:
        return None
    return {'type': 'Subversion' if 'svn' in url else 'Git', 'url': url, 'revision': revision, 'path': ''}


def _declared_licenses(component, kind):
    licenses, names = set(), set()
    for entry in component.get('licenses', []):
        if 'expression' in entry:
            licenses.add(entry['expression'])
        else:
            lic = entry.get('license', {})
            value = lic.get('id') or lic.get('name')
            if value:
                licenses.add(value)
                if not lic.get('id'):
                    names.add(value)
    ordered = sorted(licenses)
    named = {lic for lic in ordered if lic in names}
    return ordered, named
