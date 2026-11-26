// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { createRequire } = require('module');

const ALL_PLATFORMS = '--all-platforms';
const args = process.argv.slice(2);
const allPlatforms = args.includes(ALL_PLATFORMS);
const [output, rootType, rootPurl] = args.filter(arg => arg !== ALL_PLATFORMS);
if (!output || !['application', 'library'].includes(rootType) || !rootPurl) {
  throw new Error(`Usage: node yarn-sbom.js <output file> <application|library> <root purl> [${ALL_PLATFORMS}]`);
}
const MODULE = process.cwd();
const OUTPUT = path.resolve(MODULE, output);
const LOCKED = 'yarn.lock:';

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
const lock = parseLockfile();
const locked = lockedPackages();
const notInstalled = new Map();

main().catch(error => {
  console.error(error);
  process.exit(1);
});

async function main() {
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

  const edges = new Map();
  const queue = [MODULE];
  for (const node of queue) {
    if (edges.has(node)) {
      continue;
    }
    const dependencies = dependenciesOf(node);
    edges.set(node, dependencies);
    queue.push(...dependencies.map(dependency => dependency.target));
  }

  const byNode = new Map();
  const byPurl = new Map();
  for (const node of edges.keys()) {
    if (node === MODULE) {
      continue;
    }
    const component = node.startsWith(LOCKED) ? await lockedComponent(node) : makeComponent(readManifest(node), node);
    const known = byPurl.get(component.purl);
    if (known) {
      if (!node.startsWith(LOCKED)) {
        known.properties.add(new Models.Property('cdx:npm:package:path', packagePath(node)));
      }
      byNode.set(node, known);
    } else {
      byPurl.set(component.purl, component);
      byNode.set(node, component);
      bom.components.add(component);
    }
  }
  for (const [node, dependencies] of edges) {
    const from = node === MODULE ? root : byNode.get(node);
    dependencies.forEach(dependency => from.dependencies.add(byNode.get(dependency.target).bomRef));
  }
  if (allPlatforms) {
    const required = new Set();
    const pending = [MODULE];
    while (pending.length > 0) {
      const node = pending.pop();
      for (const dependency of edges.get(node)) {
        const component = byNode.get(dependency.target);
        if (!dependency.optional && !required.has(component)) {
          required.add(component);
          pending.push(dependency.target);
        }
      }
    }
    byPurl.forEach(component => {
      if (!required.has(component)) {
        component.scope = Enums.ComponentScope.Optional;
      }
    });
  }

  const serializer = new Serialize.JsonSerializer(new Serialize.JSON.Normalize.Factory(Spec.Spec1dot6));
  fs.mkdirSync(path.dirname(OUTPUT), { recursive: true });
  fs.writeFileSync(OUTPUT, `${serializer.serialize(bom, { sortLists: true, space: 2 })}\n`);
  console.log(`Wrote ${OUTPUT} with ${byPurl.size} components${notInstalled.size > 0 ? `, ${notInstalled.size} of them optional packages for other platforms` : ''}`);
}

function dependenciesOf(node) {
  if (node.startsWith(LOCKED)) {
    return [];
  }
  const manifest = readManifest(node);
  const required = Object.keys(manifest.dependencies || {}).map(name => {
    const target = resolvePackageDir(node, name);
    if (!target) {
      throw new Error(`${packagePath(node) || manifest.name} depends on ${name}, which is not installed`);
    }
    return { target, optional: false };
  });
  const optional = Object.entries(manifest.optionalDependencies || {}).flatMap(([name, range]) => {
    const target = resolvePackageDir(node, name);
    if (target) {
      return [{ target, optional: true }];
    }
    return allPlatforms ? [{ target: notInstalledNode(name, range), optional: true }] : [];
  });
  const seen = new Set();
  return [...required, ...optional].filter(dependency => !seen.has(dependency.target) && seen.add(dependency.target));
}

function notInstalledNode(name, range) {
  const entry = lock[`${name}@${range}`];
  if (!entry) {
    throw new Error(`yarn.lock does not list the optional dependency ${name}@${range}`);
  }
  const node = `${LOCKED}${name}@${entry.version}`;
  notInstalled.set(node, { name, entry });
  return node;
}

