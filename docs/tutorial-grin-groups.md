# Tutorial: Understanding GRIN Groups

Groups are the only runtime-addressable unit in GRIN. Each pixel belongs to
exactly one group (0-15) and can be targeted by rules via a bitmask. For
authoring, use the control label alphabet (`G H J K L M N P Q R S T U V W X`)
so the control channel never overlaps RGBA hex digits.

## Control Byte Recap

The control byte (C) uses bits 0-3 for the group ID and bit 7 for the lock bit.
Lock state is labeled with a suffix: `Y` = unlocked, `Z` = locked.

```text
bit 7: lock
bits 0-3: group ID (0-15)
```

Control label example: `LY` means group L (ID 4), unlocked.

## Rule Group Masks

Each rule has a 16-bit mask. Bit i targets group i.

Examples:

```text
0x0001 -> group 0
0x0003 -> groups 0 and 1
0x8000 -> group 15
0xFFFF -> all groups
```

## Assigning Groups with the Encoder

The CLI encoder reads group IDs from a mask image:

```bash
node tools/bin/grin-encode.js input.png output.grin --groups group-mask.png
```

Group IDs are derived from the mask image red channel:

- 0-15 map directly to group IDs.
- 16-255 map via `round(value / 17)` and clamp to 0-15.

This lets you author mask images using standard 8-bit grayscale tools.

## Example Rules File

```json
{
  "rules": [
    { "groupMask": 1, "opcode": "0x03", "timing": "0x27" },
    { "groupMask": 2, "opcode": "0x04", "timing": "0x17" }
  ]
}
```

This pulses group 0 and shifts red on group 1.
