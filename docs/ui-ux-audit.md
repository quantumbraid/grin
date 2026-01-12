# GRIN UI/UX Audit (Phase 15.1)

**Audit date:** 2025-09-28

This audit inventories current user-facing surfaces, captures flows and pain points, and establishes a
heuristics checklist, core journeys, and a prioritized UX backlog for Phase 15.

## 1) Inventory of end-user surfaces

| Surface | Location | Primary flows | Pain points | Screenshot notes |
| --- | --- | --- | --- | --- |
| Web demo | `web/demo/index.html`, `web/demo/demo.js`, `web/demo/styles.css` | Drag/drop `.grin` file, choose file, play/pause/stop, seek via slider, load bundled samples. | No visible validation errors, playback controls lack disabled states, metadata panel text-only without hierarchy, missing empty state for samples fetch errors. | Screenshot captured during audit run; not committed to repo to avoid binary artifacts. |
| Android demo | `android/demo/src/main/kotlin/io/grin/demo/MainActivity.kt`, `android/demo/src/main/res/layout/activity_main.xml` | Launch app, load bundled sample, play/stop animation, view metadata. | No clear file-open CTA, limited affordances for scrub/seek, metadata readout dense, lacks error recovery UI. | Screenshots unavailable in current environment (no Android runtime). |
| CLI tools | `tools/bin/grin-encode.js`, `tools/bin/grin-decode.js`, `tools/bin/grin-inspect.js`, `tools/bin/grin-validate.js` | Encode from PNG + rules, decode to PNG, inspect headers, validate files. | Usage output is terse, no examples, errors lack remediation steps, flags not surfaced consistently across tools. | CLI is text-only; captured output notes in this document. |
| Docs onboarding | `docs/README.md` plus `docs/tutorial-first-file.md` | Discover documentation entry points, follow CLI quick start, find spec references. | No explicit “start here” ordering, no UX/UX checklist link before audit, missing callouts for demos. | Screenshots not required; doc navigation captured in audit notes. |

### CLI help capture (sample)

```
Usage: grin-encode <input.png> <output.grin> [--groups <mask.png>] [--rules <rules.json>]
```

## 2) UX heuristics checklist

| Heuristic | Web demo | Android demo | CLI | Docs |
| --- | --- | --- | --- | --- |
| Navigation clarity (clear next step) | Partial | Partial | Partial | Partial |
| Error state clarity (actionable messaging) | Missing | Missing | Partial | Partial |
| Keyboard/mouse affordances (shortcuts, focus) | Missing | Missing | Missing | Partial |
| Accessibility (contrast, target size, labels) | Partial | Partial | N/A | Partial |
| Feedback & system status (loading/progress) | Missing | Missing | Partial | Partial |
| Consistent terminology across surfaces | Partial | Partial | Partial | Partial |
| Discoverability of sample content | Partial | Partial | N/A | Partial |

## 3) Core user journeys + success criteria

### Journey A: Open file
1. User finds the file-open entry point.
2. User selects or drops a `.grin` file.
3. UI validates and loads the file.

**Success criteria**
- Clear CTA for opening a file.
- Validation errors are visible with next-step guidance.
- The UI shows a loaded state with metadata.

### Journey B: Play animation
1. User presses play.
2. Playback starts, status updates, and timing aligns with tick rate.
3. User can pause/stop and resume without losing state.

**Success criteria**
- Playback controls show current state.
- Seek/step is responsive with clear bounds.
- No console-only errors.

### Journey C: Inspect groups
1. User finds group summary or mask information.
2. User filters or inspects group counts.

**Success criteria**
- Group counts are visible.
- Filters are discoverable and reversible.

### Journey D: Export
1. User selects export action.
2. Validation runs and confirms output.
3. User is notified of output location and success/failure.

**Success criteria**
- Export path is explicit.
- Validation results are summarized.
- Failure states provide remediation steps.

## 4) Prioritized UX backlog

| Priority | Platform | Backlog item |
| --- | --- | --- |
| P0 | Web | Add validation error panel with remediation and status indicators next to playback. |
| P0 | Android | Add file-open CTA and error recovery UI when decode fails. |
| P0 | CLI | Provide `--help` with examples for encode/decode/validate and align flag naming. |
| P1 | Web | Add disabled/active states for playback controls and clearer seek bounds. |
| P1 | Android | Introduce playback scrubber + speed controls with accessible touch targets. |
| P1 | Docs | Add “Start here” block and demo links in `docs/README.md`. |
| P2 | Web | Add keyboard shortcuts help modal for playback + group filters. |
| P2 | CLI | Add consistent error taxonomy and remediation hints. |
| P2 | Docs | Add quick UX FAQ and link to audit checklist. |
