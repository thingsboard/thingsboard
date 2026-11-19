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

A package with neither a source archive nor a repository cannot be scanned, so it needs a curation that gives its source
or says that it has none: see source_curated().

Each of these stops the notices with a message that says what to do.

It also writes the list of the jars of the Maven packages, each with its SHA-256, for notice_files.py.

A component that is neither a Maven nor an npm package, but whose SBOM gives the address of its license file, or whose
license file is kept in this repository (--license-files-here), is written to the list for license_files.py.

Usage: sbom_to_ort.py --project-version <version> --out <ortproject.json> --cache <file> --curations <file>
       --jars <file> --license-files <file> --license-files-here <file> [--own <group>/<name> ...] <SBOM> ...
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

# The root of this repository, which the paths of the files kept in it are relative to
ROOT = os.path.realpath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
CURATIONS = 'tools/src/main/notices/ort-config/curations/curations.json'
BUILD_PROBLEM = 'This is a problem of the build or of these tools, not something to decide in the files of the notices.'


def stop(text):
    """What stops the notices, with the words that every stop starts with."""
    return SystemExit(f'Third-party notices: {text}')
MAVEN_REPOSITORIES = ['https://repo1.maven.org/maven2', 'https://repo.thingsboard.io/artifactory/libs-release-public']
SHA1 = re.compile(r'^[0-9a-f]{40}$')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--project-version', required=True, help='version of the ThingsBoard modules, which are left out')
    parser.add_argument('--out', required=True)
    parser.add_argument('--jars', required=True, help='file to write the jars of the Maven packages to')
    parser.add_argument('--license-files', required=True,
                        help='file to write the components that are taken from their license file to')
    parser.add_argument('--license-files-here', required=True,
                        help='the license files kept in this repository, of the components that are not scanned')
    parser.add_argument('--own', action='append', default=[], help='<group>/<name> of our own code released separately')
    parser.add_argument('--cache', required=True, help='file that remembers the sources jars of Maven packages')
    parser.add_argument('--curations', required=True, help='the package curations of ORT, as JSON')
    parser.add_argument('sboms', nargs='+')
    args = parser.parse_intermixed_args()

    absent = [sbom for sbom in args.sboms if not os.path.exists(sbom)]
    if absent:
        raise stop('these SBOMs do not exist, and the notices are made from them:\n  ' + '\n  '.join(absent)
                   + f'\n{BUILD_PROBLEM} The Maven build makes them with -Dsbom.skip=false.')

    here = read_here(args.license_files_here)
    components, skipped, jars, license_files = {}, {}, {}, set()
    for sbom in args.sboms:
        for component in json.load(open(sbom)).get('components', []):
            purl = component.get('purl') or ''
            key = re.sub(r'[?#].*', '', purl)
            # What the scan of an image found and our build did not list came with the base image, and has its
            # license texts there
            if any(p['name'].startswith('syft:location:') for p in component.get('properties', [])):
                skipped.setdefault('part of the base image', set()).add(purl or component.get('name'))
            elif not is_maven(purl) and not purl.startswith('pkg:npm/'):
                where = license_file(component, here)
                if where:
                    license_files.add((component.get('group', ''), component['name'], component.get('version', ''),
                                       tuple(sorted(where.items()))))
                else:
                    skipped.setdefault('not a Maven or npm package', set()).add(purl or component.get('name'))
            elif is_maven(purl) and own(component, args):
                skipped.setdefault('our own module', set()).add(key)
            else:
                # The same package can be listed several times, as jars with different classifiers
                components.setdefault(key, component)
                if is_jar(purl):
                    jars.setdefault(purl, jar(component))

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
    dependencies, without_source, unnamed, unreviewed_versions, unscanned = [], [], [], [], []
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
            if not vcs and not source_curated(dependency['id'], curations):
                unscanned.append(dependency['id'])
        licenses, names = declared_licenses(component)
        if licenses:
            dependency['declaredLicenses'] = licenses
        unnamed += [(dependency['id'], name) for name in names if not mapped(dependency['id'], name, curations)]
        ranges = unreviewed(dependency['id'], curations)
        if ranges:
            unreviewed_versions.append((dependency['id'], ranges))
        dependencies.append(dependency)
    if unreviewed_versions:
        raise stop(unreviewed_message(unreviewed_versions))
    if unnamed:
        raise stop(unnamed_message(unnamed))
    if unscanned:
        raise stop(unscanned_message(unscanned))

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    json.dump({'projectName': 'thingsboard', 'dependencies': dependencies}, open(args.out, 'w'), indent=1)
    json.dump([jars[purl] for purl in sorted(jars)], open(args.jars, 'w'), indent=1)
    json.dump([{'group': group, 'name': name, 'version': version, **dict(where)}
               for group, name, version, where in sorted(license_files)], open(args.license_files, 'w'), indent=1)
    print(f'{len(dependencies)} packages')
    for reason, items in sorted(skipped.items()):
        print(f'Left out, {reason}: {len(items)}')
        for item in sorted(items):
            print(f'  {item}')
    print(f'Without a source archive: {len(without_source)}')
    for item in without_source:
        print(f'  {item}')


