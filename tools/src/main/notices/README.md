# Third-party notices generator

Makes the third-party notices of the product, or of one package or image, from SBOMs. What the notices are, when to
regenerate them and how to decide what a stop asks for is in
[docs/licensing-guide.md](../../../../docs/licensing-guide.md). This README is about the generator itself.

## Run

```bash
mvn -T1C clean package -DskipTests -Dsbom.skip=false   # the SBOMs
mvn -N exec:exec@notices-product                        # the notices of the product, into THIRD-PARTY-NOTICES
```

The second command runs `notices.sh` over every SBOM that the modules left in their `target/sbom`. Build the whole
project, without `-pl`, `-Dpkg.skip*`, `-Dskip.yarn` or `-Dskip.ui.build`: the generator reads every SBOM it finds, and
a build that skipped some gives an incomplete file without an error.

The generator needs Linux or macOS (WSL on Windows), Docker and Python 3.11 or later. Its work directory, with the logs
of every step, is `target/notices-product/`.

## Steps

1. `scan_cache.py download` adds the scans that others have made.
2. `sbom_to_ort.py` turns the SBOMs into the list of their third-party packages, and checks them against the curations.
3. ORT analyzes the list and scans the source of each package for licenses and copyright statements.
4. `package_configurations.py` excludes the paths of a package that are not shipped, such as tests and documentation.
5. `notice_files.py` collects the NOTICE files of the packages.
6. ORT writes the notices with `THIRD-PARTY-NOTICES.ftl`.

## Scans

A scan of a package takes minutes, so ORT keeps its results in `scanner/` of its home. A shared archive holds the scans
of every machine: each run downloads what it does not have yet, and only CI uploads new scans. ScanCode's options in
`ort-config/config.yml` must not change, or no stored scan is reused.

| Environment variable                                     | Default                  | Effect                                               |
|----------------------------------------------------------|--------------------------|------------------------------------------------------|
| `NOTICES_ORT_HOME`                                       | `~/.ort`                 | Where ORT keeps its scans and downloads              |
| `NOTICES_SCAN_CACHE_URL`                                 | the shared archive       | Another archive, or `none` for no archive            |
| `NOTICES_MAVEN_REPOSITORY`                               | Maven's local repository | Where the jars are read from                         |
| `NOTICES_SCAN_CACHE_USER`, `NOTICES_SCAN_CACHE_PASSWORD` | none                     | The account that may upload to the archive (CI only) |

## Test

From the repository root:

```bash
python3 -m unittest discover -s tools/src/main/notices
```