async function lockedComponent(node) {
  const { name, entry } = notInstalled.get(node);
  const manifest = await manifestFromTarball(name, entry);
  if (manifest.name !== name || manifest.version !== entry.version) {
    throw new Error(`The tarball of ${name}@${entry.version} contains ${manifest.name}@${manifest.version}`);
  }
  const dependencies = Object.keys({ ...manifest.dependencies, ...manifest.optionalDependencies });
  if (dependencies.length > 0) {
    throw new Error(`${name}@${entry.version} is not installed on this platform and has dependencies of its own (${dependencies.join(', ')}), which cannot be resolved without installing it`);
  }
  return makeComponent(manifest, null);
}

async function manifestFromTarball(name, entry) {
  const url = entry.resolved.split('#')[0];
  if (!entry.integrity) {
    throw new Error(`yarn.lock gives no integrity for ${name}@${entry.version}, so its tarball cannot be verified`);
  }
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Cannot download ${url}: HTTP ${response.status}`);
  }
  const tarball = Buffer.from(await response.arrayBuffer());
  const verified = entry.integrity.split(/\s+/).some(integrity => {
    const [algorithm, expected] = integrity.split(/-(.*)/s);
    return crypto.getHashes().includes(algorithm) && crypto.createHash(algorithm).update(tarball).digest('base64') === expected;
  });
  if (!verified) {
    throw new Error(`The tarball ${url} does not match the integrity that yarn.lock gives for ${name}@${entry.version}`);
  }
  const tar = zlib.gunzipSync(tarball);
  for (let offset = 0; offset + 512 <= tar.length;) {
    const header = tar.subarray(offset, offset + 512);
    if (header.every(byte => byte === 0)) {
      break;
    }
    const prefix = field(header, 345, 155);
    const fileName = prefix ? `${prefix}/${field(header, 0, 100)}` : field(header, 0, 100);
    const size = parseInt(field(header, 124, 12).trim() || '0', 8);
    const type = String.fromCharCode(header[156]);
    offset += 512;
    if ((type === '0' || type === '\0') && /^[^/]+\/package\.json$/.test(fileName)) {
      return JSON.parse(tar.subarray(offset, offset + size).toString('utf8'));
    }
    offset += Math.ceil(size / 512) * 512;
  }
  throw new Error(`The tarball ${url} has no package.json`);
}

function field(header, start, length) {
  const bytes = header.subarray(start, start + length);
  const end = bytes.indexOf(0);
  return bytes.subarray(0, end < 0 ? bytes.length : end).toString('utf8');
}

function makeComponent(manifest, dir) {
  const declaredVersion = manifest.version;
  normalizePackageData(manifest);
  manifest.version = declaredVersion;
  const component = componentBuilder.makeComponent(manifest);
  if (!component) {
    throw new Error(`Cannot build an SBOM component from ${dir ? `${packagePath(dir)}/package.json` : `the package.json of ${manifest.name}@${manifest.version}`}`);
  }
  component.licenses.forEach(license => {
    license.acknowledgement = Enums.LicenseAcknowledgement.Declared;
  });
  if (dir) {
    component.properties.add(new Models.Property('cdx:npm:package:path', packagePath(dir)));
  }
  const entry = locked.get(`${manifest.name}@${manifest.version}`);
  if (!entry) {
    throw new Error(`${readManifest(MODULE).name} depends on ${manifest.name}@${manifest.version}, which yarn.lock does not list`);
  }
  const [url, revision] = entry.resolved.split('#');
  const comment = 'as detected from yarn.lock property "resolved"';
  let qualifiers;
  if (GIT_URL.test(url)) {
    qualifiers = { [PurlQualifierNames.VcsUrl]: `${url.startsWith('git') ? url : `git+${url}`}@${revision}` };
    component.externalReferences.add(new Models.ExternalReference(entry.resolved, Enums.ExternalReferenceType.VCS, { comment }));
  } else {
    qualifiers = DEFAULT_REGISTRY.test(url) ? {} : { [PurlQualifierNames.DownloadUrl]: url };
    const dist = new Models.ExternalReference(url, Enums.ExternalReferenceType.Distribution, { comment });
    if (entry.integrity) {
      dist.hashes.set(...Utils.parsePackageIntegrity(entry.integrity));
    }
    component.externalReferences.add(dist);
  }
  component.purl = new PackageURL('npm', component.group, component.name, manifest.version, qualifiers, undefined).toString();
  component.bomRef.value = component.purl;
  return component;
}

function parseLockfile() {
  const { type, object } = lockfile.parse(fs.readFileSync(path.join(MODULE, 'yarn.lock'), 'utf8'));
  if (type !== 'success') {
    throw new Error(`Cannot parse yarn.lock: ${type}`);
  }
  return object;
}

function lockedPackages() {
  const result = new Map();
  for (const [spec, entry] of Object.entries(lock)) {
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
