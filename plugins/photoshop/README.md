# GRIN Photoshop Plugin (UXP)

## Overview

The GRIN Photoshop panel helps artists author GRIN metadata alongside their artwork. It exports
RGBA art, group maps, lock maps, and rules sidecars that can be compiled into a `.grin` file
using the CLI tools described in `docs/creative-suite-foundations.md`.

## Installation

1. Open Adobe Photoshop and enable UXP developer mode.
2. Load the plugin from `plugins/photoshop/` using the UXP Developer Tool.
3. Launch the **GRIN Export** panel from the Plugins menu.

## Authoring Workflow

1. Paint your RGBA artwork in standard layers.
2. Add metadata layers:
   - **Group map layer** named `GRIN_GROUPS` (or `GRIN_GROUP_MAP`).
   - **Lock map layer** named `GRIN_LOCK` (or `GRIN_LOCK_MAP`).
3. Paint the group map using grayscale values **0-15** so each pixel maps to a GRIN group ID.
4. Paint the lock map using **0** for unlocked pixels and **255** for locked pixels.
5. Optionally tag layer names with group labels and lock hints, e.g. `Hero GL [Z]`.

## Layer Tagging Conventions

The panel scans layer names for metadata tags to help with preview summaries:

- `GG`–`GX` or `[GG]`–`[GX]` set the group label (`G H J K L M N P Q R S T U V W X`).
- `LOCK` or `[Z]` marks a layer as locked (`Z` suffix).
- `UNLOCK` or `[Y]` clears the lock hint (`Y` suffix).

## Export Pipeline

1. Click **Export** to choose an output folder.
2. The panel writes:
   - `<name>.png` (RGBA art)
   - `<name>.groups.png` (group map)
   - `<name>.lock.png` (lock map)
   - `<name>.rules.json` (rules sidecar)
3. Run the CLI commands displayed in the status bar:

```bash
node tools/bin/grin-encode.js <name>.png <name>.grin --groups <name>.groups.png --rules <name>.rules.json
node tools/bin/grin-validate.js <name>.grin
```

## Notes

- The export uses the active document title to build file names.
- The group/lock layers must already be authored in grayscale values; the plugin exports them as-is.
- The rules JSON is generated with an empty rule list by default.