def read_here(path):
    """The license files kept in this repository, by the purl of the component, or by its name when it has none: each is
    the path of the file, or {"path": ..., "platform": ...} for a file with sections for one platform, which the notices
    of another platform leave out (see license_files.py)."""
    here = {}
    for key, value in json.load(open(path, encoding='utf-8')).items():
        entry = {'path': value} if isinstance(value, str) else value
        if not isinstance(entry, dict) or 'path' not in entry or set(entry) - {'path', 'platform'}:
            raise stop(f'the entry of {key} in {path} is neither the path of a file nor {{"path": ..., "platform": ...}}.')
        here[key] = entry
    return here


def license_file(component, here):
    """Where the license file of the component is: {'url': the address that the SBOM gives}, the entry that `here` gives
    for the purl of the component, or for its name when it has none, or None."""
    name = component.get('purl') or component['name']
    urls = [r['url'] for r in component.get('externalReferences', []) if r.get('type') == 'license']
    if len(urls) > 1:
        raise stop(f"the SBOM gives several license files for {name}: {', '.join(urls)}. Give the one that covers the "
                   'component and what it contains.')
    if urls and name in here:
        raise stop(f'the SBOM gives a license file for {name}, {urls[0]}, and so does license-files-here.json, '
                   f'{here[name]["path"]}. Keep one of them.')
    if urls:
        return {'url': urls[0]}
    if name in here:
        return here[name]
    # A file kept for another version of the component would leave this version out of the notices without a word
    other = [key for key in here if key.startswith('pkg:') and without_version(key) == without_version(name)]
    if other:
        raise stop(f'license-files-here.json has the license file of {other[0]}, {here[other[0]]["path"]}, and the SBOM '
                   f'has {name}. Make the file for this version, as the README beside it says, and then change its entry.')
    return None


def without_version(purl):
    return re.sub(r'@[^?#]*', '', purl)


def is_maven(purl):
    return purl.startswith('pkg:maven/')


def is_jar(purl):
    """Whether a Maven package is a jar, whose NOTICE files notice_files.py reads: a package can also be another file,
    such as the executable of WinSW."""
    return is_maven(purl) and file_type(purl) == 'jar'


def file_type(purl):
    """The type of the file of a Maven package, as its purl gives it: jar unless it says otherwise."""
    return qualifiers(purl).get('type', 'jar')


def qualifiers(purl):
    return dict(urllib.parse.parse_qsl(urllib.parse.urlsplit(purl).query))


def read_cache(path):
    """The sources jars found so far. That a package has none is not remembered: it may get one, or be in a repository
    that is added later."""
    cache = json.load(open(path)) if os.path.exists(path) else {}
    return {key: source for key, source in cache.items() if source}


def identifier(component):
    """The name ORT's own analysis gives the package: curations and package configurations refer to it."""
    kind = 'Maven' if is_maven(component['purl']) else 'NPM'
    return f"{kind}:{component.get('group', '')}:{component['name']}:{component['version']}"


def jar(component):
    """One file of a Maven package, as the artifact contains it: a package can have several, with classifiers."""
    sha256 = [h['content'] for h in component.get('hashes', []) if h['alg'] == 'SHA-256']
    if not sha256:
        raise stop(f"the SBOM gives no SHA-256 for {component['purl']}. {BUILD_PROBLEM}")
    return {'group': component['group'], 'name': component['name'], 'version': component['version'],
            'classifier': qualifiers(component['purl']).get('classifier', ''), 'type': file_type(component['purl']), 'sha256': sha256[0]}


def own(component, args):
    group, name, version = component.get('group', ''), component['name'], component.get('version')
    if f'{group}/{name}' in args.own:
        return True
    return (group == 'org.thingsboard' or group.startswith('org.thingsboard.')) and version == args.project_version


def maven_sources(component):
    """The address and SHA-1 of the sources jar, from the first repository that has it."""
    path = maven_path(component['group'], component['name'], component['version'], 'sources')
    for url, content in from_repositories(path + '.sha1', timeout=30):
        sha1 = content.decode().split()[0].lower()
        if not SHA1.match(sha1):
            raise stop(f'{url} is no SHA-1: {sha1}. {BUILD_PROBLEM}')
        return {'url': url.removesuffix('.sha1'), 'sha1': sha1}
    return None


