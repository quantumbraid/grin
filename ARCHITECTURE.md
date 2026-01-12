# GRIN Architecture

## Overview

GRIN (Graphic Readdressable Indexed Nodes) is a deterministic image container format designed for simplicity, security, and wide compatibility. This document describes the system architecture and design decisions.

## Design Principles

### 1. Constrained by Design

GRIN intentionally limits expressiveness to ensure:
- **Predictable behavior**: Every GRIN file behaves identically on all platforms
- **Bounded resources**: Memory and CPU usage are calculable from header alone
- **Security by construction**: No executable code, no external resources

### 2. Low Bar for Entry

Rendering a .grin file to screen requires minimal code:
- **~200 lines** for a basic reader (any language)
- **~100 additional lines** for playback
- **Zero dependencies** beyond standard library

### 3. Self-Contained Logic

The GRIN format embeds all animation logic:
- Rules stored in fixed 64-byte header block
- Opcodes are simple, stateless transformations
- External code only provides tick scheduling and display

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           GRIN Ecosystem                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
│  │   Encoder    │     │   .grin      │     │   Decoder    │            │
│  │   (Tool)     │────>│   File       │────>│   (Tool)     │            │
│  └──────────────┘     └──────────────┘     └──────────────┘            │
│                              │                                          │
│                              │ Load                                     │
│                              ▼                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      GRIN Core Library                            │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │  │
│  │  │   Reader    │  │  Validator  │  │   Opcodes   │               │  │
│  │  │  (Binary)   │  │  (Rules)    │  │  (Effects)  │               │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘               │  │
│  │         │                │                │                       │  │
│  │         ▼                ▼                ▼                       │  │
│  │  ┌───────────────────────────────────────────────────────────┐   │  │
│  │  │                   Playback Engine                          │   │  │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │   │  │
│  │  │  │  Tick    │  │  Rule    │  │  Pixel   │  │ Display  │  │   │  │
│  │  │  │ Scheduler│─>│  Engine  │─>│ Pipeline │─>│  Buffer  │  │   │  │
│  │  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │   │  │
│  │  └───────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                              │                                          │
│              ┌───────────────┼───────────────┐                         │
│              ▼               ▼               ▼                         │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              │
│  │   Android    │   │     Web      │   │     CLI      │              │
│  │  (Bitmap)    │   │  (Canvas)    │   │   (PNG out)  │              │
│  └──────────────┘   └──────────────┘   └──────────────┘              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## File Format Structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         GRIN File (128 + N bytes)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  HEADER (128 bytes, fixed)                                              │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ Offset │ Size │ Field            │ Description                     │ │
│  ├────────┼──────┼──────────────────┼─────────────────────────────────┤ │
│  │ 0      │ 4    │ Magic            │ "GRIN" (0x47 0x52 0x49 0x4E)    │ │
│  │ 4      │ 1    │ VersionMajor     │ Format version (0)              │ │
│  │ 5      │ 1    │ VersionMinor     │ Format version (0)              │ │
│  │ 6      │ 2    │ HeaderSize       │ Must be 128                     │ │
│  │ 8      │ 4    │ Width            │ Image width in pixels           │ │
│  │ 12     │ 4    │ Height           │ Image height in pixels          │ │
│  │ 16     │ 4    │ TickMicros       │ Playback tick duration (µs)     │ │
│  │ 20     │ 1    │ RuleCount        │ Number of active rules (0-16)   │ │
│  │ 21     │ 1    │ OpcodeSetId      │ Opcode set identifier           │ │
│  │ 22     │ 2    │ Flags            │ Reserved (0)                    │ │
│  │ 24     │ 8    │ PixelDataLength  │ Width × Height × 5              │ │
│  │ 32     │ 8    │ FileLength       │ Total file size (optional)      │ │
│  │ 40     │ 8    │ PixelDataOffset  │ Must be 128                     │ │
│  │ 48     │ 16   │ Reserved         │ Must be 0                       │ │
│  │ 64     │ 64   │ RulesBlock       │ 16 × 4-byte rule entries        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
│  PIXEL DATA (Width × Height × 5 bytes)                                  │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ For each pixel (5 bytes):                                          │ │
│  │   Byte 0: Red   (0-255)                                            │ │
│  │   Byte 1: Green (0-255)                                            │ │
│  │   Byte 2: Blue  (0-255)                                            │ │
│  │   Byte 3: Alpha (0-255)                                            │ │
│  │   Byte 4: Control                                                  │ │
│  │           ├─ Bits 0-3: Group ID (0-15)                             │ │
│  │           ├─ Bits 4-6: Reserved (0)                                │ │
│  │           └─ Bit 7:    Lock bit                                    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Component Details

