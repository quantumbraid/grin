# GRIN (Graphic Readdressable Indexed Nodes)

GRIN is a deterministic image container format with 5-byte pixels (RGBA + control byte)
and a fixed 128-byte header with a bounded rules block. This repo includes working
implementations for web and Android, CLI tools, tests, and sample files.

## Highlights

- Fixed-size header and rules block (no unbounded metadata).
- 16 groups per pixel with a lock bit and opcode-driven modulation.
- Web TypeScript library with a Canvas renderer and `<grin-player>` component.
- Android Kotlin library with a demo app.
- CLI tools: `grin-validate`, `grin-inspect`, `grin-encode`, `grin-decode`.
- Tests and benchmarks across web and Android.

## Specs and docs

- Format spec: `grin_technical_specification.md` (implementation-aligned)
- Spec draft: `grin_technical_specification_v_2.md`
- Architecture: `ARCHITECTURE.md`
- Contributing: `CONTRIBUTING.md`
- Security: `SECURITY.md`
- Samples: `samples/README.md`
- Docs index: `docs/README.md`

## Repository layout

- `web/` - TypeScript implementation, web component, demo, tests
- `android/` - Kotlin implementation and demo app
- `tools/` - Node.js CLI utilities
- `samples/` - Reference `.grin` files (valid + invalid)
- `benchmarks/` - Performance harness
- `tests/` - Shared fixtures and generators
- `core/` - Reserved for a future cross-platform reference core

## CLI quick start (Node >= 18)

```bash
node tools/bin/grin-validate.js samples/minimal.grin
node tools/bin/grin-inspect.js samples/pulse_red.grin --header --rules
node tools/bin/grin-encode.js input.png output.grin
node tools/bin/grin-decode.js samples/pulse_red.grin output.png --frame 0
```

## Web library

```bash
cd web
npm install
npm test
npm run build
```

The demo lives in `web/demo/`. Build first so `web/demo/demo.js` can load `web/dist`.

## Web embed (no backend)

Use the drop-in embed script to load the viewer and render a `.grin` URL:

```html
<div
  data-grin-src="https://your-cdn.example/samples/minimal.grin"
  data-grin-autoplay="true"
  data-grin-playbackrate="1"
></div>
<script
  type="module"
  src="https://cdn.jsdelivr.net/gh/quantumbraid/grin@main/web/embed/grin-embed.js"
></script>
```

By default the embed script loads `../dist/grin-player.js` relative to its own URL
(with a fallback to the GitHub jsDelivr URL).
You will need to host the built `web/dist/grin-player.js` (or override with
`data-grin-lib`).

If you host the viewer elsewhere, override it:

```html
<script
  type="module"
  src="https://cdn.jsdelivr.net/gh/quantumbraid/grin@main/web/embed/grin-embed.js"
  data-grin-lib="https://your-cdn.example/grin-player.js"
></script>
```

Note: the `.grin` URL must allow CORS for browser fetch.

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
