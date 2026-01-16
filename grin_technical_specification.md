# GRIN Technical Specification

Status: Final (implementation-aligned)
Version: 0.1

GRIN (Graphic Readdressable Indexed Nodes) is a deterministic image container
format with bounded metadata, a fixed rule budget, and a predictable playback
model. This document reflects the current implementations in `web/`, `android/`,
and `tools/`.

## 1. Scope and Non-Goals

GRIN defines:

- A binary file format for storing a single image plus a fixed rule block.
- A control byte per pixel that encodes group membership and lock state.
- A fixed opcode set for deterministic, stateless modulation during playback.
- A bounded, inspectable runtime model.

GRIN does not define:

- Image generation, prompting, or orchestration.
- Per-pixel runtime addressing beyond group targeting.
- Executable code in files.

## 2. Constants and Types

- Magic: ASCII "GRIN" (0x47 0x52 0x49 0x4E)
- VersionMajor: 0
- VersionMinor: 0
- HeaderSizeBytes: 128 (fixed)
- PixelSizeBytes: 5 (RGBA + control)
- RuleCount: 0-16
- RulesBlockSize: 64 bytes (16 entries x 4 bytes)
- Endianness: Little-endian for all multi-byte integers

## 3. File Layout

```text
Header (128 bytes) + PixelData (Width * Height * 5 bytes)
```

### 3.1 Header Layout (128 bytes)

| Offset | Size | Field | Type | Notes |
| ---: | ---: | --- | --- | --- |
| 0 | 4 | Magic | ASCII | Must be "GRIN" |
| 4 | 1 | VersionMajor | uint8 | 0 |
| 5 | 1 | VersionMinor | uint8 | 0 |
| 6 | 2 | HeaderSize | uint16 | Must be 128 |
| 8 | 4 | Width | uint32 | Pixels |
| 12 | 4 | Height | uint32 | Pixels |
| 16 | 4 | TickMicros | uint32 | Tick duration in microseconds |
| 20 | 1 | RuleCount | uint8 | 0-16 |
| 21 | 1 | OpcodeSetId | uint8 | 0 for base set |
| 22 | 2 | Flags | uint16 | Reserved (0) |
| 24 | 8 | PixelDataLength | uint64 | Must equal Width * Height * 5 |
| 32 | 8 | FileLength | uint64 | Total bytes, 0 allowed |
| 40 | 8 | PixelDataOffset64 | uint64 | Must be 128 |
| 48 | 8 | ReservedA | uint64 | 0 |
| 56 | 8 | ReservedB | uint64 | 0 |
| 64 | 64 | RulesBlock | bytes | 16 rule entries |

### 3.2 Rules Block

RulesBlock is always 64 bytes. Only the first RuleCount entries are active.
Unused entries must be zero.

Each rule entry (4 bytes):

| Byte | Field | Type | Notes |
| ---: | --- | --- | --- |
| 0-1 | GroupMask | uint16 | Bit i targets group i |
| 2 | Opcode | uint8 | Must be valid for OpcodeSetId |
| 3 | Timing | uint8 | See Section 5 |

### 3.3 Pixel Layout (5 bytes)

| Byte | Field | Type | Notes |
| ---: | --- | --- | --- |
| 0 | R | uint8 | Red |
| 1 | G | uint8 | Green |
| 2 | B | uint8 | Blue |
| 3 | A | uint8 | Alpha |
| 4 | C | uint8 | Control byte |

Control byte bits:

- Bits 0-3: Group ID (0-15)
- Bits 4-6: Reserved (must be 0)
- Bit 7: Lock bit (1 = locked)

Control labels used in authoring/UI (not stored in the file):

- Group labels: `G H J K L M N P Q R S T U V W X` (skip `I` and `O`).
- Lock suffix: `Y` = unlocked, `Z` = locked.

## 4. Validation Rules

Readers MUST reject if any of the following are true:

- Magic != "GRIN"
- HeaderSize != 128
- RuleCount > 16
- PixelDataOffset64 != 128
- PixelDataLength != Width * Height * 5
- FileLength is non-zero and FileLength < 128 + PixelDataLength

Readers SHOULD warn if:

- Reserved header fields are non-zero
- Control byte reserved bits (4-6) are non-zero
- Unknown OpcodeSetId or opcode

## 5. Timing Byte

Timing is an 8-bit field used to compute a waveform value per tick.

Bit layout:

```text
bits 7-6: Phase offset (0-3, quarter-phase increments)
bits 5-4: Waveform type
bits 3-0: Period (0-15 stored, interpreted as 1-16 ticks)
```

Derived values:

```text
period = (timing & 0x0F) + 1
waveform = (timing >> 4) & 0x03
phaseOffset = ((timing >> 6) & 0x03) / 4
position = ((tick / period) + phaseOffset) % 1
```

Waveforms:

- 0: square (0 for first half, 1 for second half)
- 1: triangle (ramp up then down)
- 2: sine (0.5 - 0.5 * cos(2 * pi * position))
- 3: sawtooth (ramp up 0 -> 1)

## 6. Opcode Set 0 (OpcodeSetId = 0)

The base set is fixed and stateless. Unknown opcodes are invalid.

| ID | Name | Behavior |
| ---: | --- | --- |
| 0x00 | NOP | No change |
| 0x01 | FADE_IN | Alpha *= wave |
| 0x02 | FADE_OUT | Alpha *= (1 - wave) |
| 0x03 | PULSE | Alpha *= wave |
| 0x04 | SHIFT_R | R += delta |
| 0x05 | SHIFT_G | G += delta |
| 0x06 | SHIFT_B | B += delta |
| 0x07 | SHIFT_A | A += delta |
| 0x08 | INVERT | R = 255 - R, G = 255 - G, B = 255 - B |
| 0x09 | ROTATE_HUE | Rotate hue by wave * 360 deg |
| 0x0A | LOCK | Set lock bit |
| 0x0B | UNLOCK | Clear lock bit |
| 0x0C | TOGGLE_LOCK | Toggle lock bit |

SHIFT_* delta:

```text
delta = round((wave * 2 - 1) * 255)
```

All channel writes clamp to 0-255.

## 7. Playback Model

Playback operates on a working copy per pixel and writes to a display buffer.
Source pixels are never mutated.

Rule activation uses the timing waveform level. A rule is active when:

```text
TimingInterpreter.evaluate(timing, tick) > 0.5
```

Per-tick processing:

```text
for each pixel in source:
  control = controlByte
  if control.locked:
    output = source rgba
    continue

  working = copy(source, control)
  for each activeRule:
    if rule.groupMask targets control.groupId:
      apply opcode to working

  output = working rgba
  controlByte = working control (reserved bits cleared)
```

## 8. Security Model

GRIN enforces:

- Fixed header size and fixed rule count
- No executable code in files
- No external resource access during playback
- Bounded memory and CPU cost per tick

Any implementation MUST validate before allocating or processing data.
