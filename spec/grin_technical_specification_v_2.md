# GRIN — Graphic Readdressable Indexed Nodes

## Canonical Scope

This document defines **GRIN** as a standalone, implementation-ready specification. GRIN is a deterministic image container and playback substrate. It is not a generator, not a renderer, and not a general-purpose execution environment. GRIN defines how image state is stored, addressed, grouped, locked, and modulated over time under strict, inspectable constraints.

This document intentionally excludes all image-generation logic, orchestration, prompting, or creative systems. Any system that emits GRIN-compliant data is external to this specification.

---

## 1. Design Intent

GRIN exists to provide:

- A bounded, inspectable image format
- Deterministic runtime behavior
- Predictable memory, CPU, and metadata costs
- Structural resistance to hidden logic or malicious complexity

GRIN prioritizes **correctness over expressiveness**. If a behavior cannot be expressed within GRIN’s constraints, it is intentionally disallowed.

GRIN is intentionally incomplete.

---

## 2. Core Mental Model

- Images are **state**, not timelines
- Pixels are **nodes**, not colors
- Motion is **modulation**, not replacement
- Groups are **buses**, not selections
- Editing is powerful; playback is dumb
- External systems may observe GRIN state but never execute within it

Nothing inside a GRIN file is allowed to be clever.

---

## 3. Output Contract

All imagery is emitted directly in **GRIN RGBA+C** format.

- No conversion step
- No tagging pass
- No semantic metadata layer

GRIN is the final output format.

---

## 4. Pixel Definition

Each pixel is exactly **5 bytes**:

| Byte | Name | Type | Description |
|---:|---|---|---|
| 0 | R | uint8 | Red channel |
| 1 | G | uint8 | Green channel |
| 2 | B | uint8 | Blue channel |
| 3 | A | uint8 | Alpha channel |
| 4 | C | uint8 | Control byte |

This represents a deliberate 25% size increase over RGBA, paid once.

### 4.1 Control Byte (C)

- Bits 0–3: **Group ID** (0–15)
- Bit 7: **Lock bit**
- Bits 4–6: Reserved (must be zero)

Each pixel:

- Belongs to exactly **one of 16 groups**
- May be **locked or unlocked**

There is no per-pixel metadata beyond this.

---

## 5. Groups

- Exactly **16 groups** exist
- Groups are the **only runtime-addressable unit**

There is:

- No per-pixel addressing at runtime
- No exclusion logic
- No conditionals
- No Venn diagrams

If a group is not called, it is unaffected.

If a subset must be protected, it is locked.

---

## 6. Locking Semantics

Locking is explicit, reversible, and costly.

- Locked pixels ignore all runtime actions
- Unlocking requires an explicit action
- Re-locking requires an explicit action
- Locking and unlocking consume rule budget

Protection is a decision, not a default.

---

## 7. File Container and Fixed Header

GRIN files are **self-describing** within strict limits. Parsing is intentionally small and bounded so that a minimal Java/Kotlin implementation (Android) or a minimal JavaScript implementation (web) can reliably render and optionally play back GRIN.

### 7.1 Byte Order and Invariants

- All multi-byte integers are **little-endian**.
- Header size is **fixed** to enable O(1) seek and predictable worst-case costs.
- Reserved fields **must be written as 0** and **ignored by readers**.
- A compliant file must be parsable without scanning or heuristics.

### 7.2 Fixed Header Size

- **HeaderSizeBytes = 128** (fixed, always present).
- **PixelDataOffset = 128**.

A reader never searches for metadata. It jumps to known offsets.

### 7.3 Header Layout (128 bytes)

All offsets below are from file start.

