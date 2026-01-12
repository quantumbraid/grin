/*
 * MIT License
 *
 * Copyright (c) 2025 GRIN Project Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import {
  CONTROL_BYTE_MASKS,
  HEADER_SIZE_BYTES,
  MAGIC_BYTES,
  MAX_RULE_COUNT,
  PIXEL_SIZE_BYTES,
  VERSION_MAJOR,
  VERSION_MINOR,
} from "./format.js";

export function validateMagic(bytes) {
  if (bytes.length !== MAGIC_BYTES.length) {
    return fail(`Magic must be ${MAGIC_BYTES.length} bytes`);
  }
  for (let i = 0; i < MAGIC_BYTES.length; i += 1) {
    if (bytes[i] !== MAGIC_BYTES[i]) {
      return fail("Magic bytes do not match GRIN");
    }
  }
  return ok();
}

export function validateVersion(major, minor) {
  if (!Number.isInteger(major) || !Number.isInteger(minor)) {
    return fail("Version fields must be integers");
  }
  if (major !== VERSION_MAJOR || minor !== VERSION_MINOR) {
    return warn("Unexpected GRIN version");
  }
  return ok();
}

export function validateHeaderSize(size) {
  if (size !== HEADER_SIZE_BYTES) {
    return fail(`HeaderSize must be ${HEADER_SIZE_BYTES}`);
  }
  return ok();
}

export function validateDimensions(width, height) {
  if (!Number.isInteger(width) || !Number.isInteger(height)) {
    return fail("Width and height must be integers");
  }
  if (width < 0 || height < 0) {
    return fail("Width and height must be non-negative");
  }
  if (width > 0xffffffff || height > 0xffffffff) {
    return fail("Width and height must fit in uint32");
  }
  return ok();
}

export function validateTickMicros(tick) {
  if (!Number.isInteger(tick) || tick < 0 || tick > 0xffffffff) {
    return fail("TickMicros must fit in uint32");
  }
  return ok();
}

export function validateRuleCount(count) {
  if (!Number.isInteger(count) || count < 0 || count > MAX_RULE_COUNT) {
    return fail(`RuleCount must be 0-${MAX_RULE_COUNT}`);
  }
  return ok();
}

export function validateOpcodeSetId(id) {
  if (!Number.isInteger(id) || id < 0 || id > 0xff) {
    return fail("OpcodeSetId must fit in uint8");
  }
  if (id !== 0) {
    return fail("Unknown OpcodeSetId");
  }
  return ok();
}

export function validatePixelDataLength(length, width, height) {
  if (length < 0n) {
    return fail("PixelDataLength must be non-negative");
  }
  const dimResult = validateDimensions(width, height);
  if (!dimResult.ok) {
    return dimResult;
  }
  const expected = BigInt(width) * BigInt(height) * BigInt(PIXEL_SIZE_BYTES);
  if (length !== expected) {
    return fail("PixelDataLength does not match width * height * 5");
  }
  return ok();
}

export function validateFileLength(fileLen, dataLen) {
  if (fileLen < 0n) {
    return fail("FileLength must be non-negative");
  }
  const minLen = BigInt(HEADER_SIZE_BYTES) + dataLen;
  if (fileLen !== 0n && fileLen < minLen) {
    return fail("FileLength is smaller than header + pixel data");
  }
  return ok();
}

export function validatePixelDataOffset(offset) {
  if (offset !== BigInt(HEADER_SIZE_BYTES)) {
    return fail("PixelDataOffset64 must be 128");
  }
  return ok();
}

export function validateReservedFields(reservedA, reservedB, flags) {
  if (flags !== 0 || reservedA !== 0n || reservedB !== 0n) {
    return warn("Reserved header fields are non-zero");
  }
  return ok();
}

export function validateControlByte(controlByte) {
  const errors = [];
  const warnings = [];

  if (!Number.isInteger(controlByte) || controlByte < 0 || controlByte > 0xff) {
    errors.push("Control byte must fit in uint8");
  } else {
    const groupResult = validateGroupId(controlByte & CONTROL_BYTE_MASKS.GROUP_ID);
    mergeResult(errors, warnings, groupResult);
    const reservedResult = validateReservedBits(controlByte);
    mergeResult(errors, warnings, reservedResult);
  }

  return finalize(errors, warnings);
}

export function validateGroupId(groupId) {
  if (!Number.isInteger(groupId) || groupId < 0 || groupId > 0x0f) {
    return fail("Group ID must be 0-15");
  }
  return ok();
}

export function validateReservedBits(controlByte) {
  if ((controlByte & CONTROL_BYTE_MASKS.RESERVED) !== 0) {
    return warn("Control byte reserved bits are non-zero");
  }
  return ok();
}

export function validateGroupMask(mask) {
  if (!Number.isInteger(mask) || mask < 0 || mask > 0xffff) {
    return fail("GroupMask must fit in uint16");
  }
  return ok();
}

export function validateOpcode(opcode, opcodeSetId) {
  if (!Number.isInteger(opcode) || opcode < 0 || opcode > 0xff) {
    return fail("Opcode must fit in uint8");
  }
  if (opcodeSetId !== 0) {
    return fail("Unknown OpcodeSetId");
  }
  if (opcode > 0x0c) {
    return fail("Unknown opcode for base set");
  }
  return ok();
}

export function validateTiming(timing) {
  if (!Number.isInteger(timing) || timing < 0 || timing > 0xff) {
    return fail("Timing must fit in uint8");
  }
  return ok();
}

export function validateGrinFile(file, mode = "permissive") {
  const errors = [];
  const warnings = [];
  const info = [];

  const header = file.header;
  mergeResult(errors, warnings, validateMagic(header.magic));
  mergeResult(errors, warnings, validateVersion(header.versionMajor, header.versionMinor));
  mergeResult(errors, warnings, validateHeaderSize(header.headerSize));
  mergeResult(errors, warnings, validateDimensions(header.width, header.height));
  mergeResult(errors, warnings, validateTickMicros(header.tickMicros));
  mergeResult(errors, warnings, validateRuleCount(header.ruleCount));
  mergeResult(errors, warnings, validateOpcodeSetId(header.opcodeSetId));
  mergeResult(
    errors,
    warnings,
    validatePixelDataLength(header.pixelDataLength, header.width, header.height)
  );
  mergeResult(errors, warnings, validateFileLength(header.fileLength, header.pixelDataLength));
  mergeResult(errors, warnings, validatePixelDataOffset(header.pixelDataOffset));
  mergeResult(errors, warnings, validateReservedFields(header.reservedA, header.reservedB, header.flags));

  const expectedPixelCount = header.width * header.height;
  if (!Number.isSafeInteger(expectedPixelCount) || expectedPixelCount < 0) {
    errors.push("Pixel count exceeds safe integer range");
  } else if (file.pixels.length !== expectedPixelCount) {
    errors.push("Pixel array length does not match header dimensions");
  }

  if (file.rules.length !== header.ruleCount) {
    errors.push("Rule array length does not match header ruleCount");
  }

  let reservedBitsCount = 0;
  for (const pixel of file.pixels) {
    if ((pixel.c & CONTROL_BYTE_MASKS.RESERVED) !== 0) {
      reservedBitsCount += 1;
    }
  }
  if (reservedBitsCount > 0) {
    warnings.push(`Control byte reserved bits set on ${reservedBitsCount} pixels`);
  }

  file.rules.forEach((rule, index) => {
    const groupMaskResult = validateGroupMask(rule.groupMask);
    const opcodeResult = validateOpcode(rule.opcode, header.opcodeSetId);
    const timingResult = validateTiming(rule.timing);
    prefixResult(errors, warnings, groupMaskResult, `Rule ${index}: `);
    prefixResult(errors, warnings, opcodeResult, `Rule ${index}: `);
    prefixResult(errors, warnings, timingResult, `Rule ${index}: `);
  });

  info.push(`Pixels: ${file.pixels.length}`);
  info.push(`Rules: ${file.rules.length}`);

  const okResult =
    mode === "strict" ? errors.length === 0 && warnings.length === 0 : errors.length === 0;
  return { ok: okResult, errors, warnings, info };
}

function ok() {
  return { ok: true, errors: [], warnings: [] };
}

function warn(message) {
  return { ok: true, errors: [], warnings: [message] };
}

function fail(message) {
  return { ok: false, errors: [message], warnings: [] };
}

function finalize(errors, warnings) {
  return { ok: errors.length === 0, errors, warnings };
}

function mergeResult(errors, warnings, result) {
  errors.push(...result.errors);
  warnings.push(...result.warnings);
}

function prefixResult(errors, warnings, result, prefix) {
  errors.push(...result.errors.map((error) => `${prefix}${error}`));
  warnings.push(...result.warnings.map((warning) => `${prefix}${warning}`));
}
