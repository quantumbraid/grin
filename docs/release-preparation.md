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

## Package Publishing

### Android (Maven Central / JitPack)

1. Ensure `android/lib/build.gradle.kts` has publishing metadata and signing configured.
2. Export credentials for Sonatype and signing:
   - `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`
   - `SIGNING_KEY`, `SIGNING_PASSWORD`
3. Publish the release artifacts:
   - `./gradlew :lib:publish` (from `android/`)
4. Close and release the staging repository in Sonatype OSSRH.

For JitPack, push a Git tag and confirm the build at `https://jitpack.io/#grin-format/grin`.

### Web (npm)

1. From `web/`, run `npm run build`.
2. Verify `dist/` output and `web/README.md` contents.
3. Publish with:
   - `npm publish --access public`

### GitHub Releases

1. Push a version tag in the format `vX.Y.Z`.
2. Confirm the `release.yml` workflow attaches the packaged artifacts.
3. Add release notes referencing the CHANGELOG entry.
