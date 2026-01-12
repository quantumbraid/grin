*STILL FIXING GRADLE, EVERYTHING ELSE SHOULD WORK*

# GRIN (Graphic Readdressable Indexed Nodes)

Grin tries to conceptualy give an upgrade to the concept of images of gifs. 
Rather than storing a still imgage, or a series of frames grin is about addressing
pixles in programmable groups. These groups can be any pixels you desire, 
they do not need to be the same color, object, or "layer."

It is easiest to think of this like a dmx lighting controller, where lights are pixels.

Another way to think about it, is simmilar to chroma key based graphics. 
Rather than addressing colors, you address the value of an unseen property carried by every pixel.
Instead of locking onto blue or green for instance, there is a unseen "color" with 16 possible values.
Pixels can also be locked or unlocked.

The header of the .grin file contains nearly all logic necessary to drive the animation.
small narrow scripts are aloud to run on each chanel, doing things like shifting colors by group.
each script can be timed, looped, or not.
The file is not a loop of frames, it is capable of diverging, or a repeating pattern.
This tool is intended to create a highly adoptable, low outside infrastructure, artist friend.
.grin files contain most logic, and it would take only some minor JS to display them.


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

- Format spec: `grin_technical_specification_v_2.md`
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

Plans for future development include Photoshop, Illustrator, and Gimp plugins. Also planned is an app that translates .grin files for actual DMX stage ussage. .grin files should be highly compatible in being converted to a large videowall, or led arrays.

THIS REPO IS CURRENTLY UNLICENSED, FOR USAGE CONTACT ME DIRECTLY AT QUANTUMBRAID@GMAIL.COM
EVENTUALLY THIS CODE WILL PICK A LICENSE FOR WIDE GENERAL USAGE,
UNTIL THEN, FEEL FREE TO BUILD USE AND PLAY WITH IT, BUT DO NOT DISTRIBUTE THE CODE
DO NOT INCORPERATE THE CODE INTO OTHER UNRELATED PROJECTS. FOR RELATED PROJECTS, CONTACT DIRECTLY BEFORE RELEASING CODE.
DO NOT USE THIS CODE OR IT'S OUTPUTS FOR COMERCIAL PURPOSES WITHOUT CONSENT, CONTACT DIRECTLY FOR COMERCIAL INFO.
TO COLLABERATE PLEASE CONTACT ME, SAME GOES WITH IMPROVEMENT SUGGESTIONS.
THIS STANDARD IS CONSTRAINED INTENTIONALLY. THESE CONSTRAINTS ARE SECURITY RELATED, HOWEVER DESIGNED TO INCREASE INGINUITY AND CREATIVITY.


