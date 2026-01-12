# GRIN Timing Byte Reference

The timing byte is an 8-bit field used by opcodes that need a wave value.
Both web and Android implementations interpret it the same way.

## Bit Layout

```text
bits 7-6: Phase offset (0-3, quarter-phase increments)
bits 5-4: Waveform type
bits 3-0: Period (0-15 stored, interpreted as 1-16 ticks)
```

## Derived Values

```text
period = (timing & 0x0F) + 1
waveform = (timing >> 4) & 0x03
phaseOffset = ((timing >> 6) & 0x03) / 4

position = ((tick / period) + phaseOffset) % 1
```

Waveform mapping:

| Value | Waveform | Output |
| ---: | --- | --- |
| 0 | square | 0 for first half, 1 for second half |
| 1 | triangle | ramp up then down |
| 2 | sine | 0.5 - 0.5 * cos(2 * pi * position) |
| 3 | sawtooth | ramp up 0 -> 1 |

## Examples

- `0x00`: period 1, square wave, phase 0.
- `0x17`: period 8, triangle wave, phase 0.
- `0x27`: period 8, sine wave, phase 0.
- `0xE3`: period 4, sawtooth wave, phase 3 (offset 0.75).
