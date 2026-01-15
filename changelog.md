# GRIN Change Log — Completed Task Record

## Overview

**Project**: GRIN (Graphic Readdressable Indexed Nodes)  
**File Extensions**: `.grin`, `.grn`  
**Target Platforms**: Android (Java/Kotlin), Web Browsers (JavaScript)  
**Architecture**: Deterministic image container with 5-byte pixel structure (RGBA+C), 16 addressable channels, fixed 128-byte header, and bounded rule-based modulation.  
**Status**: This document now serves as the completed-task change log; remaining work has been moved to `shortlist.txt`.

---

## Phase 0: Project Scaffolding and Environment Setup

### 0.1 Repository Structure
- Create root directory structure:
  ```
  grin/
  ├── spec/                    # Specification documents
  ├── core/                    # Core library (reference implementation)
  │   ├── src/
  │   ├── tests/
  │   └── build/
  ├── android/                 # Android/Java implementation
  │   ├── lib/
  │   ├── demo/
  │   └── tests/
  ├── web/                     # JavaScript/Browser implementation
  │   ├── lib/
  │   ├── demo/
  │   └── tests/
  ├── tools/                   # CLI utilities
  │   ├── encoder/
  │   ├── decoder/
  │   └── validator/
  ├── samples/                 # Sample .grin files
  └── docs/                    # Generated documentation
  ```

### *** 0.2 Dependency Manifest Creation
- Create `package.json` for web implementation (npm/yarn)
- Create `build.gradle` / `pom.xml` for Android/Java implementation
- Create `Makefile` or `CMakeLists.txt` for native tooling (optional C reference)
- Define minimum SDK versions: Android API 19, ES6+ for browsers

### *** 0.3 CI/CD Pipeline Configuration
- Create `.github/workflows/build.yml` for automated builds
- Create `.github/workflows/test.yml` for automated testing
- Create `.github/workflows/lint.yml` for code quality checks
- Define artifact publishing rules for releases

### 0.4 Development Environment Documentation
- Create `CONTRIBUTING.md` with development setup instructions
- Create `ARCHITECTURE.md` with system overview diagrams
- Create `SECURITY.md` with security considerations

---

## Phase 1: Binary Format Specification Implementation

### 1.1 Magic Number and Versioning Constants
- Define `MAGIC_BYTES` = `[0x47, 0x52, 0x49, 0x4E]` ("GRIN" in ASCII)
  - Summary: Added `MAGIC_BYTES` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `VERSION_MAJOR` = `0x00`
  - Summary: Added `VERSION_MAJOR` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `VERSION_MINOR` = `0x00`
  - Summary: Added `VERSION_MINOR` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `HEADER_SIZE_BYTES` = `128`
  - Summary: Added `HEADER_SIZE_BYTES` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `PIXEL_SIZE_BYTES` = `5`
  - Summary: Added `PIXEL_SIZE_BYTES` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `MAX_RULE_COUNT` = `16`
  - Summary: Added `MAX_RULE_COUNT` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define `RULES_BLOCK_SIZE` = `64`
  - Summary: Added `RULES_BLOCK_SIZE` constants in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.

### 1.2 Header Structure Definition (128 bytes total)
- Implement header byte layout structure:
  ```
  Offset 0-3:   Magic (4 bytes, ASCII "GRIN")
  Offset 4:     VersionMajor (1 byte, uint8)
  Offset 5:     VersionMinor (1 byte, uint8)
  Offset 6-7:   HeaderSize (2 bytes, uint16 LE, must be 128)
  Offset 8-11:  Width (4 bytes, uint32 LE)
  Offset 12-15: Height (4 bytes, uint32 LE)
  Offset 16-19: TickMicros (4 bytes, uint32 LE)
  Offset 20:    RuleCount (1 byte, uint8, 0-16)
  Offset 21:    OpcodeSetId (1 byte, uint8)
  Offset 22-23: Flags (2 bytes, uint16 LE, reserved=0)
  Offset 24-31: PixelDataLength (8 bytes, uint64 LE)
  Offset 32-39: FileLength (8 bytes, uint64 LE, 0 allowed)
  Offset 40-47: PixelDataOffset64 (8 bytes, uint64 LE, must be 128)
  Offset 48-55: ReservedA (8 bytes, must be 0)
  Offset 56-63: ReservedB (8 bytes, must be 0)
  Offset 64-127: RulesBlock (64 bytes)
  ```
  - Summary: Added header field offsets/sizes in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.

### 1.3 Rules Block Structure (64 bytes, 16 × 4-byte entries)
- Define rule entry structure (4 bytes each):
  ```
  Bytes 0-1: GroupMask (uint16 LE, bit i → Group i)
  Byte 2:    Opcode (uint8)
  Byte 3:    Timing (uint8, oscillator parameter)
  ```
  - Summary: Added rule entry offsets/sizes in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Implement GroupMask bitmask interpretation (bits 0-15 → groups 0-15)
  - Summary: Added group mask helper functions in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Define timing parameter semantics (reader-defined oscillator control)
  - Summary: Documented timing semantics as a constant in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.

### 1.4 Pixel Structure Definition (5 bytes per pixel)
- Define pixel byte layout:
  ```
  Byte 0: R (uint8, Red channel, 0-255)
  Byte 1: G (uint8, Green channel, 0-255)
  Byte 2: B (uint8, Blue channel, 0-255)
  Byte 3: A (uint8, Alpha channel, 0-255)
  Byte 4: C (uint8, Control byte)
  ```
  - Summary: Added pixel field offsets in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.

### 1.5 Control Byte (C) Bit Field Definition
- Implement control byte bit extraction:
  ```
  Bits 0-3: Group ID (0-15, extracted via C & 0x0F)
  Bits 4-6: Reserved (must be 0, mask 0x70)
  Bit 7:    Lock bit (extracted via (C >> 7) & 0x01)
  ```
  - Summary: Added control-byte masks and extraction helpers in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.
- Create helper functions:
  - `getGroupId(controlByte)` → returns 0-15
  - `isLocked(controlByte)` → returns boolean
  - `setGroupId(controlByte, groupId)` → returns modified byte
  - `setLocked(controlByte, locked)` → returns modified byte
  - Summary: Added control-byte masks and helper functions in `web/lib/format.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFormat.kt`.

---

## Phase 2: Core Data Structures

### 2.1 GrinHeader Class/Struct
- Implement `GrinHeader` with all header fields
  - Summary: Added `GrinHeader` data structures in `web/lib/grin-header.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt`.
- Implement `GrinHeader.serialize()` → byte array (128 bytes)
  - Summary: Implemented 128-byte header serialization with little-endian writes in `web/lib/grin-header.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt`.
- Implement `GrinHeader.deserialize(bytes)` → GrinHeader
  - Summary: Added header deserialization for byte arrays with field parsing in `web/lib/grin-header.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt`.
- Implement `GrinHeader.validate()` → boolean with error messages
  - Summary: Added validation results with errors/warnings in `web/lib/grin-header.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` plus `android/lib/src/main/kotlin/io/grin/lib/GrinValidation.kt`.

### *** 2.2 GrinPixel Class/Struct
- Implement `GrinPixel` with R, G, B, A, C fields
  - Summary: Added `GrinPixel` data structures in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.getGroupId()` → int (0-15)
  - Summary: Implemented group ID accessors in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.isLocked()` → boolean
  - Summary: Implemented lock state checks in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.setGroupId(int)` → void
  - Summary: Implemented control-byte group mutation in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.setLocked(boolean)` → void
  - Summary: Implemented control-byte lock mutation in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.toBytes()` → byte[5]
  - Summary: Added pixel serialization in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `GrinPixel.fromBytes(byte[5])` → GrinPixel
  - Summary: Added pixel deserialization helpers in `web/lib/grin-pixel.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.

### *** 2.3 GrinRule Class/Struct
- Implement `GrinRule` with GroupMask, Opcode, Timing fields
  - Summary: Added `GrinRule` data structures in `web/lib/grin-rule.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinRule.kt`.
- Implement `GrinRule.targetsGroup(int groupId)` → boolean
  - Summary: Implemented group targeting checks in `web/lib/grin-rule.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinRule.kt`.
