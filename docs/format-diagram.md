# GRIN Format Diagram

This diagram summarizes the byte-level layout implemented in the current web,
Android, and CLI libraries. All multi-byte integers are little-endian.

## File Layout (128 + pixel data)

```text
0x0000 ┌────────────────────────────────────────────────────────────┐
       │ Header (128 bytes)                                          │
0x0080 ├────────────────────────────────────────────────────────────┤
       │ Pixel data (Width * Height * 5 bytes)                       │
       └────────────────────────────────────────────────────────────┘
```

## Header Layout (128 bytes)

| Offset | Size | Field | Type | Notes |
| ---: | ---: | --- | --- | --- |
| 0 | 4 | Magic | ASCII | "GRIN" |
| 4 | 1 | VersionMajor | uint8 | current 0 |
| 5 | 1 | VersionMinor | uint8 | current 0 |
| 6 | 2 | HeaderSize | uint16 | must be 128 |
| 8 | 4 | Width | uint32 | pixels |
| 12 | 4 | Height | uint32 | pixels |
| 16 | 4 | TickMicros | uint32 | tick duration in microseconds |
| 20 | 1 | RuleCount | uint8 | 0-16 |
| 21 | 1 | OpcodeSetId | uint8 | 0 for base set |
| 22 | 2 | Flags | uint16 | reserved (0) |
| 24 | 8 | PixelDataLength | uint64 | width * height * 5 |
| 32 | 8 | FileLength | uint64 | total bytes (0 allowed) |
| 40 | 8 | PixelDataOffset64 | uint64 | must be 128 |
| 48 | 8 | ReservedA | uint64 | 0 |
| 56 | 8 | ReservedB | uint64 | 0 |
| 64 | 64 | RulesBlock | bytes | 16 rule entries (4 bytes each) |

## Rules Block (64 bytes)

Each rule entry is 4 bytes. Only the first RuleCount entries are active.

| Byte | Field | Type | Notes |
| ---: | --- | --- | --- |
| 0-1 | GroupMask | uint16 | bit i targets group i |
| 2 | Opcode | uint8 | must be valid for OpcodeSetId |
| 3 | Timing | uint8 | see `docs/timing.md` |

## Pixel Layout (5 bytes)

| Byte | Field | Type | Notes |
| ---: | --- | --- | --- |
| 0 | R | uint8 | red |
| 1 | G | uint8 | green |
| 2 | B | uint8 | blue |
| 3 | A | uint8 | alpha |
| 4 | C | uint8 | control byte |

Control byte bits:

```text
7   6   5   4   3   2   1   0
L   R   R   R   G   G   G   G

G = Group ID (0-15)
R = Reserved (must be 0)
L = Lock bit
```

## Reader Validation (Required)

A reader must reject the file if any of the following are true:

- Magic != "GRIN"
- HeaderSize != 128
- RuleCount > 16
- PixelDataOffset64 != 128
- PixelDataLength != Width * Height * 5
- FileLength != 0 and FileLength < 128 + PixelDataLength

Readers should warn if reserved fields or control-byte reserved bits are non-zero.
