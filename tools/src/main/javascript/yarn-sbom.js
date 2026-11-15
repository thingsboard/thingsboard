// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { createRequire } = require('module');
const { fileURLToPath } = require('url');
const { parseArgs } = require('util');

const USAGE = 'Usage: node yarn-sbom.js <output file> <application|library> <root purl> [--all-platforms | --angular-stats <stats.json of the Angular build>]';
const { values: { 'all-platforms': allPlatforms, 'angular-stats': angularStats }, positionals: [output, rootType, rootPurl, ...unexpected] } = parseArgs({
  allowPositionals: true,
  options: { 'all-platforms': { type: 'boolean' }, 'angular-stats': { type: 'string' } }
});
if (!output || !['application', 'library'].includes(rootType) || !rootPurl || unexpected.length > 0 || (allPlatforms && angularStats)) {
  throw new Error(USAGE);
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
const SASS_FILE = /\.(?:scss|sass)$/;

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

  // With --angular-stats the SBOM lists what the build put into its output, not what is installed
  const contained = angularStats ? bundledPackages() : undefined;
  const edges = new Map();
  const queue = [MODULE, ...(contained || [])];
  for (const node of queue) {
    if (edges.has(node)) {
      continue;
    }
    const dependencies = dependenciesOf(node);
    edges.set(node, dependencies);
    queue.push(...dependencies.map(dependency => dependency.target));
  }
  if (contained) {
    leaveOnly(contained, edges);
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
    dependencies.map(dependency => byNode.get(dependency.target)).filter(to => to !== from).forEach(to => from.dependencies.add(to.bomRef));
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

/**
 * The installed packages with a file in the output of the production build of Angular:
 * - what the bundler put into a script or a stylesheet, as the statistics of the build tell;
 * - what the Sass files of the build take their CSS from: Sass reads the files that a Sass file loads itself, so the
 *   statistics name only the Sass file;
 * - the assets and scripts of angular.json, which the build copies or adds as they are: the statistics do not name them.
 */
function bundledPackages() {
  const files = new Set();
  const stats = JSON.parse(fs.readFileSync(path.resolve(MODULE, angularStats), 'utf8'));
  for (const outputFile of Object.values(stats.outputs)) {
    for (const [input, { bytesInOutput }] of Object.entries(outputFile.inputs || {})) {
      if (bytesInOutput > 0) {
        // A file that a stylesheet refers to, a font or an image, is named with this prefix
        files.add(input.replace(/^angular:css-resource:/, ''));
      }
    }
  }
  if (packagesOf(files, angularStats).size === 0) {
    throw new Error(`${angularStats} names no file of an installed package; was the UI built with --stats-json?`);
  }
  const projects = Object.values(JSON.parse(fs.readFileSync(path.join(MODULE, 'angular.json'), 'utf8')).projects);
  if (projects.length !== 1) {
    throw new Error(`angular.json has ${projects.length} projects; the statistics do not tell which of them was built`);
  }
  const { options, configurations = {} } = projects[0].architect.build;
  // A list of the production configuration replaces the list of the options
  const built = { ...options, ...configurations.production };
  sassSources([...files].filter(file => SASS_FILE.test(file)), built.stylePreprocessorOptions).forEach(source => files.add(source));
  for (const entry of [...(built.assets || []), ...(built.scripts || [])]) {
    files.add(typeof entry === 'string' ? entry : entry.input);
  }
  return packagesOf(files, 'The output of the Angular build');
}

/**
 * The files that the CSS of the Sass files comes from. Each Sass file is compiled again, with the Sass compiler and
 * the load paths of the build, and the source map of the result names the files that gave a rule to it. A file that
 * only gives variables, functions or mixins that nothing uses gives no CSS and is not named.
 */
function sassSources(sassFiles, { includePaths = [], sass: { silenceDeprecations = [] } = {} } = {}) {
  const sass = createRequire(moduleRequire.resolve('@angular/build/package.json'))('sass');
  const loadPaths = includePaths.map(includePath => path.resolve(MODULE, includePath));
  const sources = new Set();
  for (const sassFile of sassFiles) {
    let compiled;
    try {
      compiled = sass.compile(path.resolve(MODULE, sassFile), { loadPaths, sourceMap: true, silenceDeprecations, logger: sass.Logger.silent });
    } catch (error) {
      throw new Error(`Cannot compile ${sassFile}, so the SBOM cannot tell which packages its CSS comes from: ${error.message}`);
    }
    compiled.sourceMap.sources.filter(source => source.startsWith('file:')).forEach(source => sources.add(fileURLToPath(source)));
  }
  return sources;
}

/**
 * The directories of the installed packages that the files belong to; a file outside node_modules belongs to none.
 */
function packagesOf(files, origin) {
  const packages = new Set();
  for (const file of files) {
    const parts = path.normalize(file).split(path.sep);
    const index = parts.lastIndexOf('node_modules');
    if (index < 0) {
      // A name that holds node_modules, though not as a directory of its path, is a name this function cannot read
      if (file.includes('node_modules')) {
        throw new Error(`${origin} contains ${file}, and its package cannot be told from that name`);
      }
      continue;
    }
    const length = index + (parts[index + 1]?.startsWith('@') ? 3 : 2);
    // A directory that holds packages, node_modules or a scope, does not tell which of them the build takes
    if (!parts[length - 1]) {
      throw new Error(`${origin} contains ${file}, which is a directory of packages, not a package`);
    }
    const dir = path.resolve(MODULE, parts.slice(0, length).join(path.sep));
    if (!packages.has(dir) && !fs.existsSync(path.join(dir, 'package.json'))) {
      throw new Error(`${origin} contains ${file}, which belongs to no installed package`);
    }
    packages.add(dir);
  }
  return packages;
}

/**
 * Takes the packages that are not contained out of the graph: what depended on such a package depends on what that
 * package depended on. A contained package that nothing leads to any more, such as a development dependency that
 * the application imports, hangs on the module itself.
 */
function leaveOnly(contained, edges) {
  const kept = node => node === MODULE || contained.has(node);
  const reached = new Set();
  for (const node of [...edges.keys()].filter(kept)) {
    const targets = new Set();
    const followed = new Set();
    const pending = [...edges.get(node)];
    while (pending.length > 0) {
      const { target } = pending.pop();
      if (kept(target)) {
        targets.add(target);
      } else if (!followed.has(target)) {
        followed.add(target);
        pending.push(...edges.get(target));
      }
    }
    edges.set(node, [...targets].map(target => ({ target, optional: false })));
    targets.forEach(target => reached.add(target));
  }
  [...edges.keys()].filter(node => !kept(node)).forEach(node => edges.delete(node));
  const direct = edges.get(MODULE);
  [...contained].filter(node => !reached.has(node)).forEach(target => direct.push({ target, optional: false }));
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
    // What follows the address of a tarball in yarn.lock is its SHA-1
    if (/^[0-9a-f]{40}$/.test(revision)) {
      dist.hashes.set(Enums.HashAlgorithm['SHA-1'], revision);
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