- Implement `GrinRule.serialize()` → byte[4]
  - Summary: Added rule serialization with little-endian group masks in `web/lib/grin-rule.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinRule.kt`.
- Implement `GrinRule.deserialize(byte[4])` → GrinRule
  - Summary: Added rule deserialization helpers in `web/lib/grin-rule.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinRule.kt`.

### 2.4 GrinImage Class/Struct
- Implement `GrinImage` container:
  - `header: GrinHeader`
  - `pixels: GrinPixel[]` (length = width × height)
  - `rules: GrinRule[]` (length = ruleCount, max 16)
  - Summary: Added `GrinImage` containers with header/pixel/rule storage in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement pixel indexing: `getPixel(x, y)` → GrinPixel
  - Summary: Implemented indexed pixel accessors in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement pixel setting: `setPixel(x, y, pixel)` → void
  - Summary: Implemented indexed pixel mutation in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement bounds checking for all accessors
  - Summary: Added coordinate bounds validation in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement `getPixelsByGroup(int groupId)` → GrinPixel[]
  - Summary: Added group filtering helpers in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement `getLockedPixels()` → GrinPixel[]
  - Summary: Added locked pixel filters in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.
- Implement `getUnlockedPixels()` → GrinPixel[]
  - Summary: Added unlocked pixel filters in `web/lib/grin-image.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinImage.kt`.

### 2.5 GrinFile Class/Struct
- Implement `GrinFile` as complete file representation
  - Summary: Added `GrinFile` containers in `web/lib/grin-file.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFile.kt`.
- Implement `GrinFile.load(path)` → GrinFile
  - Summary: Implemented path-based load for Android and Node.js in `android/lib/src/main/kotlin/io/grin/lib/GrinFile.kt` and `web/lib/grin-file.ts`.
- Implement `GrinFile.load(InputStream)` → GrinFile
  - Summary: Implemented InputStream-based load in `android/lib/src/main/kotlin/io/grin/lib/GrinFile.kt`.
- Implement `GrinFile.load(ArrayBuffer)` → GrinFile (JS)
  - Summary: Implemented ArrayBuffer/Uint8Array load in `web/lib/grin-file.ts`.
- Implement `GrinFile.save(path)` → void
  - Summary: Implemented path-based save for Android and Node.js in `android/lib/src/main/kotlin/io/grin/lib/GrinFile.kt` and `web/lib/grin-file.ts`.
- Implement `GrinFile.toBytes()` → byte[]
  - Summary: Implemented full file serialization in `web/lib/grin-file.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinFile.kt`.

---

## Phase 3: Binary I/O Implementation

### 3.1 Endianness Utilities
- Implement `writeUint16LE(value)` → byte[2]
  - Summary: Added LE write helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Implement `writeUint32LE(value)` → byte[4]
  - Summary: Added LE write helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Implement `writeUint64LE(value)` → byte[8]
  - Summary: Added LE write helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Implement `readUint16LE(bytes, offset)` → int
  - Summary: Added LE read helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Implement `readUint32LE(bytes, offset)` → long
  - Summary: Added LE read helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Implement `readUint64LE(bytes, offset)` → long
  - Summary: Added LE read helpers in `web/lib/endianness.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinEndianness.kt`.
- Create unit tests for all endianness functions
  - Summary: Added JUnit coverage in `android/lib/src/test/kotlin/io/grin/lib/GrinEndiannessTest.kt`.

### 3.2 Header Reader Implementation
- Implement `readHeader(InputStream)` → GrinHeader
  - Summary: Added header reads with validation in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and byte-based parsing in `web/lib/grin-io.ts`.
- Implement magic number validation (reject if != "GRIN")
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement header size validation (reject if != 128)
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement RuleCount validation (reject if > 16)
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement PixelDataOffset64 validation (reject if != 128)
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement PixelDataLength validation (reject if != width × height × 5)
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement FileLength validation (reject if non-zero and < 128 + PixelDataLength)
  - Summary: Enforced in `GrinHeader.validate()` via `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt` and `web/lib/grin-header.ts`, used by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement reserved field warnings (warn if non-zero)
  - Summary: Validation warnings returned by `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

### 3.3 Header Writer Implementation
- Implement `writeHeader(GrinHeader, OutputStream)` → void
  - Summary: Added header writers in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and byte-based writer in `web/lib/grin-io.ts`.
- Implement automatic PixelDataLength calculation
  - Summary: Calculated in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement automatic FileLength calculation (optional)
  - Summary: Optional file length auto-fill in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Ensure reserved fields are written as 0
  - Summary: Cleared flags/reserved fields in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

### 3.4 Pixel Data Reader Implementation
- Implement `readPixelData(InputStream, width, height)` → GrinPixel[]
  - Summary: Added InputStream reader in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and byte-based reader in `web/lib/grin-io.ts`.
- Implement streaming pixel read (minimize memory for large images)
  - Summary: Implemented sequential InputStream reads in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt`.
- Implement bounds validation (ensure exact byte count)
  - Summary: Enforced pixel length checks in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Implement control byte reserved bits validation (warn if bits 4-6 non-zero)
  - Summary: Added reserved-bit warnings in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

### 3.5 Pixel Data Writer Implementation
- Implement `writePixelData(GrinPixel[], OutputStream)` → void
  - Summary: Added streaming writers in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and byte-based writer in `web/lib/grin-io.ts`.
- Implement streaming pixel write for memory efficiency
  - Summary: Implemented per-pixel OutputStream writes in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt`.
- Ensure control byte reserved bits are cleared before write
  - Summary: Sanitized control bytes in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

### 3.6 Rules Block Reader Implementation
- Implement `readRulesBlock(byte[64], ruleCount)` → GrinRule[]
  - Summary: Added rules block readers in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Validate only first `ruleCount` entries are read
  - Summary: Implemented slicing by ruleCount in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Validate unused entries are zero (warn if not)
  - Summary: Added non-zero unused entry warnings in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Validate opcode against OpcodeSetId (reject unknown opcodes)
  - Summary: Enforced opcode validity for base set in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

### 3.7 Rules Block Writer Implementation
- Implement `writeRulesBlock(GrinRule[], ruleCount)` → byte[64]
  - Summary: Added rules block writers in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Zero-fill unused rule entries
  - Summary: Zero-filled via default-initialized blocks in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.
- Validate GroupMask values (all 16 bits valid)
  - Summary: Added group mask range checks in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `web/lib/grin-io.ts`.

---

## Phase 4: Validation Engine

### 4.1 Header Validation Suite
- Implement `validateMagic(bytes)` → ValidationResult
  - Summary: Added magic validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateVersion(major, minor)` → ValidationResult
  - Summary: Added version validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateHeaderSize(size)` → ValidationResult
  - Summary: Added header size validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateDimensions(width, height)` → ValidationResult
  - Summary: Added dimension validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateTickMicros(tick)` → ValidationResult
  - Summary: Added tick validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateRuleCount(count)` → ValidationResult
  - Summary: Added rule count validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateOpcodeSetId(id)` → ValidationResult
  - Summary: Added opcode set validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validatePixelDataLength(length, width, height)` → ValidationResult
  - Summary: Added pixel data length validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateFileLength(fileLen, dataLen)` → ValidationResult
  - Summary: Added file length validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validatePixelDataOffset(offset)` → ValidationResult
  - Summary: Added pixel data offset validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateReservedFields(a, b, flags)` → ValidationResult
  - Summary: Added reserved field validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.

### 4.2 Pixel Validation Suite
- Implement `validateControlByte(c)` → ValidationResult
  - Summary: Added control byte validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateGroupId(groupId)` → ValidationResult
  - Summary: Added group ID validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateReservedBits(c)` → ValidationResult
  - Summary: Added reserved bit validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.

### 4.3 Rule Validation Suite
- Implement `validateGroupMask(mask)` → ValidationResult
  - Summary: Added group mask validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateOpcode(opcode, opcodeSetId)` → ValidationResult
  - Summary: Added opcode validation for base set in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement `validateTiming(timing)` → ValidationResult
  - Summary: Added timing validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.

