# Licensing

> [!NOTE]
> This guide is not a license and not legal advice. Where it differs from [LICENSE](../LICENSE), the license texts of
> the third-party components or the Contributor License Agreement, those apply.

This guide covers the license of ThingsBoard itself, the license headers of its files, and the third-party notices it
ships. How the components are listed is in [sbom.md](sbom.md).

## The project's license

Since version 4.4, ThingsBoard is licensed under the Business Source License 1.1 (BUSL-1.1): see [LICENSE](../LICENSE).
Each release converts to the Apache License 2.0 four years after it is published. Versions of this repository released
before 4.4 remain under the Apache License 2.0.

The code that comes from those versions, ThingsBoard Community Edition, stays under the Apache License 2.0. Its text
is not kept apart: the Apache-2.0 section of [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES) opens with ThingsBoard
Community Edition and ends with the license text. The packages and images carry `LICENSE` next to their notices.

Contributions are accepted under the Contributor License Agreement: see
[CONTRIBUTING.md](../CONTRIBUTING.md#contributor-license-agreement).

## File headers

Every source file starts with an SPDX header. Which one depends on where the file comes from. The tooling calls the
Apache-2.0 history of ThingsBoard before 4.4 "CE".

| Header        | For                                   |
|---------------|---------------------------------------|
| `busl`        | a file that does not come from CE     |
| `ce-modified` | a CE file that has been changed since |
| `apache`      | a CE file that is unchanged           |

`busl`:

```
SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
SPDX-License-Identifier: BUSL-1.1
```

`ce-modified`:

```
SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
```

`apache`:

```
SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
SPDX-License-Identifier: Apache-2.0
```

The comment style follows the file type, for example `//` lines in Java and TypeScript, an XML comment in `pom.xml`
and `#` lines in YAML and shell scripts. Do not add your own copyright or author lines.

- Run `mvn license:format`. It leaves the three headers above alone and gives every other file the `busl` header, also
  a file that carries a third-party header (see [Copied third-party code](sbom.md#copied-third-party-code)).
- Then, for every file you add or change, run the provenance check, which knows from the Git history which header each
  file needs:

  ```bash
  ./check-license-headers.sh --since origin/<target branch>         # the files your branch adds or changes
  ./check-license-headers.sh --since origin/<target branch> --fix   # restamp the files it can decide
  ```

  Use `upstream/<target branch>` in a fork. The check needs the full Git history of the clone (`git fetch --unshallow`
  on a shallow one).

When the check reports "Needs a decision" for a file that resembles a CE file at another path, or whose path existed in
CE once, put the right header on it and record the decision in `tools/src/main/python/license-headers/curations.json`.
How is in the [check's README](../tools/src/main/python/license-headers/README.md#add-a-curation).

Pull requests into `master` and the release branches run the same check, and fail with a report of the files to fix.

## Third-party notices

The licenses of the third-party components ThingsBoard ships require us to pass on their license texts and copyright
notices. Each package (`.deb`, `.rpm`, `.zip`) and Docker image carries a `THIRD-PARTY-NOTICES` file generated from its
own SBOM during a release build (`-Dsbom.skip=false -Dnotices.skip=false`), and the UI bundle serves its notices at
`/THIRD-PARTY-NOTICES.txt`. Each generated file lists every bundled component grouped by license, with:

- the copyright lines found in its sources, and the text of each license;
- the NOTICE files that Apache-2.0 components ask us to pass on;
- where the source code is, for the licenses that require it (EPL, CDDL, MPL);
- a written offer of the source code, for the LGPL and GPL components.

The root [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES) in Git covers only the third-party files committed directly in
this source repository (such as vendored fonts, copied sprites, and `gateway-management-extension.js`). External Maven,
npm, Chromium, Node.js, and OS dependencies are not stored in Git and are handled at build time.

## Checking the notices locally

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=true
mvn -N exec:exec@notices-product
```

This writes the combined product notices to `target/THIRD-PARTY-NOTICES` without modifying tracked files, and stops on
any policy, classification, or curation problem. Build the whole project without `-pl`, `-Dpkg.skip=true`,
`-Dskip.yarn=true`, or `-Dskip.ui.build=true` to check every module's SBOM.

The generator needs Python 3.11 or later (standard library only). Docker is only needed when a new package is not yet
in the shared scan cache — a run with nothing new to scan runs in pure Python in a couple of seconds. The settings of
the generator are in [its README](../tools/src/main/notices/README.md).

## The pull-request check

CI checks a pull request when it comes from a member of the ThingsBoard organization and its merge changes any path
that can affect the notices (see `CI_WATCHED_PATHS` in `tools/src/main/notices/notices/cli.py` — build/dependency files,
UI sources/config/patches/esbuild, bundled fonts, LwM2M models, gateway extension, Chromium/web-report files,
`yarn-sbom.js`, and `tools/src/main/notices/`).
It builds the merge with SBOMs and runs `mvn -N exec:exec@notices-product` to verify every shipped dependency against
the license policy and curations. Other pull requests from members pass in seconds. For pull requests from outside the
organization, a maintainer runs the check before merging.

When the generator stops (next section), the check fails with its message telling you which decision to record in
`tools/src/main/notices/`.

The main branches are also checked every night. This catches what a pull request alone cannot, for example two pull
requests that each passed independently before merging, and uploads any newly scanned packages to the shared
Artifactory cache.

## When the generator stops

The generator stops rather than guess. Every stop starts with `Third-party notices:`, names the packages, and says what
to do — usually which decision to record in one of two TOML files under `tools/src/main/notices/`. If the shared scan
cache cannot be reached, check the network or run with `NOTICES_SCAN_CACHE_URL=none` to scan locally.

| File             | Records                                                                                                                               |
|------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `policy.toml`    | License categories, same-category tie-breakers, global non-SPDX aliases, display titles, excluded paths, repo-local license files, and copyright garbage |
| `curations.toml` | Per-package exceptions: source overrides / `metadata_only`, POM license fixes, file-scoped `ignore_findings`, version-ranged `concluded_license`, and custom license texts |

Every curation carries a `comment` that says where the information comes from, so that a reviewer can check it.

## Rules for license decisions

- **License choices are resolved automatically by category priority** (`nothing > notice > source-link > written-offer > forbidden`),
  using `same_category_preference` in `policy.toml` to break ties within the same tier. Unranked ties stop the generator.
- **Prefer file-scoped `ignore_findings` over `concluded_license`.** Ignoring only a specific false-positive file keeps
  every other file of the package actively scanned across version upgrades.
- **A `concluded_license` covers only the versions someone read.** Its `id` in `curations.toml` must name a version or
  a range such as `@[1.0,2.0[`. Widen the range only after reading the license of the new version.
- **Unambiguous non-SPDX license names** belong in `[license_aliases]` of `policy.toml`; ambiguous ones (e.g. unversioned
  `"LGPL"`) are mapped per package in `curations.toml`.
- **A new license needs a classification before it ships.** If you are not sure what a license requires, ask before
  adding it to `[licenses]` in `policy.toml`: a copyleft license may need legal review.
- **Some licenses cannot ship at all.** Strong copyleft (GPL and AGPL without an exception, SSPL, EUPL and the like)
  would put ThingsBoard itself under that license, and some licenses forbid commercial use (CC-BY-NC, Commons Clause,
  Elastic-2.0, BUSL-1.1). They are classified `forbidden`, and the generator stops on any shipped package under one:
  the dependency has to go. Only a misread scan is fixed with a file-scoped `ignore_findings` in `curations.toml`.
  License choices are resolved first, so `AGPL-3.0-only OR Apache-2.0` automatically selects `Apache-2.0`.
- **A package without a license is not shipped** until this is raised and resolved.
