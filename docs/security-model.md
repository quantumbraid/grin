# GRIN Security Model

GRIN is designed to be inspection-safe. Files contain no executable code and
can be validated without unbounded memory or CPU usage.

## Core Principles

- No executable code in files.
- Fixed header size and bounded rule block.
- Deterministic, stateless opcode execution.
- No external resource access during playback.

## Required Validation

All readers should validate the following before allocating or parsing:

- Magic bytes are "GRIN".
- Header size equals 128.
- RuleCount is in 0-16.
- PixelDataOffset64 equals 128.
- PixelDataLength equals Width * Height * 5.
- FileLength is zero or >= 128 + PixelDataLength.

The current implementations also warn on:

- Non-zero reserved header fields.
- Non-zero reserved control byte bits (4-6).
- Unknown opcodes for the active opcode set.

## Resource Bounds

Implementations enforce safe integer bounds when computing:

- pixel count = width * height
- pixel data length = pixel count * 5
- total file length

This avoids integer overflow and ensures allocations are predictable.

## Playback Safety

- Playback operates on a working copy per pixel.
- Source pixels are never mutated.
- Locked pixels are always copied through.
- OpCodes are stateless and O(1) per pixel.