### 4.4 Full File Validation
- Implement `validateGrinFile(GrinFile)` → ValidationReport
  - Summary: Added full file validation in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Create ValidationReport structure with errors, warnings, info
  - Summary: Added ValidationReport types in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidation.kt`.
- Implement strict mode (reject on any warning)
  - Summary: Added strict mode handling in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.
- Implement permissive mode (continue on warnings)
  - Summary: Added permissive mode handling in `web/lib/validation.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinValidationSuite.kt`.

---

## Phase 5: Opcode System

### 5.1 Opcode Set Definition (OpcodeSetId = 0, Base Set)
- Define opcode enumeration:
  ```
  0x00: NOP       - No operation
  0x01: FADE_IN   - Increase alpha over time
  0x02: FADE_OUT  - Decrease alpha over time
  0x03: PULSE     - Oscillate alpha
  0x04: SHIFT_R   - Shift red channel
  0x05: SHIFT_G   - Shift green channel
  0x06: SHIFT_B   - Shift blue channel
  0x07: SHIFT_A   - Shift alpha channel
  0x08: INVERT    - Invert RGB channels
  0x09: ROTATE_HUE - Rotate hue in HSL space
  0x0A: LOCK      - Set lock bit
  0x0B: UNLOCK    - Clear lock bit
  0x0C: TOGGLE_LOCK - Toggle lock bit
  0x0D-0x0F: Reserved for future base set expansion
  ```
  - Summary: Defined base opcode IDs in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/BaseOpcodes.kt`.

### 5.2 Opcode Interface Definition
- Define `Opcode` interface:
  ```
  interface Opcode {
    byte getId();
    String getName();
    void apply(GrinPixel pixel, int tick, byte timing);
    int getMaxCpuCost();  // Predictable worst-case
    boolean requiresState();  // Must be false for GRIN compliance
  }
  ```
  - Summary: Added Opcode interfaces in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcode.kt`.

### 5.3 Opcode Implementation (Base Set)
- Implement `NopOpcode` (0x00)
  - Summary: Added `NopOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `FadeInOpcode` (0x01)
  - Summary: Added `FadeInOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `FadeOutOpcode` (0x02)
  - Summary: Added `FadeOutOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `PulseOpcode` (0x03)
  - Summary: Added `PulseOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `ShiftROpcode` (0x04)
  - Summary: Added `ShiftROpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `ShiftGOpcode` (0x05)
  - Summary: Added `ShiftGOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `ShiftBOpcode` (0x06)
  - Summary: Added `ShiftBOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `ShiftAOpcode` (0x07)
  - Summary: Added `ShiftAOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `InvertOpcode` (0x08)
  - Summary: Added `InvertOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `RotateHueOpcode` (0x09)
  - Summary: Added `RotateHueOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `LockOpcode` (0x0A)
  - Summary: Added `LockOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `UnlockOpcode` (0x0B)
  - Summary: Added `UnlockOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.
- Implement `ToggleLockOpcode` (0x0C)
  - Summary: Added `ToggleLockOpcode` in `web/lib/opcodes.ts` and `android/lib/src/main/kotlin/io/grin/lib/Opcodes.kt`.

### 5.4 Opcode Registry
- Implement `OpcodeRegistry` singleton/factory
  - Summary: Added opcode registries in `web/lib/opcode-registry.ts` and `android/lib/src/main/kotlin/io/grin/lib/OpcodeRegistry.kt`.
- Implement `getOpcode(opcodeSetId, opcodeId)` → Opcode
  - Summary: Implemented opcode lookup in `web/lib/opcode-registry.ts` and `android/lib/src/main/kotlin/io/grin/lib/OpcodeRegistry.kt`.
- Implement `isValidOpcode(opcodeSetId, opcodeId)` → boolean
  - Summary: Implemented opcode validation in `web/lib/opcode-registry.ts` and `android/lib/src/main/kotlin/io/grin/lib/OpcodeRegistry.kt`.
- Implement `listOpcodes(opcodeSetId)` → Opcode[]
  - Summary: Implemented opcode listing in `web/lib/opcode-registry.ts` and `android/lib/src/main/kotlin/io/grin/lib/OpcodeRegistry.kt`.

### *** 5.5 Timing Parameter Interpretation
- Define timing byte semantics:
  ```
  Bits 0-3: Period (0-15, maps to tick multiplier)
  Bits 4-5: Waveform (0=square, 1=triangle, 2=sine, 3=sawtooth)
  Bits 6-7: Phase offset (0-3, quarter-phase increments)
  ```
  - Summary: Implemented timing semantics in `web/lib/timing.ts` and `android/lib/src/main/kotlin/io/grin/lib/TimingInterpreter.kt`.
- Implement `TimingInterpreter.getPeriod(timing)` → int
  - Summary: Added period extraction in `web/lib/timing.ts` and `android/lib/src/main/kotlin/io/grin/lib/TimingInterpreter.kt`.
- Implement `TimingInterpreter.getWaveform(timing)` → WaveformType
  - Summary: Added waveform decoding in `web/lib/timing.ts` and `android/lib/src/main/kotlin/io/grin/lib/TimingInterpreter.kt`.
- Implement `TimingInterpreter.getPhaseOffset(timing)` → int
  - Summary: Added phase offset decoding in `web/lib/timing.ts` and `android/lib/src/main/kotlin/io/grin/lib/TimingInterpreter.kt`.
- Implement `TimingInterpreter.evaluate(timing, tick)` → float (0.0-1.0)
  - Summary: Added waveform evaluation in `web/lib/timing.ts` and `android/lib/src/main/kotlin/io/grin/lib/TimingInterpreter.kt`.

---

## Phase 6: Playback Engine

### 6.1 Playback State Model
- Implement `PlaybackState` class:
  ```
  - currentTick: long
  - isPlaying: boolean
  - tickAccumulatorMicros: long
  - displayBuffer: RGBA[] (output, no control byte)
  ```
  - Summary: Added playback state containers in `web/lib/playback-state.ts` and `android/lib/src/main/kotlin/io/grin/lib/PlaybackState.kt`.
- Ensure no mutation of source GrinImage pixels
  - Summary: Processing uses per-pixel working copies in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement tick counter with overflow handling
  - Summary: Added tick overflow handling in `web/lib/tick-scheduler.ts` and `android/lib/src/main/kotlin/io/grin/lib/AndroidTickScheduler.kt`/`android/lib/src/main/kotlin/io/grin/lib/TestTickScheduler.kt`.

### 6.2 Tick Scheduler
- Implement `TickScheduler` interface:
  ```
  interface TickScheduler {
    void start();
    void stop();
    void setTickCallback(TickCallback cb);
    long getCurrentTick();
  }
  ```
  - Summary: Added tick scheduler interfaces in `web/lib/tick-scheduler.ts` and `android/lib/src/main/kotlin/io/grin/lib/TickScheduler.kt`.
- Implement `AndroidTickScheduler` using Handler/Choreographer
  - Summary: Implemented Choreographer-based scheduler in `android/lib/src/main/kotlin/io/grin/lib/AndroidTickScheduler.kt`.
- Implement `BrowserTickScheduler` using requestAnimationFrame
  - Summary: Implemented RAF-based scheduler in `web/lib/tick-scheduler.ts`.
- Implement `TestTickScheduler` for deterministic testing
  - Summary: Added deterministic schedulers in `web/lib/tick-scheduler.ts` and `android/lib/src/main/kotlin/io/grin/lib/TestTickScheduler.kt`.

### 6.3 Rule Evaluation Engine
- Implement `RuleEngine.evaluateRules(GrinImage, tick)` → ActiveRule[]
  - Summary: Added rule engine implementations in `web/lib/rule-engine.ts` and `android/lib/src/main/kotlin/io/grin/lib/RuleEngine.kt`.
- Implement timing-based rule activation:
  ```
  For each rule:
    - Extract timing parameter
    - Evaluate waveform at current tick
    - Determine if rule is active this tick
  ```
  - Summary: Implemented timing-based activation in `web/lib/rule-engine.ts` and `android/lib/src/main/kotlin/io/grin/lib/RuleEngine.kt`.
- Implement rule priority (lower index = higher priority, no conflicts)
  - Summary: Preserved rule order in `web/lib/rule-engine.ts` and `android/lib/src/main/kotlin/io/grin/lib/RuleEngine.kt`.

### 6.4 Pixel Processing Pipeline
- Implement per-tick processing loop:
  ```
  for each pixel in image:
    if pixel.isLocked():
      outputBuffer[i] = pixel.RGBA  // Skip, copy as-is
      continue
    
    for each activeRule:
      if activeRule.targetsGroup(pixel.groupId):
        opcode = getOpcode(activeRule.opcode)
        opcode.apply(pixel, tick, activeRule.timing)
    
    outputBuffer[i] = pixel.RGBA  // After modulation
  ```
  - Summary: Added per-tick processing in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Ensure display buffer is separate from source pixels
  - Summary: Display buffers are separate types in `web/lib/display-buffer.ts` and `android/lib/src/main/kotlin/io/grin/lib/DisplayBuffer.kt`.
- Implement SIMD optimization hints (optional)
  - Summary: Added packed RGBA output path in `web/lib/grin-player.ts` and `tools/lib/render.js`.

### 6.5 Display Buffer Management
- Implement `DisplayBuffer` class:
  ```
  - width: int
  - height: int
  - rgbaData: byte[] (width × height × 4)
  ```
  - Summary: Added display buffer implementations in `web/lib/display-buffer.ts` and `android/lib/src/main/kotlin/io/grin/lib/DisplayBuffer.kt`.
- Implement `DisplayBuffer.clear()` → void
  - Summary: Added clear methods in `web/lib/display-buffer.ts` and `android/lib/src/main/kotlin/io/grin/lib/DisplayBuffer.kt`.
- Implement `DisplayBuffer.setPixel(x, y, r, g, b, a)` → void
  - Summary: Added pixel setters in `web/lib/display-buffer.ts` and `android/lib/src/main/kotlin/io/grin/lib/DisplayBuffer.kt`.
- Implement `DisplayBuffer.toImageData()` → platform-specific image
  - Summary: Implemented `toImageData()` in `web/lib/display-buffer.ts` and `toBitmap()` in `android/lib/src/main/kotlin/io/grin/lib/DisplayBuffer.kt`.

### 6.6 Playback Controller
- Implement `GrinPlayer` class:
  ```
  - image: GrinImage
  - state: PlaybackState
  - scheduler: TickScheduler
  - ruleEngine: RuleEngine
  - displayBuffer: DisplayBuffer
  ```
  - Summary: Added playback controllers in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.load(GrinFile)` → void
  - Summary: Implemented load routines in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.play()` → void
  - Summary: Implemented play logic in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.pause()` → void
  - Summary: Implemented pause logic in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.stop()` → void
  - Summary: Implemented stop/reset logic in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.seek(tick)` → void
  - Summary: Implemented seek logic in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.
