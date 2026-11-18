# Licensing

> [!NOTE]
> This document explains, for contributors, how licensing works in this repository. It is not a license and not
> legal advice. The terms that apply are those of [LICENSE](../LICENSE), of the license texts of the third-party
> components, and of the Contributor License Agreement. Where this document differs from them, they apply.

This document covers the license of ThingsBoard itself, the license headers of its files, and the third-party notices
that it ships for the components it contains. How the components are listed in the first place is in
[sbom.md](sbom.md).

## The project's license

Since version 4.4, ThingsBoard is licensed under the Business Source License 1.1 (BUSL-1.1): see [LICENSE](../LICENSE).
Each release converts to the Apache License 2.0 four years after it is published. Versions released before 4.4 remain
under the Apache License 2.0.

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

- A new file: run `mvn license:format`, which gives every file without a header the `busl` header.
- A changed file: run the provenance check, which knows from the Git history which header each file needs:

  ```bash
  ./check-license-headers.sh --since origin/<target branch>   # the files your branch changes
  ./check-license-headers.sh --fix                            # restamp the files it can decide
  ```

  It needs the full Git history of the clone (`git fetch --unshallow` on a shallow one).

- "Needs a decision": the check cannot decide a file that resembles a CE file at another path, or whose path existed in
  CE once. Put the right header on it and record the decision in `tools/src/main/python/license-headers/curations.json`.
  How is in the [check's README](../tools/src/main/python/license-headers/README.md#add-a-curation).

Pull requests into the release branches run the same check, and fail with a report of the files to fix.

## Third-party notices

ThingsBoard ships third-party components under their own licenses, which require us to pass on their license texts
and copyright notices. The root [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES) lists every third-party component the
product ships, grouped by license, with:

- the copyright lines found in its sources, and the text of each license;
- the NOTICE files that Apache-2.0 components ask us to pass on;
- where the source code is, for the licenses that require it (EPL, CDDL, MPL);
- a written offer of the source code, for the LGPL and GPL components.

Each package and each image of the Node.js services also carries the notices of exactly what it contains, made from its
own SBOM by the same generator during a release build (`-Dsbom.skip=false -Dnotices.skip=false`).

The file is generated: never edit it by hand. Commit it whenever you change what the product ships.

## Regenerating the notices

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false   # the SBOMs, see sbom.md
mvn -N exec:exec@notices-product                        # the notices, into THIRD-PARTY-NOTICES
```

The generator needs Docker and Python 3.11 or later. It runs the
[OSS Review Toolkit](https://oss-review-toolkit.org) (ORT) in a container, which scans the source of every component
for licenses and copyright lines. Its logs are in `target/notices-product/`.

A scan of a package takes minutes, so the results are kept: in `~/.ort/scanner`, and in a shared archive that every run
downloads first. A run in which nothing new needs scanning takes about a minute. A package that is new to the product is
scanned on your machine the first time. Only CI adds new scans to the shared archive.

| Environment variable       | Default                  | Effect                                    |
|----------------------------|--------------------------|-------------------------------------------|
| `NOTICES_ORT_HOME`         | `~/.ort`                 | Where ORT keeps its scans and downloads   |
| `NOTICES_SCAN_CACHE_URL`   | the shared archive       | Another archive, or `none` for no archive |
| `NOTICES_MAVEN_REPOSITORY` | Maven's local repository | Where the jars are read from              |

## The pull-request check

A pull request from a member of the ThingsBoard organization whose merge changes a `pom.xml`, `package.json` or
`yarn.lock`, the notices tooling or `THIRD-PARTY-NOTICES` is checked: CI builds the merge with SBOMs, regenerates the
notices and compares them with the committed file. For other pull requests, a maintainer makes sure the file is up to
date before merging. A pull request that changes none of these passes in seconds.

When the files differ, the check fails and attaches the correct file to the build as `THIRD-PARTY-NOTICES`. Download
it, look at what changed, and commit it. When the generator stops instead (next section), the check fails with its
message and attaches the logs.

The main branches are also checked every night. This catches what a pull request alone cannot, for example two pull
requests that were each correct before the other one was merged.

## When the generator stops

The generator stops rather than guess. Every stop starts with `Third-party notices:` and ends with what to do. Most
stops ask for a decision, recorded in one of these files under `tools/src/main/notices/`:

| Message (start)                                                                      | Cause                                              | Where to record the decision               |
|--------------------------------------------------------------------------------------|----------------------------------------------------|--------------------------------------------|
| `these packages let us choose a license, and nobody has chosen`                      | A package offers several licenses                  | `license-choices.yml`                      |
| `these licenses are new to the product`                                              | No one has said what a license requires of us      | `ort-config/license-classifications.yml`   |
| `no license was found for these packages`                                            | Neither the package nor its sources name a license | `ort-config/curations/curations.json`      |
| `these packages name their license in words that are no standard license identifier` | The license name is not an SPDX identifier         | `ort-config/curations/curations.json`      |
| `we set the license of these packages by hand, but only for older versions`          | A reviewed license does not cover the new version  | `ort-config/curations/curations.json`      |
| `the source of these packages cannot be found`                                       | No sources jar, tarball or repository              | `ort-config/curations/curations.json`      |
| `the scan of these packages reported errors`                                         | A download or scan failed                          | run again, or `ort-config/resolutions.yml` |
| `no SBOMs in the target/sbom directories` or `these SBOMs are not of the version`    | The SBOM build is missing or stale                 | build again from clean                     |

A copyright line in the notices that cannot be about any component's code (a license text's own copyright, or code
misread as a statement) goes into `ort-config/copyright-garbage.yml`.

Every entry in these files carries a comment that says where the information comes from, so that a reviewer can check
it.

## Rules

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

## Testing the tooling

From the repository root:

```bash
python3 -m unittest discover -s tools/src/main/notices                   # the notices generator
python3 -m unittest discover -s tools/src/main/python/license-headers    # the header check
```
