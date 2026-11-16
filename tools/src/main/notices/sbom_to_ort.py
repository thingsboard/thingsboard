#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Turns CycloneDX SBOMs of ThingsBoard into an ORT project file (ortproject.json): the list of third-party packages
of the SBOMs, each with the address and SHA-1 of its source archive, or else with its repository.

Everything comes from the SBOM, except the SHA-1 of a Maven sources jar, which is read from the Maven repository
and remembered in a cache file. A package without a sources jar is asked for again each time.

A license that the SBOM gives as a name, and not as an SPDX identifier or expression, is one that nobody recognised.
ORT would guess what it stands for, so the package must have a curation that says it: see mapped().

A curation that concludes the license of a package sets aside what the scan finds in it, so it is given for the versions
that someone has looked at. Another version of that package must be looked at as well: see unreviewed().

Usage: sbom_to_ort.py --project-version <version> --out <ortproject.json> --cache <file> --curations <file>
       [--own <group>/<name> ...] [--add <Maven purl>=<SPDX license> ...] <SBOM> ...
"""
import argparse
import concurrent.futures
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

MAVEN_REPOSITORIES = ['https://repo1.maven.org/maven2', 'https://repo.thingsboard.io/artifactory/libs-release-public']
SHA1 = re.compile(r'^[0-9a-f]{40}$')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project-version', required=True, help='version of the ThingsBoard modules, which are left out')
    parser.add_argument('--out', required=True)
    parser.add_argument('--own', action='append', default=[], help='<group>/<name> of our own code released separately')
    parser.add_argument('--cache', required=True, help='file that remembers the sources jars of Maven packages')
    parser.add_argument('--curations', required=True, help='the package curations of ORT, as JSON')
    parser.add_argument('--add', action='append', default=[], metavar='PURL=LICENSE',
                        help='a Maven package that the artifact contains and the SBOMs do not list yet')
    parser.add_argument('sboms', nargs='+')
    args = parser.parse_intermixed_args()

    absent = [sbom for sbom in args.sboms if not os.path.exists(sbom)]
    if absent:
        raise SystemExit('These SBOMs do not exist, and the notices are made from them:\n  ' + '\n  '.join(absent)
                         + '\nIn the Maven build the SBOMs are made with -Dsbom.skip=false.')

    components, skipped = {}, {}
    for sbom in args.sboms:
        for component in json.load(open(sbom)).get('components', []):
            purl = component.get('purl') or ''
            key = re.sub(r'[?#].*', '', purl)
            # What the scan of an image found and our build did not list came with the base image, and has its
            # license texts there
            if any(p['name'].startswith('syft:location:') for p in component.get('properties', [])):
                skipped.setdefault('part of the base image', set()).add(purl or component.get('name'))
            elif not is_maven(purl) and not purl.startswith('pkg:npm/'):
                skipped.setdefault('not a Maven or npm package', set()).add(purl or component.get('name'))
            elif is_maven(purl) and own(component, args):
                skipped.setdefault('our own module', set()).add(key)
            else:
                # The same package can be listed several times, as jars with different classifiers
                components.setdefault(key, component)

    for added in args.add:
        purl, _, license = added.partition('=')
        group, name, version = re.fullmatch(r'pkg:maven/([^/]+)/([^@]+)@(.+)', purl).groups()
        components.setdefault(purl, {'purl': purl, 'group': group, 'name': name, 'version': version,
                                     'licenses': [{'license': {'id': license}}]})

    cache = read_cache(args.cache)
    missing = [key for key in components if is_maven(key) and key not in cache]
    found = {}
    try:
        with concurrent.futures.ThreadPoolExecutor(16) as pool:
            for key, source in zip(missing, pool.map(lambda key: maven_sources(components[key]), missing)):
                if source:
                    found[key] = source
    finally:
        if found:
            # Several modules can be built at once: what another build found meanwhile is kept, and the file is
            # replaced in one step
            cache = {**read_cache(args.cache), **found}
            temporary = f'{args.cache}.{os.getpid()}'
            json.dump(cache, open(temporary, 'w'), indent=1, sort_keys=True)
            os.replace(temporary, args.cache)

    curations = json.load(open(args.curations))
    dependencies, without_source, unnamed, unreviewed_versions = [], [], [], []
    for key, component in sorted(components.items()):
        dependency = {'id': identifier(component), 'purl': key}
        source = cache.get(key) if is_maven(key) else npm_tarball(component)
        if source:
            dependency['sourceArtifact'] = {'url': source['url'], 'hash': {'value': source['sha1'], 'algorithm': 'SHA-1'}}
        # The repository a pom names is often that of a parent module, so only npm packages take it from the SBOM
        vcs = None if source or is_maven(key) else repository(component)
        if vcs:
            dependency['vcs'] = vcs
        if not source:
            origin = f"repository {vcs['url']} {vcs['revision']}" if vcs else 'no repository from the SBOM'
            without_source.append(f'{key} ({origin})')
        licenses, names = declared_licenses(component)
        if licenses:
            dependency['declaredLicenses'] = licenses
        unnamed += [f"{dependency['id']}: {json.dumps(name)}" for name in names if not mapped(dependency['id'], name, curations)]
        ranges = unreviewed(dependency['id'], curations)
        if ranges:
            unreviewed_versions.append(f"{dependency['id']}: the license is concluded only for {', '.join(ranges)}")
        dependencies.append(dependency)
    if unreviewed_versions:
        raise SystemExit(f'{args.curations} concludes the license of these packages, but not of the version in the product. '
                         'Look at the license of that version, then widen the range of the curation or add one:\n  '
                         + '\n  '.join(unreviewed_versions))
    if unnamed:
        raise SystemExit('These packages state a license by a name that is no SPDX identifier. Add a curation without a version to '
                         f'{args.curations} that has the name in its declared_license_mapping:\n  '
                         + '\n  '.join(unnamed))

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    json.dump({'projectName': 'thingsboard', 'dependencies': dependencies}, open(args.out, 'w'), indent=1)
    print(f'{len(dependencies)} packages')
    for reason, items in sorted(skipped.items()):
        print(f'Left out, {reason}: {len(items)}')
        for item in sorted(items):
            print(f'  {item}')
    print(f'Without a source archive: {len(without_source)}')
    for item in without_source:
        print(f'  {item}')


def is_maven(purl):
    return purl.startswith('pkg:maven/')


def read_cache(path):
    """The sources jars found so far. That a package has none is not remembered: it may get one, or be in a repository
    that is added later."""
    cache = json.load(open(path)) if os.path.exists(path) else {}
    return {key: source for key, source in cache.items() if source}


def identifier(component):
    """The name ORT's own analysis gives the package: curations and package configurations refer to it."""
    kind = 'Maven' if is_maven(component['purl']) else 'NPM'
    return f"{kind}:{component.get('group', '')}:{component['name']}:{component['version']}"