- Implement `GrinPlayer.getCurrentFrame()` → DisplayBuffer
  - Summary: Implemented frame accessors in `web/lib/grin-player.ts` and `android/lib/src/main/kotlin/io/grin/lib/GrinPlayer.kt`.

---

## Phase 7: Android Implementation (Java/Kotlin)

### *** 7.1 Android Module Setup
- Create `grin-android` module with Gradle configuration
  - Summary: Confirmed module setup in `android/settings.gradle.kts` and `android/build.gradle.kts`.
- Set minimum SDK version (API 21)
  - Summary: Set minSdk to 21 in `android/lib/build.gradle.kts` and `android/demo/build.gradle.kts` (docs aligned in `CONTRIBUTING.md`).
- Configure ProGuard/R8 rules for library
  - Summary: Added ProGuard files in `android/lib/proguard-rules.pro` and `android/lib/consumer-rules.pro`.
- Set up Android-specific dependencies
  - Summary: Dependencies live in `android/lib/build.gradle.kts` and `android/demo/build.gradle.kts`.

### 7.2 Java Binary I/O
- Implement `GrinInputStream` extending `FilterInputStream`
  - Summary: Added `GrinInputStream` in `android/lib/src/main/kotlin/io/grin/lib/GrinInputStream.kt`.
- Implement `GrinOutputStream` extending `FilterOutputStream`
  - Summary: Added `GrinOutputStream` in `android/lib/src/main/kotlin/io/grin/lib/GrinOutputStream.kt`.
- Handle Java's signed byte quirks (& 0xFF for unsigned)
  - Summary: Unsigned handling is applied in `android/lib/src/main/kotlin/io/grin/lib/GrinBinaryIO.kt` and `android/lib/src/main/kotlin/io/grin/lib/GrinPixel.kt`.
- Implement `ByteBuffer` usage for efficient header parsing
  - Summary: Header parsing uses ByteBuffer in `android/lib/src/main/kotlin/io/grin/lib/GrinHeader.kt`.

### 7.3 Android Bitmap Integration
- Implement `GrinBitmapRenderer`:
  ```
  - Takes DisplayBuffer
  - Outputs android.graphics.Bitmap (ARGB_8888)
  ```
  - Summary: Added renderer in `android/lib/src/main/kotlin/io/grin/lib/GrinBitmapRenderer.kt`.
- Implement efficient pixel copying (use `copyPixelsFromBuffer`)
  - Summary: Renderer converts RGBA to ARGB and uses `copyPixelsFromBuffer` in `android/lib/src/main/kotlin/io/grin/lib/GrinBitmapRenderer.kt`.
- Handle Bitmap recycling and memory management
  - Summary: Renderer reuses existing bitmaps when dimensions match in `android/lib/src/main/kotlin/io/grin/lib/GrinBitmapRenderer.kt`.

### *** 7.4 Android View Component
- Implement `GrinView` extending `View`:
  ```
  - Handles drawing of GrinBitmap
  - Manages playback lifecycle
  - Supports touch interaction (pause/play)
  ```
  - Summary: Added `GrinView` in `android/lib/src/main/kotlin/io/grin/lib/GrinView.kt`.
- Implement `onDraw(Canvas)` with Bitmap rendering
  - Summary: Rendering implemented in `android/lib/src/main/kotlin/io/grin/lib/GrinView.kt`.
- Implement `onAttachedToWindow()` / `onDetachedFromWindow()` lifecycle
  - Summary: Lifecycle handling implemented in `android/lib/src/main/kotlin/io/grin/lib/GrinView.kt`.
- Implement `onMeasure()` with aspect ratio preservation
  - Summary: Aspect-ratio measurement implemented in `android/lib/src/main/kotlin/io/grin/lib/GrinView.kt`.

### 7.5 Android Choreographer Integration
- Implement `ChoreographerTickScheduler`:
  ```
  - Uses Choreographer.postFrameCallback
  - Calculates tick delta from frame timestamps
  - Handles vsync alignment
  ```
  - Summary: Choreographer-based scheduler provided by `android/lib/src/main/kotlin/io/grin/lib/ChoreographerTickScheduler.kt` and `android/lib/src/main/kotlin/io/grin/lib/AndroidTickScheduler.kt`.
- Implement frame rate limiting (honor TickMicros)
  - Summary: TickMicros-based throttling implemented in `android/lib/src/main/kotlin/io/grin/lib/AndroidTickScheduler.kt`.
- Handle background/foreground transitions
  - Summary: Scheduler start/stop used for lifecycle transitions via `android/lib/src/main/kotlin/io/grin/lib/GrinView.kt`.

### 7.6 Android Content Provider Support
- Implement `GrinContentProvider` for file:// and content:// URIs
  - Summary: Added provider in `android/lib/src/main/kotlin/io/grin/lib/GrinContentProvider.kt` and registered in `android/demo/src/main/AndroidManifest.xml`.
- Handle `ContentResolver.openInputStream()`
  - Summary: Added URI loading via ContentResolver in `android/lib/src/main/kotlin/io/grin/lib/GrinUriLoader.kt`.
- Support loading from assets, raw resources, and external storage
  - Summary: Implemented asset/raw/file loaders in `android/lib/src/main/kotlin/io/grin/lib/GrinUriLoader.kt`.

