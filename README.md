# GRIN (Graphic Readdressable Indexed Nodes)

GRIN is a deterministic image container format designed as an upgrade to traditional
images and GIFs. Instead of storing static frames or frame sequences, GRIN treats an
image as a programmable field of pixels that can be dynamically grouped, addressed,
and modulated over time.

## Core concept

- A `.grin` file is not a loop of frames.
- Nearly all animation logic lives in the file header.
- Narrow, bounded scripts operate on pixel groups and can shift colors, animate values,
  loop or diverge, and run once or indefinitely.
- Because logic is embedded, a `.grin` file can branch, repeat, or evolve without
  duplicating image data.
- Rendering requires only minimal runtime code. For web playback, a small amount of
  JavaScript is sufficient.
- GRIN is designed to be artist-friendly, portable, and low-infrastructure, while
  remaining fully deterministic.

## Technical summary

- Deterministic image container format
- 5-byte pixels: RGBA + control byte
- Fixed 128-byte header
- Bounded rules block (no unbounded metadata)
- 16 addressable groups per pixel
- Lock bit per pixel
- Opcode-driven modulation
- No frame storage required

This repository includes working implementations, tools, tests, and sample files.

## Implementations included

- Web TypeScript library with Canvas renderer
- `<grin-player>` web component
- Android Kotlin library with demo app
- CLI tools for validation, inspection, encoding, and decoding
- Tests and benchmarks for web and Android

## Repository layout

- `web/` - TypeScript implementation, web component, demo, tests
- `android/` - Kotlin implementation and demo app
- `tools/` - Node.js CLI utilities
- `samples/` - Reference `.grin` files (valid and invalid)
- `benchmarks/` - Performance harness
- `tests/` - Shared fixtures and generators
- `core/` - Reserved for a future cross-platform reference core

## CLI quick start (Node >= 18)

```bash
node tools/bin/grin-validate.js samples/minimal.grin
node tools/bin/grin-inspect.js samples/pulse_red.grin --header --rules
node tools/bin/grin-encode.js input.png output.grin
node tools/bin/grin-decode.js samples/pulse_red.grin output.png --frame 0
node tools/bin/grin-lanes.js lanes.txt --normalize
```

## Web library

```bash
cd web
npm install
npm test
npm run build
```

The demo lives in `web/demo/`. Build first so `web/demo/demo.js` can load `web/dist`.

## Minimal web embed (load a `.grin` URL)

A minimal embed uses the tiny loader in `web/embed/grin-embed.js`. It finds elements
with `data-grin-src`, loads `grin-player`, and mounts playback for you.

```html
<div data-grin-src="https://example.com/art/minimal.grin"></div>
<script type="module" src="web/embed/grin-embed.js"></script>
```

Optional attributes:

- `data-grin-autoplay` - set to `true` to auto-start playback.
- `data-grin-playbackrate` - numeric speed multiplier (for example `0.5` or `2`).
- `data-grin-lib` (on the script tag) - overrides the `grin-player` bundle URL.

Example using a custom `grin-player` bundle:

```html
<div data-grin-src="/media/pulse_red.grin" data-grin-autoplay="true"></div>
<script
  type="module"
  src="web/embed/grin-embed.js"
  data-grin-lib="/web/dist/grin-player.js"
></script>
```

## Android library and demo

```bash
cd android
./gradlew :demo:assembleDebug
```

The demo app loads sample files from `android/demo/src/main/assets/samples`.

## Samples

Regenerate sample files with:

```bash
node scripts/generate-samples.mjs
```

## Documentation

- Format specification: `grin_technical_specification_v_2.md`
- Architecture: `ARCHITECTURE.md`
- Contributing: `CONTRIBUTING.md`
- Security: `SECURITY.md`
- Samples: `samples/README.md`
- Docs index: `docs/README.md`

## Planned work

- Photoshop, Illustrator, and GIMP plugins
- Translation of `.grin` files to real DMX stage control
- Compatibility with large video walls and LED arrays

## Licensing status

THIS REPOSITORY IS CURRENTLY UNLICENSED.

You may build, experiment, and explore this code for personal and research purposes.

You may not redistribute the code, incorporate it into unrelated projects, or use the
code or its outputs for commercial purposes without explicit consent.

For commercial use, redistribution, or related project releases, contact:

quantumbraid@gmail.com

This standard is intentionally constrained for security reasons and to encourage
thoughtful, novel usage.
