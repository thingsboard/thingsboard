#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Turns CycloneDX SBOMs of ThingsBoard into an ORT project file (ortproject.json): the list of third-party packages
of the SBOMs, each with the address and SHA-1 of its source archive, or else with its repository.

Everything comes from the SBOM, except the SHA-1 of a Maven sources jar, which is read from the Maven repository
and remembered in a cache file. A package without a sources jar is asked for again each time.

Usage: sbom_to_ort.py --project-version <version> --out <ortproject.json> --cache <file> [--own <group>/<name> ...]
       [--add <Maven purl>=<SPDX license> ...] <SBOM> ...
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
# License names of poms that offer a choice of licenses. ORT maps a name that stands alone, but not one inside a choice.
# A name that says too little is here together with the address the pom gives for it.
LICENSE_NAMES = {
    'Eclipse Distribution License - Version 1.0': 'BSD-3-Clause',
    'GNU General Public License Version 2': 'GPL-2.0-only',
    'GNU General Public License, version 2': 'GPL-2.0-only',
    ('GNU Lesser General Public License', 'http://www.gnu.org/licenses/lgpl-3.0.txt'): 'LGPL-3.0-only',
    'GNU Lesser General Public License Version 2.1': 'LGPL-2.1-only',
    'Mozilla Public License Version 2.0': 'MPL-2.0',
    'The W3C License': 'W3C',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project-version', required=True, help='version of the ThingsBoard modules, which are left out')
    parser.add_argument('--out', required=True)
    parser.add_argument('--own', action='append', default=[], help='<group>/<name> of our own code released separately')
    parser.add_argument('--cache', required=True, help='file that remembers the sources jars of Maven packages')
    parser.add_argument('--add', action='append', default=[], metavar='PURL=LICENSE',
                        help='a Maven package that the artifact contains and the SBOMs do not list yet')
    parser.add_argument('sboms', nargs='+')
    args = parser.parse_intermixed_args()

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

    dependencies, without_source = [], []
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
        licenses = declared_licenses(component)
        if licenses:
            dependency['declaredLicenses'] = licenses
        dependencies.append(dependency)

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
    """The licenses as the SBOM states them; ORT maps the names it knows to SPDX identifiers."""
    licenses = set()
    for entry in component.get('licenses', []):
        if 'expression' in entry:
            licenses.add(entry['expression'])
        else:
            license = entry['license']
            licenses.add(license.get('id') or LICENSE_NAMES.get((license.get('name'), license.get('url'))) or license.get('name'))
    licenses = sorted(license for license in licenses if license)
    # Several licenses in a pom are a choice, by the convention ORT follows too; ORT would read a list as "all apply"
    if is_maven(component['purl']) and len(licenses) > 1:
        licenses = [LICENSE_NAMES.get(license, license) for license in licenses]
        unknown = [license for license in licenses if ' ' in license and ' WITH ' not in license and ' OR ' not in license]
        if unknown:
            raise ValueError(f"{component['purl']} offers a choice of licenses, and LICENSE_NAMES has no SPDX identifier for: {unknown}")
        return [' OR '.join(licenses)]
    return licenses


if __name__ == '__main__':
    sys.exit(main())