### *** 7.7 Android Demo Application
- Create demo app with file picker
  - Summary: Added file picker flow in `android/demo/src/main/kotlin/io/grin/demo/MainActivity.kt`.
- Implement gallery view of sample .grin files
  - Summary: Added asset-backed samples list in `android/demo/src/main/kotlin/io/grin/demo/MainActivity.kt` and `android/demo/src/main/assets/samples/minimal.grin`.
- Add playback controls (play, pause, seek)
  - Summary: Added playback controls in `android/demo/src/main/res/layout/activity_main.xml` and wired in `android/demo/src/main/kotlin/io/grin/demo/MainActivity.kt`.
- Display file metadata (dimensions, rule count, tick rate)
  - Summary: Metadata display implemented in `android/demo/src/main/kotlin/io/grin/demo/MainActivity.kt`.

### *** 7.8 Android Grid Camera + Gallery App
- Define UX flow and data model for grid camera + gallery app
  - Document screen map (camera preview, capture review, gallery grid, editor)
  - Specify posterization palette handling (12-14 color groups) and channel mapping (0-9/A-F)
  - Specify GRIM/GRIN file metadata updates (header fields, channel settings)
  - Define export options (GRIN header update, PNG snapshot with alpha, 12-15 frame GIF loop)
  - Summary: Documented UX flow, data model, palette strategy, and export requirements in `docs/android-grid-camera-ux.md`.
- Implement posterized camera preview pipeline
  - Add camera preview frame acquisition (CameraX ImageAnalysis, YUV->RGB conversion)
  - Enforce grid-aligned output dimensions (crop/scale to N x M grid cells)
  - Apply 12-14 color posterization shader (RenderScript/RenderEffect) or CPU pipeline (LUT + k-means fallback)
  - Display posterized preview with live grid overlay (cell borders + channel labels)
  - Document performance targets (30/60fps) and fallback quality settings (reduced grid, lower poster bins)
- Implement capture flow and channel assignment
  - Capture posterized frame buffer and generate GRIM/GRIN payload (RGBA+C)
  - Build palette histogram and auto-assign bins to channels 0-9/A-F (stable sort by frequency)
  - Persist GRIM/GRIN files with updated header settings (TickMicros, RuleCount, channel metadata)
  - Log capture metadata for gallery indexing (timestamp, dimensions, palette bins)
  - Summary: Capture flow saves posterized frames to GRIN/GRIM with stable channel mapping plus metadata logging in `android/demo/src/main/kotlin/io/grin/demo/GrinAssetStore.kt`.
- Build gallery grid and editor UI
  - Render gallery grid of GRIN/GRIM assets with posterized thumbnails (lazy paging, disk cache)
  - Add channel selector dropdown (0-9/A-F) in editor (default to most frequent bin)
  - Add sliders for frequency, color intonation, and transparency (live preview updates)
  - Apply edits to in-memory preview and persist to header settings (rules + channel overrides)
  - Summary: Gallery grid and editor controls implemented in `android/demo/src/main/kotlin/io/grin/demo/GalleryActivity.kt`, `GalleryAdapter.kt`, and `EditorActivity.kt`.
- Export workflows
  - Export PNG snapshot with alpha from current GRIN state (ARGB_8888 bitmap)
  - Export GIF loop (12-15 frames) derived from GRIN playback (fixed tick interval)
  - Export updated GRIN/GRIM file with new header settings (channel metadata + rules)
- Validation, tests, and documentation
  - Validate posterization color bin counts and channel mapping (unit tests for palette mapping)
  - Add unit tests for channel assignment and header updates
  - Add integration tests for export formats (PNG/GIF round-trip)
  - Update docs for camera/gallery workflow and export options
  - Summary: Added Android instrumented tests for channel mapping/header updates and PNG/GIF export round-trips; updated grid camera docs with export validation/storage notes.

---

## Phase 8: Web/Browser Implementation (JavaScript)

### *** 8.1 JavaScript Module Setup
- Create `grin-web` package with npm configuration
  - Summary: Package metadata and scripts live in `web/package.json`.
- Configure TypeScript (optional, recommended)
  - Summary: TypeScript configuration remains in `web/tsconfig.json`.
- Set up Rollup/Webpack for bundling
  - Summary: Added Rollup config in `web/rollup.config.mjs` and updated scripts in `web/package.json`.
- Configure ESM and UMD output formats
  - Summary: Rollup outputs ESM, CJS, and UMD bundles via `web/rollup.config.mjs`.

### 8.2 JavaScript Binary I/O
- Implement `GrinReader` class:
  ```
  - Takes ArrayBuffer or Uint8Array
  - Uses DataView for endian-correct reads
  ```
  - Summary: Added `GrinReader` in `web/lib/grin-reader.ts`.
- Implement `GrinWriter` class:
  ```
  - Outputs Uint8Array
  - Uses DataView for endian-correct writes
  ```
  - Summary: Added `GrinWriter` in `web/lib/grin-writer.ts`.
- Handle JavaScript number limitations (safe integers)
  - Summary: Safe integer checks remain in `web/lib/grin-file.ts` and `web/lib/grin-io.ts`.

### 8.3 Canvas Rendering Integration
- Implement `GrinCanvasRenderer`:
  ```
  - Takes DisplayBuffer
  - Outputs to HTMLCanvasElement
  - Uses ImageData and putImageData
  ```
  - Summary: Added canvas renderer in `web/lib/grin-canvas.ts`.
- Implement efficient pixel transfer
  - Summary: Renderer reuses ImageData buffers in `web/lib/grin-canvas.ts`.
- Support OffscreenCanvas for worker rendering
  - Summary: Renderer accepts OffscreenCanvas in `web/lib/grin-canvas.ts`.

### *** 8.4 Web Component
- Implement `<grin-player>` custom element:
  ```html
  <grin-player src="image.grin" autoplay loop></grin-player>
  ```
  - Summary: Added custom element in `web/lib/grin-element.ts`.
- Implement attribute observation (src, autoplay, loop, playbackrate)
  - Summary: Observed attributes handled in `web/lib/grin-element.ts`.
- Implement shadow DOM encapsulation
  - Summary: Shadow DOM with canvas container in `web/lib/grin-element.ts`.
- Expose JavaScript API (play, pause, currentTime, etc.)
  - Summary: Exposed play/pause/currentTime APIs in `web/lib/grin-element.ts` and tick accessors in `web/lib/grin-player.ts`.

### 8.5 requestAnimationFrame Scheduler
- Implement `RAFTickScheduler`:
  ```
  - Uses requestAnimationFrame
  - Calculates tick delta from timestamps
  - Handles tab visibility changes
  ```
  - Summary: Added RAFTickScheduler in `web/lib/tick-scheduler.ts`.
- Implement frame rate limiting (honor TickMicros)
  - Summary: TickMicros throttling in `web/lib/tick-scheduler.ts`.
- Handle `document.hidden` for background tabs
  - Summary: Visibility handling in `web/lib/tick-scheduler.ts`.

### 8.6 Fetch/File API Integration
- Implement `GrinLoader.fromURL(url)` → Promise<GrinFile>
  - Summary: Added URL loading in `web/lib/grin-loader.ts`.
- Implement `GrinLoader.fromFile(File)` → Promise<GrinFile>
  - Summary: Added File loading in `web/lib/grin-loader.ts`.
- Implement `GrinLoader.fromBlob(Blob)` → Promise<GrinFile>
  - Summary: Added Blob loading in `web/lib/grin-loader.ts`.
- Handle streaming for large files (ReadableStream)
  - Summary: Stream-to-buffer support in `web/lib/grin-loader.ts`.

### *** 8.7 Web Demo Application
- Create demo HTML page with drag-and-drop
  - Summary: Added demo page in `web/demo/index.html` and `web/demo/styles.css`.
- Implement file upload interface
  - Summary: File picker and drop zone implemented in `web/demo/demo.js`.
- Add playback controls (play, pause, seek slider)
  - Summary: Playback controls wired in `web/demo/demo.js` and UI in `web/demo/index.html`.
