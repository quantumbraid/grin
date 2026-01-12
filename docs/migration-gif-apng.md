# Migration Guide: GIF/APNG to GRIN

GRIN is not a frame-based animation format. It stores a single image plus
group assignments and rule-driven modulation. Migrating from GIF/APNG is
about translating *behavior* into GRIN rules, not copying frames.

## Key Differences

- GIF/APNG: sequence of discrete frames.
- GRIN: one base image, plus opcodes that modulate pixels over time.
- GRIN rules target groups, not individual pixels at runtime.
- Some complex frame-by-frame animations are not expressible in GRIN.

## Migration Strategy

1. Choose a base frame to represent the initial state.
2. Assign groups to areas you want to modulate (up to 16).
3. Translate repeating or periodic effects into GRIN rules.
4. Encode the file and validate it.

## Tools Workflow

1) Prepare a base PNG and optional group mask:

```bash
node tools/bin/grin-encode.js base.png output.grin --groups group-mask.png --rules rules.json
```

2) Example `rules.json`:

```json
{
  "tickMicros": 16666,
  "opcodeSetId": 0,
  "rules": [
    { "groupMask": 1, "opcode": "0x03", "timing": "0x27" }
  ]
}
```

3) Inspect the result:

```bash
node tools/bin/grin-inspect.js output.grin --header --rules --groups
```

## When to Split into Multiple GRIN Files

If your source animation has complex per-frame edits, consider exporting
multiple GRIN files and switching between them externally. GRIN is designed
for deterministic modulation, not arbitrarily scripted timelines.
