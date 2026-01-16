# GRIN GIMP Plugin (Python-Fu)

## Overview

The GRIN GIMP plugin adds a Python-Fu menu entry that helps artists manage GRIN group/lock metadata
and export GRIN-ready assets. It creates dedicated metadata layers, lets you paint group and lock
values into selections, exports RGBA + group/lock PNGs, writes a rules sidecar, and can run the
GRIN CLI validation tools.

## Installation

1. Copy `plugins/gimp/grin_gimp_plugin.py` into your GIMP plug-ins folder:
   - Linux: `~/.config/GIMP/2.10/plug-ins/`
   - macOS: `~/Library/Application Support/GIMP/2.10/plug-ins/`
   - Windows: `%APPDATA%\GIMP\2.10\plug-ins\`
2. Ensure the script is executable (`chmod +x grin_gimp_plugin.py` on macOS/Linux).
3. Restart GIMP.

The plugin appears under **Filters → GRIN → GRIN Export...**

## Authoring Workflow

1. Paint your RGBA artwork in standard layers.
2. Run **Filters → GRIN → GRIN Export...** to create the metadata layers.
3. Use selections (lasso, brush selection, etc.) to mark regions and re-run the plugin with:
   - **Active Group ID** set to the desired group (0-15, labeled `G H J K L M N P Q R S T U V W X`).
   - **Lock pixels** toggled when you want the lock map filled.
4. The plugin updates the `GRIN_GROUPS` and `GRIN_LOCK` layers, preserving your original art.

## Export Pipeline

The export step writes the standard GRIN interchange artifacts:

- `<name>.png` (merged RGBA art)
- `<name>.groups.png` (group map)
- `<name>.lock.png` (lock map)
- `<name>.rules.json` (rules sidecar)

After export you can run the CLI manually if desired:

```bash
node tools/bin/grin-encode.js <name>.png <name>.grin --groups <name>.groups.png --rules <name>.rules.json
node tools/bin/grin-validate.js <name>.grin
```

## Notes

- Group map pixels are written with grayscale values 0-15 to preserve exact group IDs.
- Lock map pixels use 0 (unlocked) and 255 (locked).
- The plugin can run `grin-encode` and `grin-validate` directly when configured with the
  correct Node.js executable and CLI script paths.
