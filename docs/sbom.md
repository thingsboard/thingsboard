# SBOMs

ThingsBoard describes every artifact it ships in a software bill of materials (SBOM): a list of the third-party
components inside it, with their versions, licenses and hashes. The SBOMs use the [CycloneDX](https://cyclonedx.org)
JSON format. They are also the input from which the third-party notices are made, so a component that is missing from an
SBOM is missing from the notices too: see [licensing-guide.md](licensing-guide.md).

## What gets an SBOM

| Artifact                                                                 | SBOM                                                                                           | Made by                                      |
|--------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------|
| The Spring Boot jar of each service                                      | `target/sbom/application.cdx.json`, and inside the jar at `META-INF/sbom/application.cdx.json` | the `jar` goal                               |
| The deb, rpm and Windows zip of those services and of `msa/web-report`   | next to the package: `target/<package file>.cdx.json`                                          | the `package` goal                           |
| The Angular UI (`ui-ngx`)                                                | inside the UI jar at `META-INF/sbom/ui-ngx.cdx.json`                                           | `tools/src/main/javascript/yarn-sbom.js`     |
| The Node.js services (`msa/web-ui`, `msa/js-executor`, `msa/web-report`) | `target/sbom/*.cdx.json`                                                                       | `yarn-sbom.js`                               |
| The Docker images                                                        | attached to the image in the registry, one per architecture                                    | the `image` goal, when the images are pushed |

The services are the modules that build a Spring Boot jar and declare the plugin in their `pom.xml`, such as
`application`, the transports and the integrations. The `jar`, `bundled`, `package`, `fonts`, `metadata` and `image`
goals belong to [sbom-maven-plugin](https://github.com/thingsboard/sbom-maven-plugin). The UI SBOM is also nested in
the SBOM of `application`, which serves the UI.

Library modules (`common/*`, `rest-client`, `netty-mqtt` and the like) get no SBOM of their own (except `common/report`,
which runs `fonts` for its bundled PDF fonts): they are described as components of the services that pack them.
`msa/web-ui` and `msa/js-executor` ship only as Docker images, so they have no package SBOMs.

## Building with SBOMs

SBOMs are made only on request:

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false
```

No Docker is needed. Node.js and Yarn come from the build, as for any build of the UI. The build needs network access
for the Node.js services, whose SBOMs read the packages of other platforms from the npm registry.

| Flag                                               | Effect on the SBOMs                                                                                                                                   |
|----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-Dsbom.skip=false`                                | Turns on the SBOMs of the jars, the packages and the npm modules                                                                                      |
| `-Dnotices.skip=false`                             | Also generates scoped third-party notices for each package, Docker image, and UI bundle (`/THIRD-PARTY-NOTICES.txt`); combine with `-Dsbom.skip=false` (needs Python 3.11+, and Docker only on scan cache misses) |
| `-Dskip.yarn=true`, `-Dskip.ui.build=true`         | `-Dskip.yarn=true` skips every npm SBOM, `-Dskip.ui.build=true` those of `ui-ngx`, `msa/web-ui` and `msa/web-report`; either also skips all packaging |
| `-Dpkg.skip=true` and the finer `pkg.skip.*` flags | Skip the packages and their SBOMs; `pkg.skip.bootjar` also skips the jar's SBOM                                                                       |

The SBOMs carry the time in `project.build.outputTimestamp` of the root `pom.xml`. It is set together with the release
version, so the same sources give the same SBOMs.

## How each SBOM is made

### Jars

The `jar` goal runs in `prepare-package`, after the jar is repackaged. It reads the libraries the jar packs, whatever
the dependency tree says, and writes one component per library, with its purl, hashes and the licenses from its pom. A
library in the jar that is not a Maven dependency of the module fails the build, unless it is declared under the
`libraries` parameter of the goal.

### Packages

What a package carries beside the jar is declared in the poms: the Windows service wrapper (WinSW) in the zip, and what
a module declares under `bundled-sbom` in its `pom.xml`
(see [Components declared in a pom](#components-declared-in-a-pom)). Before the packages are built, the `bundled` goal
writes it to `target/sbom/<package name>-<deb|rpm|zip>.bundled.cdx.json`, where the third-party notices read it. After
they are built, the `package` goal makes one SBOM per package from the SBOM of the jar and that file.

### npm modules

Each npm module has a `sbom` script in its `package.json`, which the Maven build runs. It calls `yarn-sbom.js`, which
reads `yarn.lock` and the installed packages, and lists:

| Module                                                     | What the SBOM lists                                                                                                                       |
|------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `ui-ngx` (`--angular-stats`)                               | Only the packages with code in the production bundle, the packages the styles take CSS from, and the assets and scripts in `angular.json` |
| `msa/web-report` packages (`sbom:executable`, `--pkg-log`) | Only the packages that `pkg` put into the executables                                                                                     |
| `msa/web-report` image (`sbom`)                            | The installed production dependencies                                                                                                     |
| `msa/web-ui`, `msa/js-executor` (`--all-platforms`)        | Every production dependency, including the optional ones of other platforms, read from their tarballs                                     |

A package that is installed but not bundled is not shipped, so it is not listed. `devDependencies` are not listed either,
except packages whose code ends up in a shipped artifact: bundles imported by the UI build, and `pkg` / `pkg-fetch` in
the `msa/web-report` executable SBOM (the executables embed the `pkg` prelude and the patched Node.js runtime built by
`pkg-fetch`). Every listed package must be in `yarn.lock`. The Node.js runtime and bundled Chromium are declared in
`msa/web-report/pom.xml`.

### Docker images

Image SBOMs are made at the release, by the build that pushes the multi-architecture images with `-Dsbom.skip=false`:
see the `image` goal in the [plugin's README](https://github.com/thingsboard/sbom-maven-plugin#image-goal). The image
SBOM combines what Syft finds in the image with the SBOM of the package or the npm module that the image installs. The
components of the base image are listed but left out of the notices: the base image ships its own license texts.

## Components that are not Maven or npm dependencies

Some shipped files come from neither Maven nor npm. They must be declared by hand, or they are in no SBOM.

### The gateway management extension

`application/src/main/data/resources/js_extensions/gateway-management-extension.js` is a prebuilt bundle, and
`application/src/main/sbom/gateway-management-extension.cdx.json` is its SBOM. The SBOM records the SHA-256 of the
bundle, and every build checks that they match. To update the extension, take both files from the same build of the
[gateway-management-extensions-dist](https://github.com/thingsboard/gateway-management-extensions-dist) repository and
commit them together. The bundle is committed byte for byte (`.gitattributes` marks it `-text`), so its hash does
not change.

### Components declared in a pom

A file that a package ships beside the jar is declared as a component of the `bundled-sbom` execution of the module,
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
[plugin's README](https://github.com/thingsboard/sbom-maven-plugin#bundled-goal).

The notices scan only Maven and npm packages. For any other component, give the license file that its vendor publishes
for it, at the address of that version, and the notices include that file as it is:

```xml
<externalReferences>
    <externalReference>
        <type>license</type>
        <url>https://raw.githubusercontent.com/nodejs/node/v${node.linux.version}/LICENSE</url>
    </externalReference>
</externalReferences>
```

When the vendor publishes no such file, keep one in this repository and add it under `[license_files]` in
`tools/src/main/notices/policy.toml`, by the purl of the component, or by its name when it has none. The LwM2M
object models have their notice generated on the fly from their XML headers (`tools/src/main/notices/notices/lwm2m.py`).
The Chromium of web-report has one for its Linux and Windows builds, its credits, with a section for each platform that
the notices of the other leave out: see `msa/web-report/chromium-credits/README.md`.

Non-Maven/non-npm components that carry both a declared license and copyright text in the SBOM (such as vendored fonts
from `sbom-maven-plugin:fonts`) are also included directly. Any other component without a license file or declared
license + copyright is skipped by the notices generator.

### Copied third-party code

Prefer a dependency to a copy. Code copied into our sources ends up inside our own jars or bundles, where no SBOM sees
it. If a copy cannot be avoided, keep its original license header and copyright lines, and add the file to the
`<excludes>` of `license-maven-plugin` in the root `pom.xml`, as `NetworkReceive.java` is: otherwise
`mvn license:format` puts our header on it. Say in the pull request where it comes from and under which license, and
expect a reviewer to decide how it is declared.

## When you change what ships

| Change                                                                                                             | What to do                                                                                                                                                                                                                 |
| ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Add, update or remove a Maven dependency                                                                           | Nothing for the SBOMs: they follow the jar. Run the notices check (or let CI check it) and add a curation if it stops.                                                                                                     |
| Add, update or remove an npm dependency                                                                            | Update `yarn.lock` with `yarn install` in the module (Yarn 1, which the build provides under the module's `target/`), and commit it. Run the notices check (or let CI check it).                                           |
| Import a package in `ui-ngx` for the first time, stop using one, or change the assets or scripts in `angular.json` | Run the notices check (or let CI check it): the UI SBOM lists only what the bundle contains.                                                                                                                               |
| Update the gateway extension                                                                                       | See [The gateway management extension](#the-gateway-management-extension), and update root `THIRD-PARTY-NOTICES` if its bundled dependencies change.                                                                       |
| Ship a new file that is not a dependency                                                                           | Declare it as a component of the module's `bundled-sbom`, with its license, and update root `THIRD-PARTY-NOTICES` if the third-party file is committed in Git.                                                             |
| Add a module that ships a Spring Boot jar or package                                                               | Set `sbom.bundled.phase` to `prepare-package` (`sbom.jar.phase` follows `sbom.bundled.phase` and `sbom.package.phase` follows `pkg.package.phase` automatically), and declare `sbom-maven-plugin` after `spring-boot-maven-plugin`, `gradle-maven-plugin`, and `maven-assembly-plugin`, as `transport/mqtt/pom.xml` does. |
| Copy third-party code                                                                                              | See [Copied third-party code](#copied-third-party-code), and update root `THIRD-PARTY-NOTICES` for third-party code committed in Git.                                                                                      |
| Change a Docker base image                                                                                         | Nothing: the image SBOM is made from the pushed image.                                                                                                                                                                     |

"Run the notices check" means: build with SBOMs and run `mvn -N exec:exec@notices-product`.
See [Checking the notices locally](licensing-guide.md#checking-the-notices-locally).

## Troubleshooting

| Message                                                                          | Meaning and fix                                                                                                                      |
|----------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `... which yarn.lock does not list`                                              | A package is installed but not locked. Run `yarn install` in the module and commit `yarn.lock`.                                      |
| `... names no file of an installed package; was the UI built with --stats-json?` | The UI was built without the stats that the SBOM reads. Build it with `yarn build:prod` or through Maven, which pass `--stats-json`. |
| `The executables do not contain the same packages`                               | The web-report executables of Linux and Windows differ. Build them from the same `node_modules`.                                     |