def write_atomically(path, data):
    """Writes the file in one step: several builds can run at once, and none must read a file half written."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    temporary = f'{path}.{os.getpid()}'
    with open(temporary, 'wb') as file:
        file.write(data)
    os.replace(temporary, path)


def maven_path(group, name, version, classifier='', type='jar'):
    """Where a file of a Maven package is in a repository."""
    return f"{group.replace('.', '/')}/{name}/{version}/{name}-{version}{'-' + classifier if classifier else ''}.{type}"


def from_repositories(path, timeout):
    """The address and the content of a file, from each Maven repository that has it."""
    for repository_url in MAVEN_REPOSITORIES:
        url = f'{repository_url}/{path}'
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'sbom-to-ort'}), timeout=timeout) as response:
                yield url, response.read()
        except urllib.error.HTTPError as error:
            if error.code != 404:
                raise


def npm_tarball(component):
    """The tarball the package was installed from, as yarn.lock names it."""
    for reference in component.get('externalReferences', []):
        if reference.get('type') == 'distribution':
            sha1 = [h['content'] for h in reference.get('hashes', []) if h['alg'] == 'SHA-1']
            if not sha1:
                raise stop(f"the SBOM gives no SHA-1 for {reference['url']}. {BUILD_PROBLEM}")
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


def unreviewed_message(packages):
    identifier, ranges = packages[0]
    package, _, version = identifier.rpartition(':')
    return ('we set the license of these packages by hand, but only for older versions.\n'
            + ''.join(f'  {i}   (checked: {", ".join(r)})\n' for i, r in packages)
            + 'A license can change between versions, so it must not carry over to a version nobody has read.\n'
            f'To fix: read the license of the new version (its LICENSE file and source headers), then in {CURATIONS} either\n'
            f'  - widen the range of the entry, if the license is the same: "id": "{package}:[<from>,<to>[" '
            f'(for example a range that holds {version}), or\n'
            '  - add an entry for the new version or range, with the license you found and a "comment" saying where.')


def unnamed_message(packages):
    identifier, name = packages[0]
    package = identifier.rpartition(':')[0]
    return ('these packages name their license in words that are no standard license identifier.\n'
            + ''.join(f'  {i}: {json.dumps(n)}\n' for i, n in packages)
            + 'The tool would guess what the words mean, and a wrong guess puts a wrong license into the notices.\n'
            f'To fix: find the standard identifier (https://spdx.org/licenses), and add to {CURATIONS} an entry without a version,\n'
            'which then holds for every version that names its license in the same words:\n'
            f'  {{"id": "{package}",\n'
            '   "curations": {"comment": "<where you checked>",\n'
            f'                 "declared_license_mapping": {{{json.dumps(name)}: "<identifier>"}}}}}}\n'
            'A package already in that file gets the mapping in its entry without a version.')


def unscanned_message(identifiers):
    return ('the source of these packages cannot be found, so their licenses and copyrights cannot be read.\n'
            + ''.join(f'  {i}\n' for i in identifiers)
            + f'They have no sources jar or tarball, and no repository. To fix, add to {CURATIONS} one of:\n'
            '  - where the source is, at the commit of this version:\n'
            f'    {{"id": "{identifiers[0]}",\n'
            '     "curations": {"comment": "<why this commit>",\n'
            '                   "vcs": {"type": "Git", "url": "<repository>", "revision": "<commit>", "path": ""}}}\n'
            '  - or, if the authors publish no source at all: "is_metadata_only": true, with a comment that says so.\n'
            '    Its license then comes only from what the package declares.')


def source_curated(identifier, curations):
    """Whether a curation for the version gives the source of a package that the SBOM gives none for, or says that it
    has none."""
    return any({'vcs', 'source_artifact', 'is_metadata_only'} & curation.keys()
               for _, curation in curations_of(identifier, curations, version_too=True))


def curations_of(identifier, curations, version_too=False):
    """The curations of the package, as ORT applies them, each as the versions its id names and what it says: those
    of its type, namespace and name, and those of its namespace without a name. With version_too, only those that
    apply to the version of the package; an id without a version applies to all."""
    kind, namespace, name, version = identifier.split(':', 3)
    found = []
    for curation in curations:
        parts = (curation['id'].split(':', 3) + ['', '', ''])[:4]
        if parts[0] == kind and parts[1] == namespace and parts[2] in ('', name):
            found.append((parts[3], curation['curations']))
    return [(versions, c) for versions, c in found if not version_too or not versions or covers(versions, version)]


def mapped(identifier, name, curations):
    """Whether a curation says what the named license of the package is. Only a curation without a version counts,
    which applies to every version: the name itself changes when the license of a later version does."""
    return any(not versions and name in curation.get('declared_license_mapping', {})
               for versions, curation in curations_of(identifier, curations))


def unreviewed(identifier, curations):
    """The versions for which curations conclude the license of the package, when the version of the package is not
    among them; nothing when it is, or when no curation concludes its license."""
    concluded = [versions for versions, curation in curations_of(identifier, curations) if 'concluded_license' in curation]
    applying = curations_of(identifier, curations, version_too=True)
    return [] if any('concluded_license' in curation for _, curation in applying) else concluded


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
