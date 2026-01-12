# GRIN Android API Draft Reference

This is a draft API reference for the Android/Kotlin implementation. It is
hand-written to avoid long Gradle runs on slow machines. When ready, generate
full HTML docs via Dokka:

```bash
cd android
./gradlew :lib:dokkaHtml
```

Output is configured to land in `docs/api/android`.

## Package

All APIs live under `io.grin.lib`.

## Core File Model

- `GrinHeader` - header model, serialize/deserialize, validation helpers.
- `GrinPixel` - 5-byte pixel structure with control byte helpers.
- `GrinRule` - rule entry (group mask, opcode, timing).
- `GrinImage` - container (header, pixels, rules).
- `GrinFile` - load/save/serialize full `.grin` files.

## Binary I/O

- `GrinBinaryIO` - binary readers/writers for header, pixels, rules.
- `GrinInputStream` / `GrinOutputStream` - stream helpers.
- `GrinEndianness` - little-endian read/write helpers.
- `GrinFormat` - constants and offsets for header/pixel layout.

## Playback

- `GrinPlayer` - playback controller, per-tick rendering to `DisplayBuffer`.
- `RuleEngine` - rule evaluation per tick.
- `DisplayBuffer` - RGBA output buffer (4 bytes per pixel).
- `PlaybackState` - current tick and play state.
- `TickScheduler` - scheduler interface.
- `AndroidTickScheduler` / `ChoreographerTickScheduler` - vsync-driven schedulers.
- `TestTickScheduler` - deterministic scheduler for tests.

## Opcodes and Timing

- `Opcode` - opcode interface.
- `BaseOpcodes` - opcode IDs (OpcodeSetId = 0).
- `OpcodeRegistry` - lookup and validation for opcodes.
- `Opcodes` - opcode implementations (fade, pulse, shifts, invert, lock).
- `TimingInterpreter` - timing byte evaluation.

## Rendering and UI

- `GrinBitmapRenderer` - convert `DisplayBuffer` to `Bitmap`.
- `GrinView` - view that handles loading and playback.

## Loading Helpers

- `GrinUriLoader` - load from assets, files, or content URIs.
- `GrinContentProvider` - content provider integration.

## Validation

- `GrinValidation` / `GrinValidationSuite` - validation results and checks.
