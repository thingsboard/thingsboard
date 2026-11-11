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

The check needs the full Git history of the clone. It runs Git and Mycila
subprocesses in parallel on all CPUs but one; set `LICENSE_HEADERS_JOBS` to
another number, for example `1` for a sequential run.

## Cache

The check removes the header of every CE file it compares with Mycila. CE files
never change, so their header-free contents are kept in
`target/license-headers-cache` and reused by the next run. The cache is valid
while the Mycila version and configuration in the root `pom.xml`, the templates
and the check itself stay the same; anything else starts a new cache, and the
check normalizes again instead of using the old one. Pass `--no-cache` to
normalize every CE file again, or `--cache` to keep the cache elsewhere.

In GitHub Actions, pushes and manual runs save the cache and pull requests only
read it: a pull request takes the cache of its base branch, then of `master`.

## Check only changed files

```sh
./check-license-headers.sh --since origin/lts-4.4
```

Checks the files that differ between the merge base of the given ref and HEAD
and the working tree, untracked files included, and the files whose curation
changed. Pull requests run the check this way against their target branch;
pushes and manual runs check every file. Curations of files no longer in scope
are still reported for the whole repository.

The check falls back to every file when a change can move the verdict of an
unchanged file: new CE commits merged into the branch, a changed Mycila
configuration in the root `pom.xml`, or a change to the check itself.

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
