# GRIN Opcode Reference (OpcodeSetId = 0)

The current implementations (web, Android, tools) define a base opcode set
with IDs 0x00 through 0x0C. Unknown opcode values are treated as invalid.

All opcodes are stateless and operate on a per-pixel working copy during playback.

| ID | Name | Behavior | Notes |
| ---: | --- | --- | --- |
| 0x00 | NOP | No change | CPU cost 1 |
| 0x01 | FADE_IN | Alpha *= timing wave | Uses `TimingInterpreter.evaluate` |
| 0x02 | FADE_OUT | Alpha *= (1 - timing wave) | Uses `TimingInterpreter.evaluate` |
| 0x03 | PULSE | Alpha *= timing wave | Same as FADE_IN in current code |
| 0x04 | SHIFT_R | R += delta | delta = round((wave * 2 - 1) * 255) |
| 0x05 | SHIFT_G | G += delta | delta = round((wave * 2 - 1) * 255) |
| 0x06 | SHIFT_B | B += delta | delta = round((wave * 2 - 1) * 255) |
| 0x07 | SHIFT_A | A += delta | delta = round((wave * 2 - 1) * 255) |
| 0x08 | INVERT | R = 255 - R, etc | Alpha unchanged |
| 0x09 | ROTATE_HUE | Rotate hue by wave * 360 deg | HSL conversion, CPU cost 3 |
| 0x0A | LOCK | Set lock bit | Sets control byte bit 7 |
| 0x0B | UNLOCK | Clear lock bit | Clears control byte bit 7 |
| 0x0C | TOGGLE_LOCK | Toggle lock bit | XOR bit 7 |

Reserved IDs: 0x0D-0x0F.

## Example Rule Entry

```text
GroupMask: 0x0001  (targets group 0)
Opcode:    0x03    (PULSE)
Timing:    0x27    (period 8 ticks, sine wave, phase 0)
```

## Implementation Notes

- SHIFT_* opcodes clamp channel values to 0-255 after applying the delta.
- LOCK/UNLOCK/TOGGLE_LOCK mutate the control byte, which affects future ticks.
- Rotate hue uses HSL conversion and clamps resulting RGB values.
