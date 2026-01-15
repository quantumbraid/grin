# GRIN User Guide (Simplified)

## What GRIN is

GRIN (Graphic Readdressable Indexed Nodes) is a deterministic image container format.
Instead of storing a list of frames, a `.grin` file stores one image plus a small,
fixed rules block that modulates pixel groups over time. The format is intentionally
bounded so files are predictable to validate and play back.

## Think in groups (base‑16, not “colors”)

Each pixel belongs to exactly one group, identified by a 4‑bit value (0‑15). The
rules in the header target these groups, not colors. It helps to think of the
scripting layer as base‑16 counting: group IDs are just hexadecimal digits. Groups
can be any set of pixels and do **not** need to share the same color.

## The environment and its intentional limitations

GRIN is designed to be safe, deterministic, and easy to validate:

- **Fixed header size (128 bytes)** with reserved fields that must be zero.
- **Bounded rules block (16 rules max)** and a fixed opcode set.
- **No executable scripts**: rules are a tiny, fixed instruction table.
- **Deterministic playback**: given the same input, every renderer produces the
  same output.
- **Group‑only addressing**: runtime operations target groups, not arbitrary
  per‑pixel logic.

These limits are deliberate. They keep files small, parsing fast, and behavior
predictable across web, Android, and CLI tooling.

## GRIN file layout (high‑level)

- **Header (128 bytes)**: format identity, dimensions, timing, and rules.
- **Pixel data**: width × height × 5 bytes (RGBA + control byte).
- **Control byte**: 4 bits for group ID (0‑15) and 1 lock bit.

## How rules work (the “scripting” layer)

Rules are 4‑byte entries in the header. Each rule has:

- **Group mask (16 bits)**: selects which groups are affected.
- **Opcode (8 bits)**: a small, fixed set (e.g., pulse alpha, shift channels).
- **Timing (8 bits)**: defines waveform + period (think phase + frequency).

Because rules target group masks, you can build complex effects by organizing
pixels into logical groups rather than by color.

## Basic usage (CLI flow)

1) **Start with a PNG** (e.g., 16×16 for testing).
2) **(Optional) Build a group mask image** where pixel values map to group IDs.
3) **Create a rules JSON file** describing the rule table.
4) **Encode** the `.grin` file with the CLI tools.
5) **Inspect or decode** frames to verify output.

Example commands:

```bash
node tools/bin/grin-encode.js input.png output.grin --rules rules.json
node tools/bin/grin-encode.js input.png output.grin --groups group-mask.png --rules rules.json
node tools/bin/grin-inspect.js output.grin --header --rules --groups
node tools/bin/grin-decode.js output.grin output.png --frame 0
```

## Where to go next

- `docs/tutorial-first-file.md` for a step‑by‑step walkthrough.
- `docs/tutorial-grin-groups.md` for group mask guidance.
- `docs/opcodes.md` and `docs/timing.md` for rule semantics.
- `grin_technical_specification.md` for the full binary spec.
