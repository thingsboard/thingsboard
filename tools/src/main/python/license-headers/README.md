# License header provenance check

Verifies that every file carries the SPDX header its origin requires: `apache`
for unchanged CE files, `ce-modified` for CE files with changes, and the third
header of this repository, `busl`, for files that do not come from CE. The
third header is the only `templates/license-header-<kind>.txt` besides the
`ce-modified` one, and its `<kind>` names it in the output and in curations.

CE is the history of the ThingsBoard Community Edition under Apache-2.0. The
check compares against the latest CE commits merged into the current branch:

- By default, CE commits are the ones that do not have the relicensing commit
  `31936b09d24` in their history, for example pre-4.4 master and the lts-4.3
  commits merged into lts-4.4.
- With `--ce-ref`, CE commits are the history of the given CE branch, tag, or
  commit, without the commits that have the relicensing commit in their
  history. Use it when the history of the current branch does not come from CE
  alone. The ref is fetched from `https://github.com/thingsboard/thingsboard.git`
  on every run; pass `--ce-remote` to fetch from another URL or a local clone.

## Rules

A file whose path exists in CE is `apache` when its content without the
header equals a CE version of it, or a clean merge of its CE versions, and
`ce-modified` otherwise.

A file whose path does not exist in CE is `busl`, except when it needs a
decision: its content is at least 50% similar to a CE file, or CE history once
had this path. A `pom.xml` added under a directory that has no files in CE is
`busl` even when it resembles a CE `pom.xml`: module identity, dependencies and
packaging define the new artifact.

## Run

```sh
./check-license-headers.sh
```

The check needs the full Git history of the clone.

## Fix automatically

```sh
./check-license-headers.sh --fix
```

Restamps every file whose expected header the check determined by itself and
removes curations that are redundant, because the check decides the same header
by itself, or that belong to files no longer in scope.

## Add a curation

For files the check cannot decide ("Needs a decision"), record the decision in
`curations.json` next to this file:

1. Put the correct header on the file. A file that exists in CE, even renamed
   or moved, is `ce-modified`; a file that does not exist in CE is `busl`.
2. Run `sha256sum path/to/File.java`.
3. Add an entry and rerun the check:

```json
{
  "path": "path/to/File.java",
  "header": "busl",
  "reason": "Which CE file this was compared against and why the header follows.",
  "contentHash": "sha256:<64 hex digits>"
}
```

A curation whose file changed since the review is stale and fails the check
until someone reviews the file again and updates `contentHash`.