| Offset | Size | Name | Type | Notes |
|---:|---:|---|---|---|
| 0 | 4 | Magic | ASCII | Must be `GRIN` |
| 4 | 1 | VersionMajor | uint8 | Current: 0 |
| 5 | 1 | VersionMinor | uint8 | Current: 0 |
| 6 | 2 | HeaderSize | uint16 | Must be 128 |
| 8 | 4 | Width | uint32 | Pixels |
| 12 | 4 | Height | uint32 | Pixels |
| 16 | 4 | TickMicros | uint32 | Playback tick duration in microseconds |
| 20 | 1 | RuleCount | uint8 | 0–16 |
| 21 | 1 | OpcodeSetId | uint8 | Identifies the fixed opcode set (reader-known) |
| 22 | 2 | Flags | uint16 | Reserved (0) |
| 24 | 8 | PixelDataLength | uint64 | Must equal `Width * Height * 5` |
| 32 | 8 | FileLength | uint64 | Total bytes in file (optional; 0 allowed) |
| 40 | 8 | PixelDataOffset64 | uint64 | Must be 128 |
| 48 | 8 | ReservedA | uint64 | 0 |
| 56 | 8 | ReservedB | uint64 | 0 |
| 64 | 64 | RulesBlock | bytes | Fixed-size rule block; see §7.4 |

#### 7.3.1 Reader Validation Rules

A reader MUST reject the file if any of these are true:

- Magic != `GRIN`
- HeaderSize != 128
- RuleCount > 16
- PixelDataOffset64 != 128
- PixelDataLength != Width * Height * 5
- FileLength is non-zero and FileLength < 128 + PixelDataLength

A reader SHOULD reject if any reserved field is non-zero.

### 7.4 Rules Block (Fixed 64 bytes)

Rules are stored inside the fixed header to prevent unbounded metadata growth.

- RulesBlock is always 64 bytes.
- Only the first `RuleCount` entries are active.
- Unused entries must be zero.

Each rule entry is 4 bytes (16 rules × 4 bytes = 64 bytes):

| Bytes | Name | Type | Notes |
|---:|---|---|---|
| 0–1 | GroupMask | uint16 | Bit i targets Group i (0–15) |
| 2 | Opcode | uint8 | Must be valid within OpcodeSetId |
| 3 | Timing | uint8 | On/off oscillator timing parameter (reader-defined) |

Rules express **declarative schedules**, not programs.

Timing is evaluated per rule on each global tick. Each rule’s timing and phase
offset is independent, so channels can drift or diverge indefinitely without
re-synchronizing to a file-level loop. Timing value `0x00` is reserved for
one-shot activation (active on tick 0, inactive afterward); non-zero timings
define recurring modulation periods.

---

## 8. Opcodes (Fixed Set)

GRIN supports a small, fixed opcode set identified by OpcodeSetId.

This spec defines the **existence** of a fixed opcode set but does not enumerate it here unless and until the opcode list is frozen. Readers must treat any unknown Opcode value as invalid.

Constraints that must hold for any opcode set:

- Must be statically inspectable
- Must have predictable worst-case CPU cost per tick
- Must not require dynamic allocation
- Must not allow hidden logic or unbounded expressiveness

---

## 9. Runtime Behavior Model

Playback is intentionally simple.

On each fixed tick:

1. Determine active rules
2. For each pixel:
   - If locked, skip
   - If pixel’s group is targeted, apply effect
3. Present output

There is:

- No state accumulation
- No drift
- No mutation of stored pixels

---

## 10. Broadcast Model

GRIN uses **broadcast, not invocation**.

- Group state may be broadcast outward
- Nothing may be called inward

GRIN:

- Does not call external code
- Does not request actions
- Does not embed hooks

If nothing is listening, nothing happens.

---

## 11. Editing vs Playback

Editing may:

- Reassign groups
- Change lock states
- Rewrite pixels

Playback may:

- Modulate display only
- Never rewrite stored data

These modes are strictly separated.

---

## 12. Security by Construction

GRIN enforces:

- Fixed header size
- Fixed rule count
- Fixed opcode set
- No executable code
- No dynamic allocation
- No external resource access

Malicious complexity is structurally impossible.

---

## 13. What GRIN Is Not

GRIN is not:

- A video format
- A scripting engine
- A shader language
- A game engine
- A procedural world system

Attempts to turn it into one violate its intent.

---

## 14. Final Principle

If a feature:

- Increases expressiveness without bound
- Requires interpretation instead of inspection
- Cannot be reasoned about statically
- Makes worst-case cost unknowable

It does not belong in GRIN.

Minimalism here is **structural**, not aesthetic.
