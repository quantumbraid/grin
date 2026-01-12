# GRIN Illustrator Plugin (UXP)

## Overview

The GRIN Illustrator panel flattens vector artboards into GRIN-ready raster assets. It exports
RGBA art, group maps, lock maps, and a rules sidecar that can be compiled into a `.grin` file
using the CLI tools described in `docs/creative-suite-foundations.md`.

## Installation

1. Open Adobe Illustrator and enable UXP developer mode.
2. Load the plugin from `plugins/illustrator/` using the UXP Developer Tool.
3. Launch the **GRIN Export** panel from the Plugins menu.

## Vector Flattening Rules

1. Each artboard is flattened into a raster frame.
2. The artboard width maps to the selected **Rasterize Resolution (px)**; height scales to preserve aspect ratio.
3. The artboard origin is treated as the GRIN pixel (0, 0), with pixels laid out left-to-right and top-to-bottom.
4. Stroke, fill, and effects are rasterized using Illustrator's PNG export engine.

## Group + Lock Tagging

The panel reads metadata from page item names or notes:

- `G0`–`G15` or `[G0]`–`[G15]` set the group ID.
- `LOCK` or `[L]` marks a page item as locked.
- `UNLOCK` or `[U]` clears the lock hint.

Use tags on layers or objects to preview how many items are annotated. Tagged metadata can be
used to drive group/lock map creation in your art.

## Export Pipeline

1. Author your vector art on an artboard.
2. Create dedicated metadata layers:
   - **Group map layer** named `GRIN_GROUPS` (or `GRIN_GROUP_MAP`).
   - **Lock map layer** named `GRIN_LOCK` (or `GRIN_LOCK_MAP`).
3. Paint these layers using grayscale values **0-15** for group IDs and **0/255** for lock state.
4. Click **Export Assets** and choose an output folder.
5. The panel writes:
   - `<name>.png` (flattened RGBA art)
   - `<name>.groups.png` (group map)
   - `<name>.lock.png` (lock map)
   - `<name>.rules.json` (rules sidecar)
6. Run the CLI commands displayed in the status bar:

```bash
node tools/bin/grin-encode.js <name>.png <name>.grin --groups <name>.groups.png --rules <name>.rules.json
node tools/bin/grin-validate.js <name>.grin
```

## Notes

- The export uses the active document title to build file names.
- The panel previews tagged metadata but relies on the dedicated metadata layers for map export.
- The rules JSON is generated with an empty rule list by default.
