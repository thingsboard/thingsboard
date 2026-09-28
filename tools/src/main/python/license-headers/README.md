# License header provenance check

Verifies that every file carries the SPDX header its origin requires: `apache`
for unchanged CE files, `ce-modified` for CE files with PE changes, `pe-only`
for files that do not exist in CE. A `pom.xml` added in PE under a directory
that has no files in CE is `pe-only` even when it resembles a CE `pom.xml`:
module identity, dependencies and packaging define the PE artifact.

## Run

```sh
./check-license-headers.sh lts-4.2
```

The argument is the CE branch, tag, or commit matching the current PE branch. It
is fetched from `https://github.com/thingsboard/thingsboard.git` on every run, so
the clone needs network access but no CE remote of its own. Pass `--ce-remote`
to fetch from another URL or a local CE clone instead.

## Fix automatically

```sh
./check-license-headers.sh lts-4.2 --fix
```

Restamps every file whose expected header the check determined by itself.

## Add a curation

For files the check cannot decide ("Needs a decision"), record the decision in
`curations.json` next to this file:

1. Put the correct header on the file. A file that exists in CE, even renamed
   or moved, is `ce-modified`; a file that does not exist in CE is `pe-only`.
2. Run `sha256sum path/to/File.java`.
3. Add an entry and rerun the check:

```json
{
  "path": "path/to/File.java",
  "contentHash": "sha256:<64 hex digits>",
  "header": "pe-only",
  "reason": "Which CE file this was compared against and why the header follows."
}
```