- Display file metadata and validation results
  - Summary: Metadata display added in `web/demo/demo.js`.
- Add sample .grin file gallery
  - Summary: Sample gallery implemented in `web/demo/demo.js` with `web/demo/samples/samples.json` and `web/demo/samples/minimal.grin`.

---

## Phase 9: CLI Tools

### *** 9.1 Validator Tool (`grin-validate`)
- Implement command-line interface:
  ```
  grin-validate <file.grin> [--strict] [--json]
  ```
  - Summary: Added `tools/bin/grin-validate.js` with strict/json flags and shared parsing/validation helpers in `tools/lib/grin.js` + `tools/lib/validation.js`.
- Output validation results (errors, warnings)
  - Summary: Emits per-file info/warnings/errors in text mode or structured JSON.
- Return exit code 0 on success, non-zero on failure
  - Summary: Exits 0 only when all inputs validate (strict mode treats warnings as failures).
- Support batch validation of multiple files
  - Summary: Accepts multiple file paths and aggregates output/exit status.

### *** 9.2 Inspector Tool (`grin-inspect`)
- Implement command-line interface:
  ```
  grin-inspect <file.grin> [--header] [--rules] [--groups] [--pixels]
  ```
  - Summary: Added `tools/bin/grin-inspect.js` with header/rules/groups/pixels selectors and JSON output.
- Output human-readable header information
  - Summary: Prints header fields including sizes, offsets, and version metadata.
- Output rule definitions with decoded opcodes
  - Summary: Lists rule masks/opcodes with opcode name mapping from `BaseOpcodeId`.
- Output group pixel counts and statistics
  - Summary: Emits per-group pixel counts plus locked pixel count.
- Support JSON output format
  - Summary: JSON report includes header/rules/groups/pixels sections as requested.

### *** 9.3 Encoder Tool (`grin-encode`)
- Implement command-line interface:
  ```
  grin-encode <input.png> <output.grin> [--groups <mask.png>] [--rules <rules.json>]
  ```
  - Summary: Added `tools/bin/grin-encode.js` using `tools/lib/png.js` and `tools/lib/grin.js`.
- Read PNG/JPEG input and convert to GRIN format
  - Summary: `tools/lib/png.js` reads PNG/JPEG into RGBA for encoder.
- Support group assignment via mask image (indexed colors → group IDs)
  - Summary: Mask image red channel maps to group IDs with 0-15 clamping.
- Support rule definition via JSON configuration
  - Summary: Encoder parses rule JSON (array or `{ rules, tickMicros, opcodeSetId }`) and builds rules block.
- Validate output file after encoding
  - Summary: Runs `validateGrinFile` before writing output bytes.

### *** 9.4 Decoder Tool (`grin-decode`)
- Implement command-line interface:
  ```
  grin-decode <input.grin> <output.png> [--frame <tick>] [--groups]
  ```
  - Summary: Added `tools/bin/grin-decode.js` with frame selection and group map output.
- Export single frame as PNG (at specified tick or tick 0)
  - Summary: `tools/lib/render.js` renders ticks with opcodes and `tools/lib/png.js` writes PNG/JPEG output.
- Export group visualization (color-coded by group ID)
  - Summary: `renderGroupMap()` uses a 16-color palette per group ID.
- Support animated GIF export (optional)
  - Summary: Added GIF export to `tools/bin/grin-decode.js` with `tools/lib/gif.js`.

---

## Phase 10: Testing Framework

### 10.1 Unit Test Infrastructure
- Set up JUnit 5 for Java/Android tests
  - Summary: Added JUnit 5 plugin/deps in `android/build.gradle.kts` and `android/lib/build.gradle.kts`, with a JUnit 5 reference test in `android/lib/src/test/kotlin/io/grin/lib/GrinReferenceTest.kt`.
- Set up Jest/Vitest for JavaScript tests
  - Summary: Switched web tests to Vitest with `web/vitest.config.ts` and updated `web/package.json`.
- Create shared test fixtures directory
  - Summary: Added shared fixtures at `tests/fixtures` with `tests/fixtures/grin-fixtures.json`.
- Implement test file generators
  - Summary: Added generator script `scripts/generate-test-files.js` to emit fixtures into `tests/fixtures/generated`.

### 10.2 Binary Format Tests
- Test header serialization/deserialization round-trip
  - Summary: Added `web/tests/format-roundtrip.test.ts` to cover `GrinHeader` serialize/deserialize.
- Test pixel serialization/deserialization round-trip
  - Summary: Added `GrinPixel` round-trip coverage in `web/tests/format-roundtrip.test.ts`.
- Test rule serialization/deserialization round-trip
  - Summary: Added `GrinRule` round-trip coverage in `web/tests/format-roundtrip.test.ts`.
- Test endianness handling on big-endian systems (simulated)
  - Summary: Added LE/BE simulation coverage in `web/tests/endianness.test.ts`.
- Test boundary values (max width, max height, max rules)
  - Summary: Added boundary checks for max dimensions and rule count in `web/tests/validation-boundaries.test.ts`.

### 10.3 Validation Tests
- Test magic number validation (valid and invalid)
  - Summary: Added magic validation assertions in `web/tests/validation-boundaries.test.ts`.
- Test header size validation (valid and invalid)
  - Summary: Added header size validation assertions in `web/tests/validation-boundaries.test.ts`.
- Test dimension validation (0, 1, max, overflow)
  - Summary: Added dimension boundary tests in `web/tests/validation-boundaries.test.ts`.
- Test rule count validation (0, 16, 17)
  - Summary: Added rule count boundary tests in `web/tests/validation-boundaries.test.ts`.
- Test PixelDataLength validation (correct, underflow, overflow)
  - Summary: Added pixel data length checks in `web/tests/validation-boundaries.test.ts`.
- Test reserved field warnings
  - Summary: Added reserved field warning coverage in `web/tests/validation-boundaries.test.ts`.

### 10.4 Opcode Tests
- Test each opcode implementation individually
  - Summary: Covered base opcode behavior in `android/lib/src/test/kotlin/io/grin/lib/OpcodesTest.kt`.
- Test opcode timing parameter interpretation
  - Summary: Added waveform/period evaluation tests in `web/tests/timing.test.ts`.
- Test opcode worst-case CPU cost bounds
  - Summary: Verified `getMaxCpuCost()` bounds in `web/tests/opcodes.test.ts`.
- Test opcode statelessness (no accumulated state)
  - Summary: Verified determinism and `requiresState()` in `web/tests/opcodes.test.ts`.

### 10.5 Playback Tests
- Test tick advancement accuracy
  - Summary: Added scheduler-driven tick tests in `web/tests/playback.test.ts`.
- Test rule activation based on timing
  - Summary: Added timing-triggered rule activation coverage in `web/tests/playback.test.ts`.
- Test locked pixel immunity
  - Summary: Added locked pixel immunity coverage in `web/tests/playback.test.ts`.
- Test group targeting correctness
  - Summary: Added group mask targeting assertions in `web/tests/playback.test.ts`.
- Test display buffer isolation from source
  - Summary: Added source immutability checks in `web/tests/playback.test.ts`.

### 10.6 Integration Tests
- Test full file load → validate → play → render cycle
  - Summary: Added full cycle test in `web/tests/integration.test.ts`.
- Test Android implementation against reference
  - Summary: Added reference byte comparison in `android/lib/src/test/kotlin/io/grin/lib/GrinReferenceTest.kt`.
- Test JavaScript implementation against reference
  - Summary: Added reference byte comparison in `web/tests/reference.test.ts`.
- Test cross-platform file compatibility
  - Summary: Added CLI parser compatibility checks in `web/tests/integration.test.ts`.
- Run Android Gradle unit tests
  - Summary: `./gradlew test` succeeded.

### 10.7 Fuzz Testing
- Implement header fuzzer (randomized invalid headers)
  - Summary: Added randomized header coverage in `web/tests/fuzz.test.ts`.
- Implement pixel data fuzzer (truncated, corrupted)
  - Summary: Added truncation fuzz coverage in `web/tests/fuzz.test.ts`.