### Binary Reader

The reader is intentionally simple:

```
read_grin(file):
    header = read_bytes(128)
    validate_magic(header[0:4])       # "GRIN"
    validate_header_size(header[6:8]) # Must be 128
    
    width  = read_uint32_le(header, 8)
    height = read_uint32_le(header, 12)
    
    pixels = read_bytes(width * height * 5)
    return GrinImage(header, pixels)
```

### Validation Engine

Validation is strict and fail-fast:

1. **Magic Check**: First 4 bytes must be "GRIN"
2. **Header Size**: Bytes 6-7 must equal 128
3. **Dimension Check**: Width × Height × 5 = PixelDataLength
4. **Rule Count**: Must be 0-16
5. **Offset Check**: PixelDataOffset must be 128
6. **Reserved Fields**: Should be zero (warning if not)

### Opcode System

Opcodes are stateless functions:

```
opcode(pixel, tick, timing) -> modified_pixel

Properties:
  - No side effects
  - No state accumulation
  - O(1) time complexity
  - Deterministic output
```

Base Opcode Set (ID = 0):

| Code | Name        | Effect                          |
|------|-------------|---------------------------------|
| 0x00 | NOP         | No operation                    |
| 0x01 | FADE_IN     | Increase alpha                  |
| 0x02 | FADE_OUT    | Decrease alpha                  |
| 0x03 | PULSE       | Oscillate alpha                 |
| 0x04 | SHIFT_R     | Shift red channel               |
| 0x05 | SHIFT_G     | Shift green channel             |
| 0x06 | SHIFT_B     | Shift blue channel              |
| 0x07 | SHIFT_A     | Shift alpha channel             |
| 0x08 | INVERT      | Invert RGB                      |
| 0x09 | ROTATE_HUE  | Rotate in HSL space             |
| 0x0A | LOCK        | Set lock bit                    |
| 0x0B | UNLOCK      | Clear lock bit                  |
| 0x0C | TOGGLE_LOCK | Toggle lock bit                 |

### Playback Engine

Per-tick processing:

```
process_tick(image, tick):
    active_rules = evaluate_timing(image.rules, tick)
    
    for i, pixel in image.pixels:
        if pixel.is_locked():
            output[i] = pixel.rgba
            continue
        
        working = copy(pixel)
        for rule in active_rules:
            if rule.targets(pixel.group_id):
                opcode = get_opcode(rule.opcode)
                working = opcode(working, tick, rule.timing)
        
        output[i] = working.rgba
    
    return output
```

---

## Platform Implementations

### Android (Kotlin/Java)

```
Core Components:
├── GrinFile.kt         - File loading and parsing
├── GrinHeader.kt       - Header structure
├── GrinPixel.kt        - Pixel with control byte
├── GrinRule.kt         - Rule definition
├── GrinImage.kt        - Image container
├── GrinValidator.kt    - Validation logic
├── GrinOpcodes.kt      - Opcode implementations
├── GrinPlayer.kt       - Playback controller
├── GrinView.kt         - Android View component
└── GrinBitmap.kt       - Bitmap rendering

Dependencies:
- androidx.annotation (annotations only)
- No third-party libraries
```