def own(component, args):
    group, name, version = component.get('group', ''), component['name'], component.get('version')
    if f'{group}/{name}' in args.own:
        return True
    return (group == 'org.thingsboard' or group.startswith('org.thingsboard.')) and version == args.project_version


def maven_sources(component):
    """The address and SHA-1 of the sources jar, from the first repository that has it."""
    group, name, version = component['group'], component['name'], component['version']
    for repository_url in MAVEN_REPOSITORIES:
        url = f"{repository_url}/{group.replace('.', '/')}/{name}/{version}/{name}-{version}-sources.jar"
        try:
            with urllib.request.urlopen(urllib.request.Request(url + '.sha1', headers={'User-Agent': 'sbom-to-ort'}), timeout=30) as response:
                sha1 = response.read().decode().split()[0].lower()
        except urllib.error.HTTPError as error:
            if error.code == 404:
                continue
            raise
        if not SHA1.match(sha1):
            raise ValueError(f'{url}.sha1 is no SHA-1: {sha1}')
        return {'url': url, 'sha1': sha1}
    return None


def npm_tarball(component):
    """The tarball the package was installed from, as yarn.lock names it."""
    for reference in component.get('externalReferences', []):
        if reference.get('type') == 'distribution':
            sha1 = [h['content'] for h in reference.get('hashes', []) if h['alg'] == 'SHA-1']
            if not sha1:
                raise ValueError(f"The SBOM gives no SHA-1 for {reference['url']}")
            # The same file under the name ORT knows the registry by
            return {'url': reference['url'].replace('https://registry.yarnpkg.com/', 'https://registry.npmjs.org/'), 'sha1': sha1[0]}
    return None