- Implement rule fuzzer (invalid opcodes, masks)
  - Summary: Added randomized rules block fuzzing in `web/tests/fuzz.test.ts`.
- Verify graceful failure (no crashes, clear errors)
  - Summary: Added error-message assertions in `web/tests/fuzz.test.ts`.

### *** 10.8 Performance Benchmarks
- Benchmark file loading time (various sizes)
  - Summary: Added multi-size benchmark harness in `benchmarks/bench.js`.
- Benchmark validation time
  - Summary: Validation timing captured in `benchmarks/bench.js`.
- Benchmark per-tick processing time
  - Summary: Per-tick timing captured via `renderFrame` loops in `benchmarks/bench.js`.
- Benchmark rendering time (Android Bitmap, Canvas)
  - Summary: Added Android render benchmarks in `android/lib/src/androidTest/kotlin/io/grin/lib/GrinBenchmarkTest.kt`.
- Establish performance regression baselines
  - Summary: Baseline storage in `benchmarks/baseline.json` with harness to update via `benchmarks/bench.js`.

---

## Phase 11: Sample File Creation

### *** 11.1 Minimal Valid File
- Create `minimal.grin`: 1×1 pixel, 0 rules
  - Summary: Added `samples/minimal.grin`.
- Create `minimal_locked.grin`: 1×1 locked pixel, 0 rules
  - Summary: Added `samples/minimal_locked.grin`.
- Document expected byte-for-byte content
  - Summary: Documented byte dumps in `samples/README.md`.

### *** 11.2 Basic Animation Files
- Create `pulse_red.grin`: 16×16, single group pulsing
  - Summary: Added `samples/pulse_red.grin`.
- Create `fade_gradient.grin`: 64×64, multi-group fade
  - Summary: Added `samples/fade_gradient.grin`.
- Create `color_shift.grin`: 32×32, RGB channel shifts
  - Summary: Added `samples/color_shift.grin`.

### *** 11.3 Complex Demo Files
- Create `groups_demo.grin`: Demonstrates all 16 groups
  - Summary: Added `samples/groups_demo.grin`.
- Create `locking_demo.grin`: Demonstrates lock/unlock opcodes
  - Summary: Added `samples/locking_demo.grin` with lock/unlock rules.
- Create `timing_demo.grin`: Demonstrates timing variations
  - Summary: Added `samples/timing_demo.grin`.

### *** 11.4 Edge Case Files
- Create `max_size.grin`: Maximum reasonable dimensions
  - Summary: Added `samples/max_size.grin` (512×512).
- Create `max_rules.grin`: 16 rules, all groups targeted
  - Summary: Added `samples/max_rules.grin`.
- Create `all_opcodes.grin`: Uses every defined opcode
  - Summary: Added `samples/all_opcodes.grin`.

### *** 11.5 Invalid Files (for testing)
- Create `invalid_magic.grin`: Wrong magic bytes
  - Summary: Added `samples/invalid_magic.grin`.
- Create `invalid_header_size.grin`: HeaderSize != 128
  - Summary: Added `samples/invalid_header_size.grin`.
- Create `truncated.grin`: File ends mid-pixel
  - Summary: Added `samples/truncated.grin`.
- Create `invalid_opcode.grin`: Unknown opcode ID
  - Summary: Added `samples/invalid_opcode.grin`.

---

## Phase 12: Documentation

### 12.1 Specification Documentation
- Finalize and version `grin_technical_specification.md`
  - Summary: Final spec added at `grin_technical_specification.md` (draft copy at `tchspecdraft.txt`).
- Create byte-level format diagram (visual)
  - Summary: Added `docs/format-diagram.md`.
- Create opcode reference table with examples
  - Summary: Added `docs/opcodes.md`.
- Create timing parameter reference
  - Summary: Added `docs/timing.md`.

### *** 12.2 API Documentation
- Generate JavaDoc for Android implementation
  - Summary: Draft API reference in `docs/api/android/README.md` and Dokka config added.
- Generate JSDoc/TypeDoc for JavaScript implementation
  - Summary: TypeDoc output in `docs/api/web`.
- Create API quick-start guide
  - Summary: Added `docs/api-quickstart.md`.
- Create migration guide (from GIF/APNG)
  - Summary: Added `docs/migration-gif-apng.md`.

### *** 12.3 Tutorial Documentation
- Write "Creating Your First GRIN File" tutorial
  - Summary: Added `docs/tutorial-first-file.md`.
- Write "Understanding GRIN Groups" tutorial
  - Summary: Added `docs/tutorial-grin-groups.md`.
- Write "Playback Integration Guide" (Android)
  - Summary: Added `docs/playback-guide-android.md`.
- Write "Playback Integration Guide" (Web)
  - Summary: Added `docs/playback-guide-web.md`.

### 12.4 Security Documentation
- Document security model (no executable code)
  - Summary: Added `docs/security-model.md`.
- Document bounded resource consumption
  - Summary: Covered in `docs/security-model.md`.
- Document validation requirements for untrusted input
  - Summary: Covered in `docs/security-model.md`.
- Create security audit checklist
  - Summary: Added `docs/security-audit-checklist.md`.

---

## Phase 13: Release Preparation

### 13.1 Version Tagging
- Define semantic versioning scheme
- Create CHANGELOG.md with release notes

### *** 13.2 Package Publishing

### 13.3 Compatibility Matrix
- Document Android API level compatibility
- Document browser version compatibility
- Document Node.js version compatibility

### 13.4 Legal and Licensing
- Choose and apply open-source license
- Create NOTICE file for third-party attributions

---

## Phase 14: Creative Suite + GIMP Plugins

### 14.1 Shared foundations (all plugins)
- Define the pixel-group editing model and UX goals for painting group IDs + lock bits
  - Deliverable: a 1-page UX spec describing how artists select groups, toggle lock, and preview rules.
  - Summary: Documented the shared UX model in `docs/creative-suite-foundations.md`.
- Decide on an interchange format between host apps and the GRIN toolchain
  - Likely: export a layered PNG + JSON sidecar with group/lock metadata.
  - Summary: Standardized PNG + groups PNG + rules JSON artifacts in `docs/creative-suite-foundations.md`.
- Create a shared validation CLI workflow for plugin export
  - Use `tools/bin/grin-validate.js` and `tools/bin/grin-inspect.js` as post-export checks.
  - Summary: Defined the CLI validation flow in `docs/creative-suite-foundations.md`.
- Draft a shared palette/legend for group IDs (0-15) and lock state overlays
  - Define color mapping and UI affordances for every plugin.
  - Summary: Added the palette/legend table in `docs/creative-suite-foundations.md`.
- Define output paths and naming conventions for exports (`.grin`, `.png`, `.json`)
  - Make sure it matches `tools/bin/grin-encode.js` expectations.
  - Summary: Standardized export naming in `docs/creative-suite-foundations.md`.

### 14.2 Photoshop plugin (UXP)
- Set up `plugins/photoshop/` workspace with UXP manifest + dev tooling
  - Include a simple panel scaffold for group/lock selection and export.
  - Summary: Added the Photoshop UXP panel scaffold, manifest, and package metadata in `plugins/photoshop/`.
- Implement layer metadata capture
  - Map layer names or layer tags to group IDs (0-15) and lock bit.
  - Summary: Added tag parsing and layer metadata capture in `plugins/photoshop/src/main.js`.
- Implement pixel-to-group map extraction
  - Render a flattened bitmap with hidden metadata encoding group + lock values.
  - Summary: Exported named metadata layers for group/lock maps in `plugins/photoshop/src/main.js`.
- Implement export pipeline
  - Generate a JSON sidecar and call `tools/bin/grin-encode.js`.
  - Summary: Added PNG export, rules sidecar creation, and CLI command guidance in `plugins/photoshop/src/main.js`.
- Add preview/validation step
  - Use `grin-validate` and report errors in the panel UI.
  - Summary: Added preview metadata summary and validation command output in `plugins/photoshop/src/main.js`.
- Write Photoshop plugin README with install + usage steps
  - Summary: Documented installation, tagging, and export workflow in `plugins/photoshop/README.md`.