### Web (TypeScript/JavaScript)

```
Core Components:
├── grin-file.ts        - File loading (ArrayBuffer)
├── grin-header.ts      - Header structure
├── grin-pixel.ts       - Pixel with control byte
├── grin-rule.ts        - Rule definition
├── grin-image.ts       - Image container
├── grin-validator.ts   - Validation logic
├── grin-opcodes.ts     - Opcode implementations
├── grin-player.ts      - Playback controller
├── grin-element.ts     - Web Component <grin-player>
└── grin-canvas.ts      - Canvas rendering

Dependencies:
- None (zero runtime dependencies)
- TypeScript for development only
```

---

## Data Flow

### Loading

```
File → Binary Reader → Header Validation → Pixel Parsing → GrinImage
```

### Playback

```
GrinImage → Tick Scheduler → Rule Engine → Pixel Pipeline → Display Buffer → Screen
           (platform)       (core)        (core)           (core)           (platform)
```

### Memory Model

```
Source Pixels (immutable during playback)
    │
    ▼
Working Copy (per-tick, temporary)
    │
    ▼
Display Buffer (RGBA only, 4 bytes/pixel)
    │
    ▼
Platform Render (Bitmap/Canvas)
```

---

## Security Model

### Threat Mitigations

| Threat                    | Mitigation                                |
|---------------------------|-------------------------------------------|
| Code execution            | No executable code in format              |
| Memory exhaustion         | Size calculable from header               |
| CPU exhaustion            | O(1) opcodes, bounded rules               |
| External resource access  | No URLs, no file references               |
| Hidden complexity         | All rules inspectable in header           |
| Malformed input           | Strict validation before processing       |

### Validation Invariants

1. File MUST start with "GRIN" magic bytes
2. Header size MUST be exactly 128 bytes
3. PixelDataLength MUST equal Width × Height × 5
4. RuleCount MUST be 0-16
5. All opcodes MUST be valid for OpcodeSetId
6. Reserved fields SHOULD be zero

---

## Extension Points

### Custom Opcode Sets

Future OpcodeSetId values can define new opcodes while maintaining backward compatibility. Readers that don't recognize an OpcodeSetId should:
1. Warn the user
2. Treat unknown opcodes as NOP
3. Still display the static image

### Reserved Fields

Reserved header fields and control byte bits are for future use:
- MUST be written as zero
- MUST be ignored by current readers
- MAY have meaning in future versions

---

## Performance Characteristics

### Memory

```
Header:         128 bytes (constant)
Source Pixels:  W × H × 5 bytes
Display Buffer: W × H × 4 bytes
Working Memory: O(1) per tick
Total:          ~9 bytes per pixel + 128 bytes
```

### CPU

```
Per-tick:
  - Rule evaluation: O(R) where R ≤ 16
  - Pixel processing: O(W × H × R)
  - Each opcode: O(1)
  
Worst case: O(W × H × 16) per tick
```

### I/O

```
File read: Single sequential read (no seeking required)
Header: 128 bytes
Pixels: W × H × 5 bytes
Total: 128 + (W × H × 5) bytes
```

---

## Glossary

| Term          | Definition                                        |
|---------------|---------------------------------------------------|
| Control Byte  | 5th byte of pixel containing group ID and lock    |
| Group         | One of 16 addressable pixel categories            |
| Lock Bit      | Flag preventing pixel modification during play    |
| Opcode        | Stateless pixel transformation function           |
| Rule          | GroupMask + Opcode + Timing (4 bytes)             |
| Tick          | Single playback time unit                         |
| Timing        | Oscillator parameter for rule scheduling          |

---

## References

- [GRIN Technical Specification v2](spec/grin_technical_specification_v_2.md)
- [Build Plan](todo.md)
- [Agent Contract](agents.txt)