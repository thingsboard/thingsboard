# Third-party notices generator

Makes the third-party notices of the product, or of one package, image, or UI bundle, from CycloneDX SBOMs.
What the notices are, when to regenerate them, and how to make license decisions is in
[docs/licensing-guide.md](../../../../docs/licensing-guide.md). This README covers the generator itself.

## Run

```bash
# 1. Build SBOMs (skipping .deb/.rpm/.zip creation makes SBOM-only builds much faster)
mvn -T1C clean package -DskipTests -Dsbom.skip=false -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=true

# 2. Check and generate combined product notices in target/THIRD-PARTY-NOTICES
mvn -N exec:exec@notices-product
```

Do not use `-pl`, `-Dpkg.skip=true`, `-Dskip.yarn=true`, or `-Dskip.ui.build=true` when running the full product check:
every module's SBOM must be present.

The generator needs Python 3.11+ (standard library only, no `pip install`). Docker is only invoked when a brand-new
package is missing from both the local cache and the shared Artifactory cache; warm-cache runs finish in ~1–2 seconds
without Docker.

## Architecture

Everything lives in one self-contained Python package (`tools/src/main/notices/notices/`) driven by two TOML files:

| File | Purpose |
|---|---|
| `policy.toml` | All global rules: license categories (`nothing`, `notice`, `source-link`, `written-offer`, `forbidden`), same-category tie-breakers, global non-SPDX aliases, display titles, excluded source paths, repo-local license files, own modules, and copyright garbage |
| `curations.toml` | Per-package exceptions: source overrides / `metadata_only`, POM declared-license fixes, file-scoped `ignore_findings`, version-ranged `concluded_license`, and inline `[custom_licenses]` |

### Pipeline (`python3 tools/src/main/notices render ...`)

1. **SBOM load (`sbom.py`)**: Reads CycloneDX SBOMs, rejects stale versions, excludes own modules and base-image Syft components.
2. **Source & cache lookup (`scanner.py`, `cache.py`)**: Resolves Maven `-sources.jar` / npm tarballs / VCS curations and fetches any missing scan results in parallel over HTTP keep-alive from Artifactory (`<base>/scans/<sha256>.json.gz`).
3. **On-demand ScanCode (`scanner.py`)**: Only packages absent from both local and remote cache are scanned with ScanCode in Docker (passing `--ignore` for excluded paths).
4. **Policy evaluation (`policy.py`, `spdx.py`)**: Normalizes declared licenses, applies file-scoped ignores and the dual-license root `LICENSE*` rule, resolves `OR` choices by category priority, and stops fail-closed on unreviewed versions, unmapped names, unscanned packages, unranked ties, forbidden licenses, unclassified licenses, or unlicensed packages.
5. **NOTICE & bundled license files (`scanner.py`, `license_files.py`, `lwm2m.py`)**: Extracts `NOTICE*` files from shipped binary Maven jars (cached per jar SHA-256) and npm tarballs, downloads URL license files (e.g. Node.js), generates LwM2M registry notices on the fly from `application/src/main/data/lwm2m-registry/*.xml`, and filters platform sections for Chromium `CREDITS.txt`.
6. **Render (`render.py`, `copyrights.py`)**: Cleans/merges copyright year ranges, loads cached SPDX/ScanCode license texts, and writes `THIRD-PARTY-NOTICES` atomically.

## Subcommands & Environment Variables

```bash
python3 tools/src/main/notices render --project-version <ver> --out <file> [--sboms-under <dir>] [SBOM ...]
python3 tools/src/main/notices cache-upload              # CI only: PUTs new immutable scan JSONs to Artifactory
python3 tools/src/main/notices migrate-ort               # One-shot migration from legacy ~/.ort/scanner
python3 tools/src/main/notices ci-should-run --target-branch <branch> --teamcity
```

| Environment variable | Default | Effect |
|---|---|---|
| `NOTICES_CACHE_DIR` | `~/.cache/tb-notices` | Local persistent cache (scans, jar NOTICEs, license texts, maven-sources) |
| `NOTICES_SCAN_CACHE_URL` | `https://repo.thingsboard.io/artifactory/ort` | Remote per-package cache base URL, or `none` |
| `NOTICES_MAVEN_REPOSITORY` | `~/.m2/repository` | Local Maven repository for binary jar NOTICE extraction |
| `NOTICES_SCANCODE_IMAGE` | `ghcr.io/oss-review-toolkit/ort:98.0.0` | Docker image providing `scancode` on cache misses |
| `NOTICES_SCAN_CACHE_USER`, `NOTICES_SCAN_CACHE_PASSWORD` | none | Artifactory upload credentials (CI only) |
| `NOTICES_FAIL_ON_UNUSED_CURATIONS` | `false` | Fail instead of warning when a `curations.toml` entry matches no package in a `--sboms-under` run (nightly CI) |

## TeamCity CI Integration

Both PR and nightly TeamCity configurations can run the exact same repo-owned steps:

1. **Unit tests**: `python3 -m unittest discover -s tools/src/main/notices/tests -t tools/src/main/notices`
2. **PR change gate** (PR config only): `python3 tools/src/main/notices ci-should-run --target-branch "$TARGET" --teamcity`
3. **Fast SBOM build**: `mvn -T6 clean package -DskipTests -Dsbom.skip=false -Dpkg.skip.deb=true -Dpkg.skip.rpm=true -Dpkg.skip.zip=true`
4. **Verify notices**: `mvn -N exec:exec@notices-product` (writes `target/THIRD-PARTY-NOTICES` and fails on any policy/curation issue)
5. **Upload new scans** (nightly/main config): `python3 tools/src/main/notices cache-upload`

Use a persistent `NOTICES_CACHE_DIR` on TeamCity agents (not `%system.teamcity.build.tempDir%`) so warm builds make zero cache downloads.

## Test

From the repository root:

```bash
python3 -m unittest discover -s tools/src/main/notices/tests -t tools/src/main/notices -v
```
