# Release Preparation

## Semantic Versioning Scheme

GRIN follows Semantic Versioning: `MAJOR.MINOR.PATCH`.

- **MAJOR**: incompatible format or API changes (binary format, opcode set, or public APIs).
- **MINOR**: backward-compatible additions (new opcodes, new helpers, new tools).
- **PATCH**: backward-compatible fixes (bug fixes, documentation corrections).

While the project is `<1.0.0`, breaking changes may increment the **MINOR** version and
backward-compatible changes may increment the **PATCH** version.

## Release Tagging

Release tags use the format `vX.Y.Z` and point at the exact commit for the release.
The initial release tag is **v0.1.0**.

## Release Artifacts

Release artifacts should include:

- Web package build (`web/dist/`)
- CLI tool bundle (`tools/`)
- Android library (`android/lib/`)
- Checksums for published binaries
