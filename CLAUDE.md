# CLAUDE.md

## SBOMs, licenses and third-party notices

Adding, updating or removing a Maven or npm dependency, copying third-party code, shipping a new file in a package, and
creating or changing a source file all have licensing consequences. The two guides below say what each of these
requires. Follow them, and tell the user when a step is left for them, such as regenerating the notices or a license
decision that needs a person.

The entries in the decision files of the notices (`tools/src/main/notices/`) and in the curations of the header check
(`tools/src/main/python/license-headers/curations.json`) each record that a person checked a license or an origin. You
may draft such an entry, with the source you checked in its comment, but tell the user that a person must review it.
Never widen a version range, choose a license or classify one unless you have verified it in the package's own files.

@docs/sbom.md

@docs/licensing-guide.md
