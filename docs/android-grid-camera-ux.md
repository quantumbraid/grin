# Android Grid Camera + Gallery UX & Data Model

## UX Flow Overview

### Screen Map
1. **Camera Preview**
   - Live posterized preview with grid overlay.
   - Primary actions: capture, toggle grid, palette view, settings.
2. **Capture Review**
   - Frozen posterized frame with channel mapping summary.
   - Actions: accept (save GRIM/GRIN), retake, open editor.
3. **Gallery Grid**
   - Posterized thumbnails in a grid, with quick metadata badges (resolution, palette size).
   - Actions: open editor, export, delete, share.
4. **Editor**
   - Channel selector (0-9/A-F) plus sliders for frequency, color intonation, transparency.
   - Actions: apply, revert, export, back to gallery.

### Navigation Rules
- Camera Preview is the default entry point.
- Capture Review is modal; accepting moves to Gallery and allows immediate Editor open.
- Gallery is the primary browsing surface; Editor is a detail view scoped to one capture.

## Data Model

### Entities
- **CaptureSession**
  - `id` (UUID)
  - `createdAt` (epoch millis)
  - `width`, `height`
  - `gridCols`, `gridRows`
  - `paletteBins` (12-14)
  - `sourceUri` (raw capture or temp buffer)
- **GrinAsset**
  - `id` (UUID)
  - `fileUri` (GRIM/GRIN output path)
  - `previewUri` (posterized PNG)
  - `width`, `height`
  - `tickMicros`
  - `ruleCount`
  - `channelMap` (ordered list of channel assignments)
  - `paletteHistogram` (bin -> count)
  - `createdAt`, `lastEditedAt`
- **ChannelSetting**
  - `channelId` (0-15)
  - `frequency`
  - `intonation`
  - `alpha`

### Storage Plan
- Persist `GrinAsset` and `ChannelSetting` metadata in Room (SQLite).
- Store GRIM/GRIN payloads and preview PNGs in app-scoped storage with stable file names.
- Cache derived thumbnails; invalidate on edit export.

### Demo Implementation Notes
- The demo app persists `GrinAsset` metadata as JSON files under app-scoped storage.
- Preview PNGs, thumbnails, and GRIM/GRIN payloads are stored in sibling folders for fast gallery paging.
- Editor exports write PNG, GIF, and updated GRIN payloads into an app-scoped `exports` directory.

## Posterization & Channel Mapping

### Palette Handling
- Target 12-14 color bins per capture; bins built from a posterization LUT.
- For high-variance scenes, allow adaptive bin count within the 12-14 range.
- Persist ordered palette bins for consistent channel mapping across edits/exports.

### Preview Pipeline Targets
- Aim for 30fps on mid-tier devices, with headroom for 60fps on flagship devices.
- If analysis latency exceeds ~40ms, drop to a fallback profile (75% grid resolution, 12 bins).
- Use CPU posterization with nearest-palette matching; keep frames capped by analysis FPS.

### Channel Mapping Rules
- Assign bins to channels 0-9/A-F using a stable sort by frequency.
- Keep a deterministic mapping table in `channelMap` to guarantee replayability.
- Provide editor overrides that reassign channels without re-running posterization.

## GRIM/GRIN Metadata Updates
- Update `TickMicros` based on capture preview cadence (e.g., 33_333 for 30fps).
- Set `RuleCount` to the number of active channels.
- Write channel metadata into rule block entries (group mask + opcode + timing) to reflect editor settings.
- Ensure header `PixelDataLength` and `FileLength` reflect generated payload sizes.

## Export Options

### GRIM/GRIN
- Persist updated header settings, channel map, and rules for playback parity.

### PNG Snapshot
- Export an ARGB_8888 bitmap with alpha preserved from the posterized preview.
  - Implementation detail: snapshot renders the updated GRIN file at tick 0 using the playback engine.

### GIF Loop
- Render 12-15 frames from GRIN playback at fixed tick intervals.
- Package as a looping GIF with consistent palette ordering.
  - Implementation detail: loop renders 12 frames at a 1-tick step using the current header tick rate.

### Export Validation + Storage
- Store exports in the app-scoped `grin_gallery/exports` directory with deterministic filenames.
- Validate PNG snapshots by decoding and confirming dimensions and key pixels.
- Validate GIF loops by decoding the header and ensuring frame dimensions match the source.