def repository(component):
    references = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'vcs']
    # An npm package installed from Git: yarn.lock names the commit
    pinned = [url for url in references if re.search(r'#[0-9a-f]{40}$', url)]
    url, _, revision = (pinned or references or [''])[0].partition('#')
    url = re.sub(r'^scm:(git|svn):', '', re.sub(r'^git\+', '', url))
    if not url:
        return None
    return {'type': 'Subversion' if 'svn' in url else 'Git', 'url': url, 'revision': revision, 'path': ''}


def declared_licenses(component):
    """The licenses as the SBOM states them, and those of them that hold a name and not only SPDX identifiers."""
    licenses, names = set(), set()
    for entry in component.get('licenses', []):
        if 'expression' in entry:
            licenses.add(entry['expression'])
        else:
            license = entry['license']
            licenses.add(license.get('id') or license.get('name'))
            if not license.get('id'):
                names.add(license.get('name'))
    licenses = sorted(license for license in licenses if license)
    named = [license for license in licenses if license in names]
    # Several licenses in a pom are a choice, by the convention ORT follows too; ORT would read a list as "all apply".
    # With a name among them the text is no expression, and the curation of the package maps the whole of it.
    if is_maven(component['purl']) and len(licenses) > 1:
        licenses = [' OR '.join(licenses)]
        return licenses, licenses if named else []
    return licenses, named


def mapped(identifier, name, curations):
    """Whether a curation says what the named license of the package is. Only a curation without a version counts,
    which applies to every version: the name itself changes when the license of a later version does."""
    kind, namespace, package = identifier.split(':')[:3]
    for curation in curations:
        parts = (curation['id'].split(':') + ['', '', ''])[:4]
        if parts[0] == kind and parts[1] == namespace and parts[2] in ('', package) and not parts[3]:
            if name in curation['curations'].get('declared_license_mapping', {}):
                return True
    return False


def unreviewed(identifier, curations):
    """The versions for which curations conclude the license of the package, when the version of the package is not
    among them; nothing when it is, or when no curation concludes its license."""
    package, _, version = identifier.rpartition(':')
    concluded = [curation['id'][len(package) + 1:] for curation in curations
                 if 'concluded_license' in curation['curations'] and curation['id'].startswith(package + ':')]
    return [] if any(covers(versions, version) for versions in concluded) else concluded


def covers(versions, version):
    """Whether a version is the one a curation names, or is within its range: [1.0,2.0[ is 1.0 and later, before 2.0."""
    limits = re.fullmatch(r'([\[\]])([^,]*),([^,]*)([\[\]])', versions)
    if not limits:
        return versions == version
    opening, lowest, highest, closing = limits.groups()
    parts = [numbers(text) for text in (version, lowest, highest)]
    # 3 is 3.0: without the zeros a shorter version would be before a longer one that starts the same
    number, low, high = (part + [0] * (max(map(len, parts)) - len(part)) for part in parts)
    return ((not lowest or number > low or number == low and opening == '[')
            and (not highest or number < high or number == high and closing == ']'))


def numbers(version):
    """The leading numbers of a version, which say where it is in a range: 2.4.0-b180830.0359 is 2.4.0."""
    return [int(number) for number in re.match(r'[0-9.]*', version).group().split('.') if number]


if __name__ == '__main__':
    sys.exit(main())
