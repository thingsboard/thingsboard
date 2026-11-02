// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const fs = require('fs');
const path = require('path');
const { createRequire } = require('module');

const [output, rootType, rootPurl] = process.argv.slice(2);
if (!output || !['application', 'library'].includes(rootType) || !rootPurl) {
  throw new Error('Usage: node yarn-sbom.js <output file> <application|library> <root purl>');
}
const MODULE = process.cwd();
const OUTPUT = path.resolve(MODULE, output);

const moduleRequire = createRequire(path.join(MODULE, 'package.json'));
const { Contrib, Enums, Models, Serialize, Spec } = moduleRequire('@cyclonedx/cyclonedx-library');
const { PackageURL, PurlQualifierNames } = moduleRequire('packageurl-js');
const spdxExpressionParse = moduleRequire('spdx-expression-parse');
const lockfile = moduleRequire('@yarnpkg/lockfile');
const normalizePackageData = moduleRequire('normalize-package-data');

const DEFAULT_REGISTRY = /^https?:\/\/registry\.(?:npmjs\.org|yarnpkg\.com)(?:\/|$)/;
const GIT_URL = /^git[+:]|\.git$/;

const { Builders, Factories, Utils } = Contrib.FromNodePackageJson;
const componentBuilder = new Builders.ComponentBuilder(
  new Factories.ExternalReferenceFactory(), new Contrib.License.Factories.LicenseFactory(spdxExpressionParse));
const locked = lockedPackages();

const bom = new Models.Bom();
bom.metadata.supplier = new Models.OrganizationalEntity({ name: 'ThingsBoard, Inc.', url: new Set([new URL('https://thingsboard.io')]) });
bom.metadata.tools.components.add(componentBuilder.makeComponent(moduleRequire('@cyclonedx/cyclonedx-library/package.json')));
bom.metadata.properties.add(new Models.Property('cdx:reproducible', 'true'));

const rootManifest = readManifest(MODULE);
const rootId = PackageURL.fromString(rootPurl);
const root = new Models.Component(rootType, rootId.name, {
  bomRef: rootPurl,
  group: rootId.namespace,
  version: rootId.version,
  description: rootManifest.description,
  supplier: bom.metadata.supplier,
  purl: rootPurl
});
bom.metadata.component = root;

const byDir = new Map();
const byPurl = new Map();
const queue = productionDependencies(MODULE);
for (const dir of queue) {
  if (byDir.has(dir)) {
    continue;
  }
  const component = makeComponent(dir);
  const known = byPurl.get(component.purl);
  if (known) {
    known.properties.add(new Models.Property('cdx:npm:package:path', packagePath(dir)));
    byDir.set(dir, known);
  } else {
    byPurl.set(component.purl, component);
    byDir.set(dir, component);
    bom.components.add(component);
  }
  queue.push(...productionDependencies(dir));
}
for (const [dir, component] of byDir) {
  productionDependencies(dir).forEach(target => component.dependencies.add(byDir.get(target).bomRef));
}
productionDependencies(MODULE).forEach(target => root.dependencies.add(byDir.get(target).bomRef));

const serializer = new Serialize.JsonSerializer(new Serialize.JSON.Normalize.Factory(Spec.Spec1dot6));
fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
fs.writeFileSync(OUTPUT, `${serializer.serialize(bom, { sortLists: true, space: 2 })}\n`);
console.log(`Wrote ${OUTPUT} with ${byPurl.size} components`);

function productionDependencies(dir) {
  const manifest = readManifest(dir);
  const required = Object.keys(manifest.dependencies || {}).map(name => {
    const target = resolvePackageDir(dir, name);
    if (!target) {
      throw new Error(`${packagePath(dir) || rootManifest.name} depends on ${name}, which is not installed`);
    }
    return target;
  });
  const optional = Object.keys(manifest.optionalDependencies || {})
    .map(name => resolvePackageDir(dir, name))
    .filter(Boolean);
  return [...new Set([...required, ...optional])];
}

function makeComponent(dir) {
  const manifest = readManifest(dir);
  const declaredVersion = manifest.version;
  normalizePackageData(manifest);
  manifest.version = declaredVersion;
  const component = componentBuilder.makeComponent(manifest);
  if (!component) {
    throw new Error(`Cannot build an SBOM component from ${packagePath(dir)}/package.json`);
  }
  component.licenses.forEach(license => {
    license.acknowledgement = Enums.LicenseAcknowledgement.Declared;
  });
  component.properties.add(new Models.Property('cdx:npm:package:path', packagePath(dir)));
  const lock = locked.get(`${manifest.name}@${manifest.version}`);
  if (!lock) {
    throw new Error(`${rootManifest.name} depends on ${manifest.name}@${manifest.version}, which yarn.lock does not list`);
  }
  const [url, revision] = lock.resolved.split('#');
  const comment = 'as detected from yarn.lock property "resolved"';
  let qualifiers;
  if (GIT_URL.test(url)) {
    qualifiers = { [PurlQualifierNames.VcsUrl]: `${url.startsWith('git') ? url : `git+${url}`}@${revision}` };
    component.externalReferences.add(new Models.ExternalReference(lock.resolved, Enums.ExternalReferenceType.VCS, { comment }));
  } else {
    qualifiers = DEFAULT_REGISTRY.test(url) ? {} : { [PurlQualifierNames.DownloadUrl]: url };
    const dist = new Models.ExternalReference(url, Enums.ExternalReferenceType.Distribution, { comment });
    if (lock.integrity) {
      dist.hashes.set(...Utils.parsePackageIntegrity(lock.integrity));
    }
    component.externalReferences.add(dist);
  }
  component.purl = new PackageURL('npm', component.group, component.name, manifest.version, qualifiers, undefined).toString();
  component.bomRef.value = component.purl;
  return component;
}

function lockedPackages() {
  const { type, object } = lockfile.parse(fs.readFileSync(path.join(MODULE, 'yarn.lock'), 'utf8'));
  if (type !== 'success') {
    throw new Error(`Cannot parse yarn.lock: ${type}`);
  }
  const result = new Map();
  for (const [spec, entry] of Object.entries(object)) {
    const alias = /^(?:@[^/@]+\/)?[^/@]+@npm:((?:@[^/@]+\/)?[^/@]+)@/.exec(spec);
    const name = alias ? alias[1] : spec.slice(0, spec.indexOf('@', 1));
    result.set(`${name}@${entry.version}`, entry);
  }
  return result;
}

function resolvePackageDir(from, name) {
  for (let dir = from; ; dir = path.dirname(dir)) {
    const candidate = path.join(dir, 'node_modules', name);
    if (fs.existsSync(path.join(candidate, 'package.json'))) {
      return candidate;
    }
    if (dir === MODULE || dir === path.dirname(dir)) {
      return undefined;
    }
  }
}

function packagePath(dir) {
  return path.relative(MODULE, dir).split(path.sep).join('/');
}

function readManifest(dir) {
  return JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8'));
}
