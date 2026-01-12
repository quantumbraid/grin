GRIN
Graphic Readdressable Indexed Nodes
⚠️ Gradle is still being fixed. Everything else should work.
GRIN is a deterministic image container format designed as an upgrade to traditional images and GIFs. Instead of storing static frames or frame sequences, GRIN treats an image as a programmable field of pixels that can be dynamically grouped, addressed, and modulated over time.
Rather than thinking in layers or frames, GRIN addresses pixels in arbitrary programmable groups. A group can contain any pixels you want, regardless of color, spatial adjacency, or semantic meaning.
A useful mental model is a DMX lighting controller, where each light is a pixel and groups of lights are driven by rules instead of timelines.
Another way to think about GRIN is as a chroma-key-like system, but without visible colors. Each pixel carries an unseen control value with 16 possible states, plus a lock bit. Instead of keying on green or blue, rules key off this hidden channel. Pixels can be locked or unlocked, allowing rules to selectively affect them.
Core Concept
A .grin file is not a loop of frames.
Nearly all animation logic lives in the file header. Narrow, bounded scripts operate on pixel groups and can:
• shift colors
• animate values
• loop or diverge
• run once or indefinitely
Because logic is embedded, a .grin file can branch, repeat, or evolve without duplicating image data. Rendering requires only minimal runtime code. For web playback, a small amount of JavaScript is sufficient.
GRIN is designed to be artist-friendly, portable, and low-infrastructure, while remaining fully deterministic.
Technical Summary
• Deterministic image container format
• 5-byte pixels: RGBA + control byte
• Fixed 128-byte header
• Bounded rules block (no unbounded metadata)
• 16 addressable groups per pixel
• Lock bit per pixel
• Opcode-driven modulation
• No frame storage required
This repository includes working implementations, tools, tests, and sample files.
Implementations Included
• Web TypeScript library with Canvas renderer
• <grin-player> web component
• Android Kotlin library with demo app
• CLI tools for validation, inspection, encoding, and decoding
• Tests and benchmarks for web and Android
Repository Layout
• web/ – TypeScript implementation, web component, demo, tests
• android/ – Kotlin implementation and demo app
• tools/ – Node.js CLI utilities
• samples/ – Reference .grin files (valid and invalid)
• benchmarks/ – Performance harness
• tests/ – Shared fixtures and generators
• core/ – Reserved for a future cross-platform reference core
CLI Quick Start (Node ≥ 18)
Copy code

node tools/bin/grin-validate.js samples/minimal.grin
node tools/bin/grin-inspect.js samples/pulse_red.grin --header --rules
node tools/bin/grin-encode.js input.png output.grin
node tools/bin/grin-decode.js samples/pulse_red.grin output.png --frame 0
Web Library
Copy code

cd web
npm install
npm test
npm run build
The demo lives in web/demo/. Build first so web/dist is available.
Android Library and Demo
Copy code

cd android
./gradlew :demo:assembleDebug
The demo app loads sample files from
android/demo/src/main/assets/samples.
Samples
Regenerate sample files with:
Copy code

node scripts/generate-samples.mjs
Documentation
• Format specification: grin_technical_specification_v_2.md
• Architecture: ARCHITECTURE.md
• Contributing: CONTRIBUTING.md
• Security: SECURITY.md
• Samples: samples/README.md
• Docs index: docs/README.md
Planned Work
• Photoshop, Illustrator, and GIMP plugins
• Translation of .grin files to real DMX stage control
• Compatibility with large video walls and LED arrays
Licensing Status
This repository is currently unlicensed.
You may build, experiment, and explore this code for personal and research purposes.
You may not redistribute the code, incorporate it into unrelated projects, or use the code or its outputs for commercial purposes without explicit consent.
For commercial use, redistribution, or related project releases, contact:
📧 quantumbraid@gmail.com
This standard is intentionally constrained for security reasons and to encourage thoughtful, novel usage.