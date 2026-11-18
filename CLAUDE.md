# CLAUDE.md

## SBOMs, licenses and third-party notices

Every artifact ThingsBoard ships carries an SBOM, and the root `THIRD-PARTY-NOTICES` is generated from those SBOMs. So
adding, updating or removing a Maven or npm dependency, copying third-party code, shipping a new file in a package, and
creating or changing a source file all have licensing consequences. The two guides below say what each of these
requires. Follow them whenever you make such a change, and tell the user when a step is left for them, such as
regenerating the notices or a license decision that needs a person.

@docs/sbom.md

@docs/licensing-guide.md