### 14.3 Illustrator plugin (UXP)
- Set up `plugins/illustrator/` workspace with UXP manifest + dev tooling
  - Include a panel scaffold matching Photoshop UX.
  - Summary: Added the Illustrator UXP panel scaffold, manifest, and package metadata in `plugins/illustrator/`.
- Implement vector art flattening rules for GRIN export
  - Define how artboards/paths map to pixels, group IDs, and lock bit.
  - Summary: Documented artboard-to-pixel flattening rules and resolution handling in `plugins/illustrator/src/main.js`.
- Implement group/lock metadata tagging in document model
  - Use layer names, object tags, or custom metadata fields.
  - Summary: Added name/note tag parsing for group and lock metadata in `plugins/illustrator/src/main.js`.
- Implement export pipeline
  - Rasterize at user-specified resolution and call `grin-encode.js`.
  - Summary: Added artboard PNG export, metadata map export, and CLI command guidance in `plugins/illustrator/src/main.js`.
- Add preview/validation step
  - Use `grin-validate` and surface errors to the panel.
  - Summary: Added preview summary output and validation command output in `plugins/illustrator/src/main.js`.
- Write Illustrator plugin README with install + usage steps
  - Summary: Documented installation, flattening, tagging, and export workflow in `plugins/illustrator/README.md`.

### 14.4 GIMP plugin (Python)
- Set up `plugins/gimp/` with Python-Fu entry point + menu registration
  - Add configuration dialog for group/lock editing and export settings.
  - Summary: Added the Python-Fu entry point, menu registration, and configuration parameters in `plugins/gimp/grin_gimp_plugin.py`.
- Implement group/lock editing overlays
  - Use layer groups or selection masks to represent group IDs.
  - Summary: Implemented group/lock metadata layers and selection painting overlays in `plugins/gimp/grin_gimp_plugin.py`.
- Implement pixel map extraction
  - Read pixel data + custom metadata for group and lock state.
  - Summary: Added grayscale pixel extraction and group/lock summaries in `plugins/gimp/grin_gimp_plugin.py`.
- Implement export pipeline
  - Export PNG + JSON sidecar and call `grin-encode.js`.
  - Summary: Implemented PNG exports, rules sidecar writing, and optional CLI invocation in `plugins/gimp/grin_gimp_plugin.py`.
- Add preview/validation step
  - Use `grin-validate` and show errors in a dialog.
  - Summary: Added preview metadata summaries and validation output dialogs in `plugins/gimp/grin_gimp_plugin.py`.
- Write GIMP plugin README with install + usage steps
  - Summary: Documented installation, metadata authoring, and export workflow in `plugins/gimp/README.md`.

---

## Phase 15: UI/UX Polish + Windows Desktop Integration

### 15.1 UX Audit + Requirements Backlog
- Inventory all end-user surfaces: `web/demo/`, `android/demo/`, CLI help (`tools/bin/`), and docs onboarding (`docs/README.md`)
  - Capture screenshots, flows, and current pain points.
  - Summary: Captured the current UX surface inventory, flows, and pain points in `docs/ui-ux-audit.md`.
- Create a UX heuristics checklist (navigation clarity, error states, keyboard/mouse affordances, accessibility)
  - Store the checklist in `docs/ui-ux-audit.md` and link from `docs/README.md`.
  - Summary: Added a UX heuristics checklist to `docs/ui-ux-audit.md` and linked it from `docs/README.md`.
- Define core user journeys (open file, play animation, inspect groups, export) with success criteria
  - Add journey maps to `docs/ui-ux-audit.md`.
  - Summary: Documented core journeys and success criteria in `docs/ui-ux-audit.md`.
- Prioritize the UX backlog with severity tags (P0/P1/P2) and platform labels (web/android/cli)
  - Maintain a prioritized list in `docs/ui-ux-audit.md`.
  - Summary: Added a prioritized, labeled UX backlog to `docs/ui-ux-audit.md`.

### 15.2 Web Demo UI Polish (Web)
- Refresh layout hierarchy in `web/demo/` (clear header, playback controls, inspector panel)
  - Summary: Split the web demo layout into a playback panel and a dedicated inspector panel with updated sidebar styling.
  - Ensure consistent spacing, typography, and control grouping.
- Add accessibility coverage (ARIA labels, focus order, contrast checks)
  - Summary: Added ARIA labels, keyboard focus support, and stronger focus/contrast styling in the web demo.
  - Document a11y checks in `docs/ui-ux-audit.md`.
- Improve empty, loading, and error states (missing file, validation errors, decode failures)
  - Summary: Added status messaging plus sample/file load error handling and control disabling in the web demo.
  - Provide actionable messaging with next steps.
- Add keyboard shortcuts for playback, frame stepping, and group filtering
  - Summary: Added document-level shortcuts for play/pause, frame stepping, and group filter selection in the web demo.
  - Surface shortcuts in a help tooltip or modal.
- Add a validation summary panel (header warnings, rule count, invalid pixels)
  - Summary: Added a validation summary panel with error/warning counts, invalid pixel tracking, and rule count in the web demo inspector.
  - Link to existing validation error outputs.

### 15.3 Android Demo UI Polish (Android)
- Align the demo UI with Material 3 components in `android/demo/`
  - Summary: Swapped demo layouts to Material 3 widgets (MaterialToolbar, MaterialButton, MaterialCardView, MaterialTextView, Slider) and updated slider logic in MainActivity and EditorActivity.
  - Rebuild the activity layout with proper app bars, cards, and controls.

### 15.4 Cross-Platform UX Consistency

### 15.5 Windows Desktop Viewer + File Associations

---

## Planned Upgrades (Unimplemented)


---

## Dependency Graph (Critical Path)

```
Phase 0 (Setup) ──┬──> Phase 1 (Binary Format) ──> Phase 2 (Data Structures)
                  │                                         │
                  │                                         v
                  │                                  Phase 3 (Binary I/O)
                  │                                         │
                  │                    ┌────────────────────┼────────────────────┐
                  │                    v                    v                    v
                  │             Phase 4 (Validation)  Phase 5 (Opcodes)   Phase 6 (Playback)
                  │                    │                    │                    │
                  │                    └────────────────────┴────────────────────┘
                  │                                         │
                  │              ┌──────────────────────────┼──────────────────────────┐
                  │              v                          v                          v
                  │       Phase 7 (Android)          Phase 8 (Web)            Phase 9 (CLI)
                  │              │                          │                          │
                  │              └──────────────────────────┴──────────────────────────┘
                  │                                         │
                  v                                         v
           Phase 10 (Testing) <────────────────────> Phase 11 (Samples)
                  │                                         │
                  └──────────────────────┬──────────────────┘
                                         v
                                  Phase 12 (Docs)
                                         │
                                         v
                                  Phase 13 (Release)
                                         │
                                         v
                            Phase 15 (UI/UX + Windows)
```

---

## Legend

- `***` at step beginning: Task can run in parallel or out of order with other tasks in same phase
- Tasks without `***`: Must be completed sequentially within phase
- Phases generally depend on previous phases unless otherwise noted
- Testing (Phase 10) should run continuously alongside development

---

## Estimated Complexity

| Phase | Estimated Hours | Parallelizable |
|-------|-----------------|----------------|
| 0     | 4-8            | Partially      |
| 1     | 8-12           | No             |
| 2     | 12-16          | Partially      |
| 3     | 16-24          | Partially      |
| 4     | 8-12           | No             |
| 5     | 16-24          | Partially      |
| 6     | 24-32          | No             |
| 7     | 24-32          | Partially      |
| 8     | 24-32          | Partially      |
| 9     | 16-24          | Yes            |
| 10    | 24-32          | Yes            |
| 11    | 8-12           | Yes            |
| 12    | 16-24          | Yes            |
| 13    | 8-12           | Partially      |
| 15    | 32-48          | Partially      |

**Total Estimated**: 232-348 hours for full implementation

---

## Notes for Agentic Agents

1. **Atomic Commits**: Each checkbox item should correspond to a single, reviewable commit
2. **Test-Driven**: Write tests before implementation where possible
3. **Documentation**: Update docs as you implement, not after
4. **Cross-Reference**: Reference spec section numbers in code comments
