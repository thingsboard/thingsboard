# SBOMs

ThingsBoard describes every artifact it ships in a software bill of materials (SBOM): a list of the third-party
components inside it, with their versions, licenses and hashes. The SBOMs use the
[CycloneDX](https://cyclonedx.org) 1.6 JSON format. They are also the input from which the third-party notices are
made, so a component that is missing from an SBOM is missing from the notices too: see [licensing.md](licensing.md).

## What gets an SBOM

| Artifact | SBOM | Made by |
|---|---|---|
| The Spring Boot jar of each service (`application`, `edqs`, `monitoring`, `report`, the transports, the integrations, `msa/vc-executor`) | `target/sbom/application.cdx.json`, and inside the jar at `META-INF/sbom/application.cdx.json` | the `jar` goal of [sbom-maven-plugin](https://github.com/thingsboard/sbom-maven-plugin) |
| The deb, rpm and Windows zip of those services and of `msa/web-report` | next to the package: `target/<package file>.cdx.json` | the `package` goal |
| The Angular UI (`ui-ngx`) | inside the UI jar at `META-INF/sbom/ui-ngx.cdx.json`, and nested in the SBOM of `application`, which serves the UI | `tools/src/main/javascript/yarn-sbom.js` |
| The Node.js services (`msa/web-ui`, `msa/js-executor`, `msa/web-report`) | `target/sbom/*.cdx.json` | `yarn-sbom.js` |
| The Docker images | attached to the image in the registry, one per architecture | the `image` goal, when the images are pushed |

Library modules (`common/*`, `rest-client`, `netty-mqtt` and the like) get no SBOM of their own: they are described as
components of the services that pack them. `msa/web-ui` and `msa/js-executor` ship only as Docker images, so they have
no package SBOMs.

## Building with SBOMs

SBOMs are made only on request:

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false
```

No Docker is needed. Node.js and Yarn come from the build, as for any build of the UI. The build needs network access
for the Node.js services, whose SBOMs read the packages of other platforms from the npm registry.

| Flag | Effect on the SBOMs |
|---|---|
| `-Dsbom.skip=false` | Turns on the SBOMs of the jars, the packages and the npm modules |
| `-Dnotices.skip=false` | Also makes the third-party notices of each package and puts them into it (needs Docker; see [licensing.md](licensing.md)) |
| `-Dskip.yarn=true`, `-Dskip.ui.build=true` | Skip the npm SBOMs, and with them all packaging |
| `-Dpkg.skip=true` and the finer `pkg.skip.*` flags | Skip the packages, and with them their SBOMs; `pkg.skip.bootjar` also skips the SBOM of the jar |

The SBOMs carry the time in `project.build.outputTimestamp` of the root `pom.xml`. It is set together with the release
version, so the same sources give the same SBOMs.

## How each SBOM is made

### Jars

The `jar` goal reads the libraries that the finished Spring Boot jar packs, and writes one component per library, with
its purl, hashes and the licenses from its pom. It runs in `prepare-package`, after the jar is repackaged, so the SBOM
lists exactly what the jar ships, whatever the dependency tree says. A library that is in the jar but is not a
dependency of the module fails the build.

### Packages

The `package` goal makes one SBOM per package from the SBOM of the jar, and adds what the package carries beside the
jar: the Windows service wrapper (WinSW) in the zip, and what a module declares under `package-sbom` in its `pom.xml`
(see [Components that are not Maven or npm dependencies](#components-that-are-not-maven-or-npm-dependencies)).

### npm modules

Each npm module has a `sbom` script in its `package.json`, which the Maven build runs. It calls `yarn-sbom.js`, which
reads `yarn.lock` and the installed packages, and lists:

| Module | What the SBOM lists |
|---|---|
| `ui-ngx` (`--angular-stats`) | Only the packages with code in the production bundle, the packages the styles take CSS from, and the assets and scripts in `angular.json`. A package that is installed but not bundled is not shipped, so it is not listed. |
| `msa/web-report` packages (`sbom:executable`, `--pkg-log`) | Only the packages that `pkg` put into the executables. The Node.js runtime inside them and the bundled Chromium are declared in `msa/web-report/pom.xml`. |
| `msa/web-report` image (`sbom`) | The installed production dependencies |
| `msa/web-ui`, `msa/js-executor` (`--all-platforms`) | Every production dependency, including the optional ones of other platforms, read from their tarballs and checked against `yarn.lock` |

devDependencies are not listed, except a package that the UI bundle actually contains. Every listed package must be in
`yarn.lock`.

### Docker images

Image SBOMs are made only by a build that pushes the multi-architecture images (the `push-docker-amd-arm-images` and
`push-lts-docker-amd-arm-images` profiles of `msa/pom.xml`) with `-Dsbom.skip=false`. After the push, the `image` goal
scans each architecture with [Syft](https://github.com/anchore/syft) and attaches the SBOM to the image with
[ORAS](https://oras.land). It needs Docker with buildx, `syft` and `oras` on the `PATH`, and a login to the registry.
This is part of the release, not of everyday work.

The image SBOM combines what Syft finds in the image with the SBOM of the package or the npm module that the image
installs. The components of the base image are listed but left out of the notices: the base image ships its own license
texts.

## Components that are not Maven or npm dependencies

Some shipped files come from neither Maven nor npm. They must be declared by hand, or they are in no SBOM.

### The gateway management extension

`application/src/main/data/resources/js_extensions/gateway-management-extension.js` is a prebuilt bundle, and
`application/src/main/sbom/gateway-management-extension.cdx.json` is its SBOM. The SBOM records the SHA-256 of the
bundle, and every build checks that they match. To update the extension, take both files from the same build of the
[gateway-management-extensions-dist](https://github.com/thingsboard/gateway-management-extensions-dist) repository and
commit them together. The bundle is committed byte for byte (`.gitattributes` marks it `-text`), because its hash must
not change.

### Components declared in a pom

A file that a package ships beside the jar is declared as a component of the `package-sbom` execution of the module,
for example in `application/pom.xml`:

```xml
<component>
    <type>library</type>
    <group>com.example</group>
    <name>example</name>
    <version>1.2.3</version>
    <purl>pkg:maven/com.example/example@1.2.3</purl>
    <license>MIT</license>
    <file>${project.build.directory}/path/inside/the/package</file>
</component>
```

`license` must be an SPDX license expression, such as `MIT` or `EPL-2.0 OR BSD-3-Clause`. Other types are `data` (for
example the LwM2M object models) and `cryptographic-asset` (a certificate, which must be X.509). A component can also
take its details from an SBOM of its own, as the gateway extension does. The full list of fields is in the
[plugin's README](https://github.com/thingsboard/sbom-maven-plugin#package-goal).

### Copied third-party code

Prefer a dependency to a copy. Code copied into our sources ends up inside our own jars or bundles, where no SBOM sees
it. If a copy cannot be avoided, keep its original license header and copyright lines, say in the pull request where it
comes from and under which license, and expect a reviewer to decide how it is declared.

## When you change dependencies

| Change | What to do |
|---|---|
| Add, update or remove a Maven dependency | Nothing for the SBOMs: they follow the jar. Regenerate the notices. |
| Add, update or remove an npm dependency | Update `yarn.lock` with `yarn install` in the module, and commit it. Regenerate the notices. |
| Update the gateway extension | Replace the bundle and its SBOM together, from the same build. |
| Ship a new file that is not a dependency | Declare it as a component of the module's `package-sbom`, with its license. |
| Copy third-party code | See [Copied third-party code](#copied-third-party-code). |
| Change a Docker base image | Nothing: the image SBOM is made from the pushed image. |

"Regenerate the notices" means: build with SBOMs, run the notices generator, and commit the root `THIRD-PARTY-NOTICES`.
A pull request that changes dependencies and not the notices fails its check. See [licensing.md](licensing.md).

## Troubleshooting

| Message | Meaning and fix |
|---|---|
| `...gateway-management-extension.cdx.json does not describe ...gateway-management-extension.js` | The bundle and its SBOM come from different builds. Take both from the same build. |
| `... which yarn.lock does not list` | A package is installed but not locked. Run `yarn install` in the module and commit `yarn.lock`. |
| `... names no file of an installed package; was the UI built with --stats-json?` | The UI was built without the stats that the SBOM reads. Build it through Maven, which passes `--stats-json`. |
| `The executables do not contain the same packages` | The web-report executables of Linux and Windows differ. Build them from the same `node_modules`. |
| A failed `jar` goal: a library that is no dependency of the project | The jar packs a library that Maven does not resolve for the module. Declare it as a dependency. |
| `Third-party notices: these SBOMs are not of the version ...` | An earlier build left SBOMs of another version. Build again from clean. |
