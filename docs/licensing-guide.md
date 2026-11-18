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
notices. The root [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES) lists every such component, grouped by license, with:

- the copyright lines found in its sources, and the text of each license;
- the NOTICE files that Apache-2.0 components ask us to pass on;
- where the source code is, for the licenses that require it (EPL, CDDL, MPL);
- a written offer of the source code, for the LGPL and GPL components.

Each package, and each image of the Node.js services, also carries the notices of exactly what it contains. The same
generator makes them from its own SBOM during a release build (`-Dsbom.skip=false -Dnotices.skip=false`).

The file is generated: never edit it by hand. Commit it whenever you change what the product ships.

## Regenerating the notices

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false   # the SBOMs, see sbom.md
mvn -N exec:exec@notices-product                        # the notices, into THIRD-PARTY-NOTICES
```

Build the whole project, without `-pl`, `-Dpkg.skip*`, `-Dskip.yarn` or `-Dskip.ui.build`: the generator reads every
SBOM it finds, and a build that skipped some gives an incomplete file without an error.

The generator needs Linux or macOS (WSL on Windows), Docker and Python 3.11 or later. It runs the
[OSS Review Toolkit](https://oss-review-toolkit.org) (ORT) in a container, which scans the source of every component
for licenses and copyright lines. Its logs are in `target/notices-product/`.

A package that is new to the product is scanned on your machine the first time, which takes minutes. The scans that
others have made are downloaded first, from an archive that CI keeps up to date, so a run with nothing new to scan takes
about a minute. The settings of the generator are in [its README](../tools/src/main/notices/README.md).

## The pull-request check

CI checks a pull request when it comes from a member of the ThingsBoard organization and its merge changes a
`pom.xml`, `package.json` or `yarn.lock`, the notices tooling or `THIRD-PARTY-NOTICES`. It builds the merge with SBOMs,
regenerates the notices and compares them with the committed file. Other pull requests from members pass in seconds.
For pull requests from outside the organization, a maintainer makes sure the file is up to date before merging.

When the files differ, the check fails and attaches the correct file to the build as `THIRD-PARTY-NOTICES`. Download
it, look at what changed, and commit it. When the generator stops instead (next section), the check fails with its
message and attaches the logs.

The main branches are also checked every night. This catches what a pull request alone cannot, for example two pull
requests that were each correct before the other one was merged.

## When the generator stops

The generator stops rather than guess. Most stops start with `Third-party notices:`, name the packages, and say what to
do: usually, which decision to record in which of these files under `tools/src/main/notices/`. If the scan cache cannot
be downloaded (`Scan cache: … cannot be read`), check the network. `NOTICES_SCAN_CACHE_URL=none` runs without it, but
then every package is scanned, which takes hours.

| File                                     | Records                                                                                                |
|------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `license-choices.yml`                    | The license we take where a package offers several                                                     |
| `ort-config/license-classifications.yml` | What the notices owe for each license                                                                  |
| `ort-config/curations/curations.json`    | What a package states wrongly or not at all: its license for the versions someone has read, its source |
| `ort-config/resolutions.yml`             | Scan errors that do not affect the notices                                                             |
| `ort-config/copyright-garbage.yml`       | Copyright lines that cannot be about any component's code, such as the copyright of a license text     |

Every entry carries a comment that says where the information comes from, so that a reviewer can check it.

## Rules for license decisions

- **A license choice takes the most permissive license offered**, and among copyleft licenses EPL, CDDL or MPL before
  LGPL or GPL. A rule applies to every package that offers the same choice.
- **A license set by hand covers only the versions someone read.** Its entry in `curations.json` names a version or a
  range such as `[1.0,2.0[`. Widen the range only after reading the license of the new version.
- **A license-name mapping has no version**, so it holds wherever a package names its license in the same words.
- **A new license needs a classification before it ships.** If you are not sure what a license requires, ask before
  adding it: a copyleft license may need legal review.
- **A package without a license is not shipped** until this is raised and resolved.
- **Do not change the ScanCode options** in `ort-config/config.yml`: stored scans made with other options are not
  reused, and everything would be scanned again.
