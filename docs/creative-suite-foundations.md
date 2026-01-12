# Creative Suite + GIMP Plugin Foundations (Phase 14.1)

This document defines the shared foundations for Photoshop, Illustrator, and GIMP plugin workflows.
It provides a unified UX model, interchange format, validation workflow, palette legend, and naming
conventions that align with the existing GRIN CLI tooling.

## 1) Pixel-group editing model + UX goals (1-page spec)

**Goal:** Make group ID painting and lock-bit authoring fast, discoverable, and non-destructive,
while preserving the original art and enabling instant validation.

**Core UX elements**

- **Group ID palette (0-15):** A compact legend of 16 swatches labeled `G0`–`G15`. Selecting a
  swatch sets the active group ID for painting or tagging.
- **Lock toggle:** A single toggle/button that sets the lock bit for the active paint/selection
  action. When enabled, applied marks set the lock bit; when disabled, they clear it.
- **Dual-layer workflow (non-destructive):**
  - **Artwork layer(s):** The visible RGBA art the artist paints.
  - **Group/lock metadata layer(s):** Hidden metadata layers (or masks) that store group ID and
    lock-bit information without altering the art.
- **Overlays:**
  - **Group overlay:** Semi-transparent tint showing group ID regions using the shared palette.
  - **Lock overlay:** A hatch/stripe or icon overlay indicating locked pixels.
- **Selection tools:** Brush, lasso, and fill operate on the metadata layers while previewing
  changes on overlays in real time.
- **Rule preview:** A lightweight preview panel that runs `grin-inspect` (static) and
  `grin-validate` (rules sanity) on the exported assets and shows any errors inline.

**UX flow**

1. Artist paints the visible RGBA artwork.
2. Artist switches to **Group Mode**, selects a group from the palette, and paints selections
   into the group metadata layer.
3. Artist toggles **Lock Mode** to mark areas as locked (e.g., logos or static UI elements).
4. Artist exports using the standardized naming conventions to produce RGBA + group map + rules.
5. Validation runs automatically and surfaces warnings/errors before `.grin` export.

## 2) Interchange format between host apps and GRIN toolchain

**Artifacts (all required):**

- **RGBA art:** `<name>.png`
- **Group map:** `<name>.groups.png` (8-bit grayscale, values 0–15)
- **Rules:** `<name>.rules.json`

**Optional (lock map):**

- **Lock map:** `<name>.lock.png` (8-bit grayscale, 0 = unlocked, 255 = locked)

**Rules JSON format (compatible with `tools/bin/grin-encode.js`):**

```json
{
  "tickMicros": 33333,
  "opcodeSetId": 0,
  "rules": [
    { "groupMask": [0, 1, 2], "opcode": 3, "timing": 18 },
    { "groupMask": 8, "opcode": 1, "timing": 4 }
  ]
}
```

**Notes:**

- The group map is a grayscale PNG where pixel values map to group IDs (0–15). The existing
  encoder maps values ≤ 15 directly and rescales others; exporter should write exact 0–15 values
  for fidelity.
- The lock map is optional today; plugin exporters should still generate it for future support.
  When lock support is added to tooling, lock map pixels will set bit 7 in the control byte.

## 3) Shared validation CLI workflow

**CLI expectations:**

1. Encode with group map + rules:
   ```bash
   node tools/bin/grin-encode.js <name>.png <name>.grin --groups <name>.groups.png --rules <name>.rules.json
   ```
2. Validate the output:
   ```bash
   node tools/bin/grin-validate.js <name>.grin
   ```
3. Optional inspection for debugging:
   ```bash
   node tools/bin/grin-inspect.js <name>.grin
   ```

**Plugin behavior:**

- Run the validation step automatically after export.
- If validation fails, show a list of errors with file references and suggested fixes.

## 4) Shared palette/legend for group IDs and lock overlays

| Group | Label | Color (Hex) | Notes |
|-------|-------|-------------|-------|
| 0     | G0    | #FF1744     | Default primary group |
| 1     | G1    | #FF9100     | Warm secondary |
| 2     | G2    | #FFD600     | Highlight |
| 3     | G3    | #76FF03     | Bright green |
| 4     | G4    | #00E5FF     | Cyan |
| 5     | G5    | #2979FF     | Blue |
| 6     | G6    | #651FFF     | Indigo |
| 7     | G7    | #D500F9     | Purple |
| 8     | G8    | #F50057     | Magenta |
| 9     | G9    | #FF6D00     | Orange |
| 10    | G10   | #C6FF00     | Lime |
| 11    | G11   | #1DE9B6     | Teal |
| 12    | G12   | #00B0FF     | Light blue |
| 13    | G13   | #304FFE     | Royal blue |
| 14    | G14   | #AA00FF     | Violet |
| 15    | G15   | #F06292     | Pink |

**Lock overlay:** Use a diagonal hatch pattern (45°) at 30% opacity, with a lock icon in the
corner of the selection/preview. This remains consistent across Photoshop, Illustrator, and GIMP.

## 5) Output paths and naming conventions

**Export directory:** `exports/` at the project root by default (configurable per plugin).

**Naming standard:**

- `<name>.png` (RGBA art)
- `<name>.groups.png` (group IDs)
- `<name>.lock.png` (optional lock map)
- `<name>.rules.json` (rules)
- `<name>.grin` (final compiled output)

**Example:**

```
exports/
  hero_idle.png
  hero_idle.groups.png
  hero_idle.lock.png
  hero_idle.rules.json
  hero_idle.grin
```

This naming scheme is compatible with `tools/bin/grin-encode.js` inputs and keeps derivative
assets grouped for validation and review.
